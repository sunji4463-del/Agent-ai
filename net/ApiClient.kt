package com.lia.assistant.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val GEMINI_URL_BASE =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    private const val SYSTEM_PROMPT =
        "You are LIA, a helpful personal AI assistant on Android. " +
        "Reply concisely in the user's language (Bengali or English). " +
        "Be friendly and practical."

    // -------- Groq --------
    suspend fun askGroq(apiKey: String, history: List<GroqMessage>): String =
        withContext(Dispatchers.IO) {
            require(apiKey.isNotBlank()) { "Groq API key missing" }
            val body = groqRequestBody(history).toString()
            val raw = postJson(GROQ_URL, body, mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            ))
            parseGroqReply(raw)
        }

    // -------- Gemini --------
    suspend fun askGemini(apiKey: String, history: List<GroqMessage>): String =
        withContext(Dispatchers.IO) {
            require(apiKey.isNotBlank()) { "Gemini API key missing" }
            val url = "$GEMINI_URL_BASE?key=$apiKey"
            val body = geminiRequestBody(history, SYSTEM_PROMPT).toString()
            val raw = postJson(url, body, mapOf("Content-Type" to "application/json"))
            parseGeminiReply(raw)
        }

    // -------- HTTP --------
    private fun postJson(
        urlStr: String,
        body: String,
        headers: Map<String, String>
    ): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) {
                // Try to extract friendly error
                val msg = try {
                    JSONObject(text).optJSONObject("error")?.optString("message") ?: text
                } catch (e: Exception) { text }
                throw RuntimeException("HTTP $code: $msg")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
