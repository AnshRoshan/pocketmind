package com.pocketmind.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketmind.core.model.ModelInfo
import com.pocketmind.engine.AvailableModels
import com.pocketmind.engine.ModelDownloader
import com.pocketmind.engine.SLMEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelsUiState(
    val availableModels: List<ModelInfo> = emptyList(),
    val activeModelId: String? = null,
    val downloadProgress: Map<String, Float> = emptyMap(), // modelId -> 0.0..1.0
    val isLoadingModel: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val slmEngine: SLMEngine,
    private val modelDownloader: ModelDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        loadAvailableModels()
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            val models = AvailableModels.catalog
            val activeModel = slmEngine.getModelInfo()
            _uiState.update {
                it.copy(
                    availableModels = models,
                    activeModelId = activeModel?.id
                )
            }
        }
    }

    fun downloadModel(model: ModelInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadProgress = it.downloadProgress + (model.id to 0f)) }
            try {
                modelDownloader.download(model).collect { progress ->
                    _uiState.update { state ->
                        state.copy(
                            downloadProgress = state.downloadProgress + (model.id to progress.fraction)
                        )
                    }
                }
                // Download complete
                _uiState.update { state ->
                    val updated = state.availableModels.map { m ->
                        if (m.id == model.id) m.copy(isDownloaded = true) else m
                    }
                    state.copy(
                        availableModels = updated,
                        downloadProgress = state.downloadProgress - model.id
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        downloadProgress = state.downloadProgress - model.id,
                        errorMessage = "Download failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun activateModel(model: ModelInfo) {
        val localPath = model.localPath
        if (!model.isDownloaded || localPath == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModel = true, errorMessage = null) }
            try {
                val config = com.pocketmind.engine.ModelConfig(
                    name = model.name,
                    format = com.pocketmind.engine.ModelFormat.valueOf(model.format),
                    contextLength = model.contextLength,
                    quantization = model.quantization,
                    supportsToolCalling = model.supportsToolCalling,
                    supportsVision = model.supportsVision
                )
                val success = slmEngine.loadModel(localPath, config)
                if (success) {
                    val updated = _uiState.value.availableModels.map { m ->
                        m.copy(isActive = m.id == model.id)
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingModel = false,
                            activeModelId = model.id,
                            availableModels = updated
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingModel = false, errorMessage = "Failed to load model") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingModel = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun deleteModel(model: ModelInfo) {
        viewModelScope.launch {
            try {
                model.localPath?.let { path ->
                    java.io.File(path).delete()
                }
                val updated = _uiState.value.availableModels.map { m ->
                    if (m.id == model.id) m.copy(isDownloaded = false, localPath = null, isActive = false) else m
                }
                _uiState.update { it.copy(availableModels = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message}") }
            }
        }
    }
}
