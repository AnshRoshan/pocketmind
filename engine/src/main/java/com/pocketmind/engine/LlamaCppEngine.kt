package com.pocketmind.engine

import android.util.Log
import com.pocketmind.core.model.ModelInfo
import com.pocketmind.core.model.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LlamaCppEngine : SLMEngine {

    private var modelPtr: Long = 0L
    private var loadedModelInfo: ModelInfo? = null
    private val mutex = Mutex()

    companion object {
        private const val TAG = "LlamaCppEngine"
        init {
            try {
                System.loadLibrary("llama_android")
                Log.i(TAG, "llama_android native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native library not available — running in mock mode: ${e.message}")
            }
        }
    }

    // JNI declarations (implemented in llm_jni.cpp)
    private external fun nativeLoadModel(path: String, nCtx: Int, nGpuLayers: Int): Long
    private external fun nativeGenerate(ptr: Long, prompt: String, maxTokens: Int, temp: Float, callback: GenerationCallback): String
    private external fun nativeEmbed(ptr: Long, text: String): FloatArray
    private external fun nativeFree(ptr: Long)

    interface GenerationCallback {
        fun onToken(token: String)
    }

    override suspend fun loadModel(modelPath: String, config: ModelConfig): Boolean {
        return mutex.withLock {
            try {
                if (modelPtr != 0L) nativeFree(modelPtr)
                modelPtr = nativeLoadModel(modelPath, config.contextLength, config.gpuLayers)
                if (modelPtr != 0L) {
                    loadedModelInfo = ModelInfo(
                        id = config.name,
                        name = config.name,
                        description = "Loaded model",
                        sizeBytes = java.io.File(modelPath).length(),
                        format = config.format.name,
                        quantization = config.quantization,
                        contextLength = config.contextLength,
                        supportsToolCalling = config.supportsToolCalling,
                        supportsVision = config.supportsVision,
                        downloadUrl = "",
                        localPath = modelPath,
                        isDownloaded = true,
                        isActive = true
                    )
                    Log.i(TAG, "Model loaded: ${config.name}")
                    true
                } else {
                    Log.e(TAG, "Failed to load model from $modelPath")
                    false
                }
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native lib not available, simulating model load")
                // For development: simulate a loaded model
                loadedModelInfo = ModelInfo(
                    id = config.name, name = config.name, description = "Mock model",
                    sizeBytes = 0L, format = config.format.name, quantization = config.quantization,
                    contextLength = config.contextLength, supportsToolCalling = false,
                    supportsVision = false, downloadUrl = "", isActive = true
                )
                modelPtr = 1L // Non-zero to indicate "loaded" in mock mode
                true
            }
        }
    }

    override suspend fun generate(prompt: String, params: GenerationParams): Flow<String> = flow {
        mutex.withLock {
            if (modelPtr == 0L) {
                emit("No model loaded.")
                return@withLock
            }
            try {
                val tokens = mutableListOf<String>()
                nativeGenerate(modelPtr, prompt, params.maxTokens, params.temperature, object : GenerationCallback {
                    override fun onToken(token: String) { tokens.add(token) }
                })
                tokens.forEach { emit(it) }
            } catch (e: UnsatisfiedLinkError) {
                // Mock generation for development
                val mockResponse = "I'm Pocketmind running in development mode. The llama.cpp library is not yet compiled. Add the NDK and build the native library to enable real on-device inference."
                for (word in mockResponse.split(" ")) {
                    emit("$word ")
                    kotlinx.coroutines.delay(50)
                }
            }
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun generateWithTools(prompt: String, tools: List<ToolDefinition>): ToolCallResult {
        val result = StringBuilder()
        generate(prompt).collect { result.append(it) }
        return ToolCallResult(content = result.toString())
    }

    override suspend fun embed(text: String): FloatArray {
        return try {
            if (modelPtr != 0L) nativeEmbed(modelPtr, text)
            else FloatArray(0)
        } catch (e: UnsatisfiedLinkError) {
            FloatArray(384) { Math.random().toFloat() } // Mock embeddings
        }
    }

    override fun unloadModel() {
        if (modelPtr != 0L) {
            try { nativeFree(modelPtr) } catch (_: UnsatisfiedLinkError) {}
            modelPtr = 0L
            loadedModelInfo = null
        }
    }

    override fun getModelInfo(): ModelInfo? = loadedModelInfo

    override fun isModelLoaded(): Boolean = modelPtr != 0L
}
