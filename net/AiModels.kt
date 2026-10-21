package com.lia.assistant.net

import org.json.JSONArray
import org.json.JSONObject

// ---------- Groq ----------
data class GroqMessage(val role: String, val content: String)

fun groqRequestBody(
    messages: List<GroqMessage>,
    model: String = "llama-3.1-8b-instant"
): JSONObject {
    val arr = JSONArray()
    messages.forEach { m ->
        arr.put(JSONObject().apply {
            put("role", m.role)
            put("content", m.content)
        })
    }
    return JSONObject().apply {
        put("model", model)
        put("messages", arr)
        put("temperature", 0.7)
        put("max_tokens", 1024)
    }
}

fun parseGroqReply(json: String): String {
    val obj = JSONObject(json)
    val choices = obj.optJSONArray("choices") ?: return ""
    if (choices.length() == 0) return ""
    val first = choices.getJSONObject(0)
    val msg = first.optJSONObject("message") ?: return ""
    return msg.optString("content", "")
}

// ---------- Gemini ----------
fun geminiRequestBody(
    history: List<GroqMessage>,
    systemPrompt: String? = null
): JSONObject {
    val contents = JSONArray()
    history.forEach { m ->
        val role = if (m.role == "assistant") "model" else "user"
        contents.put(JSONObject().apply {
            put("role", role)
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", m.content))
            })
        })
    }
    val root = JSONObject().apply {
        put("contents", contents)
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 1024)
        })
    }
    if (systemPrompt != null) {
        root.put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", systemPrompt))
            })
        })
    }
    return root
}

fun parseGeminiReply(json: String): String {
    val obj = JSONObject(json)
    val candidates = obj.optJSONArray("candidates") ?: return ""
    if (candidates.length() == 0) return ""
    val c = candidates.getJSONObject(0)
    val content = c.optJSONObject("content") ?: return ""
    val parts = content.optJSONArray("parts") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until parts.length()) {
        sb.append(parts.getJSONObject(i).optString("text", ""))
    }
    return sb.toString()
}
