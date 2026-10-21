package com.lia.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceController {
    fun start(ctx: Context) {
        val intent = Intent(ctx, LiaVoiceService::class.java).apply {
            action = LiaVoiceService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    fun stop(ctx: Context) {
        val intent = Intent(ctx, LiaVoiceService::class.java).apply {
            action = LiaVoiceService.ACTION_STOP
        }
        runCatching { ctx.startService(intent) }
    }
}
