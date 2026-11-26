package com.pulsemusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.pulsemusic.player.data.AudioFolder
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.security.AppSecurityStatus
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulsePink
import com.pulsemusic.player.ui.theme.PulseSurface
import com.pulsemusic.player.ui.theme.PulseSurfaceAlt
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    track: AudioTrack,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PulseSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • ${track.folderName}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(4.dp))

            SheetOptionRow(Icons.Default.PlayArrow, "Play Song", PulseCyan) {
                onDismiss()
                onPlay()
            }
            SheetOptionRow(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = PulsePink
            ) {
                onDismiss()
                onToggleFavorite()
            }
            SheetOptionRow(Icons.Default.PlaylistAdd, "Add to Playlist", Color.White) {
                onDismiss()
                onAddToPlaylist()
            }

            HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

            SheetOptionRow(Icons.Default.Edit, "Edit Song Info (Title / Artist / Album)", PulseCyan) {
                onDismiss()
                onEdit()
            }
            SheetOptionRow(Icons.Default.DriveFileRenameOutline, "Rename File", Color.White) {
                onDismiss()
                onRename()
            }
            SheetOptionRow(Icons.Default.ContentCopy, "Copy (Paste into Folder)", Color.White) {
                onDismiss()
                onCopy()
            }
            SheetOptionRow(Icons.Default.ContentCut, "Cut (Paste into Folder)", Color.White) {
                onDismiss()
                onCut()
            }
            SheetOptionRow(Icons.Default.DriveFileMove, "Move to Folder...", PulseCyan) {
                onDismiss()
                onMoveToFolder()
            }
            SheetOptionRow(Icons.Default.Delete, "Delete Song", Color(0xFFEF4444)) {
                onDismiss()
                onDelete()
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SheetOptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, color = tint, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EditTrackDialog(
    track: AudioTrack,
    onSave: (title: String, artist: String, album: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Song Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, artist, album) }) {
                Text("Save")
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
fun RenameTrackDialog(
    track: AudioTrack,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val currentBase = remember(track.id) {
        File(track.filePath).nameWithoutExtension.ifBlank { track.title }
    }
    var newName by remember { mutableStateOf(currentBase) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Song File") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New file name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) onRename(newName.trim())
                }
            ) {
                Text("Rename")
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
fun FolderPickerDialog(
    title: String,
    folders: List<AudioFolder>,
    onPick: (AudioFolder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(folders, key = { it.folderPath }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PulseSurfaceAlt)
                            .clickable { onPick(folder) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = PulseCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.folderName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(folder.displaySubPath, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    track: AudioTrack,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Song?") },
        text = { Text("Are you sure you want to delete '${track.title}' from your device storage?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFEF4444))
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
fun SecurityStatusDialog(
    status: AppSecurityStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981)) },
        title = { Text("App Security Shield", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityRow("Network / Internet Access", if (status.hasInternetPermission) "Enabled" else "BLOCKED (100% Offline)", !status.hasInternetPermission)
                SecurityRow("Exported Service Hijack", "BLOCKED (exported=false)", true)
                SecurityRow("USB / Cloud Backup Leak", if (status.isBackupDisabled) "DISABLED" else "Enabled", status.isBackupDisabled)
                SecurityRow("Filename Path Guard", "ACTIVE (Traversal Blocked)", true)
                SecurityRow("Debugger Attached", if (status.isDebuggerAttached) "Detected" else "Clean", !status.isDebuggerAttached)
                SecurityRow("Root / Test-Keys", if (status.isRootSuspected) "Detected" else "Clean", !status.isRootSuspected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun SecurityRow(label: String, value: String, healthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 12.5.sp)
        Text(
            text = value,
            color = if (healthy) Color(0xFF10B981) else Color(0xFFF59E0B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
