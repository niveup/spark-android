// Phase 6: audiobook playback service. Hosts one MediaSession backed by an
// ExoPlayer streaming from remote URLs (LibriVox -> Archive.org). Playback
// survives the app in the background; volume/notification + lock-screen
// transport controls are provided by Media3's session defaults.
// Streams only — no audio is stored on the device (AGENTS.md 1.7).
package com.spark.app.audio

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AudioBookService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
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
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop only when nothing is queued/playing — background listening
        // must survive swiping the app away (Phase 6 requirement).
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
