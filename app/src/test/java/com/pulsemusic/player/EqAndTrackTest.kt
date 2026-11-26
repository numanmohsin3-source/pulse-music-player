package com.pulsemusic.player

import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.dsp.EqPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqAndTrackTest {

    @Test
    fun tiktokPresetHas10BandsAndMasteringEnabled() {
        val cfg = EqPreset.TIKTOK_PUNCH.defaultSettings()
        assertEquals(10, cfg.bandsDb.size)
        assertTrue(cfg.enabled)
        assertTrue(cfg.compressorEnabled)
        assertTrue(cfg.limiterEnabled)
        assertTrue(cfg.loudnessGainMb > 0)
    }

    @Test
    fun trackDurationFormatsCorrectly() {
        assertEquals("03:45", AudioTrack.formatMillis(225_000L))
        assertEquals("1:01:05", AudioTrack.formatMillis(3_665_000L))
    }
}
