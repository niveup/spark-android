// Phase 6: audiobook playback service. Hosts one MediaSession backed by an
// ExoPlayer streaming from remote URLs (LibriVox -> Archive.org). Playback
// survives the app in the background; volume/notification + lock-screen
// transport controls are provided by Media3's session defaults.
// Streams only — no audio is stored on the device (AGENTS.md 1.7).
package com.spark.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class AudioBookService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val timerHandler = Handler(Looper.getMainLooper())
    private var sleepTimer: Runnable? = null
    private val audioPreferences by lazy {
        getSharedPreferences(AudioSessionCommands.PREFERENCES, Context.MODE_PRIVATE)
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.ConnectionResult> {
            val availableSessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().apply {
                    if (controller.packageName == packageName) {
                        add(SessionCommand(AudioSessionCommands.SET_SLEEP_TIMER, Bundle.EMPTY))
                    }
                }.build()
            val result = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(availableSessionCommands)
                .build()
            return Futures.immediateFuture(result)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == AudioSessionCommands.SET_SLEEP_TIMER &&
                controller.packageName == packageName
            ) {
                setSleepTimerEndAt(args.getLong(AudioSessionCommands.EXTRA_END_AT_EPOCH_MS, 0L))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val cacheDataSourceFactory = SparkAudioCache.createDataSourceFactory(this)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true // pause on audio focus loss (calls, other apps)
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones unplug
            .setWakeMode(C.WAKE_MODE_NETWORK) // keep streaming with screen off
            .build()
        this.player = player
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
        val savedEndAt = audioPreferences.getLong(AudioSessionCommands.SLEEP_END_AT_KEY, 0L)
        if (savedEndAt > 0L) setSleepTimerEndAt(savedEndAt)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop only when nothing is queued/playing — background listening
        // must survive swiping the app away (Phase 6 requirement).
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            clearSleepTimer()
            clearAudioCache()
            stopSelf()
        }
    }

    override fun onDestroy() {
        clearSleepTimer()
        clearAudioCache()
        SparkAudioCache.release()
        mediaSession?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun clearAudioCache() {
        try {
            listOf(cacheDir, codeCacheDir, externalCacheDir).filterNotNull().forEach { dir ->
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && (
                        file.extension.lowercase() in listOf("mp3", "m4a", "webm", "ogg", "wav", "aac") ||
                        file.name.contains("media", ignoreCase = true) ||
                        file.name.contains("exoplayer", ignoreCase = true)
                    )) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun setSleepTimerEndAt(endAtEpochMs: Long) {
        sleepTimer?.let(timerHandler::removeCallbacks)
        sleepTimer = null
        if (endAtEpochMs <= 0L) {
            audioPreferences.edit { remove(AudioSessionCommands.SLEEP_END_AT_KEY) }
            return
        }

        audioPreferences.edit { putLong(AudioSessionCommands.SLEEP_END_AT_KEY, endAtEpochMs) }
        val pauseWhenTimerEnds = Runnable {
            val currentEndAt = audioPreferences.getLong(AudioSessionCommands.SLEEP_END_AT_KEY, 0L)
            if (currentEndAt == endAtEpochMs) {
                player?.pause()
                audioPreferences.edit { remove(AudioSessionCommands.SLEEP_END_AT_KEY) }
                sleepTimer = null
            }
        }
        sleepTimer = pauseWhenTimerEnds
        timerHandler.postDelayed(
            pauseWhenTimerEnds,
            (endAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
        )
    }

    private fun clearSleepTimer() {
        sleepTimer?.let(timerHandler::removeCallbacks)
        sleepTimer = null
        audioPreferences.edit { remove(AudioSessionCommands.SLEEP_END_AT_KEY) }
    }
}
