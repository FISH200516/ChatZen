package com.fishai.chatzen.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fishai.chatzen.data.model.CustomProvider
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.data.model.Provider
import com.fishai.chatzen.data.model.WebSearchProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }

    fun getApiKey(provider: Provider): Flow<String?> {
        val key = stringPreferencesKey("api_key_${provider.name}")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun saveApiKey(provider: Provider, apiKey: String) {
        val key = stringPreferencesKey("api_key_${provider.name}")
        context.dataStore.edit { preferences ->
            preferences[key] = apiKey
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAvailableModels(provider: Provider): Flow<List<ModelInfo>> {
        if (provider == Provider.CUSTOM) {
            return getCustomProviders().flatMapLatest { providers ->
                if (providers.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = providers.map { getCustomProviderModels(it.id) }
                    combine(flows) { array ->
                        array.flatMap { it }
                    }
                }
            }
        }

        val key = stringPreferencesKey("models_${provider.name}")
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[key]
            if (jsonString != null) {
                try {
                    json.decodeFromString<List<ModelInfo>>(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun saveAvailableModels(provider: Provider, models: List<ModelInfo>) {
        val key = stringPreferencesKey("models_${provider.name}")
        val jsonString = json.encodeToString(models)
        context.dataStore.edit { preferences ->
            preferences[key] = jsonString
        }
    }

    fun getSelectedModel(): Flow<Triple<String, String, String?>?> {
        val key = stringPreferencesKey("selected_model_id")
        return context.dataStore.data.map { preferences ->
            val value = preferences[key]
            if (value != null && value.contains("|")) {
                val parts = value.split("|")
                if (parts.size >= 2) {
                    val providerName = parts[0]
                    val modelId = parts[1]
                    val customProviderId = if (parts.size > 2 && parts[2].isNotEmpty()) parts[2] else null
                    Triple(providerName, modelId, customProviderId)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun saveSelectedModel(provider: Provider, modelId: String, customProviderId: String? = null) {
        val key = stringPreferencesKey("selected_model_id")
        val value = "${provider.name}|$modelId|${customProviderId ?: ""}"
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getFavoriteModels(): Flow<Set<String>> {
        val key = stringPreferencesKey("favorite_models")
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[key]
            if (jsonString != null) {
                try {
                    json.decodeFromString<Set<String>>(jsonString)
                } catch (e: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }
        }
    }

    suspend fun toggleFavoriteModel(provider: String, modelId: String, customProviderId: String? = null) {
        val key = stringPreferencesKey("favorite_models")
        val id = "$provider|$modelId|${customProviderId ?: ""}"
        context.dataStore.edit { preferences ->
            val jsonString = preferences[key]
            val currentSet = if (jsonString != null) {
                try {
                    json.decodeFromString<MutableSet<String>>(jsonString)
                } catch (e: Exception) {
                    mutableSetOf()
                }
            } else {
                mutableSetOf()
            }
            
            if (currentSet.contains(id)) {
                currentSet.remove(id)
            } else {
                if (currentSet.size < 5) {
                    currentSet.add(id)
                }
            }
            preferences[key] = json.encodeToString(currentSet)
        }
    }

    // Custom Providers
    fun getCustomProviders(): Flow<List<CustomProvider>> {
        val key = stringPreferencesKey("custom_providers")
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[key]
            if (jsonString != null) {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun saveCustomProviders(providers: List<CustomProvider>) {
        val key = stringPreferencesKey("custom_providers")
        val jsonString = json.encodeToString(providers)
        context.dataStore.edit { preferences ->
            preferences[key] = jsonString
        }
    }

    fun getCustomProviderModels(customProviderId: String): Flow<List<ModelInfo>> {
        val key = stringPreferencesKey("models_custom_$customProviderId")
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[key]
            if (jsonString != null) {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun saveCustomProviderModels(customProviderId: String, models: List<ModelInfo>) {
        val key = stringPreferencesKey("models_custom_$customProviderId")
        val jsonString = json.encodeToString(models)
        context.dataStore.edit { preferences ->
            preferences[key] = jsonString
        }
    }

    // Web Search Settings
    fun getWebSearchProvider(): Flow<WebSearchProvider> {
        val key = stringPreferencesKey("web_search_provider")
        return context.dataStore.data.map { preferences ->
            val name = preferences[key]
            if (name != null) {
                try {
                    WebSearchProvider.valueOf(name)
                } catch (e: Exception) {
                    WebSearchProvider.NONE
                }
            } else {
                WebSearchProvider.NONE
            }
        }
    }

    suspend fun saveWebSearchProvider(provider: WebSearchProvider) {
        val key = stringPreferencesKey("web_search_provider")
        context.dataStore.edit { preferences ->
            preferences[key] = provider.name
        }
    }

    fun getWebSearchApiKey(provider: WebSearchProvider): Flow<String?> {
        val key = stringPreferencesKey("api_key_search_${provider.name}")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun saveWebSearchApiKey(provider: WebSearchProvider, apiKey: String) {
        val key = stringPreferencesKey("api_key_search_${provider.name}")
        context.dataStore.edit { preferences ->
            preferences[key] = apiKey
        }
    }

    fun getWebSearchSecretKey(provider: WebSearchProvider): Flow<String?> {
        val key = stringPreferencesKey("secret_key_search_${provider.name}")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun saveWebSearchSecretKey(provider: WebSearchProvider, secretKey: String) {
        val key = stringPreferencesKey("secret_key_search_${provider.name}")
        context.dataStore.edit { preferences ->
            preferences[key] = secretKey
        }
    }

    fun getWebSearchEnabled(): Flow<Boolean> {
        val key = androidx.datastore.preferences.core.booleanPreferencesKey("web_search_enabled_toggle")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    suspend fun setWebSearchEnabled(enabled: Boolean) {
        val key = androidx.datastore.preferences.core.booleanPreferencesKey("web_search_enabled_toggle")
        context.dataStore.edit { preferences ->
            preferences[key] = enabled
        }
    }

    // OpenClaw Web UI URL
    fun getOpenClawWebUiUrl(): Flow<String?> {
        val key = stringPreferencesKey("openclaw_web_ui_url")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun saveOpenClawWebUiUrl(url: String) {
        val key = stringPreferencesKey("openclaw_web_ui_url")
        context.dataStore.edit { preferences ->
            preferences[key] = url
        }
    }

    suspend fun clearOpenClawWebUiUrl() {
        val key = stringPreferencesKey("openclaw_web_ui_url")
        context.dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    // Model Capabilities (Reasoning)
    fun getReasoningModels(): Flow<Set<String>> {
        val key = stringPreferencesKey("reasoning_models")
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[key]
            if (jsonString != null) {
                try {
                    json.decodeFromString<Set<String>>(jsonString)
                } catch (e: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }
        }
    }

    suspend fun markModelAsReasoning(modelId: String) {
        val key = stringPreferencesKey("reasoning_models")
        context.dataStore.edit { preferences ->
            val jsonString = preferences[key]
            val currentSet = if (jsonString != null) {
                try {
                    json.decodeFromString<MutableSet<String>>(jsonString)
                } catch (e: Exception) {
                    mutableSetOf()
                }
            } else {
                mutableSetOf()
            }
            
            if (!currentSet.contains(modelId)) {
                currentSet.add(modelId)
                preferences[key] = json.encodeToString(currentSet)
            }
        }
    }

    // OCR Settings
    fun getOcrModel(): Flow<Triple<String, String, String?>?> {
        val key = stringPreferencesKey("ocr_model_id")
        return context.dataStore.data.map { preferences ->
            val value = preferences[key]
            if (value != null && value.contains("|")) {
                val parts = value.split("|")
                if (parts.size >= 2) {
                    val providerName = parts[0]
                    val modelId = parts[1]
                    val customProviderId = if (parts.size > 2 && parts[2].isNotEmpty()) parts[2] else null
                    Triple(providerName, modelId, customProviderId)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun saveOcrModel(provider: Provider, modelId: String, customProviderId: String? = null) {
        val key = stringPreferencesKey("ocr_model_id")
        val value = "${provider.name}|$modelId|${customProviderId ?: ""}"
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getOcrEnabled(): Flow<Boolean> {
        val key = androidx.datastore.preferences.core.booleanPreferencesKey("ocr_enabled_toggle")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    suspend fun setOcrEnabled(enabled: Boolean) {
        val key = androidx.datastore.preferences.core.booleanPreferencesKey("ocr_enabled_toggle")
        context.dataStore.edit { preferences ->
            preferences[key] = enabled
        }
    }

    // Model Generation Settings
    fun getTemperature(): Flow<Float> {
        val key = androidx.datastore.preferences.core.floatPreferencesKey("temperature")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0.7f
        }
    }

    suspend fun saveTemperature(value: Float) {
        val key = androidx.datastore.preferences.core.floatPreferencesKey("temperature")
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getTopP(): Flow<Float> {
        val key = androidx.datastore.preferences.core.floatPreferencesKey("top_p")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 1.0f
        }
    }

    suspend fun saveTopP(value: Float) {
        val key = androidx.datastore.preferences.core.floatPreferencesKey("top_p")
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getSystemPrompt(): Flow<String> {
        val key = stringPreferencesKey("system_prompt")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }

    suspend fun saveSystemPrompt(value: String) {
        val key = stringPreferencesKey("system_prompt")
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Theme Settings
    fun getThemeMode(): Flow<String> {
        val key = stringPreferencesKey("theme_mode")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: "CUSTOM"
        }
    }

    suspend fun saveThemeMode(mode: String) {
        val key = stringPreferencesKey("theme_mode")
        context.dataStore.edit { preferences ->
            preferences[key] = mode
        }
    }

    fun getCustomThemeColor(): Flow<Int> {
        val key = intPreferencesKey("custom_theme_color")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: -16777216 // Default BLACK
        }
    }

    suspend fun saveCustomThemeColor(color: Int) {
        val key = intPreferencesKey("custom_theme_color")
        context.dataStore.edit { preferences ->
            preferences[key] = color
        }
    }

    // Independent Bubble Colors
    fun getUserBubbleColor(): Flow<Int?> {
        val key = intPreferencesKey("user_bubble_color")
        return context.dataStore.data.map { preferences ->
            preferences[key] // Return null if not set, handled by Theme.kt defaults
        }
    }

    suspend fun saveUserBubbleColor(color: Int?) {
        val key = intPreferencesKey("user_bubble_color")
        context.dataStore.edit { preferences ->
            if (color != null) {
                preferences[key] = color
            } else {
                preferences.remove(key)
            }
        }
    }

    fun getAiBubbleColor(): Flow<Int?> {
        val key = intPreferencesKey("ai_bubble_color")
        return context.dataStore.data.map { preferences ->
            preferences[key] // Return null if not set
        }
    }

    suspend fun saveAiBubbleColor(color: Int?) {
        val key = intPreferencesKey("ai_bubble_color")
        context.dataStore.edit { preferences ->
            if (color != null) {
                preferences[key] = color
            } else {
                preferences.remove(key)
            }
        }
    }

    fun getGlobalCornerRadius(): Flow<Float> {
        val key = floatPreferencesKey("global_corner_radius")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 12f // Default 12dp
        }
    }

    suspend fun saveGlobalCornerRadius(radius: Float) {
        val key = floatPreferencesKey("global_corner_radius")
        context.dataStore.edit { preferences ->
            preferences[key] = radius
        }
    }

    fun getUserAvatarUri(): Flow<String?> {
        val key = stringPreferencesKey("user_avatar_uri")
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun saveUserAvatarUri(uri: String?) {
        val key = stringPreferencesKey("user_avatar_uri")
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[key] = uri
            } else {
                preferences.remove(key)
            }
        }
    }
}
