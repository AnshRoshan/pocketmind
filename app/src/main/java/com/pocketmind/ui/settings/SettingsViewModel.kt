package com.pocketmind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketmind.cloud.KeyVault
import com.pocketmind.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val openRouterKey: String = "",
    val googleAiKey: String = "",
    val groqKey: String = "",
    val nvidiaKey: String = "",
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val cloudEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val keyVault: KeyVault
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val onboardingComplete = settingsDataStore.onboardingComplete

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val openRouter = keyVault.getKey(KeyVault.KEY_OPENROUTER) ?: ""
            val googleAi = keyVault.getKey(KeyVault.KEY_GOOGLE_AI) ?: ""
            val groq = keyVault.getKey(KeyVault.KEY_GROQ) ?: ""
            val nvidia = keyVault.getKey(KeyVault.KEY_NVIDIA) ?: ""
            val tgToken = keyVault.getKey(KeyVault.KEY_TELEGRAM_TOKEN) ?: ""
            val tgChatId = keyVault.getKey(KeyVault.KEY_TELEGRAM_CHAT_ID) ?: ""

            _uiState.update {
                it.copy(
                    openRouterKey = openRouter,
                    googleAiKey = googleAi,
                    groqKey = groq,
                    nvidiaKey = nvidia,
                    telegramBotToken = tgToken,
                    telegramChatId = tgChatId
                )
            }
        }
    }

    fun onOpenRouterKeyChanged(key: String) = _uiState.update { it.copy(openRouterKey = key) }
    fun onGoogleAiKeyChanged(key: String) = _uiState.update { it.copy(googleAiKey = key) }
    fun onGroqKeyChanged(key: String) = _uiState.update { it.copy(groqKey = key) }
    fun onNvidiaKeyChanged(key: String) = _uiState.update { it.copy(nvidiaKey = key) }
    fun onTelegramTokenChanged(token: String) = _uiState.update { it.copy(telegramBotToken = token) }
    fun onTelegramChatIdChanged(id: String) = _uiState.update { it.copy(telegramChatId = id) }
    fun onCloudEnabledChanged(enabled: Boolean) = _uiState.update { it.copy(cloudEnabled = enabled) }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            keyVault.storeKey(KeyVault.KEY_OPENROUTER, state.openRouterKey)
            keyVault.storeKey(KeyVault.KEY_GOOGLE_AI, state.googleAiKey)
            keyVault.storeKey(KeyVault.KEY_GROQ, state.groqKey)
            keyVault.storeKey(KeyVault.KEY_NVIDIA, state.nvidiaKey)
            keyVault.storeKey(KeyVault.KEY_TELEGRAM_TOKEN, state.telegramBotToken)
            keyVault.storeKey(KeyVault.KEY_TELEGRAM_CHAT_ID, state.telegramChatId)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun setOnboardingComplete() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingComplete(true)
        }
    }
}
