package com.lia.assistant.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.lia.assistant.commands.CommandExecutor
import com.lia.assistant.commands.CommandParser
import com.lia.assistant.commands.CommandResult
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiaVoiceService : Service() {

    companion object {
        const val ACTION_START = "com.lia.assistant.action.START_VOICE"
        const val ACTION_STOP = "com.lia.assistant.action.STOP_VOICE"
    }

    private var speech: SpeechRecognizerHelper? = null
    private var tts: TtsHelper? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var restartJob: Job? = null
    private var active = false
    private var muted = false
    private var stopRequested = false

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        VoiceServiceState.setRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            else -> {
                val notif = NotificationHelper.build(this, "Listening…")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(
                        NotificationHelper.NOTIF_ID,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NotificationHelper.NOTIF_ID, notif)
                }
                if (!active) beginLoop()
            }
        }
        return START_STICKY
    }

    private fun beginLoop() {
        active = true
        stopRequested = false
        VoiceStateHolder.reset()

        speech = SpeechRecognizerHelper(
            context = this,
            onPartial = {
                VoiceStateHolder.setPartial(it)
                if (VoiceStateHolder.state.value != VoiceState.Listening) {
                    VoiceStateHolder.setState(VoiceState.Listening)
                }
            },
            onResult = { text -> handleResult(text) },
            onError = { msg -> handleError(msg) },
            onReadyForSpeech = {
                VoiceStateHolder.setState(VoiceState.Listening)
                updateNotif("Listening…")
            },
            onEndOfSpeech = {}
        )

        tts = TtsHelper(this).apply {
            onStart = {
                VoiceStateHolder.setState(VoiceState.Speaking)
                updateNotif("Speaking…")
            }
            onDone = {
                if (stopRequested) {
                    stopEverything()
                    return@apply
                }
                VoiceStateHolder.setState(VoiceState.Idle)
                updateNotif("Listening…")
                restartListening()
            }
        }

        restartListening()
    }

    private fun restartListening() {
        if (!active || muted || stopRequested) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(400)
            if (!active || muted || stopRequested) return@launch
            runCatching { speech?.start() }
        }
    }

    private fun handleResult(text: String) {
        if (text.isBlank()) {
            restartListening()
            return
        }
        muted = true
        VoiceStateHolder.setUserSaid(text)
        VoiceStateHolder.setPartial("")
        VoiceStateHolder.setReply("")
        VoiceStateHolder.setState(VoiceState.Thinking)
        updateNotif("Thinking…")

        scope.launch {
            try {
                val command = CommandParser.parse(text)
                val replyText = if (command != null) {
                    if (command is CommandResult.StopListening) {
                        stopRequested = true
                    }
                    withContext(Dispatchers.IO) {
                        val r = CommandExecutor.execute(this@LiaVoiceService, command)
                        val userMsg = ChatMessage(System.currentTimeMillis(), "user", text)
                        val asstMsg = ChatMessage(System.currentTimeMillis() + 1, "assistant", r)
                        val h = ChatStorage.load(this@LiaVoiceService) + userMsg + asstMsg
                        ChatStorage.save(this@LiaVoiceService, h)
                        r
                    }
                } else {
                    withContext(Dispatchers.IO) { callApi(text) }
                }

                VoiceStateHolder.setReply(replyText)
                VoiceStateHolder.setState(VoiceState.Speaking)
                updateNotif("Speaking…")
                tts?.speak(replyText)
                muted = false

                if (stopRequested) {
                    // Wait for TTS to finish then stop (onDone will call stopEverything)
                }
            } catch (e: Exception) {
                VoiceStateHolder.setError(e.message ?: "Unknown error")
                VoiceStateHolder.setState(VoiceState.Idle)
                updateNotif("Listening…")
                muted = false
                restartListening()
            }
        }
    }

    private fun callApi(text: String): String {
        val userMsg = ChatMessage(System.currentTimeMillis(), "user", text)
        val history = ChatStorage.load(this) + userMsg
        ChatStorage.save(this, history)

        val groqKey = KeyStore.get(this, KeyStore.KEY_GROQ)
        val geminiKey = KeyStore.get(this, KeyStore.KEY_GEMINI)
        val recent = history.takeLast(20).map { GroqMessage(it.role, it.content) }

        val replyText = when {
            !groqKey.isNullOrBlank() -> ApiClient.askGroq(groqKey, recent)
            !geminiKey.isNullOrBlank() -> ApiClient.askGemini(geminiKey, recent)
            else -> throw IllegalStateException("No API key. Add it in Settings.")
        }

        val finalReply = replyText.ifBlank { "Sorry, I couldn't answer." }
        val assistantMsg = ChatMessage(System.currentTimeMillis() + 1, "assistant", finalReply)
        ChatStorage.save(this, history + assistantMsg)
        return finalReply
    }

    private fun handleError(msg: String) {
        VoiceStateHolder.setError(msg)
        VoiceStateHolder.setState(VoiceState.Idle)
        restartListening()
    }

    private fun updateNotif(text: String) {
        runCatching {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.notify(NotificationHelper.NOTIF_ID, NotificationHelper.build(this, text))
        }
    }

    private fun stopEverything() {
        active = false
        muted = false
        stopRequested = false
        restartJob?.cancel()
        runCatching { speech?.destroy() }
        runCatching { tts?.destroy() }
        speech = null
        tts = null
        VoiceStateHolder.setState(VoiceState.Idle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        active = false
        stopRequested = false
        restartJob?.cancel()
        runCatching { speech?.destroy() }
        runCatching { tts?.destroy() }
        VoiceServiceState.setRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
