package com.lia.assistant.commands

sealed class CommandResult {
    data class OpenApp(val appName: String) : CommandResult()
    data class SearchYouTube(val query: String) : CommandResult()
    data object OpenYouTube : CommandResult()
    data class GoogleSearch(val query: String) : CommandResult()
    data class OpenUrl(val url: String) : CommandResult()
    data class CallTarget(val nameOrNumber: String) : CommandResult()
    data class WhatsAppMessage(val target: String, val message: String) : CommandResult()
    data class SendSms(val target: String, val message: String) : CommandResult()
    data object LockScreen : CommandResult()
    data object GoHome : CommandResult()
    data object GoBack : CommandResult()
    data object OpenRecents : CommandResult()
    data class SetAlarm(val hour: Int, val minute: Int) : CommandResult()
    data class SetTimer(val seconds: Long) : CommandResult()
    data object FlashlightOn : CommandResult()
    data object FlashlightOff : CommandResult()
    data object OpenLia : CommandResult()
    data object StopListening : CommandResult()
    data class ReplyOnly(val message: String) : CommandResult()
}
