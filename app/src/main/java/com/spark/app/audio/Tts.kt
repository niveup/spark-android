package com.spark.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Thin wrapper over Android system TTS. Reports readiness so UI can show status. */
class TtsHelper(context: Context) {
    var isReady by mutableStateOf(false)
        private set
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val res = this.tts.setLanguage(Locale.US)
            isReady = res != TextToSpeech.LANG_MISSING_DATA &&
                res != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    /** Returns false when the engine isn't ready yet (caller shows "voice loading"). */
    fun speak(text: String): Boolean {
        if (!isReady) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spark-card")
        return true
    }

    fun stop() {
        if (!isReady) return
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}
