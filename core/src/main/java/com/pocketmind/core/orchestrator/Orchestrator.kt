package com.pocketmind.core.orchestrator

import com.pocketmind.core.action.ActionExecutor
import com.pocketmind.core.memory.MemoryManager
import com.pocketmind.core.model.ActionResult
import com.pocketmind.core.model.PhoneAction
import com.pocketmind.core.model.RoutingDecision
import com.pocketmind.core.engine.SLMEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Orchestrator @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val taskPlanner: TaskPlanner,
    private val routingEngine: RoutingEngine,
    private val actionExecutor: ActionExecutor,
    private val memoryManager: MemoryManager,
    private val slmEngine: SLMEngine
) {

    /**
     * Main entry point — process user input and return streaming response tokens.
     */
    fun process(userInput: String): Flow<String> = flow {
        // 1. Sanitize input (prevent prompt injection from external data)
        val sanitized = sanitizeInput(userInput)

        // 2. Recall relevant memories
        val memories = memoryManager.recall(sanitized)
        val memoryContext = if (memories.isNotEmpty()) {
            memories.joinToString("\n") { "- ${it.content}" }
        } else ""

        // 3. Classify intent
        val classification = intentClassifier.classify(sanitized)

        // 4. Decide routing
        val routing = routingEngine.route(sanitized, classification.complexity)

        // 5. Execute based on classification
        when (val action = classification.action) {
            is PhoneAction.Chat -> {
                // Pure conversation — route to SLM or cloud
                val systemPrompt = buildSystemPrompt(memoryContext)
                val fullPrompt = buildChatPrompt(systemPrompt, sanitized)

                when (routing) {
                    is RoutingDecision.Local, is RoutingDecision.LocalWithFallback -> {
                        if (slmEngine.isModelLoaded()) {
                            slmEngine.generate(fullPrompt).collect { token ->
                                emit(token)
                            }
                        } else {
                            emit("⚠️ No model loaded. Please download a model from the Models screen.")
                        }
                    }
                    is RoutingDecision.Cloud -> {
                        emit("☁️ Routing to cloud (${routing.provider})...\n")
                        // Cloud provider would be injected and called here
                        emit("Cloud routing is available after setting up API keys in Settings.")
                    }
                }
            }

            is PhoneAction.UIInteraction -> {
                // Multi-step UI automation
                emit("🤖 Planning steps...\n")
                val plan = taskPlanner.plan(sanitized)
                emit("📋 Plan:\n${plan.steps.mapIndexed { i, s -> "${i+1}. $s" }.joinToString("\n")}\n\n")

                val result = actionExecutor.execute(action)
                if (result.success) {
                    emit("✅ Done! ${result.output}")
                } else {
                    emit("❌ Failed: ${result.error ?: result.output}")
                }
            }

            else -> {
                // Direct phone action
                val actionDesc = describeAction(action)
                emit("⚡ Executing: $actionDesc\n")

                val result = actionExecutor.execute(action)
                if (result.success) {
                    emit("✅ ${result.output}")
                    // Save to memory if it's a notable action
                    memoryManager.remember("User asked to: $sanitized. Result: ${result.output}")
                } else {
                    emit("❌ ${result.error ?: "Action failed"}")
                }
            }
        }
    }

    /**
     * Get the name of the currently active model.
     */
    suspend fun getActiveModelName(): String {
        return slmEngine.getModelInfo()?.name ?: "No model loaded"
    }

    private fun sanitizeInput(input: String): String {
        // Remove potential prompt injection attempts
        return input
            .replace(Regex("<\\|system\\|>.*?<\\|user\\|>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("\\[INST\\].*?\\[/INST\\]", RegexOption.DOT_MATCHES_ALL), "")
            .trim()
            .take(2000) // Limit input length
    }

    private fun buildSystemPrompt(memoryContext: String): String {
        val base = """You are Pocketmind, a helpful AI assistant that lives on the user's Android phone.
You can control the phone, open apps, send messages, set alarms, and more.
You are concise, helpful, and privacy-focused.
Always respond in the language the user writes in."""

        return if (memoryContext.isNotBlank()) {
            "$base\n\nRelevant context about this user:\n$memoryContext"
        } else base
    }

    private fun buildChatPrompt(systemPrompt: String, userInput: String): String {
        return "<|system|>\n$systemPrompt\n<|user|>\n$userInput\n<|assistant|>\n"
    }

    private fun describeAction(action: PhoneAction): String = when (action) {
        is PhoneAction.OpenApp -> "Opening ${action.packageName}"
        is PhoneAction.SetAlarm -> "Setting alarm for ${action.hour}:${action.minute.toString().padStart(2, '0')} — ${action.label}"
        is PhoneAction.SendMessage -> "Sending message to ${action.contact} on ${action.app.name}"
        is PhoneAction.ReadNotifications -> "Reading notifications"
        is PhoneAction.SearchWeb -> "Searching: ${action.query}"
        is PhoneAction.NavigateTo -> "Navigating to: ${action.destination}"
        is PhoneAction.MakeCall -> "Calling ${action.number}"
        is PhoneAction.ToggleWifi -> if (action.enabled) "Enabling WiFi" else "Disabling WiFi"
        is PhoneAction.SetBrightness -> "Setting brightness to ${action.level}%"
        else -> action::class.simpleName ?: "Unknown action"
    }
}
