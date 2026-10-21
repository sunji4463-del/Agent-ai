package com.lia.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lia.assistant.commands.CommandExecutor
import com.lia.assistant.commands.CommandParser
import com.lia.assistant.data.ChatMessage
import com.lia.assistant.data.ChatStorage
import com.lia.assistant.data.KeyStore
import com.lia.assistant.net.ApiClient
import com.lia.assistant.net.GroqMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        _messages.value = ChatStorage.load(getApplication())
    }

    fun send(text: String, useGroq: Boolean = true) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _sending.value) return

        val userMsg = ChatMessage(System.currentTimeMillis(), "user", trimmed)
        _messages.value = _messages.value + userMsg
        persist()
        _error.value = null
        _sending.value = true

        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val command = CommandParser.parse(trimmed)

                val replyText = if (command != null) {
                    withContext(Dispatchers.IO) { CommandExecutor.execute(ctx, command) }
                } else {
                    val apiKey = if (useGroq)
                        KeyStore.get(ctx, KeyStore.KEY_GROQ)
                    else
                        KeyStore.get(ctx, KeyStore.KEY_GEMINI)

                    if (apiKey.isNullOrBlank()) {
                        throw IllegalStateException(
                            "Add your ${if (useGroq) "Groq" else "Gemini"} API key in Settings."
                        )
                    }

                    val history = _messages.value
                        .filter { it.content.isNotBlank() }
                        .takeLast(20)
                        .map { GroqMessage(it.role, it.content) }

                    if (useGroq) ApiClient.askGroq(apiKey, history)
                    else ApiClient.askGemini(apiKey, history)
                }

                val finalText = replyText.ifBlank { "(empty reply)" }
                _messages.value = _messages.value + ChatMessage(
                    System.currentTimeMillis() + 1,
                    "assistant",
                    finalText
                )
                persist()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _sending.value = false
            }
        }
    }

    fun clearAll() {
        _messages.value = emptyList()
        ChatStorage.clear(getApplication())
    }

    fun dismissError() { _error.value = null }

    private fun persist() {
        ChatStorage.save(getApplication(), _messages.value)
    }
}
