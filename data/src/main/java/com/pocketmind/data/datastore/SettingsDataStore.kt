package com.pocketmind.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pocketmind_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CLOUD_ENABLED = booleanPreferencesKey("cloud_enabled")
        val ACTIVE_MODEL = stringPreferencesKey("active_model")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    }

    val onboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val cloudEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.CLOUD_ENABLED] ?: true }

    val activeModel: Flow<String> =
        context.dataStore.data.map { it[Keys.ACTIVE_MODEL] ?: "" }

    val memoryEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.MEMORY_ENABLED] ?: true }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setCloudEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOUD_ENABLED] = enabled }
    }

    suspend fun setActiveModel(modelName: String) {
        context.dataStore.edit { it[Keys.ACTIVE_MODEL] = modelName }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }
    }
}
