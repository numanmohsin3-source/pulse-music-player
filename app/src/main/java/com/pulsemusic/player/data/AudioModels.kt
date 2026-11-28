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

    fun daysAgo(): Long {
        if (dateAddedSec <= 0L) return 0L
        val nowSec = System.currentTimeMillis() / 1000L
        return ((nowSec - dateAddedSec) / (24 * 3600L)).coerceAtLeast(0L)
    }

    fun relativeTimeAgo(): String {
        if (dateAddedSec <= 0L) return "Recently"
        val d = daysAgo()
        return when {
            d == 0L -> "Today"
            d == 1L -> "1 day ago"
            d < 30L -> "$d days ago"
            d < 60L -> "${d} days ago"
            d < 365L -> "${d / 30} months ago"
            else -> "${d / 365} year ago"
        }
    }

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
