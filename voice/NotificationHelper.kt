package com.lia.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lia.assistant.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "lia_voice_channel"
    const val NOTIF_ID = 1001

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "LIA Voice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background voice assistant"
                setShowBadge(false)
            }
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(ch)
        }
    }

    fun build(ctx: Context, statusText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            ctx, 1,
            Intent(ctx, LiaVoiceService::class.java).apply {
                action = LiaVoiceService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle("LIA is active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
