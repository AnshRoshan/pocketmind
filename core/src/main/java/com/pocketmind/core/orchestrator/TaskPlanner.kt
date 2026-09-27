package com.pocketmind.core.orchestrator

import com.pocketmind.core.model.UIAction
import com.pocketmind.core.model.UIStep
import javax.inject.Inject
import javax.inject.Singleton

data class ActionPlan(
    val originalRequest: String,
    val steps: List<String>,
    val uiSteps: List<UIStep> = emptyList()
)

@Singleton
class TaskPlanner @Inject constructor() {

    /**
     * Plans multi-step UI automation based on user request.
     * For MVP, uses rule-based planning. Future: use SLM for complex plans.
     */
    fun plan(request: String): ActionPlan {
        val lower = request.lowercase()

        // WhatsApp message sending
        if ((lower.contains("whatsapp") || lower.contains("send") || lower.contains("message")) &&
            (lower.contains("to") || lower.contains("mom") || lower.contains("dad"))) {
            return planWhatsAppMessage(request)
        }

        // Opening and navigating to settings
        if (lower.contains("settings") && lower.contains("wifi")) {
            return ActionPlan(
                originalRequest = request,
                steps = listOf(
                    "Open Settings app",
                    "Navigate to Network & Internet",
                    "Toggle WiFi"
                )
            )
        }

        // Generic plan
        return ActionPlan(
            originalRequest = request,
            steps = listOf("Processing: $request")
        )
    }

    private fun planWhatsAppMessage(request: String): ActionPlan {
        // Extract contact name
        val contactRegex = Regex("""(?:to|message|whatsapp)\s+(\w+(?:\s+\w+)?)""", RegexOption.IGNORE_CASE)
        val contact = contactRegex.find(request)?.groupValues?.get(1) ?: "contact"

        // Extract message text
        val msgRegex = Regex("""(?:saying|that|message:|:)\s+(.+)$""", RegexOption.IGNORE_CASE)
        val message = msgRegex.find(request)?.groupValues?.get(1) ?: request

        val steps = listOf(
            "Open WhatsApp",
            "Tap the Search icon",
            "Type \"$contact\"",
            "Tap the first result",
            "Tap the message input field",
            "Type: \"$message\"",
            "Tap Send"
        )

        val uiSteps = listOf(
            UIStep(UIAction.CLICK, "WhatsApp", null),  // Launch via intent
            UIStep(UIAction.CLICK, "Search"),
            UIStep(UIAction.TYPE, "search field", contact),
            UIStep(UIAction.CLICK, contact),
            UIStep(UIAction.CLICK, "message input"),
            UIStep(UIAction.TYPE, "message input", message),
            UIStep(UIAction.CLICK, "Send")
        )

        return ActionPlan(
            originalRequest = request,
            steps = steps,
            uiSteps = uiSteps
        )
    }
}
