package com.lia.assistant.commands

import android.content.Context
import android.content.Intent

object AppLauncher {

    private val knownPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "camera" to "com.google.android.GoogleCamera",
        "settings" to "com.android.settings",
        "calculator" to "com.google.android.calculator",
        "phone" to "com.google.android.dialer",
        "dialer" to "com.google.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "play store" to "com.android.vending",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "imo" to "com.imo.android.imoim",
        "messenger" to "com.facebook.orca",
        "signal" to "org.thoughtcrime.securesms",
        "viber" to "com.viber.voip",
        "truecaller" to "com.truecaller"
    )

    private val bnAliases = mapOf(
        "ফেসবুক" to "facebook",
        "ইউটিউব" to "youtube",
        "হোয়াটসঅ্যাপ" to "whatsapp",
        "হোয়াটসঅ্যাপে" to "whatsapp",
        "ইনস্টাগ্রাম" to "instagram",
        "টেলিগ্রাম" to "telegram",
        "ম্যাপ" to "maps",
        "ম্যাপস" to "maps",
        "ক্যামেরা" to "camera",
        "সেটিংস" to "settings",
        "ফোন" to "phone",
        "কল" to "phone",
        "মেসেজ" to "messages",
        "মেসেঞ্জার" to "messenger",
        "জিমেইল" to "gmail",
        "ক্রোম" to "chrome",
        "ক্যালকুলেটর" to "calculator",
        "প্লে স্টোর" to "play store",
        "স্পটিফাই" to "spotify",
        "নেটফ্লিক্স" to "netflix",
        "টিকটক" to "tiktok",
        "ইমো" to "imo"
    )

    fun openByName(ctx: Context, name: String): String {
        val raw = name.trim().lowercase()
        val mapped = bnAliases[raw] ?: raw

        knownPackages[mapped]?.let { pkg ->
            tryOpen(ctx, pkg)?.let { return it }
        }

        return searchInstalled(ctx, mapped)
    }

    private fun tryOpen(ctx: Context, pkg: String): String? {
        return try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return null
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            val label = runCatching {
                ctx.packageManager.getApplicationLabel(
                    ctx.packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            }.getOrDefault(pkg)
            "$label খুলছি।"
        } catch (e: Exception) {
            null
        }
    }

    private fun searchInstalled(ctx: Context, query: String): String {
        val pm = ctx.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)

        if (apps.isEmpty()) return "দুঃখিত, '$query' নামে কোনো অ্যাপ পাইনি।"

        val scored = apps.map { it to score(it.loadLabel(pm).toString().lowercase(), it.activityInfo.packageName.lowercase(), query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        val best = scored.firstOrNull()?.first
            ?: return "দুঃখিত, '$query' নামে কোনো অ্যাপ পাইনি।"

        return tryOpen(ctx, best.activityInfo.packageName)
            ?: "অ্যাপটি খুলতে পারলাম না।"
    }

    private fun score(label: String, pkg: String, query: String): Int {
        return when {
            label == query -> 100
            pkg == query -> 90
            label.startsWith(query) -> 70
            label.contains(query) -> 50
            pkg.contains(query) -> 30
            else -> 0
        }
    }
}
