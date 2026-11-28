package com.pulsemusic.player.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.pulsemusic.player.ui.components.AlphabetIndexerBar
import com.pulsemusic.player.ui.components.SortSelectionDialog
import com.pulsemusic.player.ui.components.TrackSortOrder
import com.pulsemusic.player.ui.components.sortedByOrder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.data.SmartListType
import com.pulsemusic.player.data.UserPlaylist
import com.pulsemusic.player.ui.components.TrackListRow
import com.pulsemusic.player.ui.theme.PulseBg
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulsePink
import com.pulsemusic.player.ui.theme.PulseSurface
import com.pulsemusic.player.ui.theme.PulseSurfaceAlt

@Composable
fun LibraryHubScreen(
    allTracks: List<AudioTrack>,
    favoriteIds: Set<Long>,
    playCounts: Map<Long, Int>,
    recentPlayedIds: List<Long>,
    playlists: List<UserPlaylist>,
    selectedSmartList: SmartListType?,
    selectedPlaylistId: String?,
    currentTrackId: Long?,
    loadAlbumArt: suspend (AudioTrack) -> Bitmap?,
    resolveSmartListTracks: (SmartListType) -> List<AudioTrack>,
    resolvePlaylistTracks: (UserPlaylist) -> List<AudioTrack>,
    onSelectSmartList: (SmartListType?) -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (String, Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongOptions: (AudioTrack) -> Unit,
    onOpenSecurityInfo: () -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onRescanStorage: () -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val activePlaylist = selectedPlaylistId?.let { id -> playlists.firstOrNull { it.id == id } }

    if (selectedSmartList != null) {
        val listTracks = remember(selectedSmartList, allTracks, favoriteIds, playCounts, recentPlayedIds) {
            resolveSmartListTracks(selectedSmartList)
        }
        ListDetailView(
            title = selectedSmartList.title,
            tracks = listTracks,
            isRecentlyAdded = selectedSmartList == SmartListType.RECENTLY_ADDED,
            currentTrackId = currentTrackId,
            favoriteIds = favoriteIds,
            loadAlbumArt = loadAlbumArt,
            onBack = { onSelectSmartList(null) },
            onPlayTracks = onPlayTracks,
            onToggleFavorite = onToggleFavorite,
            onOpenSongOptions = onOpenSongOptions,
            onRemoveFromList = null
        )
    } else if (activePlaylist != null) {
        val listTracks = remember(activePlaylist, allTracks) {
            resolvePlaylistTracks(activePlaylist)
        }
        ListDetailView(
            title = activePlaylist.name,
            tracks = listTracks,
            isRecentlyAdded = false,
            currentTrackId = currentTrackId,
            favoriteIds = favoriteIds,
            loadAlbumArt = loadAlbumArt,
            onBack = { onSelectPlaylist(null) },
            onPlayTracks = onPlayTracks,
            onToggleFavorite = onToggleFavorite,
            onOpenSongOptions = onOpenSongOptions,
            onRemoveFromList = { trackId -> onRemoveFromPlaylist(activePlaylist.id, trackId) }
        )
    } else {
        // Main Library Hub matching the user's reference layout
        val recentAddedCount = allTracks.size
        val favCount = allTracks.count { it.id in favoriteIds }
        val recentPlayedCount = recentPlayedIds.count { id -> allTracks.any { it.id == id } }
        val mostPlayedCount = allTracks.count { (playCounts[it.id] ?: 0) > 0 }
        val notPlayedCount = allTracks.count { (playCounts[it.id] ?: 0) == 0 }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PulseBg),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Music Library",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = onOpenSecurityInfo) {
                            Icon(Icons.Default.Shield, contentDescription = "Security Shield", tint = Color(0xFF10B981))
                        }
                        IconButton(onClick = onRescanStorage) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = PulseCyan)
                        }
                    }
                }
            }

            item {
                SmartListRow(
                    icon = Icons.Default.LibraryMusic,
                    title = "All songs",
                    count = allTracks.size,
                    iconTint = Color.White,
                    onClick = { onSelectSmartList(SmartListType.ALL_SONGS) }
                )
            }
            item {
                SmartListRow(
                    icon = Icons.Default.Favorite,
                    title = "Favorites",
                    count = favCount,
                    iconTint = PulsePink,
                    onClick = { onSelectSmartList(SmartListType.FAVORITES) }
                )
            }
            item {
                SmartListRow(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Recently added",
                    count = recentAddedCount,
                    iconTint = Color.White,
                    onClick = { onSelectSmartList(SmartListType.RECENTLY_ADDED) }
                )
            }
            item {
                SmartListRow(
                    icon = Icons.Default.History,
                    title = "Recently played",
                    count = recentPlayedCount,
                    iconTint = Color.White,
                    onClick = { onSelectSmartList(SmartListType.RECENTLY_PLAYED) }
                )
            }
            item {
                SmartListRow(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Most played",
                    count = mostPlayedCount,
                    iconTint = Color(0xFFF59E0B),
                    onClick = { onSelectSmartList(SmartListType.MOST_PLAYED) }
                )
            }
            item {
                SmartListRow(
                    icon = Icons.Default.Block,
                    title = "Not played",
                    count = notPlayedCount,
                    iconTint = Color(0xFF94A3B8),
                    onClick = { onSelectSmartList(SmartListType.NOT_PLAYED) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Your playlists",
                    color = Color(0xFF10B981),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showCreatePlaylistDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Playlist", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = "No playlists yet. Tap + New Playlist or the playlist icon on any song.",
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(playlists, key = { it.id }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PulseSurface)
                            .clickable { onSelectPlaylist(pl.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PulseSurfaceAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = PulseCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pl.trackIds.size} songs",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Playlist",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreate = { name ->
                onCreatePlaylist(name)
                showCreatePlaylistDialog = false
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }
}

@Composable
private fun SmartListRow(
    icon: ImageVector,
    title: String,
    count: Int,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            color = Color(0xFF64748B),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ListDetailView(
    title: String,
    tracks: List<AudioTrack>,
    isRecentlyAdded: Boolean = false,
    currentTrackId: Long?,
    favoriteIds: Set<Long>,
    loadAlbumArt: suspend (AudioTrack) -> Bitmap?,
    onBack: () -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongOptions: (AudioTrack) -> Unit,
    onRemoveFromList: ((Long) -> Unit)?
) {
    var selectedDayFilter by remember { mutableStateOf<Long?>(null) }
    var currentSortOrder by remember {
        mutableStateOf(if (isRecentlyAdded) TrackSortOrder.DATE_NEWEST else TrackSortOrder.NAME_AZ)
    }
    var showSortDialog by remember { mutableStateOf(false) }
    val detailListState = rememberLazyListState()

    // Day filter for Recently Added: e.g. 7, 30, 40, 60 days
    val filteredByDays = remember(tracks, selectedDayFilter, isRecentlyAdded) {
        if (!isRecentlyAdded || selectedDayFilter == null) tracks
        else tracks.filter { it.daysAgo() <= selectedDayFilter!! }
    }

    val sortedTracks = remember(filteredByDays, currentSortOrder) {
        filteredByDays.sortedByOrder(currentSortOrder)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sortedTracks.size} songs • ${currentSortOrder.label}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
            }
            IconButton(onClick = { showSortDialog = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = PulseCyan)
            }
        }

        // Recently Added Day Filter Buttons (All, 7 Days, 30 Days, 40 Days, 60 Days)
        if (isRecentlyAdded) {
            val dayOptions = listOf(
                null to "All",
                7L to "7 Days Ago",
                30L to "30 Days Ago",
                40L to "40 Days Ago",
                60L to "60 Days Ago"
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayOptions) { (days, label) ->
                    val isSelected = selectedDayFilter == days
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDayFilter = days },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PulsePink,
                            selectedLabelColor = Color.White,
                            containerColor = PulseSurface,
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (sortedTracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onPlayTracks(sortedTracks, 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = PulsePink),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onPlayTracks(sortedTracks.shuffled(), 0) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle", color = PulseCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (sortedTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecentlyAdded && selectedDayFilter != null)
                        "No songs added within the last ${selectedDayFilter} days."
                    else
                        "No songs in $title yet.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = detailListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 26.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sortedTracks, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                TrackListRow(
                                    track = track,
                                    isCurrent = track.id == currentTrackId,
                                    isFavorite = track.id in favoriteIds,
                                    subtitleExtra = if (isRecentlyAdded) track.relativeTimeAgo() else null,
                                    loadAlbumArt = loadAlbumArt,
                                    onToggleFavorite = { onToggleFavorite(track.id) },
                                    onMoreOptions = if (onRemoveFromList == null) {
                                        { onOpenSongOptions(track) }
                                    } else null,
                                    onClick = { onPlayTracks(sortedTracks, index) }
                                )
                            }
                            if (onRemoveFromList != null) {
                                IconButton(onClick = { onRemoveFromList(track.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove from playlist",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Vertical A-Z indexer
                AlphabetIndexerBar(
                    tracks = sortedTracks,
                    listState = detailListState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }

    if (showSortDialog) {
        SortSelectionDialog(
            currentOrder = currentSortOrder,
            onSelectOrder = { currentSortOrder = it },
            onDismiss = { showSortDialog = false }
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onCreate(name.trim())
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: AudioTrack,
    playlists: List<UserPlaylist>,
    onSelectPlaylist: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to Playlist",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = track.title,
                    color = PulseCyan,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PulseSurfaceAlt)
                            .clickable { onSelectPlaylist(pl.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = PulseCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pl.name,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${pl.trackIds.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Or create new playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (newName.isNotBlank()) {
                TextButton(onClick = { onCreateAndAdd(newName.trim()) }) {
                    Text("Create & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
