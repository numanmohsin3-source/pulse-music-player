package com.pulsemusic.player

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.ui.ClipboardMode
import com.pulsemusic.player.ui.MusicViewModel
import com.pulsemusic.player.ui.components.DeleteConfirmDialog
import com.pulsemusic.player.ui.components.EditTrackDialog
import com.pulsemusic.player.ui.components.FolderPickerDialog
import com.pulsemusic.player.ui.components.MiniPlayerBar
import com.pulsemusic.player.ui.components.NowPlayingDialog
import com.pulsemusic.player.ui.components.RenameTrackDialog
import com.pulsemusic.player.ui.components.SecurityStatusDialog
import com.pulsemusic.player.ui.components.SongOptionsBottomSheet
import com.pulsemusic.player.ui.screens.AddToPlaylistDialog
import com.pulsemusic.player.ui.screens.EqualizerScreen
import com.pulsemusic.player.ui.screens.FoldersScreen
import com.pulsemusic.player.ui.screens.LibraryHubScreen
import com.pulsemusic.player.ui.screens.SongsScreen
import com.pulsemusic.player.ui.theme.PulseBg
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulseMusicTheme
import com.pulsemusic.player.ui.theme.PulseSurface

private enum class MusicTab {
    LIBRARY, FOLDERS, SONGS, EQUALIZER
}

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsState()
            var activeTab by remember { mutableStateOf(MusicTab.LIBRARY) }
            var showFullPlayer by remember { mutableStateOf(false) }

            var trackToAddPlaylist by remember { mutableStateOf<AudioTrack?>(null) }
            var optionsTrack by remember { mutableStateOf<AudioTrack?>(null) }
            var editTrack by remember { mutableStateOf<AudioTrack?>(null) }
            var renameTrack by remember { mutableStateOf<AudioTrack?>(null) }
            var moveTrack by remember { mutableStateOf<AudioTrack?>(null) }
            var deleteTrack by remember { mutableStateOf<AudioTrack?>(null) }
            var showSecurityDialog by remember { mutableStateOf(false) }

            val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            var hasAudioPerm by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, audioPermission) ==
                        PackageManager.PERMISSION_GRANTED
                )
            }

            val permLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                hasAudioPerm = grants[audioPermission] == true ||
                    ContextCompat.checkSelfPermission(this, audioPermission) ==
                    PackageManager.PERMISSION_GRANTED
            }

            val consentLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                viewModel.onUserConsentResult(result.resultCode == Activity.RESULT_OK)
            }

            LaunchedEffect(state.pendingConsentSender) {
                val sender = state.pendingConsentSender ?: return@LaunchedEffect
                consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }

            LaunchedEffect(state.statusMessage) {
                val msg = state.statusMessage ?: return@LaunchedEffect
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                viewModel.setStatusMessage(null)
            }

            LaunchedEffect(Unit) {
                if (!hasAudioPerm) {
                    val req = buildList {
                        add(audioPermission)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    permLauncher.launch(req)
                }
            }

            LaunchedEffect(hasAudioPerm) {
                if (hasAudioPerm) {
                    viewModel.scanLibrary()
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, hasAudioPerm) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && hasAudioPerm) {
                        viewModel.rescanStorage(onlyRecent = true)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            BackHandler(
                enabled = (activeTab == MusicTab.FOLDERS && state.selectedFolder != null) ||
                    (activeTab == MusicTab.LIBRARY && (state.selectedSmartList != null || state.selectedPlaylistId != null))
            ) {
                if (activeTab == MusicTab.FOLDERS) {
                    viewModel.selectFolder(null)
                } else {
                    viewModel.selectSmartList(null)
                    viewModel.selectPlaylist(null)
                }
            }

            PulseMusicTheme {
                if (!hasAudioPerm) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PulseBg)
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = PulseCyan,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Storage Audio Access Required",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Allow audio storage permission to scan and manage all music folders and songs on your phone.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val req = buildList {
                                    add(audioPermission)
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                                permLauncher.launch(req)
                            }
                        ) {
                            Text("Grant Storage Permission")
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = PulseBg,
                        bottomBar = {
                            Column(modifier = Modifier.background(PulseBg)) {
                                state.currentTrack?.let { track ->
                                    MiniPlayerBar(
                                        track = track,
                                        isPlaying = state.isPlaying,
                                        positionMs = state.positionMs,
                                        durationMs = state.durationMs,
                                        loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onSkipNext = { viewModel.skipNext() },
                                        onOpenFullPlayer = { showFullPlayer = true }
                                    )
                                }
                                NavigationBar(containerColor = PulseSurface) {
                                    NavigationBarItem(
                                        selected = activeTab == MusicTab.LIBRARY,
                                        onClick = { activeTab = MusicTab.LIBRARY },
                                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                        label = { Text("Library") }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == MusicTab.FOLDERS,
                                        onClick = { activeTab = MusicTab.FOLDERS },
                                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                        label = { Text("Folders") }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == MusicTab.SONGS,
                                        onClick = { activeTab = MusicTab.SONGS },
                                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                                        label = { Text("Songs") }
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == MusicTab.EQUALIZER,
                                        onClick = { activeTab = MusicTab.EQUALIZER },
                                        icon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                                        label = { Text("Equalizer") }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeTab) {
                                MusicTab.LIBRARY -> LibraryHubScreen(
                                    allTracks = state.allTracks,
                                    favoriteIds = state.favoriteIds,
                                    playCounts = state.playCounts,
                                    recentPlayedIds = state.recentPlayedIds,
                                    playlists = state.playlists,
                                    selectedSmartList = state.selectedSmartList,
                                    selectedPlaylistId = state.selectedPlaylistId,
                                    currentTrackId = state.currentTrack?.id,
                                    loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                    resolveSmartListTracks = { viewModel.resolveSmartListTracks(it) },
                                    resolvePlaylistTracks = { viewModel.resolvePlaylistTracks(it) },
                                    onSelectSmartList = { viewModel.selectSmartList(it) },
                                    onSelectPlaylist = { viewModel.selectPlaylist(it) },
                                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                    onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                    onRemoveFromPlaylist = { plId, tId -> viewModel.removeTrackFromPlaylist(plId, tId) },
                                    onToggleFavorite = { tId -> viewModel.toggleFavorite(tId) },
                                    onOpenSongOptions = { track -> optionsTrack = track },
                                    onOpenSecurityInfo = { showSecurityDialog = true },
                                    onPlayTracks = { list, idx -> viewModel.playFromList(list, idx) },
                                    onRescanStorage = { viewModel.rescanStorage() }
                                )

                                MusicTab.FOLDERS -> FoldersScreen(
                                    folders = state.folders,
                                    selectedFolder = state.selectedFolder,
                                    currentTrackId = state.currentTrack?.id,
                                    favoriteIds = state.favoriteIds,
                                    clipboard = state.clipboard,
                                    isScanning = state.isScanning,
                                    loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                    onSelectFolder = { viewModel.selectFolder(it) },
                                    onPlayTracks = { list, idx -> viewModel.playFromList(list, idx) },
                                    onToggleFavorite = { tId -> viewModel.toggleFavorite(tId) },
                                    onOpenSongOptions = { track -> optionsTrack = track },
                                    onPasteHere = { folder -> viewModel.pasteClipboardIntoFolder(folder) },
                                    onClearClipboard = { viewModel.clearClipboard() },
                                    onRescanStorage = { viewModel.rescanStorage() }
                                )

                                MusicTab.SONGS -> SongsScreen(
                                    tracks = state.allTracks,
                                    searchQuery = state.searchQuery,
                                    currentTrackId = state.currentTrack?.id,
                                    favoriteIds = state.favoriteIds,
                                    loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onPlayTracks = { list, idx -> viewModel.playFromList(list, idx) },
                                    onToggleFavorite = { tId -> viewModel.toggleFavorite(tId) },
                                    onOpenSongOptions = { track -> optionsTrack = track },
                                    onRescanStorage = { viewModel.rescanStorage() }
                                )

                                MusicTab.EQUALIZER -> EqualizerScreen(
                                    settings = state.eqSettings,
                                    isPlaying = state.isPlaying,
                                    isProDspActive = state.isProDspActive,
                                    onToggleDsp = { viewModel.toggleDsp(it) },
                                    onSelectPreset = { viewModel.selectPreset(it) },
                                    onBandChange = { idx, gain -> viewModel.setBandGain(idx, gain) },
                                    onBassChange = { viewModel.setBassBoost(it) },
                                    onVirtualizerChange = { viewModel.setVirtualizer(it) },
                                    onLoudnessChange = { viewModel.setLoudnessGain(it) },
                                    onCompressorToggle = { viewModel.setCompressorEnabled(it) },
                                    onLimiterToggle = { viewModel.setLimiterEnabled(it) }
                                )
                            }
                        }
                    }

                    if (showFullPlayer) {
                        state.currentTrack?.let { track ->
                            NowPlayingDialog(
                                track = track,
                                isPlaying = state.isPlaying,
                                isFavorite = track.id in state.favoriteIds,
                                positionMs = state.positionMs,
                                durationMs = state.durationMs,
                                shuffleEnabled = state.shuffleEnabled,
                                repeatMode = state.repeatMode,
                                playbackSpeed = state.playbackSpeed,
                                eqPresetLabel = state.eqSettings.preset.displayName,
                                loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                onSeek = { viewModel.seekTo(it) },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.skipNext() },
                                onPrev = { viewModel.skipPrevious() },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                onAddToPlaylist = { trackToAddPlaylist = track },
                                onOpenEqualizer = {
                                    showFullPlayer = false
                                    activeTab = MusicTab.EQUALIZER
                                },
                                onDismiss = { showFullPlayer = false }
                            )
                        }
                    }

                    optionsTrack?.let { target ->
                        val activeFolderTracks = state.selectedFolder?.tracks ?: state.allTracks
                        val idx = activeFolderTracks.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                        SongOptionsBottomSheet(
                            track = target,
                            isFavorite = target.id in state.favoriteIds,
                            onPlay = { viewModel.playFromList(activeFolderTracks, idx) },
                            onToggleFavorite = { viewModel.toggleFavorite(target.id) },
                            onAddToPlaylist = { trackToAddPlaylist = target },
                            onEdit = { editTrack = target },
                            onRename = { renameTrack = target },
                            onCopy = { viewModel.setClipboard(target, ClipboardMode.COPY) },
                            onCut = { viewModel.setClipboard(target, ClipboardMode.CUT) },
                            onMoveToFolder = { moveTrack = target },
                            onDelete = { deleteTrack = target },
                            onDismiss = { optionsTrack = null }
                        )
                    }

                    editTrack?.let { target ->
                        EditTrackDialog(
                            track = target,
                            onSave = { t, a, alb ->
                                viewModel.editTrackInfo(target, t, a, alb)
                                editTrack = null
                            },
                            onDismiss = { editTrack = null }
                        )
                    }

                    renameTrack?.let { target ->
                        RenameTrackDialog(
                            track = target,
                            onRename = { newName ->
                                viewModel.renameTrack(target, newName)
                                renameTrack = null
                            },
                            onDismiss = { renameTrack = null }
                        )
                    }

                    moveTrack?.let { target ->
                        FolderPickerDialog(
                            title = "Move '${target.title}' to...",
                            folders = state.folders,
                            onPick = { dest ->
                                viewModel.moveTrackToFolder(target, dest)
                                moveTrack = null
                            },
                            onDismiss = { moveTrack = null }
                        )
                    }

                    deleteTrack?.let { target ->
                        DeleteConfirmDialog(
                            track = target,
                            onConfirm = {
                                viewModel.deleteTrack(target)
                                deleteTrack = null
                            },
                            onDismiss = { deleteTrack = null }
                        )
                    }

                    if (showSecurityDialog) {
                        SecurityStatusDialog(
                            status = state.securityStatus,
                            onDismiss = { showSecurityDialog = false }
                        )
                    }

                    trackToAddPlaylist?.let { targetTrack ->
                        AddToPlaylistDialog(
                            track = targetTrack,
                            playlists = state.playlists,
                            onSelectPlaylist = { plId ->
                                viewModel.addTrackToPlaylist(plId, targetTrack.id)
                                trackToAddPlaylist = null
                            },
                            onCreateAndAdd = { name ->
                                viewModel.createPlaylist(name, initialTrackId = targetTrack.id)
                                trackToAddPlaylist = null
                            },
                            onDismiss = { trackToAddPlaylist = null }
                        )
                    }
                }
            }
        }
    }
}
