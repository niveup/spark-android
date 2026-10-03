package com.spark.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Thin wrapper over Android system TTS. Silently no-ops until the engine is ready. */
class TtsHelper(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val res = this.tts.setLanguage(Locale.US)
            ready = res != TextToSpeech.LANG_MISSING_DATA &&
                res != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spark-card")
    }

    fun stop() {
        if (!ready) return
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}
