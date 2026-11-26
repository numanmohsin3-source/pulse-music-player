package com.pulsemusic.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>
)

enum class SmartListType(val title: String) {
    ALL_SONGS("All songs"),
    FAVORITES("Favorites"),
    RECENTLY_ADDED("Recently added"),
    RECENTLY_PLAYED("Recently played"),
    MOST_PLAYED("Most played"),
    NOT_PLAYED("Not played")
}

class LibraryDataStore(context: Context) {

    private val prefs = context.getSharedPreferences("pulse_library_data", Context.MODE_PRIVATE)

    fun loadFavorites(): Set<Long> {
        val raw = prefs.getString(KEY_FAVORITES, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun toggleFavorite(trackId: Long): Set<Long> {
        val next = loadFavorites().toMutableSet()
        if (!next.add(trackId)) {
            next.remove(trackId)
        }
        prefs.edit().putString(KEY_FAVORITES, next.joinToString(",")).apply()
        return next
    }

    fun loadPlayCounts(): Map<Long, Int> {
        val raw = prefs.getString(KEY_PLAY_COUNTS, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val map = mutableMapOf<Long, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                k.toLongOrNull()?.let { id -> map[id] = obj.optInt(k, 0) }
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun loadRecentPlayed(): List<Long> {
        val raw = prefs.getString(KEY_RECENT_PLAYED, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun recordPlay(trackId: Long): Pair<Map<Long, Int>, List<Long>> {
        val counts = loadPlayCounts().toMutableMap()
        counts[trackId] = (counts[trackId] ?: 0) + 1
        val countsJson = JSONObject().apply {
            counts.forEach { (id, cnt) -> put(id.toString(), cnt) }
        }.toString()

        val recent = loadRecentPlayed().toMutableList()
        recent.removeAll { it == trackId }
        recent.add(0, trackId)
        val trimmedRecent = recent.take(100)

        prefs.edit()
            .putString(KEY_PLAY_COUNTS, countsJson)
            .putString(KEY_RECENT_PLAYED, trimmedRecent.joinToString(","))
            .apply()

        return counts to trimmedRecent
    }

    fun loadPlaylists(): List<UserPlaylist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val trackArr = obj.optJSONArray("trackIds") ?: JSONArray()
                    val ids = buildList {
                        for (j in 0 until trackArr.length()) {
                            add(trackArr.getLong(j))
                        }
                    }
                    add(
                        UserPlaylist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            trackIds = ids
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun createPlaylist(name: String): List<UserPlaylist> {
        val clean = name.trim().ifBlank { "My Playlist" }
        val current = loadPlaylists().toMutableList()
        current.add(0, UserPlaylist(id = UUID.randomUUID().toString(), name = clean, trackIds = emptyList()))
        savePlaylists(current)
        return current
    }

    fun renamePlaylist(playlistId: String, newName: String): List<UserPlaylist> {
        val clean = newName.trim().ifBlank { return loadPlaylists() }
        val updated = loadPlaylists().map {
            if (it.id == playlistId) it.copy(name = clean) else it
        }
        savePlaylists(updated)
        return updated
    }

    fun deletePlaylist(playlistId: String): List<UserPlaylist> {
        val updated = loadPlaylists().filterNot { it.id == playlistId }
        savePlaylists(updated)
        return updated
    }

    fun addTrackToPlaylist(playlistId: String, trackId: Long): List<UserPlaylist> {
        val updated = loadPlaylists().map { pl ->
            if (pl.id == playlistId && trackId !in pl.trackIds) {
                pl.copy(trackIds = pl.trackIds + trackId)
            } else {
                pl
            }
        }
        savePlaylists(updated)
        return updated
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long): List<UserPlaylist> {
        val updated = loadPlaylists().map { pl ->
            if (pl.id == playlistId) {
                pl.copy(trackIds = pl.trackIds.filterNot { it == trackId })
            } else {
                pl
            }
        }
        savePlaylists(updated)
        return updated
    }

    private fun savePlaylists(playlists: List<UserPlaylist>) {
        val arr = JSONArray()
        for (pl in playlists) {
            val idsArr = JSONArray()
            pl.trackIds.forEach { idsArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("trackIds", idsArr)
                }
            )
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAY_COUNTS = "play_counts"
        private const val KEY_RECENT_PLAYED = "recent_played"
        private const val KEY_PLAYLISTS = "playlists"
    }
}
