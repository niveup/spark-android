// Phase 6: app-side bridge to the AudioBookService MediaSession. One instance
// is remembered per AppNav composition; every audio card reads player.state
// and toggles through play()/toggle(). All MediaController calls stay on the
// main thread (Media3 requirement); state is emitted as a StateFlow.
package com.spark.app.audio

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** UI-facing playback state; one stream plays at a time. */
data class AudioUi(
    /** Stream identity of the current item (the card's audioUrl). */
    val url: String? = null,
    val title: String? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1f,
    val sleepTimerRemainingMs: Long = 0,
    /** True after a failed load/connect (UI shows a soft error line). */
    val failed: Boolean = false
)

internal object AudioSessionCommands {
    const val SET_SLEEP_TIMER = "com.spark.app.audio.SET_SLEEP_TIMER"
    const val EXTRA_END_AT_EPOCH_MS = "sleep_timer_end_at_epoch_ms"
    const val PREFERENCES = "spark_audio"
    const val SLEEP_END_AT_KEY = "sleep_end_at_epoch_ms"
}

/** "8:31" from milliseconds; hours show when needed (1:04:12). */
fun formatMinSec(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) + 500) / 1000
    val h = totalSec / 3600
    val s = (totalSec % 60).toString().padStart(2, '0')
    return if (h > 0) {
        val m = ((totalSec % 3600) / 60).toString().padStart(2, '0')
        "$h:$m:$s"
    } else {
        "${totalSec / 60}:$s"
    }
}

class AudioPlayerBox(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioPreferences = appContext.getSharedPreferences(
        AudioSessionCommands.PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val _state = MutableStateFlow(AudioUi())
    val state: StateFlow<AudioUi> = _state.asStateFlow()

    private var controller: MediaController? = null
    /** Play request tapped before the session connected — replayed once ready. */
    private var pending: Pair<String, String>? = null
    private var pendingPlaybackSpeed: Float? = null

    /** Connects to the service and starts the position ticker. */
    @OptIn(markerClass = [UnstableApi::class])
    fun start() {
        if (controller != null) return
        scope.launch {
            val token = SessionToken(appContext, ComponentName(appContext, AudioBookService::class.java))
            val future = MediaController.Builder(appContext, token).buildAsync()
            future.addListener(
                {
                    controller = try {
                        future.get()
                    } catch (_: Exception) {
                        _state.value = AudioUi(failed = true)
                        null
                    } ?: return@addListener
                    val connectedController = controller ?: return@addListener
                    connectedController.addListener(
                        object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                push { it.copy(playing = isPlaying, buffering = false) }
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                push {
                                    it.copy(
                                        buffering = playbackState == Player.STATE_BUFFERING,
                                        playing = if (
                                            playbackState == Player.STATE_IDLE ||
                                            playbackState == Player.STATE_ENDED
                                        ) false else it.playing
                                    )
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                push { it.copy(playing = false, buffering = false, failed = true) }
                            }

                            override fun onPlaybackParametersChanged(
                                playbackParameters: androidx.media3.common.PlaybackParameters
                            ) {
                                push { it.copy(playbackSpeed = playbackParameters.speed) }
                            }

                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (mediaItem == null) {
                                    if (connectedController.mediaItemCount == 0) {
                                        _state.value = AudioUi(
                                            sleepTimerRemainingMs = readSleepTimerRemainingMs()
                                        )
                                    }
                                    return
                                }
                                push {
                                    it.copy(
                                        url = mediaItem.localConfiguration?.uri?.toString() ?: it.url,
                                        title = mediaItem.mediaMetadata.title?.toString() ?: it.title,
                                        failed = false
                                    )
                                }
                            }
                        }
                    )
                    val currentMediaItem = connectedController.currentMediaItem
                    if (currentMediaItem != null) {
                        push {
                            it.copy(
                                url = currentMediaItem.localConfiguration?.uri?.toString() ?: it.url,
                                title = currentMediaItem.mediaMetadata.title?.toString() ?: it.title,
                                playing = connectedController.isPlaying,
                                buffering = connectedController.playbackState == Player.STATE_BUFFERING,
                                positionMs = connectedController.currentPosition.coerceAtLeast(0),
                                durationMs = connectedController.duration.coerceAtLeast(0),
                                playbackSpeed = connectedController.playbackParameters.speed,
                                sleepTimerRemainingMs = readSleepTimerRemainingMs()
                            )
                        }
                    }
                    applyPendingPlaybackSpeed(connectedController)
                    sendSleepTimerCommand(
                        connectedController,
                        audioPreferences.getLong(AudioSessionCommands.SLEEP_END_AT_KEY, 0L)
                    )
                    pending?.let { (url, title) ->
                        pending = null
                        playUrl(url, title)
                    }
                    startTicker()
                },
                { it.run() } // direct executor: future is done by now
            )
        }
    }

    /** Streams one chapter; replaces anything that played before. */
    fun playUrl(url: String, title: String) {
        val resolvedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "${com.spark.app.BuildConfig.API_BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
        }

        val currentUrl = _state.value.url
        val isSameTrack = !currentUrl.isNullOrBlank() && (
            currentUrl == resolvedUrl || currentUrl == url ||
            currentUrl.substringAfterLast("/").substringBefore("?") == resolvedUrl.substringAfterLast("/").substringBefore("?")
        )

        val c = controller
        if (isSameTrack && c != null) {
            // Same track already loaded: do not reset position to 0ms; resume if paused
            if (!c.isPlaying) {
                if (c.playbackState == Player.STATE_ENDED) c.seekTo(0L)
                c.play()
            }
            return
        }

        if (c == null) {
            // Session not connected yet (first tap): remember and play once ready.
            pending = resolvedUrl to title
            _state.value = AudioUi(
                url = resolvedUrl,
                title = title,
                buffering = true,
                playbackSpeed = _state.value.playbackSpeed,
                sleepTimerRemainingMs = readSleepTimerRemainingMs()
            )
            return
        }
        _state.value = AudioUi(
            url = resolvedUrl,
            title = title,
            buffering = true,
            playbackSpeed = c.playbackParameters.speed,
            sleepTimerRemainingMs = readSleepTimerRemainingMs()
        )
        c.setMediaItem(
            MediaItem.Builder()
                .setUri(resolvedUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist("SPARK · LibriVox")
                        .build()
                )
                .build()
        )
        c.prepare()
        c.play()
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0L)
            c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        val duration = c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0, duration)
        c.seekTo(target)
        push { it.copy(positionMs = target) }
    }

    fun seekBy(deltaMs: Long) {
        seekTo((_state.value.positionMs + deltaMs).coerceAtLeast(0L))
    }

    fun setPlaybackSpeed(speed: Float) {
        val target = speed.coerceIn(0.75f, 2f)
        pendingPlaybackSpeed = target
        push { it.copy(playbackSpeed = target) }
        controller?.let(::applyPendingPlaybackSpeed)
    }

    fun setSleepTimer(minutes: Int?) {
        val endAtEpochMs = minutes
            ?.coerceIn(1, 120)
            ?.let { System.currentTimeMillis() + it * 60_000L }
            ?: 0L
        audioPreferences.edit { putLong(AudioSessionCommands.SLEEP_END_AT_KEY, endAtEpochMs) }
        push { it.copy(sleepTimerRemainingMs = (endAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)) }
        controller?.let { sendSleepTimerCommand(it, endAtEpochMs) }
    }

    fun stopAndClear() {
        pending = null
        setSleepTimer(null)
        controller?.let { c ->
            c.stop()
            c.clearMediaItems()
        }
        _state.value = AudioUi()
    }

    private fun startTicker() {
        scope.launch {
            while (isActive) {
                val c = controller
                val sleepRemainingMs = readSleepTimerRemainingMs()
                if (c != null && c.mediaItemCount > 0) {
                    val d = c.duration // C.TIME_UNSET while buffering
                    push {
                        it.copy(
                            playing = c.isPlaying,
                            buffering = c.playbackState == Player.STATE_BUFFERING,
                            positionMs = c.currentPosition.coerceAtLeast(0),
                            durationMs = if (d > 0) d else it.durationMs,
                            playbackSpeed = c.playbackParameters.speed,
                            sleepTimerRemainingMs = sleepRemainingMs
                        )
                    }
                } else if (_state.value.sleepTimerRemainingMs != sleepRemainingMs) {
                    push { it.copy(sleepTimerRemainingMs = sleepRemainingMs) }
                }
                delay(
                    when {
                        c?.isPlaying == true -> 250
                        sleepRemainingMs > 0L -> 1_000
                        else -> 5_000
                    }
                )
            }
        }
    }

    private fun applyPendingPlaybackSpeed(c: MediaController) {
        val speed = pendingPlaybackSpeed ?: return
        if (!c.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) return
        c.setPlaybackSpeed(speed)
        pendingPlaybackSpeed = null
    }

    private fun sendSleepTimerCommand(c: MediaController, endAtEpochMs: Long) {
        val args = Bundle().apply {
            putLong(AudioSessionCommands.EXTRA_END_AT_EPOCH_MS, endAtEpochMs)
        }
        c.sendCustomCommand(
            androidx.media3.session.SessionCommand(AudioSessionCommands.SET_SLEEP_TIMER, Bundle.EMPTY),
            args
        )
    }

    private fun readSleepTimerRemainingMs(): Long {
        val endAtEpochMs = audioPreferences.getLong(AudioSessionCommands.SLEEP_END_AT_KEY, 0L)
        return (endAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun push(transform: (AudioUi) -> AudioUi) {
        _state.value = transform(_state.value)
    }

    fun release() {
        scope.cancel()
        // Only the app-side controller is released — playback (and the media
        // notification) keeps running in the service, which is the point of
        // Phase 6: put headphones on, swipe the app away, keep listening.
        controller?.release()
        controller = null
        pending = null
    }

}
