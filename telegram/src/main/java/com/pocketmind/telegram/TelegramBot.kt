package com.pocketmind.telegram

import android.util.Log
import com.pocketmind.cloud.KeyVault
import com.pocketmind.core.orchestrator.Orchestrator
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Telegram bot front-end: long-polls getUpdates, forwards user text to the
 * orchestrator, and streams the response back to the chat.
 */
@Singleton
class TelegramBot @Inject constructor(
    private val httpClient: HttpClient,
    private val keyVault: KeyVault,
    private val orchestrator: Orchestrator
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val running = AtomicBoolean(false)
    private var pollJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable
    data class OutgoingMessage(val chatId: Long, val text: String)

    fun start() {
        if (!running.compareAndSet(false, true)) return
        pollJob = scope.launch { pollLoop() }
    }

    fun stop() {
        running.set(false)
        pollJob?.cancel()
        scope.cancel()
    }

    private suspend fun pollLoop() {
        var offset = 0L
        while (running.get() && kotlinx.coroutines.currentCoroutineContext().isActive) {
            val token = keyVault.getKey(KeyVault.KEY_TELEGRAM_TOKEN)
            if (token.isNullOrBlank()) {
                delay(30_000)
                continue
            }
            try {
                val url = "https://api.telegram.org/bot$token/getUpdates?timeout=30&offset=$offset"
                val body = httpClient.get(url).bodyAsText()
                val updates = json.parseToJsonElement(body).jsonObject["result"]?.jsonArray ?: continue
                for (update in updates) {
                    val obj = update.jsonObject
                    offset = (obj["update_id"]?.jsonPrimitive?.content?.toLongOrNull() ?: offset) + 1
                    val message = obj["message"]?.jsonObject ?: continue
                    val chatId = message["chat"]?.jsonObject?.get("id")?.jsonPrimitive?.content?.toLongOrNull() ?: continue
                    val text = message["text"]?.jsonPrimitive?.content ?: continue
                    handleUserMessage(token, chatId, text)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Telegram poll failed: ${e.message}")
                delay(10_000)
            }
        }
    }

    private suspend fun handleUserMessage(token: String, chatId: Long, text: String) {
        val configuredChatId = keyVault.getKey(KeyVault.KEY_TELEGRAM_CHAT_ID)?.toLongOrNull()
        if (configuredChatId != null && chatId != configuredChatId) return // only respond to the owner

        try {
            val response = StringBuilder()
            orchestrator.process(text).collect { response.append(it) }
            sendMessage(token, chatId, response.toString().ifBlank { "Done." })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Telegram message", e)
            runCatching { sendMessage(token, chatId, "❌ Error: ${e.message}") }
        }
    }

    suspend fun sendMessage(token: String, chatId: Long, text: String) {
        val url = "https://api.telegram.org/bot$token/sendMessage?chat_id=${chatId}&text=${text.take(4000).encodeURLParameter()}"
        httpClient.post(url) { contentType(ContentType.Application.Json) }
    }

    companion object {
        private const val TAG = "TelegramBot"
    }
}
