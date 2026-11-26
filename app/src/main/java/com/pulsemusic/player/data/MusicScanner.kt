package com.pulsemusic.player.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class MusicScanner(private val context: Context) {

    private val artCache = LruCache<Long, Bitmap>(64)

    suspend fun scanAllAudio(includeShortClips: Boolean = false): Pair<List<AudioTrack>, List<AudioFolder>> =
        withContext(Dispatchers.IO) {
            val tracks = mutableListOf<AudioTrack>()
            val minDuration = if (includeShortClips) 1000L else 5000L

            val projection = buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATA)
                add(MediaStore.Audio.Media.DATE_ADDED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Audio.Media.RELATIVE_PATH)
                    add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                }
            }.toTypedArray()

            val selection = "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Media.SIZE} > 0"
            val selectionArgs = arrayOf(minDuration.toString())
            val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                } else -1

                val albumArtBase = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Track"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Unknown Artist" else rawArtist
                    val rawAlbum = cursor.getString(albumCol)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") "Unknown Album" else rawAlbum
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val relPath = if (relCol >= 0) cursor.getString(relCol) ?: "" else ""
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "" else ""

                    val parentFile = if (dataPath.isNotBlank()) File(dataPath).parentFile else null
                    val folderPath = parentFile?.absolutePath
                        ?: relPath.trimEnd('/').ifBlank { "Internal Storage" }
                    val folderName = parentFile?.name?.takeIf { it.isNotBlank() }
                        ?: bucket.takeIf { it.isNotBlank() }
                        ?: folderPath.substringAfterLast('/', "Music")

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val artUri = ContentUris.withAppendedId(albumArtBase, albumId)

                    tracks.add(
                        AudioTrack(
                            id = id,
                            contentUri = contentUri,
                            albumArtUri = artUri,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            sizeBytes = size,
                            filePath = dataPath,
                            folderPath = folderPath,
                            folderName = folderName,
                            dateAddedSec = dateAdded
                        )
                    )
                }
            }

            val folders = tracks
                .groupBy { it.folderPath }
                .map { (path, folderTracks) ->
                    val name = folderTracks.firstOrNull()?.folderName ?: "Music"
                    AudioFolder(
                        folderPath = path,
                        folderName = name,
                        displaySubPath = simplifyStoragePath(path),
                        tracks = folderTracks.sortedBy { it.title.lowercase() },
                        totalDurationMs = folderTracks.sumOf { it.durationMs }
                    )
                }
                .sortedWith(compareByDescending<AudioFolder> { it.tracks.size }.thenBy { it.folderName.lowercase() })

            tracks to folders
        }

    suspend fun loadAlbumArt(track: AudioTrack): Bitmap? = withContext(Dispatchers.IO) {
        artCache.get(track.id)?.let { return@withContext it }

        val bmp: Bitmap? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(track.contentUri, Size(320, 320), null)
            } else {
                context.contentResolver.openInputStream(track.albumArtUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        } ?: extractEmbeddedArtwork(track.contentUri)

        if (bmp != null) {
            artCache.put(track.id, bmp)
        }
        bmp
    }

    private fun extractEmbeddedArtwork(uri: Uri): Bitmap? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val pic = mmr.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(pic, 0, pic.size)
        } catch (_: Exception) {
            null
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {
            }
        }
    }

    fun triggerStorageRescan(onlyRecentHours: Int? = null, onDone: () -> Unit) {
        val dirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStorageDirectory()
        )
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "amr")
        val cutoffMs = onlyRecentHours?.let { System.currentTimeMillis() - it * 3_600_000L }
        val filePaths = mutableListOf<String>()
        for (dir in dirs) {
            runCatching {
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().maxDepth(3).forEach { f ->
                        if (f.isFile && f.extension.lowercase() in audioExtensions) {
                            if (cutoffMs == null || f.lastModified() >= cutoffMs) {
                                filePaths.add(f.absolutePath)
                            }
                        }
                    }
                }
            }
        }

        val targets = filePaths.distinct().toTypedArray()
        if (targets.isEmpty()) {
            onDone()
            return
        }

        val remaining = AtomicInteger(targets.size)
        MediaScannerConnection.scanFile(context, targets, null) { _, _ ->
            if (remaining.decrementAndGet() <= 0) {
                onDone()
            }
        }
    }

    private fun simplifyStoragePath(path: String): String {
        return path
            .replace("/storage/emulated/0", "Internal Storage")
            .replace(Regex("^/storage/[A-Za-z0-9-]+"), "SD Card")
    }
}
