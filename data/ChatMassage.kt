package com.lia.assistant.data

import org.json.JSONObject

data class ChatMessage(
    val id: Long,
    val role: String,      // "user" | "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role)
        put("content", content)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(o: JSONObject): ChatMessage = ChatMessage(
            id = o.optLong("id", System.currentTimeMillis()),
            role = o.optString("role", "assistant"),
            content = o.optString("content", ""),
            timestamp = o.optLong("timestamp", System.currentTimeMillis())
        )
    }
}
