package com.lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lia.assistant.data.ChatMessage
import com.lia.assistant.data.ChatStorage

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Coming in a later phase", color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    LaunchedEffect(Unit) {
        messages = ChatStorage.load(ctx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        LiaTopBar("History", "Saved chat sessions")
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF15151F))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (msg.role == "user") "You" else "LIA",
                            color = if (msg.role == "user") Color(0xFF7C5CFF) else Color(0xFF33D6A6),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(48.dp)
                        )
                        Text(
                            msg.content,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
