package com.lia.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lia.assistant.VoiceViewModel
import com.lia.assistant.service.ServiceController
import com.lia.assistant.voice.VoiceServiceState
import com.lia.assistant.voice.VoiceState

@Composable
fun VoiceScreen(navController: NavController, vm: VoiceViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val partial by vm.partial.collectAsState()
    val reply by vm.reply.collectAsState()
    val userSaid by vm.userSaid.collectAsState()
    val error by vm.error.collectAsState()
    val bgRunning by VoiceServiceState.isRunning.collectAsState()

    val ctx = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var askedOnce by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted && !bgRunning) vm.startListening()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !askedOnce) {
            askedOnce = true
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (hasPermission && !VoiceServiceState.isRunning.value) {
            vm.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!VoiceServiceState.isRunning.value) {
                vm.stopListening()
                vm.stopSpeaking()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Voice", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (bgRunning) "Running in background" else "Speak with LIA",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            TextButton(
                onClick = {
                    if (bgRunning) {
                        ServiceController.stop(ctx)
                    } else {
                        if (!hasPermission) {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            vm.stopListening()
                            vm.stopSpeaking()
                            ServiceController.start(ctx)
                        }
                    }
                }
            ) {
                Text(
                    if (bgRunning) "BG: ON" else "BG: OFF",
                    color = if (bgRunning) Color(0xFF33D6A6) else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Orb(state = state)

        Spacer(Modifier.height(24.dp))

        Text(
            text = stateLabel(state, bgRunning),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.height(12.dp))

        when (state) {
            VoiceState.Listening -> {
                Text(
                    text = partial.ifBlank { "Listening…" },
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = if (partial.isBlank()) 0.4f else 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            VoiceState.Thinking -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (userSaid.isNotBlank()) {
                        Text(
                            "\"$userSaid\"",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Thinking…", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
                }
            }
            VoiceState.Speaking -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (userSaid.isNotBlank()) {
                        Text(
                            "\"$userSaid\"",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF15151F))
                            .padding(14.dp)
                    ) {
                        Text(reply, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
            VoiceState.Idle -> {
                if (!hasPermission) {
                    Text(
                        "Microphone permission needed",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                } else if (bgRunning) {
                    Text(
                        "Listening in background — close app to test",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FF6B6B))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(error!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.dismissError() }) {
                    Text("Dismiss", color = Color(0xFF33D6A6), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (bgRunning) return@Button
                if (!hasPermission) {
                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@Button
                }
                when (state) {
                    VoiceState.Idle -> vm.startListening()
                    VoiceState.Listening -> vm.stopListening()
                    VoiceState.Thinking -> Unit
                    VoiceState.Speaking -> vm.stopSpeaking()
                }
            },
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            shape = CircleShape,
            enabled = !bgRunning && state != VoiceState.Thinking,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (state) {
                    VoiceState.Idle, VoiceState.Listening -> Color(0xFF7C5CFF)
                    VoiceState.Speaking -> Color(0xFF33D6A6)
                    VoiceState.Thinking -> Color(0xFF33A6D6)
                },
                disabledContainerColor = Color(0xFF33A6D6).copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (state == VoiceState.Speaking) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = "Mic",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Orb(state: VoiceState) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val active = state != VoiceState.Idle
    val scale = if (active) pulse else 1f

    val color = when (state) {
        VoiceState.Idle -> Color(0xFF7C5CFF).copy(alpha = 0.55f)
        VoiceState.Listening -> Color(0xFF7C5CFF)
        VoiceState.Thinking -> Color(0xFF33A6D6)
        VoiceState.Speaking -> Color(0xFF33D6A6)
    }

    Box(
        modifier = Modifier
            .size(240.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.4f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (state) {
                    VoiceState.Idle -> "LIA"
                    VoiceState.Listening -> "🎤"
                    VoiceState.Thinking -> "…"
                    VoiceState.Speaking -> "🔊"
                },
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun stateLabel(state: VoiceState, bg: Boolean): String = when (state) {
    VoiceState.Idle -> if (bg) "Background active" else "Tap mic to speak"
    VoiceState.Listening -> "Listening…"
    VoiceState.Thinking -> "Thinking…"
    VoiceState.Speaking -> "Speaking…"
}
