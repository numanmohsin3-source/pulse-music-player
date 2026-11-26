package com.pulsemusic.player.dsp

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.abs

/**
 * High-level Audio DSP engine delivering punchy, loud, clear TikTok-style mastered audio:
 * - On Android 9+ (API 28+): uses DynamicsProcessing with 10-band EQ, 3-band Multi-Band
 *   Compressor (MBC), zero-clip Master Limiter, plus Virtualizer.
 * - On Android 8.x (or fallback HALs): uses Equalizer + BassBoost + Virtualizer + LoudnessEnhancer.
 */
object AudioDspEngine {

    private var currentSessionId: Int = 0
    private var currentSettings: EqSettings = EqSettings()

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var legacyEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isUsingProDynamicsEngine: Boolean = false
        private set

    @Synchronized
    fun attachToSession(audioSessionId: Int, settings: EqSettings) {
        currentSettings = settings
        if (audioSessionId <= 0) return

        if (currentSessionId != audioSessionId) {
            release()
            currentSessionId = audioSessionId
            initEffects(audioSessionId)
        }
        applySettings(settings)
    }

    @Synchronized
    fun applySettings(settings: EqSettings) {
        currentSettings = settings
        if (currentSessionId <= 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isUsingProDynamicsEngine) {
            applyProDynamics(settings)
        } else {
            applyLegacyEffects(settings)
        }

        // Virtualizer for 3D stereo width
        try {
            virtualizer?.let { virt ->
                virt.setEnabled(settings.enabled && settings.virtualizerStrength > 0)
                if (virt.strengthSupported) {
                    virt.setStrength(settings.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun initEffects(sessionId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cfg = buildDynamicsConfig(currentSettings)
                val dp = DynamicsProcessing(0, sessionId, cfg)
                dynamicsProcessing = dp
                isUsingProDynamicsEngine = true
            } catch (_: Throwable) {
                dynamicsProcessing = null
                isUsingProDynamicsEngine = false
            }
        } else {
            isUsingProDynamicsEngine = false
        }

        if (!isUsingProDynamicsEngine) {
            try {
                legacyEqualizer = Equalizer(0, sessionId)
            } catch (_: Throwable) {
            }
            try {
                bassBoost = BassBoost(0, sessionId)
            } catch (_: Throwable) {
            }
            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId)
            } catch (_: Throwable) {
            }
        }

        try {
            virtualizer = Virtualizer(0, sessionId)
        } catch (_: Throwable) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildDynamicsConfig(settings: EqSettings): DynamicsProcessing.Config {
        val builder = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2,
            false, 0,
            true, 3,
            true, 10,
            true
        )

        val eq = build10BandEq(settings)
        val mbc = build3BandCompressor(settings)
        val limiter = buildMasterLimiter(settings)

        builder.setMbcAllChannelsTo(mbc)
        builder.setPostEqAllChannelsTo(eq)
        builder.setLimiterAllChannelsTo(limiter)
        builder.setInputGainAllChannelsTo(computeInputGainDb(settings))

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyProDynamics(settings: EqSettings) {
        val dp = dynamicsProcessing ?: return
        try {
            dp.setEnabled(settings.enabled)
            if (!settings.enabled) return

            dp.setInputGainAllChannelsTo(computeInputGainDb(settings))
            dp.setPostEqAllChannelsTo(build10BandEq(settings))
            dp.setMbcAllChannelsTo(build3BandCompressor(settings))
            dp.setLimiterAllChannelsTo(buildMasterLimiter(settings))
        } catch (_: Throwable) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun build10BandEq(settings: EqSettings): DynamicsProcessing.Eq {
        val eq = DynamicsProcessing.Eq(true, settings.enabled, 10)
        val freqs = EqSettings.BAND_FREQUENCIES_HZ
        // Fold bassBoostStrength smoothly into sub-bass & punch bands (31, 62, 125 Hz)
        val extraSubDb = (settings.bassBoostStrength / 1000f) * 4.0f
        for (i in 0 until 10) {
            val band = eq.getBand(i)
            band.isEnabled = true
            band.cutoffFrequency = freqs[i]
            val bassWeight = when (i) {
                0 -> 1.0f
                1 -> 0.85f
                2 -> 0.45f
                else -> 0f
            }
            val gain = (settings.bandsDb.getOrElse(i) { 0f } + extraSubDb * bassWeight)
                .coerceIn(-15f, 15f)
            band.gain = gain
        }
        return eq
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun build3BandCompressor(settings: EqSettings): DynamicsProcessing.Mbc {
        val active = settings.enabled && settings.compressorEnabled
        val mbc = DynamicsProcessing.Mbc(true, active, 3)
        val cutoffs = floatArrayOf(180f, 2200f, 18000f)
        val ratios = floatArrayOf(2.6f, 2.1f, 2.3f)
        val thresholds = floatArrayOf(-20f, -18f, -16f)
        val postGains = floatArrayOf(2.2f, 1.8f, 2.0f)

        for (i in 0 until 3) {
            val b = mbc.getBand(i)
            b.isEnabled = active
            b.cutoffFrequency = cutoffs[i]
            b.attackTime = 5f
            b.releaseTime = 65f
            b.ratio = ratios[i]
            b.threshold = thresholds[i]
            b.kneeWidth = 6f
            b.noiseGateThreshold = -75f
            b.expanderRatio = 1f
            b.preGain = 0f
            b.postGain = if (active) postGains[i] else 0f
        }
        return mbc
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildMasterLimiter(settings: EqSettings): DynamicsProcessing.Limiter {
        val active = settings.enabled && settings.limiterEnabled
        val boostDb = (settings.loudnessGainMb / 100f).coerceIn(0f, 10f)
        return DynamicsProcessing.Limiter(
            true,
            active,
            0,
            2f,
            45f,
            10f,
            -0.8f,
            if (active) boostDb * 0.55f else 0f
        )
    }

    private fun computeInputGainDb(settings: EqSettings): Float {
        if (!settings.enabled) return 0f
        return ((settings.loudnessGainMb / 100f) * 0.45f).coerceIn(0f, 5.0f)
    }

    private fun applyLegacyEffects(settings: EqSettings) {
        try {
            legacyEqualizer?.let { eq ->
                eq.setEnabled(settings.enabled)
                if (settings.enabled) {
                    val range = eq.bandLevelRange
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()
                    val numBands = eq.numberOfBands.toInt()
                    for (b in 0 until numBands) {
                        val centerHz = eq.getCenterFreq(b.toShort()) / 1000f
                        val nearestDb = findNearest10BandGainDb(centerHz, settings.bandsDb)
                        val levelMb = (nearestDb * 100f).toInt().coerceIn(minMb, maxMb).toShort()
                        eq.setBandLevel(b.toShort(), levelMb)
                    }
                }
            }
        } catch (_: Throwable) {
        }

        try {
            bassBoost?.let { bb ->
                bb.setEnabled(settings.enabled && settings.bassBoostStrength > 0)
                if (bb.strengthSupported) {
                    bb.setStrength(settings.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Throwable) {
        }

        try {
            loudnessEnhancer?.let { le ->
                le.setEnabled(settings.enabled && settings.loudnessGainMb > 0)
                le.setTargetGain(settings.loudnessGainMb.coerceIn(0, 1000))
            }
        } catch (_: Throwable) {
        }
    }

    private fun findNearest10BandGainDb(targetHz: Float, bandsDb: List<Float>): Float {
        var bestIdx = 0
        var bestDiff = Float.MAX_VALUE
        val freqs = EqSettings.BAND_FREQUENCIES_HZ
        for (i in freqs.indices) {
            val diff = abs(freqs[i] - targetHz)
            if (diff < bestDiff) {
                bestDiff = diff
                bestIdx = i
            }
        }
        return bandsDb.getOrElse(bestIdx) { 0f }
    }

    @Synchronized
    fun release() {
        runCatching { dynamicsProcessing?.release() }
        runCatching { legacyEqualizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        dynamicsProcessing = null
        legacyEqualizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentSessionId = 0
        isUsingProDynamicsEngine = false
    }
}
