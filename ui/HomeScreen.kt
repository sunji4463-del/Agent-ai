package com.lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LIA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Your personal AI assistant", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
            StatusPill(text = "Ready", active = true)
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C5CFF),
                            Color(0xFF33D6A6).copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A2E).copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Text("LIA", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(
            "Tap to Talk",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Speak with LIA using your voice",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("voice") },
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C5CFF)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Talk",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF15151F))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Ask anything", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Open chat to type a question", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
            TextButton(onClick = { navController.navigate(Screen.Chat.route) }) {
                Text("Open", color = Color(0xFF33D6A6))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF15151F))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("History", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("See past conversations", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
            TextButton(onClick = { navController.navigate("history") }) {
                Text("View", color = Color(0xFF33D6A6))
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
