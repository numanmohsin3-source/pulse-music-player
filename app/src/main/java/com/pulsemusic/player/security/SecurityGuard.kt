package com.pulsemusic.player.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File

data class AppSecurityStatus(
    val hasInternetPermission: Boolean,
    val isBackupDisabled: Boolean,
    val isDebuggerAttached: Boolean,
    val isRootSuspected: Boolean
)

object SecurityGuard {

    private val FORBIDDEN_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun sanitizeFileName(raw: String, fallbackExt: String = "mp3"): String {
        val cleaned = raw
            .replace("..", "")
            .replace(FORBIDDEN_FILENAME_CHARS, "_")
            .trim()
            .trim('.')
            .take(80)
        val base = cleaned.ifBlank { "audio_${System.currentTimeMillis()}" }
        return if (base.contains('.')) base else "$base.$fallbackExt"
    }

    fun sanitizeMetadataText(raw: String, maxLen: Int = 120): String {
        return raw
            .replace(Regex("\\p{Cntrl}"), " ")
            .trim()
            .take(maxLen)
    }

    fun sanitizeRelativeAudioPath(rawRelativePath: String): String {
        val parts = rawRelativePath
            .replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "." && it != ".." }
        if (parts.isEmpty()) return "Music/"
        val allowedRoots = setOf("Music", "Download", "Downloads", "Podcasts", "Audiobooks", "Ringtones", "Recordings")
        val normalized = if (parts.first() in allowedRoots) parts else listOf("Music") + parts
        return normalized.joinToString("/") + "/"
    }

    fun inspect(context: Context): AppSecurityStatus {
        val pkgInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }
        }.getOrNull()

        val hasInternet = pkgInfo?.requestedPermissions?.contains(android.Manifest.permission.INTERNET) == true

        return AppSecurityStatus(
            hasInternetPermission = hasInternet,
            isBackupDisabled = true,
            isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger(),
            isRootSuspected = checkRootIndicators()
        )
    }

    private fun checkRootIndicators(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        if (paths.any { File(it).exists() }) return true
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }
}
