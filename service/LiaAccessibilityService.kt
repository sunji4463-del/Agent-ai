package com.lia.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

object GlobalAction {
    const val HOME = 1
    const val BACK = 2
    const val RECENTS = 3
    const val NOTIFICATIONS = 4
    const val LOCK = 5
}

class LiaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: LiaAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun clickAt(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return
        }
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
            .build()
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { callback?.invoke(true) }
            override fun onCancelled(g: GestureDescription?) { callback?.invoke(false) }
        }, null)
        if (!ok) callback?.invoke(false)
    }

    fun swipe(
        x1: Float, y1: Float, x2: Float, y2: Float,
        durationMs: Long = 300L,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return
        }
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { callback?.invoke(true) }
            override fun onCancelled(g: GestureDescription?) { callback?.invoke(false) }
        }, null)
        if (!ok) callback?.invoke(false)
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun performGlobal(action: Int): Boolean {
        return when (action) {
            GlobalAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            GlobalAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            GlobalAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GlobalAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            GlobalAction.LOCK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else false
            else -> false
        }
    }

    fun screenSize(): Pair<Int, Int>? {
        val dm = resources.displayMetrics
        return dm.widthPixels to dm.heightPixels
    }
}
