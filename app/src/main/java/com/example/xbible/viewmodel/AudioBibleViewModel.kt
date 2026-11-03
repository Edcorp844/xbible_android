package com.example.xbible.viewmodel

import android.annotation.SuppressLint
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

    private val forwardSkipMs = 30_000L
    private val backwardSkipMs = 15_000L

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

    private val _decodedArtwork = MutableStateFlow<Bitmap?>(null)
    val decodedArtwork = _decodedArtwork.asStateFlow()

    private val _backgroundGradientColors = MutableStateFlow<List<Long>>(listOf(0xFF000000))
    val backgroundGradientColors = _backgroundGradientColors.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private val _activeText = MutableStateFlow("")
    val activeText = _activeText.asStateFlow()

    private var internalPlaybackState: PlaybackState? = null
    private var flattenedChaptersCache: List<AudioNode> = emptyList()
    private var player: AudioBiblePlayer? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var legacyFocusListener: AudioManager.OnAudioFocusChangeListener? = null

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

    init {
        audioRepo.initialize()
        setupAudioFocus()
    }

    private fun setupAudioFocus() {
        val context = getApplication<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    player?.pause()
                    _isPlaying.value = false
                }
                AudioManager.AUDIOFOCUS_GAIN -> { /* optionally resume */ }
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

        // 1. Decode Artwork AND extract colors from the Rust image bytes
        module.artwork.imageBytes()?.let { data ->
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            _decodedArtwork.value = bitmap

            // EXTRACT COLORS using the same logic as the Store
            try {
                val artwork = module.artwork
                val colors = artwork.extractColors(4u)

                if (colors.isNotEmpty()) {
                    val extractedLongs = colors.map { color ->
                        val r = (color.red * 255).toLong().coerceIn(0, 255)
                        val g = (color.green * 255).toLong().coerceIn(0, 255)
                        val b = (color.blue * 255).toLong().coerceIn(0, 255)
                        val a = (color.alpha * 255).toLong().coerceIn(0, 255)
                        (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    _backgroundGradientColors.value = extractedLongs
                    Log.d(TAG, "🎨 Extracted colors from Rust: $extractedLongs")
                } else {
                    Log.w(TAG, "🎨 Rust returned 0 colors for ${module.fileName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "🎨 Failed to extract colors", e)
            }
        }

        val basePath = audioRepo.getAudioModulesPath()
        val directFile = File(basePath, module.fileName)

        val finalURL = if (directFile.exists()) directFile
        else File(basePath).walkTopDown().firstOrNull { it.name == module.fileName }

        if (finalURL == null) {
            Log.e(TAG, "CRITICAL: Could not find ${module.fileName} in $basePath")
            _isLoading.value = false
            return
        }

        Log.d(TAG, "🎵 Found module at: ${finalURL.path}")

        player?.release()

        val newPlayer = AudioBiblePlayer(
            context = getApplication(),
            moduleFilePath = finalURL.path,
            audioRepo = audioRepo
        )
        player = newPlayer

        loadAndCacheNavigationTree()
        setupStateListener()

        _isLoading.value = false
    }

    private fun setupStateListener() {
        viewModelScope.launch {
            player?.playbackState
                ?.filterNotNull()
                ?.collect { state ->
                    internalPlaybackState = state

                    // KEY FIX: always publish time, not gated by isFullPlayerVisible
                    _currentTimeMs.value = state.currentTimeMs
                    _activeText.value = state.activeText

                    if (_isPlaying.value != state.isPlaying) {
                        _isPlaying.value = state.isPlaying
                    }
                    syncActiveChapter(state.currentTimeMs)
                }
        }
    }

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
            _isPlaying.value = it.isPlaying
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
        audioRepo.getPlaybackState()?.let { internalPlaybackState = it }
    }

    fun setRepeatMode(mode: RepeatMode) {
        audioRepo.setRepeatMode(mode = mode)
        audioRepo.getPlaybackState()?.let { internalPlaybackState = it }
    }

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

    fun getChapterIndex(chapterId: String): Int {
        val idx = flattenedChaptersCache.indexOfFirst { it.id == chapterId }
        return if (idx != -1) idx + 1 else 1
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(ms: Long): String {
        val totalSeconds = maxOf(0L, ms / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        player?.release()
        val audioManager = getApplication<Application>()
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyFocusListener?.let { audioManager.abandonAudioFocus(it) }
        }
    }
}