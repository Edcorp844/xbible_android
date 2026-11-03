package com.example.xbible.core.audio

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.xbible.data.AudioBibleRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private const val TAG = "PlaybackService"

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioRepo: AudioBibleRepository

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    private val pendingAudio: ConcurrentHashMap<String, ByteArray> = ConcurrentHashMap()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: building ExoPlayer + MediaSession")

        // ── 1. Custom MediaSource.Factory ─────────────────────────────
        val customMediaSourceFactory = object : MediaSource.Factory {

            override fun getSupportedTypes(): IntArray {
                return intArrayOf(C.CONTENT_TYPE_OTHER)
            }

            override fun setDrmSessionManagerProvider(
                drmSessionManagerProvider: DrmSessionManagerProvider
            ): MediaSource.Factory = this

            override fun setLoadErrorHandlingPolicy(
                loadErrorHandlingPolicy: LoadErrorHandlingPolicy
            ): MediaSource.Factory = this

            @OptIn(UnstableApi::class)
            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                val key = mediaItem.localConfiguration?.uri?.toString()
                val bytes = key?.let { pendingAudio.remove(it) }

                val dataSourceFactory: DataSource.Factory = if (bytes != null) {
                    Log.d(TAG, "Factory: serving ${bytes.size} bytes for $key")
                    DataSource.Factory { ByteArrayDataSource(bytes) }
                } else {
                    Log.d(TAG, "Factory: no cached bytes for $key, using DefaultDataSource")
                    DefaultDataSource.Factory(this@PlaybackService)
                }

                return ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }

        // ── 2. Build ExoPlayer ────────────────────────────────────────
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(customMediaSourceFactory)
            .build()
            .also { it.addAnalyticsListener(EventLogger()) }

        // ── 3. Build MediaSession ─────────────────────────────────────
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(object : MediaSession.Callback {

                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    Log.d(TAG, "onConnect: controller=${controller.packageName}")
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                        .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                        .build()
                }

                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    Log.d(TAG, "onPlaybackResumption called")
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(mutableListOf(), 0, 0L)
                    )
                }

                @OptIn(UnstableApi::class)
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    Log.d(TAG, "onAddMediaItems: count=${mediaItems.size}")

                    val incoming = mediaItems.firstOrNull()
                    val filePath = incoming?.requestMetadata?.mediaUri?.path
                    val title = incoming?.mediaMetadata?.title?.toString() ?: "Unknown Title"
                    val artist = incoming?.mediaMetadata?.artist?.toString() ?: "Unknown Artist"

                    Log.d(TAG, "  filePath=$filePath")

                    if (filePath == null) {
                        Log.w(TAG, "  filePath is null, returning empty")
                        return Futures.immediateFuture(mutableListOf<MediaItem>())
                    }

                    val rawBytes = audioRepo.loadAudioModule(filePath)
                    Log.d(TAG, "  Engine returned ${rawBytes.size} bytes")

                    if (rawBytes.isEmpty()) {
                        Log.e(TAG, "  Engine returned EMPTY bytes")
                        return Futures.immediateFuture(mutableListOf<MediaItem>())
                    }

                    // FIX: Evict oldest entry if the map grows too large.
                    // Rapid tapping on modules would otherwise leak bytes
                    // for any URI the factory never got around to serving.
                    if (pendingAudio.size > 5) {
                        pendingAudio.keys.firstOrNull()?.let { pendingAudio.remove(it) }
                    }

                    val playbackUri = "xbible://audio/${System.currentTimeMillis()}".toUri()
                    pendingAudio[playbackUri.toString()] = rawBytes
                    Log.d(TAG, "  stashed bytes under $playbackUri")

                    val playbackItem = MediaItem.Builder()
                        .setUri(playbackUri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .build()
                        )
                        .build()

                    return Futures.immediateFuture(mutableListOf(playbackItem))
                }
            })
            .build()

        Log.d(TAG, "onCreate: MediaSession built")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "onGetSession called from ${controllerInfo.packageName}")
        return mediaSession
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer?.release()
        exoPlayer = null
        pendingAudio.clear()

        // FIX: Clean up any temp audio files left over from previous runs
        // (harmless today since the current path uses in-memory bytes, but
        // protects if the pipeline is ever switched to file-based playback).
        cacheDir.listFiles { file ->
            file.name.startsWith("audio_") || file.name.startsWith("playback_")
        }?.forEach { it.delete() }

        super.onDestroy()
    }
}