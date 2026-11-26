package com.pulsemusic.player.data

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pulsemusic.player.security.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class FileOpResult {
    data class Success(val message: String) : FileOpResult()
    data class NeedsUserConsent(
        val intentSender: IntentSender,
        val retryAction: suspend () -> FileOpResult
    ) : FileOpResult()
    data class Error(val message: String) : FileOpResult()
}

class AudioFileManager(private val context: Context) {

    suspend fun editMetadata(
        track: AudioTrack,
        newTitle: String,
        newArtist: String,
        newAlbum: String
    ): FileOpResult = withContext(Dispatchers.IO) {
        val title = SecurityGuard.sanitizeMetadataText(newTitle).ifBlank { track.title }
        val artist = SecurityGuard.sanitizeMetadataText(newArtist).ifBlank { track.artist }
        val album = SecurityGuard.sanitizeMetadataText(newAlbum).ifBlank { track.album }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.ALBUM, album)
        }

        try {
            val rows = context.contentResolver.update(track.contentUri, values, null, null)
            if (rows > 0) {
                FileOpResult.Success("Song info updated")
            } else {
                FileOpResult.Error("Unable to update song info")
            }
        } catch (sec: SecurityException) {
            handleWriteSecurityException(track.contentUri, sec) {
                editMetadata(track, newTitle, newArtist, newAlbum)
            }
        } catch (e: Exception) {
            FileOpResult.Error(e.message ?: "Edit failed")
        }
    }

    suspend fun renameTrack(track: AudioTrack, newBaseName: String): FileOpResult =
        withContext(Dispatchers.IO) {
            val oldName = File(track.filePath).name.ifBlank { "${track.title}.mp3" }
            val ext = oldName.substringAfterLast('.', "mp3").ifBlank { "mp3" }
            val cleanBase = newBaseName.substringBeforeLast('.').ifBlank { newBaseName }
            val finalDisplayName = SecurityGuard.sanitizeFileName("$cleanBase.$ext", ext)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, finalDisplayName)
                    put(MediaStore.Audio.Media.TITLE, SecurityGuard.sanitizeMetadataText(cleanBase))
                }
                try {
                    val rows = context.contentResolver.update(track.contentUri, values, null, null)
                    if (rows > 0) {
                        FileOpResult.Success("Renamed to $finalDisplayName")
                    } else {
                        FileOpResult.Error("Rename failed")
                    }
                } catch (sec: SecurityException) {
                    handleWriteSecurityException(track.contentUri, sec) {
                        renameTrack(track, newBaseName)
                    }
                } catch (e: Exception) {
                    FileOpResult.Error(e.message ?: "Rename failed")
                }
            } else {
                legacyRename(track, finalDisplayName, cleanBase)
            }
        }

    suspend fun copyTrackToFolder(track: AudioTrack, destFolder: AudioFolder): FileOpResult =
        withContext(Dispatchers.IO) {
            val sourceFileName = File(track.filePath).name.ifBlank {
                SecurityGuard.sanitizeFileName(track.title, "mp3")
            }
            val ext = sourceFileName.substringAfterLast('.', "mp3")
            val safeName = SecurityGuard.sanitizeFileName(sourceFileName, ext)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relPath = deriveRelativePathFromFolder(destFolder)
                val uniqueName = makeUniqueFileName(relPath, safeName)
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, uniqueName)
                    put(MediaStore.Audio.Media.TITLE, track.title)
                    put(MediaStore.Audio.Media.ARTIST, track.artist)
                    put(MediaStore.Audio.Media.ALBUM, track.album)
                    put(MediaStore.Audio.Media.MIME_TYPE, guessMimeType(ext))
                    put(MediaStore.Audio.Media.RELATIVE_PATH, relPath)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val destUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext FileOpResult.Error("Cannot create file in ${destFolder.folderName}")

                try {
                    resolver.openInputStream(track.contentUri)?.use { input ->
                        resolver.openOutputStream(destUri)?.use { output ->
                            input.copyTo(output, bufferSize = 65536)
                        } ?: error("Cannot open destination stream")
                    } ?: error("Cannot read source audio")

                    val doneValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    resolver.update(destUri, doneValues, null, null)
                    FileOpResult.Success("Copied to ${destFolder.folderName}")
                } catch (e: Exception) {
                    runCatching { resolver.delete(destUri, null, null) }
                    FileOpResult.Error(e.message ?: "Copy failed")
                }
            } else {
                legacyCopy(track, destFolder, safeName)
            }
        }

    suspend fun moveTrackToFolder(track: AudioTrack, destFolder: AudioFolder): FileOpResult =
        withContext(Dispatchers.IO) {
            if (track.folderPath == destFolder.folderPath) {
                return@withContext FileOpResult.Error("Song is already in ${destFolder.folderName}")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relPath = deriveRelativePathFromFolder(destFolder)
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, relPath)
                }
                try {
                    val rows = context.contentResolver.update(track.contentUri, values, null, null)
                    if (rows > 0) {
                        FileOpResult.Success("Moved to ${destFolder.folderName}")
                    } else {
                        // Fallback: copy then delete
                        copyThenDelete(track, destFolder)
                    }
                } catch (sec: SecurityException) {
                    handleWriteSecurityException(track.contentUri, sec) {
                        moveTrackToFolder(track, destFolder)
                    }
                } catch (_: Exception) {
                    copyThenDelete(track, destFolder)
                }
            } else {
                copyThenDelete(track, destFolder)
            }
        }

    private suspend fun copyThenDelete(track: AudioTrack, destFolder: AudioFolder): FileOpResult {
        return when (val copied = copyTrackToFolder(track, destFolder)) {
            is FileOpResult.Success -> {
                when (val deleted = deleteTrack(track)) {
                    is FileOpResult.Success -> FileOpResult.Success("Moved to ${destFolder.folderName}")
                    is FileOpResult.NeedsUserConsent -> deleted
                    is FileOpResult.Error -> FileOpResult.Success("Copied to ${destFolder.folderName} (source kept)")
                }
            }
            else -> copied
        }
    }

    suspend fun deleteTrack(track: AudioTrack): FileOpResult = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(track.contentUri, null, null)
            if (rows > 0) {
                FileOpResult.Success("Deleted ${track.title}")
            } else {
                FileOpResult.Error("Delete failed")
            }
        } catch (sec: SecurityException) {
            handleDeleteSecurityException(track.contentUri, sec) {
                deleteTrack(track)
            }
        } catch (e: Exception) {
            FileOpResult.Error(e.message ?: "Delete failed")
        }
    }

    private fun handleWriteSecurityException(
        uri: Uri,
        sec: SecurityException,
        retryAction: suspend () -> FileOpResult
    ): FileOpResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
            return FileOpResult.NeedsUserConsent(pi.intentSender, retryAction)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
            return FileOpResult.NeedsUserConsent(sec.userAction.actionIntent.intentSender, retryAction)
        }
        return FileOpResult.Error("Permission denied by Android storage guard")
    }

    private fun handleDeleteSecurityException(
        uri: Uri,
        sec: SecurityException,
        retryAction: suspend () -> FileOpResult
    ): FileOpResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            return FileOpResult.NeedsUserConsent(pi.intentSender, retryAction)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
            return FileOpResult.NeedsUserConsent(sec.userAction.actionIntent.intentSender, retryAction)
        }
        return FileOpResult.Error("Delete permission denied")
    }

    private fun deriveRelativePathFromFolder(folder: AudioFolder): String {
        val root = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
        val raw = if (folder.folderPath.startsWith(root)) {
            folder.folderPath.removePrefix(root).trimStart('/')
        } else {
            "Music/${folder.folderName}"
        }
        return SecurityGuard.sanitizeRelativeAudioPath(raw)
    }

    private fun makeUniqueFileName(relPath: String, baseFileName: String): String {
        val root = Environment.getExternalStorageDirectory()
        val dir = File(root, relPath)
        var candidate = File(dir, baseFileName)
        if (!candidate.exists()) return baseFileName
        val name = baseFileName.substringBeforeLast('.')
        val ext = baseFileName.substringAfterLast('.', "mp3")
        var counter = 1
        while (candidate.exists() && counter < 500) {
            val nextName = "${name}_$counter.$ext"
            candidate = File(dir, nextName)
            if (!candidate.exists()) return nextName
            counter++
        }
        return "${name}_${System.currentTimeMillis()}.$ext"
    }

    private fun guessMimeType(ext: String): String = when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "ogg", "opus" -> "audio/ogg"
        else -> "audio/mpeg"
    }

    private fun legacyRename(track: AudioTrack, finalName: String, title: String): FileOpResult {
        val src = File(track.filePath)
        if (!src.exists()) return FileOpResult.Error("File not found")
        val dst = File(src.parentFile, finalName)
        if (dst.exists()) return FileOpResult.Error("A file with that name already exists")
        return if (src.renameTo(dst)) {
            MediaScannerConnection.scanFile(context, arrayOf(src.absolutePath, dst.absolutePath), null, null)
            val cv = ContentValues().apply { put(MediaStore.Audio.Media.TITLE, title) }
            runCatching { context.contentResolver.update(track.contentUri, cv, null, null) }
            FileOpResult.Success("Renamed to $finalName")
        } else {
            FileOpResult.Error("Rename failed")
        }
    }

    private fun legacyCopy(track: AudioTrack, destFolder: AudioFolder, safeName: String): FileOpResult {
        val src = File(track.filePath)
        val dstDir = File(destFolder.folderPath)
        if (!src.exists() || !dstDir.exists()) return FileOpResult.Error("Folder not accessible")
        val dst = File(dstDir, safeName)
        return try {
            src.copyTo(dst, overwrite = false)
            MediaScannerConnection.scanFile(context, arrayOf(dst.absolutePath), null, null)
            FileOpResult.Success("Copied to ${destFolder.folderName}")
        } catch (e: Exception) {
            FileOpResult.Error(e.message ?: "Copy failed")
        }
    }
}
