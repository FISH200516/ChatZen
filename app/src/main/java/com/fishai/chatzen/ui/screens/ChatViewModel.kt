package com.fishai.chatzen.ui.screens

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fishai.chatzen.data.model.ChatMessage
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.data.model.Provider
import com.fishai.chatzen.data.model.Role
import com.fishai.chatzen.data.repository.ChatHistoryRepository
import com.fishai.chatzen.data.repository.ChatRepository
import com.fishai.chatzen.data.repository.SettingsRepository
import com.fishai.chatzen.data.repository.WebSearchRepository
import com.fishai.chatzen.notification.ChatGenerationState
import com.fishai.chatzen.notification.LiveUpdateNotificationManager
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

import com.fishai.chatzen.data.model.WebSearchResult
import com.fishai.chatzen.manager.ChatGenerationManager
import com.fishai.chatzen.manager.GenerationConfig
import com.fishai.chatzen.manager.GenerationState

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentModel: ModelInfo? = null,
    val availableModels: List<ModelInfo> = emptyList(),
    val isDeepThinkingEnabled: Boolean = false,
    val isWebSearchEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isInputExpanded: Boolean = false,
    val hasConfiguredApiKey: Boolean = false,
    val customProviderNames: Map<String, String> = emptyMap(),
    val favoriteModels: Set<String> = emptySet(),
    val quotedMessage: String? = null,
    val selectedImages: List<String> = emptyList(), // Base64 strings
    val openClawWebUiUrl: String? = null,
    val ocrModel: ModelInfo? = null,
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val isModelSettingsVisible: Boolean = false,
    val userAvatarUri: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val webSearchRepository: WebSearchRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val application: Application,
    private val chatGenerationManager: ChatGenerationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    // removed currentGenerationJob
    
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            LiveUpdateNotificationManager.initialize(application, notificationManager)
        }
        
        // Subscribe to generation updates
        viewModelScope.launch {
            chatGenerationManager.generationState.collect { state ->
                handleGenerationUpdate(state)
            }
        }
    }
    
    // ... (rest of init) ...

    private fun handleGenerationUpdate(state: GenerationState) {
        val messageId = state.messageId ?: return
        
        _uiState.update { ui ->
            var messages = ui.messages
            val existingMessage = messages.find { it.id == messageId }
            
            if (existingMessage != null) {
                // Update existing
                messages = messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            content = state.content,
                            reasoningContent = state.reasoningContent,
                            isSearching = state.isSearching,
                            isOcr = state.isOcr,
                            ocrContent = state.ocrContent,
                            searchResults = state.searchResults
                        )
                    } else msg
                }
            } else {
                // Add new if it's a valid update (not just a completed empty state)
                val hasContent = state.content.isNotEmpty() ||
                        !state.reasoningContent.isNullOrBlank() ||
                        state.isSearching ||
                        state.isOcr ||
                        !state.ocrContent.isNullOrBlank() ||
                        !state.searchResults.isNullOrEmpty()

                if (hasContent) {
                    val newMessage = ChatMessage(
                        id = messageId,
                        role = Role.ASSISTANT,
                        content = state.content,
                        reasoningContent = state.reasoningContent,
                        isSearching = state.isSearching,
                        isOcr = state.isOcr,
                        ocrContent = state.ocrContent,
                        searchResults = state.searchResults,
                        modelName = ui.currentModel?.name
                    )
                    messages = messages + newMessage
                }
            }
            
            ui.copy(
                messages = messages,
                isLoading = state.status != ChatGenerationState.COMPLETED,
                error = state.error
            )
        }
        
        if (state.error != null) {
             val errorMessage = ChatMessage(role = Role.SYSTEM, content = "系统提示: ${state.error}")
             _uiState.update { it.copy(messages = it.messages + errorMessage) }
        }
    }



    // ...

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val currentState = uiState.value
        val model = currentState.currentModel ?: return
        
        // 1. Prepare User Message
        val quotedContent = currentState.quotedMessage
        val currentImages = currentState.selectedImages.ifEmpty { null }
        
        val userMessage = ChatMessage(
            role = Role.USER, 
            content = content, 
            quotedContent = quotedContent, 
            images = currentImages
        )
        
        // 2. Update UI immediately
        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null,
                selectedImages = emptyList(),
                quotedMessage = null,
                isInputExpanded = false
            )
        }

        // Save user message
        viewModelScope.launch {
            chatHistoryRepository.saveMessage(userMessage)
        }

        // 3. Start Generation via Manager
        val config = GenerationConfig(
            temperature = currentState.temperature,
            topP = currentState.topP,
            systemPrompt = currentState.systemPrompt,
            isWebSearchEnabled = currentState.isWebSearchEnabled,
            ocrModel = currentState.ocrModel
        )
        
        // Pass history (all current messages excluding the one we just added? 
        // No, we just added it to UI.
        // We should pass the *previous* messages.
        // `currentState.messages` is the state *before* we added the new user message.
        // So `currentState.messages` is the history.
        // Wait, `_uiState.update` happens before.
        // `val currentState = uiState.value` happens at the top.
        // So `currentState.messages` DOES NOT include the new user message.
        // Perfect.
        
        chatGenerationManager.startGeneration(
            userMessage = userMessage,
            contextMessages = currentState.messages,
            model = model,
            config = config
        )
    }

    // Combine local state with available models from settings
    val uiState: StateFlow<ChatUiState> = combine(
        _uiState,
        combine(Provider.values().map { settingsRepository.getAvailableModels(it) }) { lists ->
            lists.flatMap { it.toList() }.sortedByDescending { it.created }
        },
        combine(
            settingsRepository.getSelectedModel(),
            settingsRepository.getCustomProviders(),
            settingsRepository.getFavoriteModels(),
            settingsRepository.getReasoningModels()
        ) { selected, custom, favorites, reasoning -> 
            object {
                val selected = selected
                val custom = custom
                val favorites = favorites
                val reasoning = reasoning
            }
        },
        combine(Provider.values().map { settingsRepository.getApiKey(it) }) { keys ->
            keys.any { !it.isNullOrBlank() }
        },
        combine(
            combine(
                settingsRepository.getWebSearchEnabled(),
                settingsRepository.getOpenClawWebUiUrl()
            ) { enabled, url -> Pair(enabled, url) },
            settingsRepository.getOcrModel(),
            settingsRepository.getUserAvatarUri()
        ) { webSearch, ocrModel, avatar -> Triple(webSearch, ocrModel, avatar) }
    ) { state, rawModels, settingsData, hasApiKey, (webSearchPair, ocrModelTriple, avatarUri) ->
        val (isWebSearchEnabled, openClawUrl) = webSearchPair
        
        // Enhance models with reasoning capability from persistence
        val models = rawModels.map { model ->
            if (settingsData.reasoning.contains(model.id)) {
                model.copy(isReasoningDetected = true)
            } else {
                model
            }
        }
        
        // If current model is null, try to restore from saved preference or select first available
        val current = state.currentModel
        
        val validatedModel = if (current == null) {
            // Try to restore from saved preference
            if (settingsData.selected != null) {
                val (providerName, modelId, customProviderId) = settingsData.selected
                models.find { 
                    it.provider.name == providerName && 
                    it.id == modelId &&
                    it.customProviderId == customProviderId
                } ?: models.firstOrNull()
            } else {
                models.firstOrNull()
            }
        } else {
            // Validate current model still exists
            // Use the enhanced 'models' list to check, so we preserve the isReasoningDetected flag if we switch or re-validate
            val foundModel = models.find { it.id == current.id && it.provider == current.provider && it.customProviderId == current.customProviderId }
            
            if (foundModel == null) {
                 // Current model no longer available, switch to saved or first
                 if (settingsData.selected != null) {
                     val (providerName, modelId, customProviderId) = settingsData.selected
                     models.find { 
                         it.provider.name == providerName && 
                         it.id == modelId &&
                         it.customProviderId == customProviderId
                     } ?: models.firstOrNull()
                 } else {
                     models.firstOrNull()
                 }
            } else {
                foundModel // Use the found model from the new list which has the updated flag
            }
        }
        
        // Auto-update deep thinking state if model changes
        val deepThinking = if (validatedModel?.supportsDeepThinking == true) true else false

        // Resolve OCR Model
        val resolvedOcrModel = if (ocrModelTriple != null) {
            val (providerName, modelId, customProviderId) = ocrModelTriple
            models.find { 
                it.provider.name == providerName && 
                it.id == modelId &&
                it.customProviderId == customProviderId
            }
        } else null

        state.copy(
            availableModels = models,
            currentModel = validatedModel,
            isDeepThinkingEnabled = if (validatedModel?.supportsDeepThinking == true) state.isDeepThinkingEnabled else false,
            isWebSearchEnabled = isWebSearchEnabled,
            hasConfiguredApiKey = hasApiKey,
            customProviderNames = settingsData.custom.associate { it.id to it.name },
            favoriteModels = settingsData.favorites,
            openClawWebUiUrl = openClawUrl,
            ocrModel = resolvedOcrModel,
            userAvatarUri = avatarUri
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        // Load history from database
        viewModelScope.launch {
            val history = chatHistoryRepository.getAllMessages()
            if (history.isNotEmpty()) {
                _uiState.update { it.copy(messages = history) }
            }
        }

        // Restore persisted settings
        viewModelScope.launch {
            launch {
                settingsRepository.getTemperature().collect { temp ->
                    _uiState.update { it.copy(temperature = temp) }
                }
            }
            launch {
                settingsRepository.getTopP().collect { topP ->
                    _uiState.update { it.copy(topP = topP) }
                }
            }
            launch {
                settingsRepository.getSystemPrompt().collect { prompt ->
                    _uiState.update { it.copy(systemPrompt = prompt) }
                }
            }
        }
    }

    fun stopGeneration() {
        chatGenerationManager.stopGeneration()
        _uiState.update { it.copy(isLoading = false) }
        
        val cancelMessage = ChatMessage(
            role = Role.SYSTEM, 
            content = "对话取消",
            id = java.util.UUID.randomUUID().toString()
        )
        _uiState.update { 
            it.copy(messages = it.messages + cancelMessage) 
        }
    }

    fun toggleFavorite(model: ModelInfo) {
        viewModelScope.launch {
            settingsRepository.toggleFavoriteModel(model.provider.name, model.id, model.customProviderId)
        }
    }

    fun setInputExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isInputExpanded = expanded) }
    }
    
    fun setQuotedMessage(content: String?) {
        _uiState.update { it.copy(quotedMessage = content, isInputExpanded = true) }
    }
    
    fun clearQuotedMessage() {
        _uiState.update { it.copy(quotedMessage = null) }
    }

    fun addImage(base64: String) {
        _uiState.update { it.copy(selectedImages = it.selectedImages + base64) }
    }

    fun removeImage(index: Int) {
        _uiState.update { 
            val newList = it.selectedImages.toMutableList()
            if (index in newList.indices) {
                newList.removeAt(index)
            }
            it.copy(selectedImages = newList)
        }
    }

    fun clearImages() {
        _uiState.update { it.copy(selectedImages = emptyList()) }
    }

    fun startNewChat() {
        viewModelScope.launch {
            chatHistoryRepository.clearHistory()
            _uiState.update { it.copy(messages = emptyList(), error = null) }
        }
    }

    fun toggleDeepThinking() {
        val model = uiState.value.currentModel ?: return
        
        // If the model is a REASONING type, it forces deep thinking on.
        // We do not allow turning it off as it's inherent to the model.
        if (model.type == com.fishai.chatzen.data.model.ModelType.REASONING) {
            // Ensure it's on
            if (!uiState.value.isDeepThinkingEnabled) {
                _uiState.update { it.copy(isDeepThinkingEnabled = true) }
            }
            return
        }
        
        if (model.supportsDeepThinking) {
            _uiState.update { it.copy(isDeepThinkingEnabled = !it.isDeepThinkingEnabled) }
        }
    }

    fun setModelSettingsVisible(visible: Boolean) {
        _uiState.update { it.copy(isModelSettingsVisible = visible) }
    }

    fun updateSystemPrompt(prompt: String) {
        _uiState.update { it.copy(systemPrompt = prompt) }
        viewModelScope.launch {
            settingsRepository.saveSystemPrompt(prompt)
        }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp) }
        viewModelScope.launch {
            settingsRepository.saveTemperature(temp)
        }
    }

    fun updateTopP(topP: Float) {
        _uiState.update { it.copy(topP = topP) }
        viewModelScope.launch {
            settingsRepository.saveTopP(topP)
        }
    }

    fun toggleWebSearch() {
        viewModelScope.launch {
            settingsRepository.setWebSearchEnabled(!uiState.value.isWebSearchEnabled)
        }
    }




    fun selectModel(model: ModelInfo) {
        _uiState.update { 
            it.copy(
                currentModel = model,
                isDeepThinkingEnabled = model.supportsDeepThinking // Auto-enable if supported
            ) 
        }
        viewModelScope.launch {
            settingsRepository.saveSelectedModel(model.provider, model.id, model.customProviderId)
        }
    }
}

class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val webSearchRepository: WebSearchRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val application: Application,
    private val chatGenerationManager: ChatGenerationManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository, settingsRepository, webSearchRepository, chatHistoryRepository, application, chatGenerationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
