package com.lia.assistant.data

import android.content.Context
import org.json.JSONArray
import java.io.File

object ChatStorage {
    private const val FILE = "chat_history.json"

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    fun load(ctx: Context): List<ChatMessage> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    add(ChatMessage.fromJson(arr.getJSONObject(i)))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: Context, messages: List<ChatMessage>) {
        val arr = JSONArray()
        messages.forEach { arr.put(it.toJson()) }
        file(ctx).writeText(arr.toString())
    }

    fun clear(ctx: Context) {
        file(ctx).delete()
    }
}
