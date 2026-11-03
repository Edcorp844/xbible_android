package com.example.xbible.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.AudioEngine
import uniffi.xbible_engine.AudioModuleInfo
import uniffi.xbible_engine.AudioNode
import uniffi.xbible_engine.PlaybackState
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioBibleRepo"

@Singleton
class AudioBibleRepository @Inject constructor() {

    private var audioEngine: AudioEngine? = null

    /**
     * Initializes the AudioEngine.
     * Safe to call multiple times; will only initialize once.
     */
    fun initialize(): Result<Unit> {
        if (audioEngine != null) return Result.success(Unit)

        return try {
            audioEngine = AudioEngine()
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize AudioEngine", e)
            Result.failure(e)
        }
    }

    fun getEngine(): AudioEngine {
        return audioEngine ?: throw IllegalStateException(
            "AudioBibleRepository must be initialized before use. Call initialize() first."
        )
    }

    fun isInitialized(): Boolean = audioEngine != null

    fun getAudioModules(): List<AudioModuleInfo> {
        return getEngine().getAudioModules()
    }

    fun getNavigationTree(): AudioNode? {
        return getEngine().getNavigationTree()
    }

    fun getPlaybackState(): PlaybackState? {
        return getEngine().getPlaybackState()
    }

    fun getAudioModulesPath(): String {
        return getEngine().getAudioModulesPath()
    }

    fun seekToTime(ms: Long) {
        getEngine().seekToTime(ms)
    }

    fun seekToChapter(chapterId: String) {
        getEngine().seekToChapter(chapterId)
    }

    fun togglePlayback() {
        getEngine().togglePlayback()
    }

    fun stop() {
        getEngine().stop()
    }

    fun findActiveNodeId(timeMs: Long): String? {
        return getEngine().findActiveNodeId(timeMs)
    }

    fun loadAudioModule(filePath: String): ByteArray {
        return getEngine().loadAudioModule(filePath)
    }

    fun setRepeatMode(mode: uniffi.xbible_engine.RepeatMode) {
        getEngine().setRepeatMode(mode)
    }
}