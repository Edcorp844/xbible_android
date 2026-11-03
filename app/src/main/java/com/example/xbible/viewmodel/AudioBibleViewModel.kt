package com.example.xbible.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.core.audio.AudioBiblePlayer
import com.example.xbible.data.AudioBibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uniffi.xbible_engine.AudioModuleInfo
import uniffi.xbible_engine.AudioNode
import uniffi.xbible_engine.PlaybackState
import uniffi.xbible_engine.RepeatMode
import java.io.File
import javax.inject.Inject

private const val TAG = "AudioBibleVM"

// ── Local Library Hierarchy ────────────────────────────────────────

data class InstalledLanguageNode(
    val id: String,
    val name: String,
    val collections: List<InstalledCollectionNode>
) {
    val totalModuleCount: Int get() = collections.sumOf { it.totalModuleCount }
}

data class InstalledCollectionNode(
    val id: String,
    val name: String,
    val description: String?,
    val albums: List<InstalledAlbumNode>
) {
    val totalModuleCount: Int get() = albums.sumOf { it.modules.size }
}

data class InstalledAlbumNode(
    val id: String,
    val title: String,
    val order: Int,
    val modules: List<AudioModuleInfo>
) {
    // Artwork extracted from the first module in the album
    val artworkBitmap: Bitmap?
        get() = modules.firstOrNull { it.artwork.imageBytes() != null }?.let { module ->
            module.artwork.imageBytes()?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class AudioBibleViewModel @Inject constructor(
    application: Application,
    private val audioRepo: AudioBibleRepository
) : AndroidViewModel(application) {

    // ── Skip Constants ───────────────────────────────────────────────
    private val forwardSkipMs = 30_000L
    private val backwardSkipMs = 15_000L

    // ── Published UI State (slow-updating) ───────────────────────────
    private val _selectedModule = MutableStateFlow<AudioModuleInfo?>(null)
    val selectedModule = _selectedModule.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId = _selectedNodeId.asStateFlow()

    private val _selectedNodeTitle = MutableStateFlow<String?>(null)
    val selectedNodeTitle = _selectedNodeTitle.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentActiveTitle = MutableStateFlow("")
    val currentActiveTitle = _currentActiveTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isFullPlayerPresented = MutableStateFlow(false)
    val isFullPlayerPresented = _isFullPlayerPresented.asStateFlow()

    private val _decodedArtwork = MutableStateFlow<Bitmap?>(null)
    val decodedArtwork = _decodedArtwork.asStateFlow()

    private val _backgroundGradientColors = MutableStateFlow<List<Long>>(listOf(0xFF000000))
    val backgroundGradientColors = _backgroundGradientColors.asStateFlow()

    // ── Published UI State (fast-updating) ──────────────────────────
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private val _activeText = MutableStateFlow("")
    val activeText = _activeText.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible = _isFullPlayerVisible.asStateFlow()

    // ── Private State ────────────────────────────────────────────────
    private var internalPlaybackState: PlaybackState? = null
    private var flattenedChaptersCache: List<AudioNode> = emptyList()
    private var player: AudioBiblePlayer? = null

    // Audio Focus (Android equivalent of AVAudioSession)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var legacyFocusListener: AudioManager.OnAudioFocusChangeListener? = null

    // ── Public Computed ──────────────────────────────────────────────
    val availableModules: List<AudioModuleInfo>
        get() = audioRepo.getAudioModules()

    val currentActiveSubtitle: String
        get() {
            val idx = flattenedChaptersCache.indexOfFirst { it.id == _selectedNodeId.value }
            return if (idx != -1) "Chapter ${idx + 1} of ${flattenedChaptersCache.size}" else ""
        }

    val currentRepeatMode: RepeatMode
        get() = internalPlaybackState?.repeatMode ?: RepeatMode.OFF

    val cachedChaptersList: List<AudioNode>
        get() = flattenedChaptersCache

    // ── Organisation (Same logic as Swift) ──────────────────────────
    val organizedLibrary: List<InstalledLanguageNode>
        get() {
            val organized = availableModules.mapNotNull { module ->
                val org = module.metadata?.organization ?: return@mapNotNull null
                module to org
            }

            return organized
                .groupBy { it.second.languageCode }
                .map { (langCode, entries) ->
                    val langName = entries.first().second.languageName
                    val collections = entries
                        .groupBy { it.second.collectionId }
                        .map { (collId, collEntries) ->
                            val firstColl = collEntries.first().second
                            val albums = collEntries
                                .groupBy { it.second.albumId }
                                .map { (albumId, albumEntries) ->
                                    val firstAlbum = albumEntries.first().second
                                    InstalledAlbumNode(
                                        id = albumId,
                                        title = firstAlbum.albumTitle,
                                        order = firstAlbum.albumOrder,
                                        modules = albumEntries.map { it.first }
                                            .sortedBy { it.metadata?.displayTitle ?: "" }
                                    )
                                }.sortedBy { it.order }

                            InstalledCollectionNode(
                                id = collId,
                                name = firstColl.collectionName,
                                description = firstColl.collectionDescription,
                                albums = albums
                            )
                        }.sortedBy { it.name }

                    InstalledLanguageNode(
                        id = langCode,
                        name = langName,
                        collections = collections
                    )
                }.sortedBy { it.name }
        }

    val unorganizedModules: List<AudioModuleInfo>
        get() = availableModules.filter { it.metadata?.organization == null }

    // ── Init ─────────────────────────────────────────────────────────
    init {
        // Ensure the engine is ready before anything else
        audioRepo.initialize()
        setupAudioFocus()
    }

    private fun setupAudioFocus() {
        val context = getApplication<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    player?.pause()
                    _isPlaying.value = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    player?.pause()
                    _isPlaying.value = false
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Optionally resume playback
                }
            }
        }

        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
        } else {
            legacyFocusListener = focusChangeListener
        }
    }

    private fun requestAudioFocus(): Boolean {
        val context = getApplication<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                legacyFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    // ── Module Selection ─────────────────────────────────────────────
    fun selectModule(module: AudioModuleInfo) {
        requestAudioFocus()

        flattenedChaptersCache = emptyList()
        _selectedNodeId.value = null
        _selectedNodeTitle.value = null
        _isLoading.value = true
        _selectedModule.value = module
        _currentActiveTitle.value = module.metadata?.displayTitle ?: module.fileName
        _currentTimeMs.value = 0
        _activeText.value = ""

        // Decode Artwork
        module.artwork.imageBytes()?.let { data ->
            _decodedArtwork.value = BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        val basePath = audioRepo.getAudioModulesPath()
        val directFile = File(basePath, module.fileName)

        val finalURL = if (directFile.exists()) {
            directFile
        } else {
            File(basePath).walkTopDown().firstOrNull { it.name == module.fileName }
        }

        if (finalURL == null) {
            Log.e(TAG, "CRITICAL: Could not find ${module.fileName} in $basePath")
            _isLoading.value = false
            return
        }

        Log.d(TAG, "🎵 Found module at: ${finalURL.path}")

        // Clean up old player
        player?.release()

        val newPlayer = AudioBiblePlayer(
            context = getApplication(),
            moduleFilePath = finalURL.path,
            audioRepo = audioRepo
        )
        player = newPlayer

        loadAndCacheNavigationTree()
        setupStateListener()

        viewModelScope.launch {
            delay(50) // Match Swift delay
            player?.play()
            _isPlaying.value = true
            _isLoading.value = false
        }
    }

    // ── State Listener (Throttled Flow) ──────────────────────────────
    private fun setupStateListener() {
        viewModelScope.launch {
            player?.playbackState
                ?.filterNotNull()
                ?.sample(30) // Throttle to ~30ms
                ?.collect { state ->
                    internalPlaybackState = state

                    // Fast updates (only when full player is visible)
                    if (_isFullPlayerVisible.value) {
                        _currentTimeMs.value = state.currentTimeMs
                        _activeText.value = state.activeText
                    }

                    // Slow updates
                    if (_isPlaying.value != state.isPlaying) {
                        _isPlaying.value = state.isPlaying
                    }
                    syncActiveChapter(state.currentTimeMs)
                }
        }
    }

    // ── Playback Controls ────────────────────────────────────────────
    fun togglePlayback() {
        val p = player ?: return
        if (_isPlaying.value) {
            p.pause()
            _isPlaying.value = false
        } else {
            p.play()
            _isPlaying.value = true
        }

        audioRepo.getPlaybackState()?.let {
            internalPlaybackState = it
            if (_isPlaying.value != it.isPlaying) {
                _isPlaying.value = it.isPlaying
            }
        }
    }

    fun skipForward() {
        val p = player ?: return
        val duration = _selectedModule.value?.metadata?.durationMs ?: Long.MAX_VALUE
        val target = minOf(_currentTimeMs.value + forwardSkipMs, duration)

        _currentTimeMs.value = target
        syncActiveChapter(target)
        p.seekTo(target)
    }

    fun skipBackward() {
        val p = player ?: return
        val target = maxOf(0L, _currentTimeMs.value - backwardSkipMs)

        _currentTimeMs.value = target
        syncActiveChapter(target)
        p.seekTo(target)
    }

    fun seekToTime(ms: Long) {
        val p = player ?: return
        _currentTimeMs.value = ms
        syncActiveChapter(ms)
        p.seekTo(ms)
    }

    fun seekToChapter(id: String) {
        val node = flattenedChaptersCache.firstOrNull { it.id == id } ?: return
        val startMs = node.startMs ?: return

        _selectedNodeId.value = node.id
        _selectedNodeTitle.value = node.title
        _currentActiveTitle.value = node.title
        _currentTimeMs.value = startMs
        _activeText.value = ""

        audioRepo.seekToChapter(chapterId = id)
        player?.seekTo(startMs)
    }

    fun stopPlayback() {
        player?.stop()
        _selectedModule.value = null
        _isPlaying.value = false
        _currentTimeMs.value = 0
        _activeText.value = ""

        audioRepo.getPlaybackState()?.let {
            internalPlaybackState = it
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        audioRepo.setRepeatMode(mode = mode)
        audioRepo.getPlaybackState()?.let {
            internalPlaybackState = it
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────
    private fun loadAndCacheNavigationTree() {
        val tree = audioRepo.getNavigationTree() ?: return
        flattenedChaptersCache = tree.children.flatMap { it.children }

        if (_selectedNodeId.value == null && flattenedChaptersCache.isNotEmpty()) {
            val first = flattenedChaptersCache.first()
            _selectedNodeId.value = first.id
            _selectedNodeTitle.value = first.title
            _currentActiveTitle.value = first.title
        }
    }

    private fun syncActiveChapter(timeMs: Long) {
        val activeId = audioRepo.findActiveNodeId(timeMs = timeMs) ?: return
        val matching = flattenedChaptersCache.firstOrNull {
            it.id == activeId || it.children.any { child -> child.id == activeId }
        } ?: return

        if (_selectedNodeId.value != matching.id) {
            _selectedNodeId.value = matching.id
            _selectedNodeTitle.value = matching.title
        }

        if (_currentActiveTitle.value != matching.title) {
            _currentActiveTitle.value = matching.title
        }
    }

    // ── Utilities ────────────────────────────────────────────────────
    fun getChapterIndex(chapterId: String): Int {
        val idx = flattenedChaptersCache.indexOfFirst { it.id == chapterId }
        return if (idx != -1) idx + 1 else 1
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = maxOf(0L, ms / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        val context = getApplication<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyFocusListener?.let { audioManager.abandonAudioFocus(it) }
        }
    }
}