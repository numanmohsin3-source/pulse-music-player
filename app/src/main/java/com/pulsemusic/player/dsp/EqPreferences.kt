package com.pulsemusic.player.dsp

import android.content.Context

class EqPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("pulse_eq_prefs", Context.MODE_PRIVATE)

    fun load(): EqSettings {
        val migrated = prefs.getBoolean("v2_tiktok_upgraded", false)
        val presetName = prefs.getString("preset", EqPreset.TIKTOK_PUNCH.name) ?: EqPreset.TIKTOK_PUNCH.name
        val preset = runCatching { EqPreset.valueOf(presetName) }.getOrDefault(EqPreset.TIKTOK_PUNCH)
        val defaults = preset.defaultSettings()

        if (!migrated && preset == EqPreset.TIKTOK_PUNCH) {
            save(defaults)
            prefs.edit().putBoolean("v2_tiktok_upgraded", true).apply()
            return defaults
        }

        val bandsStr = prefs.getString("bands", null)
        val bands = if (!bandsStr.isNullOrBlank()) {
            bandsStr.split(",").mapNotNull { it.toFloatOrNull() }.let { parsed ->
                if (parsed.size == 10) parsed else defaults.bandsDb
            }
        } else {
            defaults.bandsDb
        }

        return EqSettings(
            enabled = prefs.getBoolean("enabled", true),
            preset = preset,
            bandsDb = bands,
            bassBoostStrength = prefs.getInt("bass", defaults.bassBoostStrength),
            virtualizerStrength = prefs.getInt("virtualizer", defaults.virtualizerStrength),
            loudnessGainMb = prefs.getInt("loudness", defaults.loudnessGainMb),
            compressorEnabled = prefs.getBoolean("compressor", defaults.compressorEnabled),
            limiterEnabled = prefs.getBoolean("limiter", defaults.limiterEnabled)
        )
    }

    fun save(settings: EqSettings) {
        prefs.edit()
            .putBoolean("enabled", settings.enabled)
            .putString("preset", settings.preset.name)
            .putString("bands", settings.bandsDb.joinToString(","))
            .putInt("bass", settings.bassBoostStrength)
            .putInt("virtualizer", settings.virtualizerStrength)
            .putInt("loudness", settings.loudnessGainMb)
            .putBoolean("compressor", settings.compressorEnabled)
            .putBoolean("limiter", settings.limiterEnabled)
            .apply()
    }
}
