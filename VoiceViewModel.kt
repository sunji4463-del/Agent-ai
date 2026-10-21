package com.lia.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lia.assistant.data.ChatMessage
import com.lia.assistant.data.ChatStorage
import com.lia.assistant.data.KeyStore
import com.lia.assistant.net.ApiClient
import com.lia.assistant.net.GroqMessage
import com.lia.assistant.voice.SpeechRecognizerHelper
import com.lia.assistant.voice.TtsHelper
import com.lia.assistant.voice.VoiceServiceState
import com.lia.assistant.voice.VoiceState
import com.lia.assistant.voice.VoiceStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceViewModel(app: Application) : AndroidViewModel(app) {

    val state = VoiceStateHolder.state
    val partial = VoiceStateHolder.partial
    val reply = VoiceStateHolder.reply
    val userSaid = VoiceStateHolder.userSaid
    val error = VoiceStateHolder.error

    private var speech: SpeechRecognizerHelper? = null
    private var tts: TtsHelper? = null

    private fun ensureInit() {
        val ctx = getApplication<Application>()

        if (speech == null) {
            speech = SpeechRecognizerHelper(
                context = ctx,
                onPartial = { VoiceStateHolder.setPartial(it) },
                onResult = { text -> handleResult(text) },
                onError = { msg ->
                    if (VoiceStateHolder.state.value == VoiceState.Listening) {
                        VoiceStateHolder.setError(msg)
                        VoiceStateHolder.setState(VoiceState.Idle)
                    }
                },
                onReadyForSpeech = { VoiceStateHolder.setState(VoiceState.Listening) },
                onEndOfSpeech = {}
            )
        }

        if (tts == null) {
            tts = TtsHelper(ctx).apply {
                onStart = { VoiceStateHolder.setState(VoiceState.Speaking) }
                onDone = {
                    if (VoiceStateHolder.state.value == VoiceState.Speaking) {
                        VoiceStateHolder.setState(VoiceState.Idle)
                    }
                }
            }
        }
    }

    fun startListening() {
        if (VoiceServiceState.isRunning.value) return
        ensureInit()
        VoiceStateHolder.reset()
        VoiceStateHolder.setState(VoiceState.Listening)
        speech?.start()
    }

    fun stopListening() {
        speech?.stop()
        if (VoiceStateHolder.state.value == VoiceState.Listening) {
            VoiceStateHolder.setState(VoiceState.Idle)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        if (VoiceStateHolder.state.value == VoiceState.Speaking) {
            VoiceStateHolder.setState(VoiceState.Idle)
        }
    }

    fun dismissError() { VoiceStateHolder.setError(null) }

    private fun handleResult(text: String) {
        if (text.isBlank()) {
            VoiceStateHolder.setState(VoiceState.Idle)
            return
        }
        VoiceStateHolder.setUserSaid(text)
        VoiceStateHolder.setPartial("")
        VoiceStateHolder.setReply("")
        VoiceStateHolder.setState(VoiceState.Thinking)

        viewModelScope.launch {
            val ctx = getApplication<Application>()
            try {
                val userMsg = ChatMessage(System.currentTimeMillis(), "user", text)
                val history = withContext(Dispatchers.IO) {
                    val h = ChatStorage.load(ctx) + userMsg
                    ChatStorage.save(ctx, h)
                    h
                }

                val groqKey = KeyStore.get(ctx, KeyStore.KEY_GROQ)
                val geminiKey = KeyStore.get(ctx, KeyStore.KEY_GEMINI)
                val recent = history.takeLast(20).map { GroqMessage(it.role, it.content) }

                val replyText = when {
                    !groqKey.isNullOrBlank() -> ApiClient.askGroq(groqKey, recent)
                    !geminiKey.isNullOrBlank() -> ApiClient.askGemini(geminiKey, recent)
                    else -> throw IllegalStateException("No API key. Open Settings first.")
                }

                val finalReply = replyText.ifBlank { "I couldn't think of an answer." }
                val assistantMsg = ChatMessage(System.currentTimeMillis() + 1, "assistant", finalReply)
                withContext(Dispatchers.IO) {
                    ChatStorage.save(ctx, history + assistantMsg)
                }

                VoiceStateHolder.setReply(finalReply)
                VoiceStateHolder.setState(VoiceState.Speaking)
                tts?.speak(finalReply)
            } catch (e: Exception) {
                VoiceStateHolder.setError(e.message ?: "Unknown error")
                VoiceStateHolder.setState(VoiceState.Idle)
            }
        }
    }

    override fun onCleared() {
        speech?.destroy()
        tts?.destroy()
        super.onCleared()
    }
}
