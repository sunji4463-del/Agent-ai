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
        tts.setSpeechRate(1.0f)
    }

    private fun localeForText(text: String): Locale {
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        val hasLatin = text.any { it in 'a'..'z' || it in 'A'..'Z' }

        return when {
            hasBengali && !hasLatin -> Locale("bn", "BD")
            hasBengali && hasLatin -> Locale("bn", "BD")
            hasLatin -> Locale.US
            else -> Locale.getDefault()
        }
    }

    private fun applyLocale(locale: Locale) {
        val res = tts.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pendingText = text
            return
        }
        applyLocale(localeForText(text))
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lia_${System.currentTimeMillis()}")
    }

    fun stop() { runCatching { tts.stop() } }

    fun destroy() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }
}
