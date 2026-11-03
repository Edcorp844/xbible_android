package com.example.xbible.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.xbible.core.audio.PlaybackService
import com.example.xbible.data.AudioBibleRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var timeUpdateJob: Job? = null

    init {
        audioRepo.initialize()
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupMediaControllerListener()
                startTimeSync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to PlaybackService", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupMediaControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _isLoading.value = false
                }
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaMetadata?.title?.let {
                    if (it.isNotEmpty() && it != "Unknown Title") {
                        _currentActiveTitle.value = it.toString()
                    }
                }
            }
        })
    }

    private fun startTimeSync() {
        timeUpdateJob?.cancel()
        timeUpdateJob = viewModelScope.launch {
            while (true) {
                delay(50)
                mediaController?.let { controller ->
                    _currentTimeMs.value = controller.currentPosition
                    audioRepo.seekToTime(controller.currentPosition)
                    syncActiveChapter(controller.currentPosition)
                }
            }
        }
    }

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

    fun selectModule(module: AudioModuleInfo) {
        flattenedChaptersCache = emptyList()
        _selectedNodeId.value = null
        _selectedNodeTitle.value = null
        _isLoading.value = true
        _selectedModule.value = module
        _currentActiveTitle.value = module.metadata?.displayTitle ?: module.fileName
        _currentTimeMs.value = 0
        _activeText.value = ""

        module.artwork.imageBytes()?.let { data ->
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            _decodedArtwork.value = bitmap

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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract colors", e)
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

        Log.d(TAG, "🎵 Loading module at: ${finalURL.path}")

        // FIX: RequestMetadata is the only reliable way to pass the file URI
        // across the IPC boundary from a MediaController to the MediaSessionService.
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(finalURL))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(module.metadata?.displayTitle ?: module.fileName)
                    .setArtist(module.metadata?.contributor ?: "Unknown Artist")
                    .build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.fromFile(finalURL))
                    .build()
            )
            .build()

        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()

        loadAndCacheNavigationTree()
        _isLoading.value = false
    }

    fun togglePlayback() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun skipForward() {
        val controller = mediaController ?: return
        val duration = _selectedModule.value?.metadata?.durationMs ?: Long.MAX_VALUE
        val target = minOf(controller.currentPosition + forwardSkipMs, duration)
        controller.seekTo(target)
        _currentTimeMs.value = target
        syncActiveChapter(target)
    }

    fun skipBackward() {
        val controller = mediaController ?: return
        val target = maxOf(0L, controller.currentPosition - backwardSkipMs)
        controller.seekTo(target)
        _currentTimeMs.value = target
        syncActiveChapter(target)
    }

    fun seekToTime(ms: Long) {
        mediaController?.seekTo(ms)
        _currentTimeMs.value = ms
        syncActiveChapter(ms)
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
        mediaController?.seekTo(startMs)
    }

    fun stopPlayback() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _selectedModule.value = null
        _isPlaying.value = false
        _currentTimeMs.value = 0
        _activeText.value = ""
        audioRepo.stop()
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
        timeUpdateJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}