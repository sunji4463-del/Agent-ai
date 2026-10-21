package com.lia.assistant.commands

import android.Manifest
import android.app.AlarmClock
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.lia.assistant.MainActivity
import com.lia.assistant.service.AccessibilityController

object CommandExecutor {

    suspend fun execute(ctx: Context, cmd: CommandResult): String {
        return try {
            when (cmd) {
                is CommandResult.OpenApp -> AppLauncher.openByName(ctx, cmd.appName)
                is CommandResult.SearchYouTube -> openYouTubeSearch(ctx, cmd.query)
                CommandResult.OpenYouTube -> AppLauncher.openByName(ctx, "youtube")
                is CommandResult.GoogleSearch -> googleSearch(ctx, cmd.query)
                is CommandResult.OpenUrl -> openUrl(ctx, cmd.url)
                is CommandResult.CallTarget -> callTarget(ctx, cmd.nameOrNumber)
                is CommandResult.WhatsAppMessage -> whatsapp(ctx, cmd.target, cmd.message)
                is CommandResult.SendSms -> sms(ctx, cmd.target, cmd.message)
                CommandResult.LockScreen -> lockScreen()
                CommandResult.GoHome -> goHome()
                CommandResult.GoBack -> goBack()
                CommandResult.OpenRecents -> openRecents()
                is CommandResult.SetAlarm -> setAlarm(ctx, cmd.hour, cmd.minute)
                is CommandResult.SetTimer -> setTimer(ctx, cmd.seconds)
                CommandResult.FlashlightOn -> flashlight(ctx, true)
                CommandResult.FlashlightOff -> flashlight(ctx, false)
                CommandResult.OpenLia -> openLia(ctx)
                CommandResult.StopListening -> "ঠিক আছে, বন্ধ করছি।"
                is CommandResult.ReplyOnly -> cmd.message
            }
        } catch (e: Exception) {
            "দুঃখিত, কাজটি করতে পারলাম না: ${e.message ?: "অজানা ত্রুটি"}"
        }
    }

    // -------- Helpers --------

    private fun openYouTubeSearch(ctx: Context, query: String): String {
        val url = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            "YouTube এ '$query' খুঁজছি।"
        } catch (e: Exception) {
            "YouTube খুলতে পারলাম না।"
        }
    }

    private fun googleSearch(ctx: Context, query: String): String {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            "Google এ '$query' খুঁজছি।"
        } catch (e: Exception) {
            "Google খুলতে পারলাম না।"
        }
    }

    private fun openUrl(ctx: Context, raw: String): String {
        val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            "লিংক খুলছি।"
        } catch (e: Exception) {
            "URL খুলতে পারলাম না।"
        }
    }

    private fun callTarget(ctx: Context, target: String): String {
        val phone = resolveContactOrNumber(ctx, target)
            ?: return "দুঃখিত, '$target' এর নম্বর পাইনি। Settings এ Contacts পারমিশন দিন, অথবা সরাসরি নম্বর বলুন।"

        val canCall = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED

        val intent = if (canCall) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            ctx.startActivity(intent)
            if (canCall) "কল করছি $target..." else "ডায়ালার খুলছি $target এর জন্য। কল বাটনে চাপ দিন।"
        } catch (e: Exception) {
            "কল করতে পারলাম না।"
        }
    }

    private fun whatsapp(ctx: Context, target: String, message: String): String {
        val phone = resolveContactOrNumber(ctx, target)
            ?: return "দুঃখিত, '$target' এর নম্বর পাইনি। নম্বরটি country code সহ বলুন (যেমন ৮৮০১৭... )।"

        val clean = phone.replace(Regex("[^0-9+]"), "")
        val url = "https://wa.me/$clean?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            "WhatsApp এ $target কে মেসেজ পাঠাচ্ছি।"
        } catch (e: Exception) {
            "WhatsApp ইনস্টল করা নেই বা খোলা যাচ্ছে না।"
        }
    }

    private fun sms(ctx: Context, target: String, message: String): String {
        val phone = resolveContactOrNumber(ctx, target)
            ?: return "দুঃখিত, '$target' এর নম্বর পাইনি।"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ctx.startActivity(intent)
            "SMS অ্যাপে $target এর জন্য মেসেজ লিখে দিলাম। পাঠাতে Send চাপুন।"
        } catch (e: Exception) {
            "SMS অ্যাপ খুলতে পারলাম না।"
        }
    }

    private fun lockScreen(): String {
        return if (AccessibilityController.lockScreen())
            "স্ক্রিন লক করছি।"
        else
            "Accessibility সার্ভিস চালু নেই। Settings → Accessibility → LIA চালু করুন।"
    }

    private fun goHome(): String {
        return if (AccessibilityController.goHome()) "হোমে যাচ্ছি।"
        else "Accessibility সার্ভিস চালু নেই।"
    }

    private fun goBack(): String {
        return if (AccessibilityController.goBack()) "ব্যাক যাচ্ছি।"
        else "Accessibility সার্ভিস চালু নেই।"
    }

    private fun openRecents(): String {
        return if (AccessibilityController.openRecents()) "রিসেন্ট অ্যাপ দেখাচ্ছি।"
        else "Accessibility সার্ভিস চালু নেই।"
    }

    private fun setAlarm(ctx: Context, hour: Int, minute: Int): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ctx.startActivity(intent)
            val m = minute.toString().padStart(2, '0')
            "অ্যালার্ম সেট করছি ${hour}:$m এ।"
        } catch (e: Exception) {
            "অ্যালার্ম সেট করতে পারলাম না।"
        }
    }

    private fun setTimer(ctx: Context, seconds: Long): String {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.toInt())
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            putExtra(AlarmClock.EXTRA_MESSAGE, "LIA timer")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ctx.startActivity(intent)
            val label = when {
                seconds >= 3600 -> "${seconds / 3600} ঘণ্টার"
                seconds >= 60 -> "${seconds / 60} মিনিটের"
                else -> "${seconds} সেকেন্ডের"
            }
            "$label টাইমার চালু করছি।"
        } catch (e: Exception) {
            "টাইমার সেট করতে পারলাম না।"
        }
    }

    private fun flashlight(ctx: Context, on: Boolean): String {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { camId ->
                cm.getCameraCharacteristics(camId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "এই ফোনে টর্চ নেই।"
            cm.setTorchMode(id, on)
            if (on) "টর্চ জ্বালালাম।" else "টর্চ নিভালাম।"
        } catch (e: Exception) {
            "টর্চ চালাতে পারলাম না।"
        }
    }

    private fun openLia(ctx: Context): String {
        return try {
            val intent = Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ctx.startActivity(intent)
            "LIA তে ফিরছি।"
        } catch (e: Exception) {
            "LIA খুলতে পারলাম না।"
        }
    }

    private fun resolveContactOrNumber(ctx: Context, raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null

        val digits = t.replace(Regex("[^0-9+]"), "")
        if (digits.length >= 5 && t.matches(Regex("[+\\d\\s\\-()০-৯]+"))) {
            return digits
        }

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(t)
        )
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val num = c.getString(0)
                if (!num.isNullOrBlank()) {
                    return num.replace(Regex("[^0-9+]"), "")
                }
            }
        }
        return null
    }
}
