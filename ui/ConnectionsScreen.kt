package com.lia.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lia.assistant.data.KeyStore
import com.lia.assistant.service.AccessibilityController
import com.lia.assistant.voice.VoiceServiceState

private data class ConnectionItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val active: Boolean
)

@Composable
fun ConnectionsScreen() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val bgRunning by VoiceServiceState.isRunning.collectAsState()

    var hasGroq by remember { mutableStateOf(false) }
    var hasGemini by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(false) }
    var hasTts by remember { mutableStateOf(false) }
    var hasYt by remember { mutableStateOf(false) }
    var hasWa by remember { mutableStateOf(false) }
    var hasA11y by remember { mutableStateOf(false) }

    fun refresh() {
        hasGroq = !KeyStore.get(ctx, KeyStore.KEY_GROQ).isNullOrBlank()
        hasGemini = !KeyStore.get(ctx, KeyStore.KEY_GEMINI).isNullOrBlank()
        hasMic = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        hasA11y = AccessibilityController.isEnabled(ctx)
        hasYt = try {
            ctx.packageManager.getLaunchIntentForPackage("com.google.android.youtube") != null
        } catch (e: Exception) { false }
        hasWa = try {
            ctx.packageManager.getLaunchIntentForPackage("com.whatsapp") != null
        } catch (e: Exception) { false }
        hasTts = try {
            val intent = android.content.Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
            ctx.packageManager.queryIntentServices(intent, 0).isNotEmpty()
        } catch (e: Exception) { false }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(bgRunning) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val items = listOf(
        ConnectionItem("Groq", "Fast LLM (chat + voice)", Icons.Filled.Bolt, hasGroq),
        ConnectionItem("Gemini", "Google's AI model", Icons.Filled.AutoAwesome, hasGemini),
        ConnectionItem("Voice Input", "Speech recognition", Icons.Filled.Mic, hasMic),
        ConnectionItem("Voice Output", "Text-to-speech engine", Icons.Filled.VolumeUp, hasTts),
        ConnectionItem("Background Voice", "Foreground service", Icons.Filled.PlayCircle, bgRunning),
        ConnectionItem("YouTube", "Play music/videos", Icons.Filled.PlayArrow, hasYt),
        ConnectionItem("WhatsApp", "Send messages", Icons.Filled.Chat, hasWa),
        ConnectionItem("Accessibility", "Tap · type · lock", Icons.Filled.SettingsAccessibility, hasA11y)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        LiaTopBar("Connections", "Live status of LIA's features")

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF15151F))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C5CFF).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = Color(0xFF7C5CFF))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        item.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                StatusPill(text = if (item.active) "Ready" else "Off", active = item.active)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
