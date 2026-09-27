package com.pocketmind.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketmind.core.orchestrator.Orchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val currentModel: String = "No model loaded",
    val inputText: String = "",
    val errorMessage: String? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val orchestrator: Orchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadWelcomeMessage()
        updateModelName()
    }

    private fun loadWelcomeMessage() {
        val welcome = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "👋 Hi! I'm Pocketmind. I live on your phone and I'm ready to help.\n\n" +
                    "I can:\n• Control your phone (open apps, set alarms, send messages)\n" +
                    "• Answer questions using on-device AI\n" +
                    "• Automate tasks and run skills\n\n" +
                    "What would you like me to do?"
        )
        _uiState.update { it.copy(messages = listOf(welcome)) }
    }

    private fun updateModelName() {
        viewModelScope.launch {
            val modelName = orchestrator.getActiveModelName()
            _uiState.update { it.copy(currentModel = modelName) }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(role = MessageRole.USER, content = text)
        val assistantMessageId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + assistantPlaceholder,
                isLoading = true,
                inputText = "",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val responseFlow = orchestrator.process(text)
                val sb = StringBuilder()
                responseFlow.collect { token ->
                    sb.append(token)
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(content = sb.toString(), isStreaming = true)
                            } else msg
                        }
                        state.copy(messages = updated)
                    }
                }
                // Mark streaming done
                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) msg.copy(isStreaming = false)
                        else msg
                    }
                    state.copy(messages = updated, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                content = "⚠️ Error: ${e.message ?: "Unknown error"}",
                                isStreaming = false
                            )
                        } else msg
                    }
                    state.copy(messages = updated, isLoading = false, errorMessage = e.message)
                }
            }
        }
    }

    fun sendQuickAction(action: String) {
        onInputTextChanged(action)
        sendMessage()
    }

    fun clearConversation() {
        _uiState.update { state ->
            state.copy(messages = emptyList(), errorMessage = null)
        }
        loadWelcomeMessage()
    }
}
