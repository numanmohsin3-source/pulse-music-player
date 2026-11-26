package com.pulsemusic.player.ui

import android.app.Application
import android.content.ComponentName
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.pulsemusic.player.data.AudioFolder
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.data.LibraryDataStore
import com.pulsemusic.player.data.MusicScanner
import com.pulsemusic.player.data.SmartListType
import com.pulsemusic.player.data.UserPlaylist
import com.pulsemusic.player.dsp.AudioDspEngine
import com.pulsemusic.player.dsp.EqPreset
import com.pulsemusic.player.dsp.EqPreferences
import com.pulsemusic.player.dsp.EqSettings
import com.pulsemusic.player.player.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicUiState(
    val isScanning: Boolean = false,
    val includeShortClips: Boolean = false,
    val allTracks: List<AudioTrack> = emptyList(),
    val folders: List<AudioFolder> = emptyList(),
    val selectedFolder: AudioFolder? = null,
    val selectedSmartList: SmartListType? = null,
    val selectedPlaylistId: String? = null,
    val favoriteIds: Set<Long> = emptySet(),
    val playCounts: Map<Long, Int> = emptyMap(),
    val recentPlayedIds: List<Long> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val searchQuery: String = "",
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val eqSettings: EqSettings = EqSettings(),
    val isProDspActive: Boolean = false
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MusicScanner(application)
    private val eqPrefs = EqPreferences(application)
    private val libraryStore = LibraryDataStore(application)

    private val _uiState = MutableStateFlow(
        MusicUiState(
            favoriteIds = libraryStore.loadFavorites(),
            playCounts = libraryStore.loadPlayCounts(),
            recentPlayedIds = libraryStore.loadRecentPlayed(),
            playlists = libraryStore.loadPlaylists(),
            eqSettings = eqPrefs.load()
        )
    )
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var debounceSyncJob: Job? = null
    private var mediaObserver: ContentObserver? = null
    private var trackLookup: Map<Long, AudioTrack> = emptyMap()
    private var lastRecordedPlayId: Long = -1L

    init {
        connectController()
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, MusicService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val ctrl = future.get()
                controller = ctrl
                attachPlayerListeners(ctrl)
                syncPlayerState(ctrl)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun attachPlayerListeners(ctrl: MediaController) {
        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        isProDspActive = AudioDspEngine.isUsingProDynamicsEngine
                    )
                }
                if (isPlaying) startProgressPolling() else progressJob?.cancel()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncPlayerState(ctrl, recordHistory = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncPlayerState(ctrl)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }
        })
    }

    private fun syncPlayerState(ctrl: MediaController, recordHistory: Boolean = false) {
        val currentId = ctrl.currentMediaItem?.mediaMetadata?.extras?.getLong("track_id", -1L) ?: -1L
        val resolvedTrack = trackLookup[currentId] ?: _uiState.value.currentTrack
        if (recordHistory && resolvedTrack != null && resolvedTrack.id != lastRecordedPlayId) {
            lastRecordedPlayId = resolvedTrack.id
            val (counts, recent) = libraryStore.recordPlay(resolvedTrack.id)
            _uiState.update { it.copy(playCounts = counts, recentPlayedIds = recent) }
        }
        val dur = ctrl.duration.takeIf { it > 0 } ?: resolvedTrack?.durationMs ?: 0L
        _uiState.update {
            it.copy(
                currentTrack = resolvedTrack,
                isPlaying = ctrl.isPlaying,
                positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                durationMs = dur,
                shuffleEnabled = ctrl.shuffleModeEnabled,
                repeatMode = ctrl.repeatMode,
                isProDspActive = AudioDspEngine.isUsingProDynamicsEngine
            )
        }
        if (ctrl.isPlaying) startProgressPolling()
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val ctrl = controller ?: break
                val dur = ctrl.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
                _uiState.update {
                    it.copy(
                        positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                        durationMs = dur,
                        isProDspActive = AudioDspEngine.isUsingProDynamicsEngine
                    )
                }
                delay(300L)
            }
        }
    }

    fun scanLibrary() {
        registerMediaObserverIfNeeded()
        _uiState.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            val (tracks, folders) = scanner.scanAllAudio(_uiState.value.includeShortClips)
            trackLookup = tracks.associateBy { it.id }
            val currentFolderPath = _uiState.value.selectedFolder?.folderPath
            val refreshedSelected = currentFolderPath?.let { p -> folders.firstOrNull { it.folderPath == p } }
            _uiState.update {
                it.copy(
                    isScanning = false,
                    allTracks = tracks,
                    folders = folders,
                    selectedFolder = refreshedSelected
                )
            }
        }
    }

    private fun registerMediaObserverIfNeeded() {
        if (mediaObserver != null) return
        val ctx = getApplication<Application>()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                scheduleAutoSync()
            }
        }
        runCatching {
            ctx.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            mediaObserver = observer
        }
    }

    private fun scheduleAutoSync() {
        debounceSyncJob?.cancel()
        debounceSyncJob = viewModelScope.launch {
            delay(1000L)
            scanLibrary()
        }
    }

    fun rescanStorage(onlyRecent: Boolean = false) {
        _uiState.update { it.copy(isScanning = true) }
        viewModelScope.launch(Dispatchers.IO) {
            scanner.triggerStorageRescan(onlyRecentHours = if (onlyRecent) 24 else null) {
                scanLibrary()
            }
        }
    }

    fun selectFolder(folder: AudioFolder?) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun selectSmartList(type: SmartListType?) {
        _uiState.update { it.copy(selectedSmartList = type, selectedPlaylistId = null) }
    }

    fun selectPlaylist(playlistId: String?) {
        _uiState.update { it.copy(selectedPlaylistId = playlistId, selectedSmartList = null) }
    }

    fun resolveSmartListTracks(type: SmartListType): List<AudioTrack> {
        val state = _uiState.value
        return when (type) {
            SmartListType.ALL_SONGS -> state.allTracks
            SmartListType.FAVORITES -> state.allTracks.filter { it.id in state.favoriteIds }
            SmartListType.RECENTLY_ADDED -> state.allTracks.sortedByDescending { it.dateAddedSec }
            SmartListType.RECENTLY_PLAYED -> {
                val map = state.allTracks.associateBy { it.id }
                state.recentPlayedIds.mapNotNull { map[it] }
            }
            SmartListType.MOST_PLAYED -> {
                state.allTracks
                    .filter { (state.playCounts[it.id] ?: 0) > 0 }
                    .sortedByDescending { state.playCounts[it.id] ?: 0 }
            }
            SmartListType.NOT_PLAYED -> {
                state.allTracks.filter { (state.playCounts[it.id] ?: 0) == 0 }
            }
        }
    }

    fun resolvePlaylistTracks(playlist: UserPlaylist): List<AudioTrack> {
        val map = _uiState.value.allTracks.associateBy { it.id }
        return playlist.trackIds.mapNotNull { map[it] }
    }

    fun toggleFavorite(trackId: Long) {
        val next = libraryStore.toggleFavorite(trackId)
        _uiState.update { it.copy(favoriteIds = next) }
    }

    fun createPlaylist(name: String, initialTrackId: Long? = null) {
        var updated = libraryStore.createPlaylist(name)
        if (initialTrackId != null && updated.isNotEmpty()) {
            updated = libraryStore.addTrackToPlaylist(updated.first().id, initialTrackId)
        }
        _uiState.update { it.copy(playlists = updated) }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val updated = libraryStore.renamePlaylist(playlistId, newName)
        _uiState.update { it.copy(playlists = updated) }
    }

    fun deletePlaylist(playlistId: String) {
        val updated = libraryStore.deletePlaylist(playlistId)
        _uiState.update {
            it.copy(
                playlists = updated,
                selectedPlaylistId = if (it.selectedPlaylistId == playlistId) null else it.selectedPlaylistId
            )
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        val updated = libraryStore.addTrackToPlaylist(playlistId, trackId)
        _uiState.update { it.copy(playlists = updated) }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
        val updated = libraryStore.removeTrackFromPlaylist(playlistId, trackId)
        _uiState.update { it.copy(playlists = updated) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun playFromList(tracks: List<AudioTrack>, startIndex: Int = 0) {
        val ctrl = controller ?: return
        if (tracks.isEmpty()) return

        val mediaItems = tracks.map { track ->
            val extras = Bundle().apply { putLong("track_id", track.id) }
            val meta = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setExtras(extras)
                .build()
            MediaItem.Builder()
                .setUri(track.contentUri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(meta)
                .build()
        }

        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val selected = tracks[safeIndex]
        lastRecordedPlayId = selected.id
        val (counts, recent) = libraryStore.recordPlay(selected.id)

        ctrl.setMediaItems(mediaItems, safeIndex, 0L)
        ctrl.prepare()
        ctrl.play()

        _uiState.update {
            it.copy(
                currentTrack = selected,
                durationMs = selected.durationMs,
                positionMs = 0L,
                playCounts = counts,
                recentPlayedIds = recent
            )
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 4000L) {
            ctrl.seekTo(0L)
        } else {
            ctrl.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun toggleShuffle() {
        val ctrl = controller ?: return
        ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val ctrl = controller ?: return
        ctrl.repeatMode = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleDsp(enabled: Boolean) {
        updateEq { it.copy(enabled = enabled) }
    }

    fun selectPreset(preset: EqPreset) {
        val presetSettings = preset.defaultSettings().copy(enabled = true)
        saveAndApplyEq(presetSettings)
    }

    fun setBandGain(index: Int, gainDb: Float) {
        updateEq { cur ->
            val updated = cur.bandsDb.toMutableList()
            if (index in updated.indices) {
                updated[index] = gainDb.coerceIn(-12f, 12f)
            }
            cur.copy(preset = EqPreset.CUSTOM, bandsDb = updated)
        }
    }

    fun setBassBoost(strength: Int) {
        updateEq { it.copy(preset = EqPreset.CUSTOM, bassBoostStrength = strength.coerceIn(0, 1000)) }
    }

    fun setVirtualizer(strength: Int) {
        updateEq { it.copy(preset = EqPreset.CUSTOM, virtualizerStrength = strength.coerceIn(0, 1000)) }
    }

    fun setLoudnessGain(gainMb: Int) {
        updateEq { it.copy(preset = EqPreset.CUSTOM, loudnessGainMb = gainMb.coerceIn(0, 1000)) }
    }

    fun setCompressorEnabled(enabled: Boolean) {
        updateEq { it.copy(preset = EqPreset.CUSTOM, compressorEnabled = enabled) }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        updateEq { it.copy(preset = EqPreset.CUSTOM, limiterEnabled = enabled) }
    }

    private fun updateEq(transform: (EqSettings) -> EqSettings) {
        val next = transform(_uiState.value.eqSettings)
        saveAndApplyEq(next)
    }

    private fun saveAndApplyEq(settings: EqSettings) {
        eqPrefs.save(settings)
        AudioDspEngine.applySettings(settings)
        _uiState.update {
            it.copy(
                eqSettings = settings,
                isProDspActive = AudioDspEngine.isUsingProDynamicsEngine
            )
        }
    }

    suspend fun loadAlbumArt(track: AudioTrack): Bitmap? = scanner.loadAlbumArt(track)

    override fun onCleared() {
        progressJob?.cancel()
        debounceSyncJob?.cancel()
        mediaObserver?.let { obs ->
            runCatching { getApplication<Application>().contentResolver.unregisterContentObserver(obs) }
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
