package com.pulsemusic.player.data

import android.net.Uri
import java.util.Locale

data class AudioTrack(
    val id: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val dateAddedSec: Long
) {
    fun formattedDuration(): String = formatMillis(durationMs)

    companion object {
        fun formatMillis(ms: Long): String {
            val totalSec = (ms / 1000L).coerceAtLeast(0L)
            val hrs = totalSec / 3600L
            val mins = (totalSec % 3600L) / 60L
            val secs = totalSec % 60L
            return if (hrs > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format(Locale.US, "%02d:%02d", mins, secs)
            }
        }
    }
}

data class AudioFolder(
    val folderPath: String,
    val folderName: String,
    val displaySubPath: String,
    val tracks: List<AudioTrack>,
    val totalDurationMs: Long
)
