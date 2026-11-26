package com.pulsemusic.player.dsp

enum class EqPreset(val displayName: String) {
    TIKTOK_PUNCH("TikTok Punch"),
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
            bandsDb = listOf(4.5f, 5.5f, 4.0f, -1.5f, 0.0f, 1.5f, 3.0f, 4.0f, 4.5f, 3.5f),
            bassBoostStrength = 550,
            virtualizerStrength = 350,
            loudnessGainMb = 450,
            compressorEnabled = true,
            limiterEnabled = true
        )
        BASS_EXTREME -> EqSettings(
            enabled = true,
            preset = BASS_EXTREME,
            bandsDb = listOf(8.0f, 7.5f, 5.5f, 1.5f, -1.0f, 0.0f, 1.5f, 2.5f, 3.0f, 2.5f),
            bassBoostStrength = 850,
            virtualizerStrength = 300,
            loudnessGainMb = 400,
            compressorEnabled = true,
            limiterEnabled = true
        )
        STUDIO_VOCAL -> EqSettings(
            enabled = true,
            preset = STUDIO_VOCAL,
            bandsDb = listOf(-2.0f, -1.0f, 0.5f, 1.5f, 3.0f, 4.5f, 5.0f, 4.0f, 2.5f, 1.5f),
            bassBoostStrength = 150,
            virtualizerStrength = 250,
            loudnessGainMb = 350,
            compressorEnabled = true,
            limiterEnabled = true
        )
        CLUB_EDM -> EqSettings(
            enabled = true,
            preset = CLUB_EDM,
            bandsDb = listOf(6.0f, 5.5f, 3.5f, 0.0f, -1.5f, 1.0f, 3.0f, 4.5f, 5.5f, 5.0f),
            bassBoostStrength = 700,
            virtualizerStrength = 550,
            loudnessGainMb = 500,
            compressorEnabled = true,
            limiterEnabled = true
        )
        WARM_CHILL -> EqSettings(
            enabled = true,
            preset = WARM_CHILL,
            bandsDb = listOf(3.0f, 3.5f, 2.0f, 1.0f, 0.5f, 0.0f, -0.5f, 0.5f, 1.5f, 1.0f),
            bassBoostStrength = 300,
            virtualizerStrength = 200,
            loudnessGainMb = 200,
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
    val bassBoostStrength: Int = 550,
    val virtualizerStrength: Int = 350,
    val loudnessGainMb: Int = 450,
    val compressorEnabled: Boolean = true,
    val limiterEnabled: Boolean = true
) {
    companion object {
        val BAND_LABELS = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
        val BAND_FREQUENCIES_HZ = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    }
}
