package com.spark.app.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Thin wrapper over Android system TTS. Reports readiness and speaking status. */
class TtsHelper(context: Context) {
    var isReady by mutableStateOf(false)
        private set
    var isSpeaking by mutableStateOf(false)
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val res = this.tts.setLanguage(Locale.US)
            isReady = res != TextToSpeech.LANG_MISSING_DATA &&
                res != TextToSpeech.LANG_NOT_SUPPORTED
            this.tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post { isSpeaking = true }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post { isSpeaking = false }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { isSpeaking = false }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post { isSpeaking = false }
                }
            })
        }
    }

    /** Returns false when the engine isn't ready yet (caller shows "voice loading"). */
    fun speak(text: String): Boolean {
        if (!isReady) return false
        isSpeaking = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spark-card")
        return true
    }

    fun stop() {
        if (!isReady) return
        tts.stop()
        isSpeaking = false
    }

    /** Toggles speech on or off. Returns true if speaking was started. */
    fun toggle(text: String): Boolean {
        return if (isSpeaking) {
            stop()
            false
        } else {
            speak(text)
        }
    }

    fun shutdown() {
        tts.shutdown()
        isSpeaking = false
    }
}
