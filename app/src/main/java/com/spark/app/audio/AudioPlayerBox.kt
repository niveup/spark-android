// Phase 6: app-side bridge to the AudioBookService MediaSession. One instance
// is remembered per AppNav composition; every audio card reads player.state
// and toggles through play()/toggle(). All MediaController calls stay on the
// main thread (Media3 requirement); state is emitted as a StateFlow.
package com.spark.app.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
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
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /** True after a failed load/connect (UI shows a soft error line). */
    val failed: Boolean = false
)

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
    private val _state = MutableStateFlow(AudioUi())
    val state: StateFlow<AudioUi> = _state.asStateFlow()

    private var controller: MediaController? = null
    /** Play request tapped before the session connected — replayed once ready. */
    private var pending: Pair<String, String>? = null

    /** Connects to the service and starts the position ticker. */
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
                    controller?.addListener(
                        object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                push { it.copy(playing = isPlaying) }
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                                    push { it.copy(playing = false) }
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                push { it.copy(playing = false, failed = true) }
                            }
                        }
                    )
                    push { it.copy(durationMs = controller?.duration ?: 0) }
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
        val c = controller
        if (c == null) {
            // Session not connected yet (first tap): remember and play once ready.
            pending = url to title
            _state.value = AudioUi(url = url, title = title)
            return
        }
        _state.value = AudioUi(url = url, title = title)
        c.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
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
            c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceIn(0, (controller?.duration ?: 0L).coerceAtLeast(0L)))
    }

    private fun startTicker() {
        scope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.mediaItemCount > 0) {
                    val d = c.duration // C.TIME_UNSET while buffering
                    push {
                        it.copy(
                            playing = c.isPlaying,
                            positionMs = c.currentPosition.coerceAtLeast(0),
                            durationMs = if (d > 0) d else it.durationMs
                        )
                    }
                }
                delay(500)
            }
        }
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
