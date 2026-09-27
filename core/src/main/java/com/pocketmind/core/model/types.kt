package com.pocketmind.core.model

sealed interface PhoneAction {
    data class OpenApp(val packageName: String) : PhoneAction
    data class SendMessage(val app: MessagingApp, val contact: String, val message: String) : PhoneAction
    data class SetAlarm(val hour: Int, val minute: Int, val label: String = "") : PhoneAction
    data class CreateCalendarEvent(val event: CalendarEvent) : PhoneAction
    data class TakePhoto(val frontCamera: Boolean = false) : PhoneAction
    data class ReadNotifications(val appFilter: String? = null) : PhoneAction
    data class SearchWeb(val query: String) : PhoneAction
    data class ReadFile(val path: String) : PhoneAction
    data class WriteFile(val path: String, val content: String) : PhoneAction
    data class MakeCall(val number: String) : PhoneAction
    data class SetBrightness(val level: Int) : PhoneAction
    data class ToggleWifi(val enabled: Boolean) : PhoneAction
    data class ToggleBluetooth(val enabled: Boolean) : PhoneAction
    data class NavigateTo(val destination: String) : PhoneAction
    data class UIInteraction(val steps: List<UIStep>) : PhoneAction
    data class Chat(val message: String) : PhoneAction
    data class SetVolume(val level: Int, val stream: AudioStream) : PhoneAction
    data class PlayMedia(val query: String) : PhoneAction
}

data class UIStep(
    val action: UIAction,
    val target: String,
    val value: String? = null
)

enum class UIAction { CLICK, TYPE, SCROLL_DOWN, SCROLL_UP, SWIPE_LEFT, SWIPE_RIGHT, BACK, HOME, LONG_PRESS }

enum class MessagingApp(val packageName: String) {
    WHATSAPP("com.whatsapp"),
    TELEGRAM("org.telegram.messenger"),
    SMS("com.google.android.apps.messaging"),
    GMAIL("com.google.android.gm"),
    INSTAGRAM("com.instagram.android")
}

enum class AudioStream { RING, MEDIA, ALARM, NOTIFICATION }

data class ActionResult(
    val success: Boolean,
    val output: String,
    val error: String? = null
)

enum class TaskComplexity {
    SIMPLE_ACTION,
    MEDIUM,
    COMPLEX,
    PRIVATE_DATA
}

sealed interface RoutingDecision {
    data class Local(val reason: String) : RoutingDecision
    data class Cloud(val provider: String, val reason: String, val askConsent: Boolean = false) : RoutingDecision
    data class LocalWithFallback(val fallbackProvider: String) : RoutingDecision
}

enum class MemoryType {
    FACT,
    PREFERENCE,
    CONTEXT,
    ROUTINE,
    SKILL_RESULT
}
