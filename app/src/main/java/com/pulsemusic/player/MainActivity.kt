package com.pulsemusic.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.pulsemusic.player.ui.MusicViewModel
import com.pulsemusic.player.ui.components.MiniPlayerBar
import com.pulsemusic.player.ui.components.NowPlayingDialog
import com.pulsemusic.player.ui.screens.EqualizerScreen
import com.pulsemusic.player.ui.screens.FoldersScreen
import com.pulsemusic.player.ui.screens.SongsScreen
import com.pulsemusic.player.ui.theme.PulseBg
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulseMusicTheme
import com.pulsemusic.player.ui.theme.PulseSurface

private enum class MusicTab {
    FOLDERS, SONGS, EQUALIZER
}

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsState()
            var activeTab by remember { mutableStateOf(MusicTab.FOLDERS) }
            var showFullPlayer by remember { mutableStateOf(false) }

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

            LaunchedEffect(Unit) {
                if (!hasAudioPerm) {
                    val req = buildList {
                        add(audioPermission)
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

            BackHandler(enabled = activeTab == MusicTab.FOLDERS && state.selectedFolder != null) {
                viewModel.selectFolder(null)
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
                            text = "Allow audio storage permission to scan all music folders and songs on your phone.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val req = buildList {
                                    add(audioPermission)
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
                                MusicTab.FOLDERS -> FoldersScreen(
                                    folders = state.folders,
                                    selectedFolder = state.selectedFolder,
                                    currentTrackId = state.currentTrack?.id,
                                    isScanning = state.isScanning,
                                    loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                    onSelectFolder = { viewModel.selectFolder(it) },
                                    onPlayTracks = { list, idx -> viewModel.playFromList(list, idx) },
                                    onRescanStorage = { viewModel.rescanStorage() }
                                )

                                MusicTab.SONGS -> SongsScreen(
                                    tracks = state.allTracks,
                                    searchQuery = state.searchQuery,
                                    currentTrackId = state.currentTrack?.id,
                                    loadAlbumArt = { viewModel.loadAlbumArt(it) },
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onPlayTracks = { list, idx -> viewModel.playFromList(list, idx) },
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
                                onOpenEqualizer = {
                                    showFullPlayer = false
                                    activeTab = MusicTab.EQUALIZER
                                },
                                onDismiss = { showFullPlayer = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
