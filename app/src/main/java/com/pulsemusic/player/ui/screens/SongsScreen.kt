package com.pulsemusic.player.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pulsemusic.player.ui.components.AlphabetIndexerBar
import com.pulsemusic.player.ui.components.SortSelectionDialog
import com.pulsemusic.player.ui.components.TrackSortOrder
import com.pulsemusic.player.ui.components.sortedByOrder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.ui.components.TrackListRow
import com.pulsemusic.player.ui.theme.PulseBg
import com.pulsemusic.player.ui.theme.PulseCyan

@Composable
fun SongsScreen(
    tracks: List<AudioTrack>,
    searchQuery: String,
    currentTrackId: Long?,
    favoriteIds: Set<Long>,
    loadAlbumArt: suspend (AudioTrack) -> Bitmap?,
    onSearchChange: (String) -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongOptions: (AudioTrack) -> Unit,
    onRescanStorage: () -> Unit
) {
    val filtered = remember(tracks, searchQuery) {
        if (searchQuery.isBlank()) tracks
        else {
            val q = searchQuery.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q)
            }
        }
    }

    var sortOrder by remember { mutableStateOf(TrackSortOrder.NAME_AZ) }
    var showSortDialog by remember { mutableStateOf(false) }
    val songsListState = rememberLazyListState()

    val sortedTracks = remember(filtered, sortOrder) {
        filtered.sortedByOrder(sortOrder)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Songs",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sortedTracks.size} tracks • ${sortOrder.label}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = PulseCyan)
                }
                IconButton(onClick = onRescanStorage) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = Color.White)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search songs, artists, folders...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (sortedTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (tracks.isEmpty()) "No songs found on device" else "No matching songs",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = songsListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 26.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sortedTracks, key = { _, t -> t.id }) { index, track ->
                        TrackListRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isFavorite = track.id in favoriteIds,
                            loadAlbumArt = loadAlbumArt,
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onMoreOptions = { onOpenSongOptions(track) },
                            onClick = { onPlayTracks(sortedTracks, index) }
                        )
                    }
                }

                // Vertical A to Z indexer
                AlphabetIndexerBar(
                    tracks = sortedTracks,
                    listState = songsListState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }

    if (showSortDialog) {
        SortSelectionDialog(
            currentOrder = sortOrder,
            onSelectOrder = { sortOrder = it },
            onDismiss = { showSortDialog = false }
        )
    }
}
