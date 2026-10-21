package com.lia.assistant.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object AccessibilityController {

    fun isEnabled(ctx: Context): Boolean {
        val serviceName = "${ctx.packageName}/${LiaAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(serviceName, ignoreCase = true)) return true
        }
        return false
    }

    fun openSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    fun lockScreen(): Boolean =
        LiaAccessibilityService.instance?.performGlobal(GlobalAction.LOCK) ?: false

    fun goHome(): Boolean =
        LiaAccessibilityService.instance?.performGlobal(GlobalAction.HOME) ?: false

    fun goBack(): Boolean =
        LiaAccessibilityService.instance?.performGlobal(GlobalAction.BACK) ?: false

    fun openRecents(): Boolean =
        LiaAccessibilityService.instance?.performGlobal(GlobalAction.RECENTS) ?: false

    fun openNotifications(): Boolean =
        LiaAccessibilityService.instance?.performGlobal(GlobalAction.NOTIFICATIONS) ?: false

    fun clickAt(x: Float, y: Float, cb: ((Boolean) -> Unit)? = null): Boolean {
        val svc = LiaAccessibilityService.instance
        if (svc == null) { cb?.invoke(false); return false }
        svc.clickAt(x, y, cb)
        return true
    }

    fun swipe(
        x1: Float, y1: Float, x2: Float, y2: Float,
        dur: Long = 300L, cb: ((Boolean) -> Unit)? = null
    ): Boolean {
        val svc = LiaAccessibilityService.instance
        if (svc == null) { cb?.invoke(false); return false }
        svc.swipe(x1, y1, x2, y2, dur, cb)
        return true
    }

    fun typeText(text: String): Boolean =
        LiaAccessibilityService.instance?.typeText(text) ?: false
}
