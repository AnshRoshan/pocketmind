package com.pocketmind.core.orchestrator

import com.pocketmind.core.model.RoutingDecision
import com.pocketmind.core.model.TaskComplexity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingEngine @Inject constructor() {

    private var hasOpenRouterKey = false
    private var hasGoogleKey = false
    private var hasGroqKey = false

    fun updateCloudAvailability(openRouter: Boolean, google: Boolean, groq: Boolean) {
        hasOpenRouterKey = openRouter
        hasGoogleKey = google
        hasGroqKey = groq
    }

    fun route(input: String, complexity: TaskComplexity): RoutingDecision {
        return when (complexity) {
            TaskComplexity.SIMPLE_ACTION ->
                RoutingDecision.Local("Simple phone action — always local")

            TaskComplexity.PRIVATE_DATA ->
                RoutingDecision.Local("Contains sensitive data — never leaving device")

            TaskComplexity.MEDIUM ->
                RoutingDecision.LocalWithFallback(
                    fallbackProvider = getBestCloud() ?: "none"
                )

            TaskComplexity.COMPLEX -> {
                val cloud = getBestCloud()
                if (cloud != null) {
                    RoutingDecision.Cloud(
                        provider = cloud,
                        reason = "Complex task benefits from larger cloud model",
                        askConsent = true
                    )
                } else {
                    RoutingDecision.Local("No cloud keys available — using local model")
                }
            }
        }
    }

    private fun getBestCloud(): String? = when {
        hasGroqKey -> "groq"      // Fastest
        hasGoogleKey -> "google"  // Best quality
        hasOpenRouterKey -> "openrouter"
        else -> null
    }
}
