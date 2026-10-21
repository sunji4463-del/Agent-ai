package com.lia.assistant.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lia.assistant.data.KeyStore
import com.lia.assistant.service.AccessibilityController

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var gemini by remember { mutableStateOf(KeyStore.get(ctx, KeyStore.KEY_GEMINI) ?: "") }
    var groq by remember { mutableStateOf(KeyStore.get(ctx, KeyStore.KEY_GROQ) ?: "") }
    var geminiVisible by remember { mutableStateOf(false) }
    var groqVisible by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        LiaTopBar("Settings", "API keys, permissions, device control")

        // ---------- Gemini ----------
        LiaCard {
            Text("Gemini API Key", fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = gemini,
                onValueChange = { gemini = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AIza…") },
                singleLine = true,
                visualTransformation = if (geminiVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { geminiVisible = !geminiVisible }) {
                        Icon(
                            if (geminiVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // ---------- Groq ----------
        LiaCard {
            Text("Groq API Key", fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = groq,
                onValueChange = { groq = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("gsk_…") },
                singleLine = true,
                visualTransformation = if (groqVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { groqVisible = !groqVisible }) {
                        Icon(
                            if (groqVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (gemini.isNotBlank()) KeyStore.save(ctx, KeyStore.KEY_GEMINI, gemini)
                    else KeyStore.remove(ctx, KeyStore.KEY_GEMINI)
                    if (groq.isNotBlank()) KeyStore.save(ctx, KeyStore.KEY_GROQ, groq)
                    else KeyStore.remove(ctx, KeyStore.KEY_GROQ)
                    savedMsg = "Saved ✓"
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save Keys") }
            OutlinedButton(
                onClick = {
                    gemini = ""
                    groq = ""
                    KeyStore.remove(ctx, KeyStore.KEY_GEMINI)
                    KeyStore.remove(ctx, KeyStore.KEY_GROQ)
                    savedMsg = "Cleared"
                },
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
        }

        if (savedMsg != null) {
            Spacer(Modifier.height(8.dp))
            Text(savedMsg!!, color = Color(0xFF33D6A6), fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        // ---------- Accessibility ----------
        AccessibilityCard()

        Spacer(Modifier.height(24.dp))
        Text("About", fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "LIA — Local Intelligent Assistant\nVersion 1.0 (Phase 5)\nBuilt with Kotlin + Jetpack Compose",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AccessibilityCard() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(AccessibilityController.isEnabled(ctx)) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = AccessibilityController.isEnabled(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LiaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Accessibility Service",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    "Enables tap, type, and lock-screen",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            StatusPill(text = if (enabled) "On" else "Off", active = enabled)
        }

        Spacer(Modifier.height(12.dp))

        if (!enabled) {
            Text(
                "Enable LIA under Settings → Accessibility so it can perform device actions when you ask.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { AccessibilityController.openSettings(ctx) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open Accessibility Settings") }
        } else {
            Text(
                "Test actions",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { AccessibilityController.goHome() },
                    modifier = Modifier.weight(1f)
                ) { Text("Home", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { AccessibilityController.goBack() },
                    modifier = Modifier.weight(1f)
                ) { Text("Back", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { AccessibilityController.lockScreen() },
                    modifier = Modifier.weight(1f),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ) { Text("Lock", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Note: Lock requires Android 9+. Home minimizes LIA — expected.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
