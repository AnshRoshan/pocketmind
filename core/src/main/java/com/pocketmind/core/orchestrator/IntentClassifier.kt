package com.pocketmind.core.orchestrator

import com.pocketmind.core.model.PhoneAction
import com.pocketmind.core.model.TaskComplexity
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val action: PhoneAction,
    val complexity: TaskComplexity,
    val confidence: Float = 1.0f
)

@Singleton
class IntentClassifier @Inject constructor() {

    /**
     * Rule-based intent classification for reliability on small models.
     * Falls back to SLM-based classification for ambiguous cases.
     */
    fun classify(input: String): ClassificationResult {
        val lower = input.lowercase().trim()

        // Check for private data patterns first → always local
        if (containsPrivateData(lower)) {
            return ClassificationResult(
                action = PhoneAction.Chat(input),
                complexity = TaskComplexity.PRIVATE_DATA
            )
        }

        // Simple actions — keyword matching
        parseSimpleAction(lower, input)?.let { return it }

        // Medium complexity
        if (isMediumComplexity(lower)) {
            return ClassificationResult(
                action = PhoneAction.Chat(input),
                complexity = TaskComplexity.MEDIUM
            )
        }

        // Complex — large generation tasks
        if (isComplex(lower)) {
            return ClassificationResult(
                action = PhoneAction.Chat(input),
                complexity = TaskComplexity.COMPLEX
            )
        }

        // Default: chat
        return ClassificationResult(
            action = PhoneAction.Chat(input),
            complexity = TaskComplexity.MEDIUM
        )
    }

    private fun parseSimpleAction(lower: String, original: String): ClassificationResult? {
        // Alarm
        val alarmRegex = Regex("""(set|create|add).*alarm.*?(\d{1,2})[: ]?(\d{2})?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
        alarmRegex.find(lower)?.let { match ->
            val hour = match.groupValues[2].toIntOrNull() ?: 7
            val minute = match.groupValues[3].toIntOrNull() ?: 0
            val isPm = match.groupValues[4].lowercase() == "pm"
            val adjustedHour = if (isPm && hour < 12) hour + 12 else if (!isPm && hour == 12) 0 else hour
            return ClassificationResult(
                action = PhoneAction.SetAlarm(adjustedHour, minute),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Open app
        val openRegex = Regex("""(open|launch|start)\s+(.+)""", RegexOption.IGNORE_CASE)
        openRegex.find(lower)?.let { match ->
            val appName = match.groupValues[2].trim()
            val packageName = resolvePackageName(appName)
            return ClassificationResult(
                action = PhoneAction.OpenApp(packageName),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Read notifications
        if (lower.contains("notification") || lower.contains("missed") && lower.contains("message")) {
            return ClassificationResult(
                action = PhoneAction.ReadNotifications(),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Toggle WiFi
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            val enable = lower.contains("on") || lower.contains("enable") || lower.contains("turn on")
            return ClassificationResult(
                action = PhoneAction.ToggleWifi(enable),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Web search
        val searchRegex = Regex("""(search|google|look up|find)\s+(.+)""", RegexOption.IGNORE_CASE)
        searchRegex.find(lower)?.let { match ->
            return ClassificationResult(
                action = PhoneAction.SearchWeb(match.groupValues[2]),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Make call
        val callRegex = Regex("""(call|phone|dial)\s+(.+)""", RegexOption.IGNORE_CASE)
        callRegex.find(lower)?.let { match ->
            val target = match.groupValues[2].trim()
            return ClassificationResult(
                action = PhoneAction.MakeCall(target),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Navigation
        val navRegex = Regex("""(navigate|directions|take me|go)\s+to\s+(.+)""", RegexOption.IGNORE_CASE)
        navRegex.find(lower)?.let { match ->
            return ClassificationResult(
                action = PhoneAction.NavigateTo(match.groupValues[2].trim()),
                complexity = TaskComplexity.SIMPLE_ACTION
            )
        }

        // Send message — complex UI automation
        val msgRegex = Regex("""(send|message|text|whatsapp)\s+(.+?)\s+(saying|that|:)\s+(.+)""", RegexOption.IGNORE_CASE)
        msgRegex.find(lower)?.let { match ->
            val contact = match.groupValues[2].trim()
            val message = match.groupValues[4].trim()
            return ClassificationResult(
                action = PhoneAction.UIInteraction(emptyList()), // Planner will fill steps
                complexity = TaskComplexity.MEDIUM
            )
        }

        return null
    }

    private fun isMediumComplexity(lower: String): Boolean {
        val mediumKeywords = listOf(
            "summarize", "summary", "explain", "what is", "how do",
            "translate", "write a", "draft", "compose", "help me",
            "remind me", "check my", "analyze"
        )
        return mediumKeywords.any { lower.contains(it) }
    }

    private fun isComplex(lower: String): Boolean {
        val complexKeywords = listOf(
            "write a detailed", "create a plan", "business plan", "proposal",
            "research", "compare", "comprehensive", "full analysis",
            "debug this code", "write code", "generate a report"
        )
        return complexKeywords.any { lower.contains(it) }
    }

    private fun containsPrivateData(lower: String): Boolean {
        val privateKeywords = listOf(
            "my password", "my bank", "my credit card", "my ssn",
            "my health", "my medical", "my salary", "my account"
        )
        return privateKeywords.any { lower.contains(it) }
    }

    private fun resolvePackageName(appName: String): String {
        return when (appName.lowercase().trim()) {
            "whatsapp" -> "com.whatsapp"
            "telegram" -> "org.telegram.messenger"
            "chrome" -> "com.android.chrome"
            "gmail" -> "com.google.android.gm"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "youtube" -> "com.google.android.youtube"
            "instagram" -> "com.instagram.android"
            "twitter", "x" -> "com.twitter.android"
            "spotify" -> "com.spotify.music"
            "camera" -> "com.android.camera2"
            "settings" -> "com.android.settings"
            "calendar" -> "com.google.android.calendar"
            "photos" -> "com.google.android.apps.photos"
            "netflix" -> "com.netflix.mediaclient"
            "uber" -> "com.ubercab"
            else -> appName.lowercase().replace(" ", ".")
        }
    }
}
