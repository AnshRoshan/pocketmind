package com.pocketmind.cloud

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores API keys and secrets in the encrypted SharedPreferences instance
 * provided by the app module's DI graph.
 */
@Singleton
class KeyVault @Inject constructor(
    private val prefs: SharedPreferences
) {

    fun storeKey(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getKey(key: String): String? {
        return prefs.getString(key, null)
    }

    fun deleteKey(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun hasKey(key: String): Boolean {
        return !getKey(key).isNullOrBlank()
    }

    companion object {
        const val KEY_OPENROUTER = "openrouter_api_key"
        const val KEY_GOOGLE_AI = "google_ai_api_key"
        const val KEY_GROQ = "groq_api_key"
        const val KEY_NVIDIA = "nvidia_api_key"
        const val KEY_TELEGRAM_TOKEN = "telegram_bot_token"
        const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
    }
}
