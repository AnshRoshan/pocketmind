package com.pocketmind.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val actions: List<ExecutedAction> = emptyList()
)

enum class MessageRole { USER, ASSISTANT, SYSTEM, TOOL }

@Serializable
data class ExecutedAction(
    val type: String,
    val params: Map<String, String>,
    val result: String,
    val success: Boolean
)

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val format: String,
    val quantization: String,
    val contextLength: Int,
    val supportsToolCalling: Boolean,
    val supportsVision: Boolean,
    val downloadUrl: String,
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val isActive: Boolean = false
)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, ToolParameter>
)

@Serializable
data class ToolParameter(
    val type: String,
    val description: String,
    val required: Boolean = false
)

@Serializable
data class ToolCall(
    val toolName: String,
    val arguments: Map<String, String>
)

@Serializable
data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val description: String? = null
)

data class NotificationItem(
    val id: Int,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)
