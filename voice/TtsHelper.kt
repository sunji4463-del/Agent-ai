package com.lia.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsHelper(context: Context) {

    private var ready = false
    private var pendingText: String? = null
    private lateinit var tts: TextToSpeech

    var onStart: (() -> Unit)? = null
    var onDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupLanguage()
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onStart?.invoke() }
                    override fun onDone(utteranceId: String?) { onDone?.invoke() }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { onDone?.invoke() }
                })
                ready = true
                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        }
    }

    private fun setupLanguage() {
        val res = tts.setLanguage(Locale.getDefault())
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.US
        }
        tts.setSpeechRate(1.0f)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pendingText = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lia_${System.currentTimeMillis()}")
    }

    fun stop() { runCatching { tts.stop() } }

    fun destroy() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }
}
