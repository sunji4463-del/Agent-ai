package com.lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lia.assistant.ChatViewModel
import com.lia.assistant.data.ChatMessage
import com.lia.assistant.data.KeyStore

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val sending by vm.sending.collectAsState()
    val error by vm.error.collectAsState()

    var input by remember { mutableStateOf("") }
    var useGroq by remember { mutableStateOf(true) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasGroq by remember { mutableStateOf(false) }
    var hasGemini by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasGroq = KeyStore.get(ctx, KeyStore.KEY_GROQ)?.isNotBlank() == true
                hasGemini = KeyStore.get(ctx, KeyStore.KEY_GEMINI)?.isNotBlank() == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Chat", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (useGroq) "Groq · llama-3.1-8b" else "Gemini · 1.5-flash",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Row {
                TextButton(onClick = { useGroq = !useGroq }) {
                    Text(
                        if (useGroq) "→ Gemini" else "→ Groq",
                        color = Color(0xFF33D6A6),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (!hasGroq && !hasGemini) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FF6B6B))
            ) {
                Text(
                    "⚠ No API key found. Go to Settings and add Groq or Gemini key.",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No messages yet",
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Ask something to get started",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Bubble(msg)
            }

            if (sending) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF15151F))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "LIA is thinking…",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FF6B6B))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    error!!,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { vm.dismissError() }) {
                    Text("Dismiss", color = Color(0xFF33D6A6), fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF15151F))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…", color = Color.White.copy(alpha = 0.4f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 4,
                enabled = !sending
            )
            IconButton(
                enabled = !sending && input.isNotBlank(),
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        input = ""
                        vm.send(text, useGroq = useGroq)
                    }
                }
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = if (!sending && input.isNotBlank()) Color(0xFF33D6A6)
                    else Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear chat?") },
            text = { Text("All messages will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    showClearConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) Color(0xFF7C5CFF) else Color(0xFF15151F))
                .padding(12.dp)
        ) {
            Text(text = msg.content, color = Color.White, fontSize = 14.sp)
        }
    }
}
