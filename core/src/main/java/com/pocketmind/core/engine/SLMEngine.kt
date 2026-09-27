package com.pocketmind.core.engine

import com.pocketmind.core.model.ModelInfo
import com.pocketmind.core.model.ToolDefinition
import kotlinx.coroutines.flow.Flow

interface SLMEngine {
    suspend fun loadModel(modelPath: String, config: ModelConfig): Boolean
    suspend fun generate(prompt: String, params: GenerationParams = GenerationParams()): Flow<String>
    suspend fun generateWithTools(prompt: String, tools: List<ToolDefinition>): ToolCallResult
    suspend fun embed(text: String): FloatArray
    fun unloadModel()
    fun getModelInfo(): ModelInfo?
    fun isModelLoaded(): Boolean
}

data class ModelConfig(
    val name: String,
    val format: ModelFormat = ModelFormat.GGUF,
    val contextLength: Int = 2048,
    val quantization: String = "Q4_K_M",
    val gpuLayers: Int = 0,
    val supportsToolCalling: Boolean = false,
    val supportsVision: Boolean = false
)

data class GenerationParams(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val stopSequences: List<String> = listOf("<|user|>", "<|system|>", "</s>")
)

data class ToolCallResult(
    val content: String,
    val toolCalls: List<com.pocketmind.core.model.ToolCall> = emptyList()
)

enum class ModelFormat { GGUF, LITERT, ONNX }
