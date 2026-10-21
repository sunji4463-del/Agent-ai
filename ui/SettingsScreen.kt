package com.lia.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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

        // ---- Gemini ----
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

        // ---- Groq ----
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
                    if (gemini.isNotBlank()) KeyStore.save(ctx, KeyStore.KEY_GEMINI, gemini.trim())
                    else KeyStore.remove(ctx, KeyStore.KEY_GEMINI)
                    if (groq.isNotBlank()) KeyStore.save(ctx, KeyStore.KEY_GROQ, groq.trim())
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

        // ---- App Permissions ----
        AppPermissionsCard()

        Spacer(Modifier.height(12.dp))

        // ---- Accessibility ----
        AccessibilityCard()

        Spacer(Modifier.height(24.dp))
        Text("About", fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "LIA — Local Intelligent Assistant\nVersion 1.0 (Phase 7 · Stable)\nBuilt with Kotlin + Jetpack Compose",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AppPermissionsCard() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun has(perm: String) =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    var contacts by remember { mutableStateOf(has(Manifest.permission.READ_CONTACTS)) }
    var call by remember { mutableStateOf(has(Manifest.permission.CALL_PHONE)) }
    var mic by remember { mutableStateOf(has(Manifest.permission.RECORD_AUDIO)) }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { contacts = it }

    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { call = it }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { mic = it }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                contacts = has(Manifest.permission.READ_CONTACTS)
                call = has(Manifest.permission.CALL_PHONE)
                mic = has(Manifest.permission.RECORD_AUDIO)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LiaCard {
        Text("App Permissions", fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(
            "Required for voice, calls, and messaging",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(12.dp))

        PermissionRow(
            label = "Microphone",
            subtitle = "For voice input",
            granted = mic,
            onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
        Spacer(Modifier.height(8.dp))
        PermissionRow(
            label = "Contacts",
            subtitle = "Resolve names to numbers for calls/SMS",
            granted = contacts,
            onRequest = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
        )
        Spacer(Modifier.height(8.dp))
        PermissionRow(
            label = "Phone",
            subtitle = "Direct call (else dialer opens)",
            granted = call,
            onRequest = { callLauncher.launch(Manifest.permission.CALL_PHONE) }
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        if (granted) {
            StatusPill(text = "Granted", active = true)
        } else {
            TextButton(onClick = onRequest) {
                Text("Grant", color = Color(0xFF7C5CFF), fontSize = 12.sp)
            }
        }
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
