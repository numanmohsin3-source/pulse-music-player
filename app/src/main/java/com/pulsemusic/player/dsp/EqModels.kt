package com.pulsemusic.player.dsp

enum class EqPreset(val displayName: String) {
    TIKTOK_PUNCH("TikTok Punch"),
    TIKTOK_ULTRA("TikTok Ultra Bass"),
    BASS_EXTREME("Bass Extreme"),
    STUDIO_VOCAL("Studio Vocal"),
    CLUB_EDM("Club / EDM"),
    WARM_CHILL("Warm Chill"),
    FLAT("Flat"),
    CUSTOM("Custom");

    fun defaultSettings(): EqSettings = when (this) {
        TIKTOK_PUNCH -> EqSettings(
            enabled = true,
            preset = TIKTOK_PUNCH,
            // Punchy TikTok loudness curve: deep 62Hz punch, clean 250Hz, crisp 2-8kHz vocals/air
            bandsDb = listOf(5.5f, 6.5f, 4.5f, -2.0f, 0.5f, 2.0f, 3.5f, 5.0f, 5.5f, 4.5f),
            bassBoostStrength = 650,
            virtualizerStrength = 400,
            loudnessGainMb = 650,
            compressorEnabled = true,
            limiterEnabled = true
        )
        TIKTOK_ULTRA -> EqSettings(
            enabled = true,
            preset = TIKTOK_ULTRA,
            bandsDb = listOf(8.0f, 8.5f, 6.0f, -1.5f, 0.0f, 2.0f, 4.0f, 5.5f, 6.0f, 5.0f),
            bassBoostStrength = 850,
            virtualizerStrength = 500,
            loudnessGainMb = 750,
            compressorEnabled = true,
            limiterEnabled = true
        )
        BASS_EXTREME -> EqSettings(
            enabled = true,
            preset = BASS_EXTREME,
            bandsDb = listOf(9.0f, 8.0f, 5.5f, 1.5f, -1.0f, 0.0f, 1.5f, 2.5f, 3.0f, 2.5f),
            bassBoostStrength = 900,
            virtualizerStrength = 350,
            loudnessGainMb = 500,
            compressorEnabled = true,
            limiterEnabled = true
        )
        STUDIO_VOCAL -> EqSettings(
            enabled = true,
            preset = STUDIO_VOCAL,
            bandsDb = listOf(-2.0f, -1.0f, 0.5f, 1.5f, 3.0f, 4.5f, 5.0f, 4.0f, 2.5f, 1.5f),
            bassBoostStrength = 150,
            virtualizerStrength = 250,
            loudnessGainMb = 400,
            compressorEnabled = true,
            limiterEnabled = true
        )
        CLUB_EDM -> EqSettings(
            enabled = true,
            preset = CLUB_EDM,
            bandsDb = listOf(6.5f, 6.0f, 4.0f, 0.0f, -1.5f, 1.0f, 3.0f, 4.5f, 5.5f, 5.0f),
            bassBoostStrength = 750,
            virtualizerStrength = 600,
            loudnessGainMb = 600,
            compressorEnabled = true,
            limiterEnabled = true
        )
        WARM_CHILL -> EqSettings(
            enabled = true,
            preset = WARM_CHILL,
            bandsDb = listOf(3.0f, 3.5f, 2.0f, 1.0f, 0.5f, 0.0f, -0.5f, 0.5f, 1.5f, 1.0f),
            bassBoostStrength = 300,
            virtualizerStrength = 200,
            loudnessGainMb = 250,
            compressorEnabled = false,
            limiterEnabled = true
        )
        FLAT -> EqSettings(
            enabled = true,
            preset = FLAT,
            bandsDb = List(10) { 0f },
            bassBoostStrength = 0,
            virtualizerStrength = 0,
            loudnessGainMb = 0,
            compressorEnabled = false,
            limiterEnabled = false
        )
        CUSTOM -> TIKTOK_PUNCH.defaultSettings().copy(preset = CUSTOM)
    }
}

data class EqSettings(
    val enabled: Boolean = true,
    val preset: EqPreset = EqPreset.TIKTOK_PUNCH,
    val bandsDb: List<Float> = EqPreset.TIKTOK_PUNCH.defaultSettings().bandsDb,
    val bassBoostStrength: Int = 650,
    val virtualizerStrength: Int = 400,
    val loudnessGainMb: Int = 650,
    val compressorEnabled: Boolean = true,
    val limiterEnabled: Boolean = true
) {
    companion object {
        val BAND_LABELS = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
        val BAND_FREQUENCIES_HZ = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    }
}
