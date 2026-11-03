package com.example.xbible.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.util.Log
import com.example.xbible.data.AudioBibleRepository
import uniffi.xbible_engine.AudioNode
import uniffi.xbible_engine.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AudioBiblePlayer"

class AudioBiblePlayer(
    context: Context,
    moduleFilePath: String,
    private val audioRepo: AudioBibleRepository
) {
    private var mediaPlayer: MediaPlayer? = null
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isInteracting: Boolean = false
    private var isPrepared: Boolean = false

    private val _playbackState = MutableStateFlow<PlaybackState?>(null)
    val playbackState: StateFlow<PlaybackState?> = _playbackState.asStateFlow()

    private val _navigationTree = MutableStateFlow<AudioNode?>(null)
    val navigationTree: StateFlow<AudioNode?> = _navigationTree.asStateFlow()

    init {
        try {
            val rawAudioBytes = audioRepo.loadAudioModule(moduleFilePath)
            _navigationTree.value = audioRepo.getNavigationTree()

            Log.d(TAG, "Loaded audio module: ${rawAudioBytes.size} bytes")

            if (rawAudioBytes.isEmpty()) {
                Log.e(TAG, "Rust engine returned 0 bytes for audio module")
            }

            val dataSource = ByteArrayMediaDataSource(rawAudioBytes)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(dataSource)

                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared. Duration: ${it.duration}ms")
                    isPrepared = true
                    // Auto-play once ready
                    it.start()
                }

                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    _playbackState.value = audioRepo.getPlaybackState()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    isPrepared = false
                    true
                }

                prepareAsync()
            }

            audioRepo.seekToTime(0)
            _playbackState.value = audioRepo.getPlaybackState()

            setupSyncLoop()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load secure .xba module: ${e.message}", e)
        }
    }

    fun play() {
        val player = mediaPlayer
        if (player == null || !isPrepared) {
            Log.w(TAG, "play() called but player not prepared yet")
            return
        }

        withInteractionLock {
            try {
                if (!player.isPlaying) {
                    player.start()
                }
                if (!(audioRepo.getPlaybackState()?.isPlaying ?: false)) {
                    audioRepo.togglePlayback()
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "play() failed", e)
            }
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        withInteractionLock {
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                if (audioRepo.getPlaybackState()?.isPlaying ?: true) {
                    audioRepo.togglePlayback()
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "pause() failed", e)
            }
        }
    }

    fun stop() {
        withInteractionLock {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
                isPrepared = false
            } catch (e: IllegalStateException) {
                Log.e(TAG, "stop() failed", e)
            }
            audioRepo.stop()
        }
    }

    fun seekTo(ms: Long) {
        val player = mediaPlayer
        if (player == null || !isPrepared) return

        withInteractionLock {
            try {
                player.seekTo(ms.toInt())
                audioRepo.seekToTime(ms)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "seekTo() failed", e)
            }
        }
    }

    private fun withInteractionLock(action: () -> Unit) {
        isInteracting = true
        action()
        executeTickSync()

        scope.launch {
            delay(100.milliseconds)
            isInteracting = false
        }
    }

    private fun setupSyncLoop() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                if (!isInteracting && isPrepared) {
                    executeTickSync()
                }
                delay(30.milliseconds)
            }
        }
    }

    private fun executeTickSync() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return

        try {
            val timeMs = player.currentPosition.toLong()

            val engineIsPlaying = audioRepo.getPlaybackState()?.isPlaying ?: false
            if (player.isPlaying != engineIsPlaying && !isInteracting) {
                audioRepo.togglePlayback()
            }

            audioRepo.seekToTime(timeMs)

            audioRepo.getPlaybackState()?.let { state ->
                _playbackState.value = state
            }
        } catch (e: IllegalStateException) {
            // Player may be in a transient state, ignore
        }
    }

    fun release() {
        syncJob?.cancel()
        try {
            mediaPlayer?.release()
        } catch (_: IllegalStateException) {}
        mediaPlayer = null
        isPrepared = false
    }
}

private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) return -1
        val bytesToRead = minOf(size.toLong(), (data.size - position)).toInt()
        System.arraycopy(data, position.toInt(), buffer, offset, bytesToRead)
        return bytesToRead
    }

    override fun getSize(): Long = data.size.toLong()

    override fun close() { /* No-op */ }
}