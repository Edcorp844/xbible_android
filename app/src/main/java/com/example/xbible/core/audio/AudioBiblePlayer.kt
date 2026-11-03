package com.example.xbible.core.audio

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
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
    private var exoPlayer: ExoPlayer? = null
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isInteracting: Boolean = false
    private var isPrepared: Boolean = false

    private val _playbackState = MutableStateFlow<PlaybackState?>(null)
    val playbackState: StateFlow<PlaybackState?> = _playbackState.asStateFlow()

    private val _navigationTree = MutableStateFlow<AudioNode?>(null)
    val navigationTree: StateFlow<AudioNode?> = _navigationTree.asStateFlow()

    init {
        initializePlayer(context, moduleFilePath)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initializePlayer(context: Context, moduleFilePath: String) {
        try {
            val rawAudioBytes = audioRepo.loadAudioModule(moduleFilePath)
            _navigationTree.value = audioRepo.getNavigationTree()

            Log.d(TAG, "Loaded audio module: ${rawAudioBytes.size} bytes")

            if (rawAudioBytes.isEmpty()) {
                Log.e(TAG, "Rust engine returned 0 bytes for audio module")
            }

            // 1. Create a DataSource from the raw bytes
            val dataSource = ByteArrayDataSource(rawAudioBytes)

            // 2. Create a MediaSource using the DataSource
            // We use a dummy URI since the data is already in memory.
            val mediaSource = ProgressiveMediaSource.Factory { dataSource }
                .createMediaSource(MediaItem.fromUri("asset:///audio.xba"))

            // 3. Build ExoPlayer and set the MediaSource
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setMediaSource(mediaSource)
                prepare()

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            Log.d(TAG, "ExoPlayer READY. Duration: ${duration}ms")
                            isPrepared = true
                            play()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (!isInteracting) {
                            val engineIsPlaying = audioRepo.getPlaybackState()?.isPlaying ?: false
                            if (isPlaying != engineIsPlaying) {
                                audioRepo.togglePlayback()
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "ExoPlayer error", error)
                        isPrepared = false
                    }
                })
            }

            audioRepo.seekToTime(0)
            _playbackState.value = audioRepo.getPlaybackState()

            setupSyncLoop()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load secure .xba module: ${e.message}", e)
        }
    }

    fun play() {
        val player = exoPlayer
        if (player == null || !isPrepared) {
            Log.w(TAG, "play() ignored: not ready")
            return
        }

        withInteractionLock {
            try {
                if (!player.isPlaying) {
                    player.play()
                    Log.d(TAG, "play(): started, isPlaying=${player.isPlaying}")
                }
                if (!(audioRepo.getPlaybackState()?.isPlaying ?: false)) {
                    audioRepo.togglePlayback()
                }
            } catch (e: Exception) {
                Log.e(TAG, "play() failed", e)
            }
        }
    }

    fun pause() {
        val player = exoPlayer ?: return
        withInteractionLock {
            try {
                if (player.isPlaying) player.pause()
                if (audioRepo.getPlaybackState()?.isPlaying ?: true) {
                    audioRepo.togglePlayback()
                }
            } catch (e: Exception) {
                Log.e(TAG, "pause() failed", e)
            }
        }
    }

    fun stop() {
        withInteractionLock {
            try {
                exoPlayer?.stop()
                exoPlayer?.release()
                exoPlayer = null
                isPrepared = false
            } catch (e: Exception) {
                Log.e(TAG, "stop() failed", e)
            }
            audioRepo.stop()
        }
    }

    fun seekTo(ms: Long) {
        val player = exoPlayer
        if (player == null || !isPrepared) return

        withInteractionLock {
            try {
                player.seekTo(ms)
                audioRepo.seekToTime(ms)
            } catch (e: Exception) {
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
        val player = exoPlayer ?: return
        if (!isPrepared) return

        try {
            val timeMs = player.currentPosition

            val engineIsPlaying = audioRepo.getPlaybackState()?.isPlaying ?: false
            if (player.isPlaying != engineIsPlaying && !isInteracting) {
                audioRepo.togglePlayback()
            }

            audioRepo.seekToTime(timeMs)

            audioRepo.getPlaybackState()?.let { state ->
                _playbackState.value = state.copy(currentTimeMs = timeMs)
            }
        } catch (e: Exception) {
            // Player may be in a transient state, ignore
        }
    }

    fun release() {
        syncJob?.cancel()
        try {
            exoPlayer?.release()
        } catch (_: Exception) {}
        exoPlayer = null
        isPrepared = false
    }
}