package com.fishai.chatzen.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fishai.chatzen.data.api.RetrofitClient
import com.fishai.chatzen.data.model.CustomProvider
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.data.model.Provider
import com.fishai.chatzen.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

import com.fishai.chatzen.data.model.ModelRegistry

import com.fishai.chatzen.data.model.WebSearchProvider
import android.content.Context
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.fishai.chatzen.data.api.OpenAIChatRequest
import com.fishai.chatzen.data.api.OpenAIMessage

data class SettingsUiState(
    val apiKeys: Map<Provider, String> = emptyMap(),
    val availableModels: Map<Provider, List<ModelInfo>> = emptyMap(),
    val customProviders: List<CustomProvider> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedProvider: Provider? = null, // For navigation to detail
    val selectedCustomProvider: CustomProvider? = null, // For navigation to custom provider detail
    val selectedWebSearchProvider: WebSearchProvider = WebSearchProvider.NONE,
    val webSearchApiKeys: Map<WebSearchProvider, String> = emptyMap(),
    val webSearchSecretKeys: Map<WebSearchProvider, String> = emptyMap(),
    val modelTestStatuses: Map<String, TestStatus> = emptyMap(),
    val openClawWebUiUrl: String? = null,
    val ocrModel: ModelInfo? = null,
    // Theme Settings
    val themeMode: String = "DYNAMIC",
    val customThemeColor: Int = -16777216, // Black
    val userBubbleColor: Int? = null,
    val aiBubbleColor: Int? = null,
    val globalCornerRadius: Float = 12f,
    val userAvatarUri: String? = null
)

sealed class TestStatus {
    object Loading : TestStatus()
    data class Success(val message: String) : TestStatus()
    data class Failure(val error: String) : TestStatus()
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    
    // Combine local state with repository flows
    val uiState: StateFlow<SettingsUiState> = combine(
        _uiState,
        // Observe API Keys
        combine(Provider.values().map { p -> settingsRepository.getApiKey(p).map { p to (it ?: "") } }) { arr: Array<Pair<Provider, String>> -> arr.toMap() },
        // Observe Available Models (Standard & Custom)
        combine(Provider.values().map { p -> 
            settingsRepository.getAvailableModels(p).map { models -> 
                p to models.sortedWith(compareByDescending<ModelInfo> { it.created }.thenByDescending { it.id }) 
            } 
        }) { arr: Array<Pair<Provider, List<ModelInfo>>> -> arr.toMap() },
        // Observe Custom Providers
        settingsRepository.getCustomProviders(),
        // Observe Web Search (Provider & Keys) & OpenClaw & OCR Model
        combine(
            combine(
                combine(
                    settingsRepository.getWebSearchProvider(),
                    combine(WebSearchProvider.values().map { p -> settingsRepository.getWebSearchApiKey(p).map { p to (it ?: "") } }) { arr: Array<Pair<WebSearchProvider, String>> -> arr.toMap() },
                    combine(WebSearchProvider.values().map { p -> settingsRepository.getWebSearchSecretKey(p).map { p to (it ?: "") } }) { arr: Array<Pair<WebSearchProvider, String>> -> arr.toMap() }
                ) { provider, keys, secretKeys -> Triple(provider, keys, secretKeys) },
                settingsRepository.getOpenClawWebUiUrl(),
                settingsRepository.getOcrModel()
            ) { webSearch, openClaw, ocrModelTriple -> Triple(webSearch, openClaw, ocrModelTriple) },
            // Observe Theme Settings
            combine(
                settingsRepository.getThemeMode(),
                settingsRepository.getCustomThemeColor(),
                settingsRepository.getUserBubbleColor(),
                settingsRepository.getAiBubbleColor(),
                combine(
                    settingsRepository.getGlobalCornerRadius(),
                    settingsRepository.getUserAvatarUri()
                ) { r, a -> r to a }
            ) { mode, color, userBubble, aiBubble, (radius, avatar) ->
                ThemeSettingsData(
                    mode = mode,
                    customColor = color,
                    userBubbleColor = userBubble,
                    aiBubbleColor = aiBubble,
                    radius = radius,
                    avatarUri = avatar
                )
            }
        ) { webSearchAndOcr, themeSettings -> webSearchAndOcr to themeSettings }
    ) { state, keys, allModels, customProviders, (webSearchAndOcrTriple, themeSettings) ->
        val (webSearchAndOpenClaw, openClawUrl, ocrModelTriple) = webSearchAndOcrTriple
        val (webSearchInfo, _, _) = Triple(webSearchAndOpenClaw.first, webSearchAndOpenClaw.second, webSearchAndOpenClaw.third) 
        
        // Resolve OCR Model
        val resolvedOcrModel = if (ocrModelTriple != null) {
            val (providerName, modelId, customProviderId) = ocrModelTriple
            val flatModels = allModels.values.flatten()
            flatModels.find { 
                it.provider.name == providerName && 
                it.id == modelId &&
                it.customProviderId == customProviderId
            }
        } else null

        state.copy(
            apiKeys = keys, 
            availableModels = allModels,
            customProviders = customProviders,
            selectedWebSearchProvider = webSearchAndOpenClaw.first,
            webSearchApiKeys = webSearchAndOpenClaw.second,
            webSearchSecretKeys = webSearchAndOpenClaw.third,
            openClawWebUiUrl = openClawUrl,
            ocrModel = resolvedOcrModel,
            // Theme
            themeMode = themeSettings.mode,
            customThemeColor = themeSettings.customColor,
            userBubbleColor = themeSettings.userBubbleColor,
            aiBubbleColor = themeSettings.aiBubbleColor,
            globalCornerRadius = themeSettings.radius,
            userAvatarUri = themeSettings.avatarUri
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun selectProvider(provider: Provider?) {
        _uiState.update { it.copy(selectedProvider = provider, selectedCustomProvider = null, error = null) }
    }

    fun selectCustomProvider(customProvider: CustomProvider?) {
        _uiState.update { it.copy(selectedCustomProvider = customProvider, error = null) }
    }

    fun selectWebSearchProvider(provider: WebSearchProvider) {
        viewModelScope.launch {
            settingsRepository.saveWebSearchProvider(provider)
        }
    }

    fun updateWebSearchApiKey(provider: WebSearchProvider, key: String) {
        val trimmedKey = key.trim()
        viewModelScope.launch {
            settingsRepository.saveWebSearchApiKey(provider, trimmedKey)
        }
    }

    fun updateWebSearchSecretKey(provider: WebSearchProvider, key: String) {
        val trimmedKey = key.trim()
        viewModelScope.launch {
            settingsRepository.saveWebSearchSecretKey(provider, trimmedKey)
        }
    }

    fun updateApiKey(provider: Provider, key: String) {
        val trimmedKey = key.trim()
        viewModelScope.launch {
            settingsRepository.saveApiKey(provider, trimmedKey)
        }
    }

    fun fetchAndSaveModels(provider: Provider, apiKey: String) {
        val trimmedKey = apiKey.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Save Key first
                settingsRepository.saveApiKey(provider, trimmedKey)

                val models = when (provider) {
                    Provider.OPENAI -> {
                        val response = RetrofitClient.openAIApi.getModels("Bearer $trimmedKey")
                        response.data.map { ModelInfo(it.id, it.id, provider, it.created ?: 0) }
                    }
                    Provider.DEEPSEEK, Provider.SILICONFLOW, Provider.VOLCENGINE, Provider.MIMO, Provider.MINIMAX, Provider.ZHIPU, Provider.ALIYUN, Provider.NVIDIA, Provider.MOONSHOT -> {
                        val url = "${provider.baseUrl}models"
                        val response = RetrofitClient.openAIApi.getModelsUrl(url, "Bearer $trimmedKey")
                        response.data.map { ModelInfo(it.id, it.id, provider, it.created ?: 0) }
                    }
                    // For others, we might just keep current list or implement specific logic later
                    // Currently just saving empty list or existing logic won't overwrite unless we explicitly do
                    else -> emptyList() 
                }

                val finalModels = if (models.isNotEmpty()) {
                    models.sortedWith(compareByDescending<ModelInfo> { it.created }.thenByDescending { it.id })
                } else {
                    val registryModels = ModelRegistry.allModels.filter { it.provider == provider }
                    registryModels.sortedWith(compareByDescending<ModelInfo> { it.created }.thenByDescending { it.id })
                }

                if (finalModels.isNotEmpty()) {
                    val existingModels = settingsRepository.getAvailableModels(provider).first()
                    val mergedModels = finalModels.map { newModel ->
                        existingModels.find { it.id == newModel.id }?.let { existing ->
                            newModel.copy(
                                enabled = existing.enabled,
                                overrideVisionSupport = existing.overrideVisionSupport
                            )
                        } ?: newModel
                    }
                    settingsRepository.saveAvailableModels(provider, mergedModels)
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                // Try fallback on error too
                val registryModels = ModelRegistry.allModels.filter { it.provider == provider }
                if (registryModels.isNotEmpty()) {
                    // Even on error, try to merge with existing enabled states
                    val existingModels = settingsRepository.getAvailableModels(provider).first()
                    val mergedModels = registryModels.map { newModel ->
                        existingModels.find { it.id == newModel.id }?.let { existing ->
                            newModel.copy(
                                enabled = existing.enabled,
                                overrideVisionSupport = existing.overrideVisionSupport
                            )
                        } ?: newModel
                    }
                    settingsRepository.saveAvailableModels(provider, mergedModels)
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to fetch models: ${e.message}") }
                }
            }
        }
    }

    fun addCustomProvider(name: String, baseUrl: String, apiKey: String, chatEndpoint: String) {
        viewModelScope.launch {
            val currentList = settingsRepository.getCustomProviders().first()
            val newProvider = CustomProvider(
                id = UUID.randomUUID().toString(),
                name = name,
                baseUrl = baseUrl,
                apiKey = apiKey,
                chatEndpoint = chatEndpoint
            )
            settingsRepository.saveCustomProviders(currentList + newProvider)
            // Optionally auto-fetch models
            fetchAndSaveCustomModels(newProvider)
        }
    }

    fun deleteCustomProvider(id: String) {
        viewModelScope.launch {
            val currentList = settingsRepository.getCustomProviders().first()
            settingsRepository.saveCustomProviders(currentList.filter { it.id != id })
        }
    }

    fun fetchAndSaveCustomModels(provider: CustomProvider) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Infer models endpoint
                val url = if (provider.baseUrl.endsWith("/")) "${provider.baseUrl}models" else "${provider.baseUrl}/models"
                
                val response = RetrofitClient.openAIApi.getModelsUrl(url, "Bearer ${provider.apiKey}")
                val models = response.data.map { 
                    ModelInfo(
                        id = it.id, 
                        name = it.id, 
                        provider = Provider.CUSTOM, 
                        created = it.created ?: 0,
                        customProviderId = provider.id
                    ) 
                }
                
                val existingModels = settingsRepository.getCustomProviderModels(provider.id).first()
                val mergedModels = models.map { newModel ->
                    existingModels.find { it.id == newModel.id }?.let { existing ->
                        newModel.copy(
                            enabled = existing.enabled,
                            overrideVisionSupport = existing.overrideVisionSupport
                        )
                    } ?: newModel
                }

                settingsRepository.saveCustomProviderModels(provider.id, mergedModels)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to fetch models for ${provider.name}: ${e.message}") }
            }
        }
    }

    fun updateCustomProvider(provider: CustomProvider) {
        viewModelScope.launch {
            val currentList = settingsRepository.getCustomProviders().first()
            val updatedList = currentList.map { if (it.id == provider.id) provider else it }
            settingsRepository.saveCustomProviders(updatedList)
        }
    }

    private suspend fun setVisionSupport(model: ModelInfo, enabled: Boolean) {
        val newModel = model.copy(overrideVisionSupport = enabled)
        if (model.provider == Provider.CUSTOM && model.customProviderId != null) {
            val currentModels = settingsRepository.getCustomProviderModels(model.customProviderId).first()
            val updatedModels = currentModels.map { if (it.id == model.id) newModel else it }
            settingsRepository.saveCustomProviderModels(model.customProviderId, updatedModels)
        } else {
            val currentModels = settingsRepository.getAvailableModels(model.provider).first()
            val updatedModels = currentModels.map { if (it.id == model.id) newModel else it }
            settingsRepository.saveAvailableModels(model.provider, updatedModels)
        }
    }

    fun toggleVisionSupport(model: ModelInfo) {
        viewModelScope.launch {
            setVisionSupport(model, !model.supportsVision)
        }
    }

    fun toggleModelEnabled(model: ModelInfo) {
        viewModelScope.launch {
            val newModel = model.copy(enabled = !model.enabled)
            if (model.provider == Provider.CUSTOM && model.customProviderId != null) {
                val currentModels = settingsRepository.getCustomProviderModels(model.customProviderId).first()
                val updatedModels = currentModels.map { if (it.id == model.id) newModel else it }
                settingsRepository.saveCustomProviderModels(model.customProviderId, updatedModels)
            } else {
                val currentModels = settingsRepository.getAvailableModels(model.provider).first()
                val updatedModels = currentModels.map { if (it.id == model.id) newModel else it }
                settingsRepository.saveAvailableModels(model.provider, updatedModels)
            }
        }
    }

    fun enableAllModels(provider: Provider, customProviderId: String? = null) {
        viewModelScope.launch {
            if (provider == Provider.CUSTOM && customProviderId != null) {
                val current = settingsRepository.getCustomProviderModels(customProviderId).first()
                val updated = current.map { it.copy(enabled = true) }
                settingsRepository.saveCustomProviderModels(customProviderId, updated)
            } else {
                val current = settingsRepository.getAvailableModels(provider).first()
                val updated = current.map { it.copy(enabled = true) }
                settingsRepository.saveAvailableModels(provider, updated)
            }
        }
    }

    private fun getModelKey(model: ModelInfo): String {
        return "${model.provider.name}|${model.customProviderId ?: ""}|${model.id}"
    }

    fun testAllModelsVisionCapability(models: List<ModelInfo>) {
        viewModelScope.launch {
            // Mark all as loading initially
            _uiState.update { state ->
                val newStatuses = state.modelTestStatuses.toMutableMap()
                models.forEach { newStatuses[getModelKey(it)] = TestStatus.Loading }
                state.copy(modelTestStatuses = newStatuses)
            }

            // Run tests in parallel
            models.forEach { model ->
                launch {
                    testVisionCapability(model) { success, msg ->
                        val status = if (success) TestStatus.Success(msg) else TestStatus.Failure(msg)
                        _uiState.update { state ->
                            state.copy(modelTestStatuses = state.modelTestStatuses + (getModelKey(model) to status))
                        }
                    }
                }
            }
        }
    }

    fun testVisionCapability(model: ModelInfo, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // 1x1 Red Pixel Base64
                val smallImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
                
                val apiKey = if (model.provider == Provider.CUSTOM && model.customProviderId != null) {
                    val providers = settingsRepository.getCustomProviders().first()
                    providers.find { it.id == model.customProviderId }?.apiKey ?: ""
                } else {
                    settingsRepository.getApiKey(model.provider).first() ?: ""
                }
                
                if (apiKey.isBlank()) {
                    onResult(false, "请先配置 API Key")
                    return@launch
                }

                val contentJson = if (model.provider == Provider.VOLCENGINE) {
                    // Volcengine specific format
                    // IMPORTANT: Volcengine has strict minimum pixel requirements (e.g. 14x14 or similar depending on model/detail)
                    // The 1x1 pixel image might be too small and cause 400 InvalidParameter.
                    // Using a slightly larger placeholder image (16x16 transparent PNG) to be safe.
                    // Base64 for 16x16 transparent PNG:
                    val safeImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAtJREFUOE9jZKAQAAAAQAABMz013wAAAABJRU5ErkJggg=="
                    
                    buildJsonArray {
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:image/png;base64,$safeImageBase64")
                            })
                        })
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", "Describe this image in 10 words.")
                        })
                    }
                } else {
                    // Standard format
                    buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", "Describe this image in 10 words.")
                        })
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:image/png;base64,$smallImageBase64")
                            })
                        })
                    }
                }

                val request = OpenAIChatRequest(
                    model = model.id,
                    messages = listOf(
                        OpenAIMessage(
                            role = "user",
                            content = contentJson
                        )
                    )
                )

                val response = if (model.provider == Provider.CUSTOM && model.customProviderId != null) {
                    val providers = settingsRepository.getCustomProviders().first()
                    val provider = providers.find { it.id == model.customProviderId }!!
                    val url = if (provider.baseUrl.endsWith("/")) "${provider.baseUrl}${provider.chatEndpoint.trimStart('/')}" else "${provider.baseUrl}/${provider.chatEndpoint.trimStart('/')}"
                    RetrofitClient.openAIApi.chatCompletionUrl(url, "Bearer $apiKey", request)
                } else {
                    when(model.provider) {
                         Provider.OPENAI -> RetrofitClient.openAIApi.chatCompletion("Bearer $apiKey", request)
                         else -> {
                             val baseUrl = model.provider.baseUrl
                             val url = "${baseUrl}chat/completions"
                             RetrofitClient.openAIApi.chatCompletionUrl(url, "Bearer $apiKey", request)
                         }
                    }
                }
                
                // If successful and not already supported, auto-enable vision
                if (!model.supportsVision) {
                    setVisionSupport(model, true)
                }

                onResult(true, "该模型支持视觉")
            } catch (e: Exception) {
                onResult(false, "该模型不支持视觉\n(${e.message})")
            }
        }
    }

    fun saveOpenClawWebUiUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.saveOpenClawWebUiUrl(url)
        }
    }

    fun clearOpenClawWebUiUrl() {
        viewModelScope.launch {
            settingsRepository.clearOpenClawWebUiUrl()
        }
    }

    fun selectOcrModel(model: ModelInfo) {
        viewModelScope.launch {
            settingsRepository.saveOcrModel(model.provider, model.id, model.customProviderId)
        }
    }

    // Theme Methods
    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.saveThemeMode(mode)
        }
    }

    fun updateCustomThemeColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.saveCustomThemeColor(color)
        }
    }

    fun updateUserBubbleColor(color: Int?) {
        viewModelScope.launch {
            settingsRepository.saveUserBubbleColor(color)
        }
    }

    fun updateAiBubbleColor(color: Int?) {
        viewModelScope.launch {
            settingsRepository.saveAiBubbleColor(color)
        }
    }

    fun updateGlobalCornerRadius(radius: Float) {
        viewModelScope.launch {
            settingsRepository.saveGlobalCornerRadius(radius)
        }
    }

    fun updateUserAvatarUri(uri: String?) {
        viewModelScope.launch {
            settingsRepository.saveUserAvatarUri(uri)
        }
    }
}

data class ThemeSettingsData(
    val mode: String,
    val customColor: Int,
    val userBubbleColor: Int?,
    val aiBubbleColor: Int?,
    val radius: Float,
    val avatarUri: String?
)


class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
