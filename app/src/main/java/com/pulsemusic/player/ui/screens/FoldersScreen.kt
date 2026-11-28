package com.pulsemusic.player.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Sort
import com.pulsemusic.player.ui.components.AlphabetIndexerBar
import com.pulsemusic.player.ui.components.FolderSortOrder
import com.pulsemusic.player.ui.components.FolderSortSelectionDialog
import com.pulsemusic.player.ui.components.SortSelectionDialog
import com.pulsemusic.player.ui.components.TrackSortOrder
import com.pulsemusic.player.ui.components.sortedByFolderOrder
import com.pulsemusic.player.ui.components.sortedByOrder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsemusic.player.data.AudioFolder
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.ui.ClipboardMode
import com.pulsemusic.player.ui.FileClipboard
import com.pulsemusic.player.ui.components.TrackListRow
import com.pulsemusic.player.ui.theme.PulseBg
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulsePink
import com.pulsemusic.player.ui.theme.PulseSurface
import com.pulsemusic.player.ui.theme.PulseSurfaceAlt

@Composable
fun FoldersScreen(
    folders: List<AudioFolder>,
    selectedFolder: AudioFolder?,
    currentTrackId: Long?,
    favoriteIds: Set<Long>,
    clipboard: FileClipboard?,
    isScanning: Boolean,
    loadAlbumArt: suspend (AudioTrack) -> Bitmap?,
    onSelectFolder: (AudioFolder?) -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenSongOptions: (AudioTrack) -> Unit,
    onPasteHere: (AudioFolder) -> Unit,
    onClearClipboard: () -> Unit,
    onRescanStorage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseBg)
    ) {
        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = PulseCyan,
                trackColor = PulseSurface
            )
        }

        if (selectedFolder == null) {
            var folderSortOrder by remember { mutableStateOf(FolderSortOrder.NAME_AZ) }
            var showFolderSortDialog by remember { mutableStateOf(false) }

            val sortedFolders = remember(folders, folderSortOrder) {
                folders.sortedByFolderOrder(folderSortOrder)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Music Folders",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sortedFolders.size} folders • ${folderSortOrder.label}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showFolderSortDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Folders",
                            tint = PulseCyan
                        )
                    }
                    IconButton(onClick = onRescanStorage) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan Storage",
                            tint = Color.White
                        )
                    }
                }
            }

            if (clipboard != null) {
                ClipboardBanner(
                    clipboard = clipboard,
                    canPasteHere = false,
                    onPaste = {},
                    onCancel = onClearClipboard
                )
            }

            if (sortedFolders.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No audio folders found on storage",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedFolders, key = { it.folderPath }) { folder ->
                        FolderRowCard(
                            folder = folder,
                            onClick = { onSelectFolder(folder) }
                        )
                    }
                }
            }

            if (showFolderSortDialog) {
                FolderSortSelectionDialog(
                    currentOrder = folderSortOrder,
                    onSelectOrder = { folderSortOrder = it },
                    onDismiss = { showFolderSortDialog = false }
                )
            }
        } else {
            // Inside a selected folder
            var currentOrder by remember { mutableStateOf(TrackSortOrder.NAME_AZ) }
            var showSortDialog by remember { mutableStateOf(false) }
            val folderListState = rememberLazyListState()

            val sortedTracks = remember(selectedFolder.tracks, currentOrder) {
                selectedFolder.tracks.sortedByOrder(currentOrder)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectFolder(null) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to folders",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFolder.folderName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${sortedTracks.size} songs • ${currentOrder.label}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sort button
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Songs",
                        tint = PulseCyan
                    )
                }
            }

            if (clipboard != null) {
                ClipboardBanner(
                    clipboard = clipboard,
                    canPasteHere = true,
                    onPaste = { onPasteHere(selectedFolder) },
                    onCancel = onClearClipboard
                )
            }

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
                    onClick = {
                        val shuffled = sortedTracks.shuffled()
                        onPlayTracks(shuffled, 0)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle", color = PulseCyan)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = folderListState,
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

                // Vertical A to Z alphabet indexer (# to Z)
                AlphabetIndexerBar(
                    tracks = sortedTracks,
                    listState = folderListState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            if (showSortDialog) {
                SortSelectionDialog(
                    currentOrder = currentOrder,
                    onSelectOrder = { currentOrder = it },
                    onDismiss = { showSortDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ClipboardBanner(
    clipboard: FileClipboard,
    canPasteHere: Boolean,
    onPaste: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF17253B))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (clipboard.mode == ClipboardMode.CUT) "Ready to Move (Cut)" else "Ready to Copy",
                color = PulseCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (canPasteHere) clipboard.track.title else "Open target folder to paste '${clipboard.track.title}'",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (canPasteHere) {
            Button(
                onClick = onPaste,
                colors = ButtonDefaults.buttonColors(containerColor = PulseCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Paste Here", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FolderRowCard(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PulseSurface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PulseSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = PulseCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.folderName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = folder.displaySubPath,
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${folder.tracks.size} songs",
                color = PulsePink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = AudioTrack.formatMillis(folder.totalDurationMs),
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
