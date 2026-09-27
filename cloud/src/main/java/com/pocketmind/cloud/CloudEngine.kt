package com.pocketmind.cloud

import com.pocketmind.core.engine.SLMEngine
import com.pocketmind.core.engine.ModelConfig
import com.pocketmind.core.engine.GenerationParams
import com.pocketmind.core.engine.ToolCallResult
import com.pocketmind.core.model.ModelInfo
import com.pocketmind.core.model.ToolDefinition
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud LLM engine speaking the OpenAI-compatible chat-completions protocol
 * (OpenRouter, Groq, NVIDIA NIM, Google AI Studio's OpenAI endpoint).
 * Implements [SLMEngine] so the orchestrator can route to it transparently.
 */
@Singleton
class CloudEngine @Inject constructor(
    private val httpClient: HttpClient,
    private val keyVault: KeyVault
) : SLMEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var activeProvider: CloudProvider = CloudProvider.OPENROUTER
    private var activeModelId: String = ""

    fun useProvider(provider: CloudProvider, modelId: String) {
        activeProvider = provider
        activeModelId = modelId
    }

    private fun apiKey(): String? = when (activeProvider) {
        CloudProvider.OPENROUTER -> keyVault.getKey(KeyVault.KEY_OPENROUTER)
        CloudProvider.GOOGLE_AI -> keyVault.getKey(KeyVault.KEY_GOOGLE_AI)
        CloudProvider.GROQ -> keyVault.getKey(KeyVault.KEY_GROQ)
        CloudProvider.NVIDIA -> keyVault.getKey(KeyVault.KEY_NVIDIA)
    }?.takeIf { it.isNotBlank() }

    override suspend fun loadModel(modelPath: String, config: ModelConfig): Boolean {
        activeModelId = config.name
        return apiKey() != null
    }

    override suspend fun generate(prompt: String, params: GenerationParams): Flow<String> = flow {
        val key = apiKey()
        if (key == null) {
            emit("⚠️ No API key configured for ${activeProvider.displayName}. Add one in Settings.")
            return@flow
        }
        try {
            val body = ChatCompletionRequest(
                model = activeModelId.ifEmpty { activeProvider.defaultModel },
                messages = listOf(Message(role = "user", content = prompt)),
                maxTokens = params.maxTokens,
                temperature = params.temperature.toDouble(),
                stream = false
            )
            val response = httpClient.post(activeProvider.baseUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $key")
                if (activeProvider == CloudProvider.OPENROUTER) {
                    header("HTTP-Referer", "https://pocketmind.app")
                    header("X-Title", "Pocketmind")
                }
                setBody(json.encodeToString(ChatCompletionRequest.serializer(), body))
            }
            val text = parseContent(response.bodyAsText())
            // Emit in chunks so consumers observe streaming-like behaviour
            text.chunked(24).forEach { emit(it) }
        } catch (e: Exception) {
            emit("❌ Cloud request failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    private fun parseContent(body: String): String = try {
        val root = json.parseToJsonElement(body).jsonObject
        root["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: body
    } catch (_: Exception) {
        body
    }

    override suspend fun generateWithTools(prompt: String, tools: List<ToolDefinition>): ToolCallResult {
        val content = StringBuilder()
        generate(prompt).collect { content.append(it) }
        return ToolCallResult(content = content.toString())
    }

    override suspend fun embed(text: String): FloatArray = FloatArray(0)

    override fun unloadModel() { activeModelId = "" }

    override fun getModelInfo(): ModelInfo? = if (apiKey() != null) {
        ModelInfo(
            id = activeModelId.ifEmpty { activeProvider.defaultModel },
            name = "${activeProvider.displayName} (cloud)",
            description = "Cloud provider: ${activeProvider.displayName}",
            sizeBytes = 0L,
            format = "cloud",
            quantization = "",
            contextLength = 0,
            supportsToolCalling = false,
            supportsVision = false,
            downloadUrl = "",
            isActive = true
        )
    } else null

    override fun isModelLoaded(): Boolean = apiKey() != null
}

enum class CloudProvider(val displayName: String, val baseUrl: String, val defaultModel: String) {
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openai/gpt-4o-mini"),
    GOOGLE_AI("Google AI", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-1.5-flash"),
    GROQ("Groq", "https://api.groq.com/openai/v1/chat/completions", "llama-3.1-8b-instant"),
    NVIDIA("NVIDIA NIM", "https://integrate.api.nvidia.com/v1/chat/completions", "meta/llama-3.1-8b-instruct")
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String
)
