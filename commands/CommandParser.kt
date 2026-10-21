package com.lia.assistant.commands

object CommandParser {

    private val bnDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private fun norm(text: String): String =
        text.trim().map { bnDigits[it] ?: it }.joinToString("")

    private fun Int?.orZero(): Int = this ?: 0

    fun parse(raw: String): CommandResult? {
        val text = norm(raw).lowercase()

        // 1. Explicit refusal: power off
        if (Regex("(ফোন|phone|device|মোবাইল)\\s*(অফ|off|বন্ধ|shutdown)").containsMatchIn(text) ||
            text.contains("power off") || text.contains("shutdown")) {
            return CommandResult.ReplyOnly(
                "দুঃখিত, রুট ছাড়া ফোন বন্ধ করা যায় না। চাইলে স্ক্রিন লক করতে পারি — বলুন 'স্ক্রিন লক করো'।"
            )
        }

        // 2. Stop listening
        if (Regex("^(বন্ধ করো|থামো|শেষ করো|stop|bye|goodbye|গুডবাই)\\s*[।.!]?$").containsMatchIn(text) ||
            text == "লিয়া বন্ধ করো") {
            return CommandResult.StopListening
        }

        // 3. Lock screen
        if (Regex("(স্ক্রিন|screen|ফোন|phone)?\\s*(লক|lock)\\s*(করো|do)?", RegexOption.IGNORE_CASE).containsMatchIn(text) &&
            (text.contains("লক") || text.contains("lock"))) {
            return CommandResult.LockScreen
        }

        // 4. Home / Back / Recents
        if (Regex("(হোমে|home)\\s*(যাও|go|করো|open)|go\\s*home|home\\s*screen", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return CommandResult.GoHome
        }
        if (Regex("(ব্যাক|back)\\s*(করো|go)|go\\s*back", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return CommandResult.GoBack
        }
        if (Regex("(রিসেন্ট|recent)\\s*(অ্যাপ|apps|দেখাও|open)", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return CommandResult.OpenRecents
        }

        // 5. LIA open
        if (Regex("(lia|লিয়া)\\s*(তে)?\\s*(ফিরে|open|খোলো|আসো)", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return CommandResult.OpenLia
        }

        // 6. Flashlight
        val torchOnPattern = Regex("(টর্চ|flashlight|torch|ফ্ল্যাশলাইট)\\s*(জ্বালাও|চালু|on|অন)", RegexOption.IGNORE_CASE)
        val torchOffPattern = Regex("(টর্চ|flashlight|torch|ফ্ল্যাশলাইট)\\s*(নেভাও|বন্ধ|off|অফ)", RegexOption.IGNORE_CASE)
        if (torchOnPattern.containsMatchIn(text)) return CommandResult.FlashlightOn
        if (torchOffPattern.containsMatchIn(text)) return CommandResult.FlashlightOff

        // 7. WhatsApp message
        val waBn = Regex("whatsapp\\s*এ\\s+(.+?)\\s*কে\\s+(.+?)\\s*(বলো|পাঠাও|মেসেজ|লিখো)", RegexOption.IGNORE_CASE)
        val waEn = Regex("send\\s+whatsapp\\s+(?:message\\s+)?to\\s+(.+?)\\s*[:\\-]\\s*(.+)", RegexOption.IGNORE_CASE)
        val waEn2 = Regex("(?:whatsapp|message)\\s+(.+?)\\s+(?:saying|and say|that)\\s+(.+)", RegexOption.IGNORE_CASE)
        (waBn.find(text) ?: waEn.find(text) ?: waEn2.find(text))?.let {
            val t = it.groupValues[1].trim()
            val m = it.groupValues[2].trim()
            if (t.isNotBlank() && m.isNotBlank()) return CommandResult.WhatsAppMessage(t, m)
        }

        // 8. SMS
        val smsBn = Regex("(.+?)\\s*কে\\s*(?:sms|এসএমএস|মেসেজ)\\s*(?:পাঠাও|করো)\\s+(.+)", RegexOption.IGNORE_CASE)
        val smsEn = Regex("send\\s+(?:an\\s+)?sms\\s+to\\s+(.+?)\\s*[:\\-]\\s*(.+)", RegexOption.IGNORE_CASE)
        (smsBn.find(text) ?: smsEn.find(text))?.let {
            val t = it.groupValues[1].trim()
            val m = it.groupValues[2].trim()
            if (t.isNotBlank() && m.isNotBlank()) return CommandResult.SendSms(t, m)
        }

        // 9. YouTube search
        val ytBn = Regex("(?:youtube|ইউটিউব)\\s*এ\\s+(.+?)\\s*(?:চালাও|সার্চ\\s*করো|প্লে\\s*করো|দেখাও|বাজাও|খুঁজে\\s*দাও)", RegexOption.IGNORE_CASE)
        val ytEn = Regex("(?:play|search)\\s+(.+?)\\s+on\\s+youtube", RegexOption.IGNORE_CASE)
        val ytBn2 = Regex("(.+?)\\s+(?:গান|ভিডিও|সিনেমা)\\s*(?:চালাও|দেখাও|বাজাও|প্লে\\s*করো)", RegexOption.IGNORE_CASE)
        (ytBn.find(text) ?: ytEn.find(text) ?: ytBn2.find(text))?.let {
            val q = it.groupValues[1].trim()
            if (q.isNotBlank()) return CommandResult.SearchYouTube(q)
        }

        // 10. Google search
        val gBn = Regex("(?:google|গুগল)\\s*এ\\s+(.+?)\\s*(?:সার্চ|খুঁজে|খোঁজো)", RegexOption.IGNORE_CASE)
        val gEn = Regex("search\\s+(.+?)\\s+on\\s+google", RegexOption.IGNORE_CASE)
        (gBn.find(text) ?: gEn.find(text))?.let {
            val q = it.groupValues[1].trim()
            if (q.isNotBlank()) return CommandResult.GoogleSearch(q)
        }

        // 11. Call
        val callBn = Regex("(.+?)\\s*কে\\s*(?:কল|ফোন)\\s*(?:করো|দাও)", RegexOption.IGNORE_CASE)
        val callEn = Regex("^(?:call|dial)\\s+(.+)", RegexOption.IGNORE_CASE)
        (callBn.find(text) ?: callEn.find(text))?.let {
            val t = it.groupValues[1].trim()
            if (t.isNotBlank()) return CommandResult.CallTarget(t)
        }

        // 12. Alarm
        val alarmBn = Regex("(.+?)\\s*(?:টায়|টার)\\s*(?:alarm|অ্যালার্ম)\\s*(?:দাও|বসাও|সেট)", RegexOption.IGNORE_CASE)
        val alarmEn = Regex("(?:set\\s+)?alarm\\s+(?:at|for)?\\s*(\\d{1,2})(?:[:\\s]?(\\d{2}))?", RegexOption.IGNORE_CASE)
        alarmBn.find(text)?.let {
            parseTime(it.groupValues[1])?.let { (h, m) -> return CommandResult.SetAlarm(h, m) }
        }
        alarmEn.find(text)?.let {
            val h = it.groupValues[1].toIntOrNull() ?: return@let
            val m = it.groupValues[2].toIntOrNull().orZero()
            return CommandResult.SetAlarm(h, m)
        }

        // 13. Timer
        val timerBn = Regex("(\\d+)\\s*(মিনিট|মিনিটের|minute|min|সেকেন্ড|second|sec)\\s*(?:এর)?\\s*(?:timer|টাইমার)", RegexOption.IGNORE_CASE)
        val timerEn = Regex("(?:set\\s+)?timer\\s+(?:for\\s+)?(\\d+)\\s*(minute|min|second|sec|hour|hr)", RegexOption.IGNORE_CASE)
        (timerBn.find(text) ?: timerEn.find(text))?.let {
            val n = it.groupValues[1].toLongOrNull() ?: return@let
            val unit = it.groupValues[2].lowercase()
            val seconds = when {
                unit.contains("hour") || unit.contains("ঘণ্টা") -> n * 3600
                unit.contains("minute") || unit.contains("min") || unit.contains("মিনিট") -> n * 60
                else -> n
            }
            return CommandResult.SetTimer(seconds)
        }

        // 14. Open URL (contains dot or http)
        val urlMatch = Regex("(https?://\\S+|\\b[\\w.-]+\\.(?:com|org|net|io|bd|co|app)\\b\\S*)", RegexOption.IGNORE_CASE).find(text)
        if (urlMatch != null && (text.contains("খোলো") || text.contains("open") || text.contains("go to") || text.contains("visit"))) {
            return CommandResult.OpenUrl(urlMatch.value)
        }

        // 15. Open app: "X খোলো" / "open X" / "X চালু করো" / "X launch"
        val openBn = Regex("^(.+?)\\s*(?:খোলো|চালু\\s*করো|ওপেন\\s*করো|খুলে\\s*দাও)\\s*[।.!]?$", RegexOption.IGNORE_CASE)
        val openEn = Regex("^(?:open|launch|start)\\s+(.+?)\\s*[।.!]?$", RegexOption.IGNORE_CASE)
        (openBn.find(text) ?: openEn.find(text))?.let {
            val name = it.groupValues[1].trim().trimEnd('।', '.', '!')
            if (name.isNotBlank() && name.length >= 2) return CommandResult.OpenApp(name)
        }

        return null
    }

    private fun parseTime(s: String): Pair<Int, Int>? {
        val t = s.trim()
        val m = Regex("^(\\d{1,2})(?:[:.](\\d{2}))?$").find(t) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: 0
        if (h !in 0..23 || min !in 0..59) return null
        return h to min
    }
}
