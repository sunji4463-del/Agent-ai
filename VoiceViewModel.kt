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
import com.lia.assistant.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    private val _reply = MutableStateFlow("")
    val reply: StateFlow<String> = _reply

    private val _userSaid = MutableStateFlow("")
    val userSaid: StateFlow<String> = _userSaid

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var speech: SpeechRecognizerHelper? = null
    private var tts: TtsHelper? = null

    private fun ensureInit() {
        val ctx = getApplication<Application>()

        if (speech == null) {
            speech = SpeechRecognizerHelper(
                context = ctx,
                onPartial = { _partial.value = it },
                onResult = { text -> handleResult(text) },
                onError = { msg ->
                    if (_state.value == VoiceState.Listening) {
                        _error.value = msg
                        _state.value = VoiceState.Idle
                    }
                },
                onReadyForSpeech = { _state.value = VoiceState.Listening },
                onEndOfSpeech = {}
            )
        }

        if (tts == null) {
            tts = TtsHelper(ctx).apply {
                onStart = { _state.value = VoiceState.Speaking }
                onDone = { if (_state.value == VoiceState.Speaking) _state.value = VoiceState.Idle }
            }
        }
    }

    fun startListening() {
        ensureInit()
        _error.value = null
        _partial.value = ""
        _reply.value = ""
        _userSaid.value = ""
        _state.value = VoiceState.Listening
        speech?.start()
    }

    fun stopListening() {
        speech?.stop()
        if (_state.value == VoiceState.Listening) _state.value = VoiceState.Idle
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_state.value == VoiceState.Speaking) _state.value = VoiceState.Idle
    }

    fun dismissError() { _error.value = null }

    private fun handleResult(text: String) {
        if (text.isBlank()) {
            _state.value = VoiceState.Idle
            return
        }
        _userSaid.value = text
        _partial.value = ""
        _reply.value = ""
        _state.value = VoiceState.Thinking

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

                _reply.value = finalReply
                _state.value = VoiceState.Speaking
                tts?.speak(finalReply)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _state.value = VoiceState.Idle
            }
        }
    }

    override fun onCleared() {
        speech?.destroy()
        tts?.destroy()
        super.onCleared()
    }
}
