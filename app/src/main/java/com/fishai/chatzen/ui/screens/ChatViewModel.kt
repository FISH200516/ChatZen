package com.fishai.chatzen.ui.screens

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
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    private var currentGenerationJob: kotlinx.coroutines.Job? = null
    
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
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _uiState.update { it.copy(isLoading = false) }
        // 添加一个“对话取消”的系统提示（可选，或者直接在UI上显示状态）
        // 这里根据需求，添加一条取消的提示消息
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

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val currentState = uiState.value
        val model = currentState.currentModel ?: return
        
        // 1. Prepare User Message
        // Handle quoted message
        val quotedContent = currentState.quotedMessage
        
        // Construct prompt with quote for API/Search context, but keep them separate in ChatMessage object
        val finalContent = if (quotedContent != null) {
            "> $quotedContent\n\n$content"
        } else {
            content
        }
        
        // Clear quoted message state
        clearQuotedMessage()
        
        val currentImages = currentState.selectedImages.ifEmpty { null }
        val userMessage = ChatMessage(
            role = Role.USER, 
            content = content, // Store only user input in content
            quotedContent = quotedContent, // Store quote separately
            images = currentImages
        )
        
        // 2. Update UI immediately
        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null,
                selectedImages = emptyList(), // Clear images after sending
                quotedMessage = null,
                isInputExpanded = false
            )
        }

        currentGenerationJob = viewModelScope.launch {
            // Save user message
            chatHistoryRepository.saveMessage(userMessage)

            var promptToSend = finalContent
            var assistantMessageId: String? = null
            
            // OCR Logic Interception
            // Only trigger if:
            // 1. Model does NOT support vision
            // 2. OCR model is configured
            // 3. There are images to process
            if (model.supportsVision == false && currentState.ocrModel != null && userMessage.images != null && userMessage.images.isNotEmpty()) {
                val ocrModel = currentState.ocrModel
                
                // Create assistant message for OCR status
                val tempId = java.util.UUID.randomUUID().toString()
                assistantMessageId = tempId
                val assistantMessage = ChatMessage(
                    id = tempId,
                    role = Role.ASSISTANT,
                    content = "", // Empty content initially
                    isSearching = true, 
                    isOcr = true
                )
                _uiState.update { it.copy(messages = it.messages + assistantMessage) }
                
                try {
                    val ocrMessage = ChatMessage(
                        role = Role.USER,
                        content = "请详细描述这张图片的内容。如果图片中包含文字，请将文字完整转录出来。",
                        images = userMessage.images
                    )
                    
                    var ocrFullContent = ""
                    
                    // Stream OCR Response
                    chatRepository.sendMessageStream(listOf(ocrMessage), ocrModel).collect { chunk ->
                        ocrFullContent += chunk.content
                        _uiState.update { state ->
                            state.copy(messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(ocrContent = ocrFullContent)
                                } else msg
                            })
                        }
                    }
                    
                    // Update prompt with OCR result
                    promptToSend = "[图片视觉分析结果]:\n$ocrFullContent\n\n用户问题: $finalContent"
                    
                    // Mark OCR as finished in UI state (update isOcr=false, isSearching=true for next step)
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(isOcr = false, isSearching = true) // Switch to searching/thinking state
                            } else msg
                        })
                    }
                    
                } catch (e: Exception) {
                     _uiState.update { it.copy(isLoading = false, error = "图片分析失败: ${e.message}") }
                     // Remove placeholder or show error
                     return@launch
                }
            }

            try {
                // Web Search Integration (Existing Logic)
                if (uiState.value.isWebSearchEnabled) {
                     // If we already have an assistantMessageId from OCR, we might need to handle this carefully.
                     // But typically users won't use both simultaneously or we can chain them.
                     // For now, let's assume if OCR is active, we skip Web Search or handle it sequentially.
                     // If assistantMessageId is null (no OCR), we create one for search.
                     
                     if (assistantMessageId == null) {
                         val tempId = java.util.UUID.randomUUID().toString()
                         assistantMessageId = tempId
                         val placeholderMsg = ChatMessage(
                             id = tempId,
                             role = Role.ASSISTANT,
                             content = "",
                             isSearching = true
                         )
                         _uiState.update { it.copy(messages = it.messages + placeholderMsg) }
                         chatHistoryRepository.saveMessage(placeholderMsg)
                     }
                     
                     // ... Web Search Logic ...
                     try {
                         val searchResults = webSearchRepository.search(finalContent)
                         
                         // Update placeholder with results
                         _uiState.update { state ->
                             state.copy(messages = state.messages.map { msg ->
                                 if (msg.id == assistantMessageId) {
                                     msg.copy(
                                         isSearching = false,
                                         searchResults = searchResults
                                     )
                                 } else msg
                             })
                         }
                         
                         // Save updated placeholder with results
                         _uiState.value.messages.find { it.id == assistantMessageId }?.let {
                             chatHistoryRepository.saveMessage(it)
                         }

                         if (searchResults.isNotEmpty()) {
                             val sb = StringBuilder()
                             sb.append("Current Date: ${java.time.LocalDate.now()}\n")
                             // If promptToSend was already modified by OCR, we append to it? 
                             // Or we wrap it?
                             // Let's assume we append context to promptToSend.
                             sb.append("User Query Context: $promptToSend\n") 
                             sb.append("Web Search Results:\n")
                             searchResults.forEachIndexed { index, result ->
                                 sb.append("${index + 1}. Title: ${result.title}\n   URL: ${result.url}\n   Snippet: ${result.snippet}\n\n")
                             }
                             sb.append("Please answer the user's query based on the search results above.")
                             promptToSend = sb.toString()
                         }
                     } catch (e: Exception) {
                         e.printStackTrace()
                         _uiState.update { state ->
                             state.copy(messages = state.messages.map { msg ->
                                 if (msg.id == assistantMessageId) msg.copy(isSearching = false) else msg
                             })
                         }
                     }
                }

                // Stream response - Pass the original userMessage (with images) to be preserved in UI,
                // but pass the modified promptToSend for the API request context logic
                streamResponse(userMessage, promptToSend, assistantMessageId)

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    handleSendError(e)
                }
            }
        }
    }

    private suspend fun streamResponse(userMessage: ChatMessage, promptToSend: String, existingAssistantId: String?) {
        val model = uiState.value.currentModel ?: return
        var currentMessageId: String? = existingAssistantId
        var fullContent = ""
        var fullReasoningContent = ""
        
        // Limit context to last 3 rounds (6 messages) + System prompt if any
        val messagesToSend = _uiState.value.messages.let { msgs ->
            // Use configured system prompt if available, otherwise fallback to history system messages
            val configuredSystemPrompt = _uiState.value.systemPrompt
            val systemMessages = if (configuredSystemPrompt.isNotBlank()) {
                listOf(ChatMessage(role = Role.SYSTEM, content = configuredSystemPrompt))
            } else {
                msgs.filter { it.role == Role.SYSTEM }
            }
            
            // Filter out the placeholder assistant message if it exists
            val relevantMsgs = msgs.filter { it.role != Role.SYSTEM && it.id != existingAssistantId }
            val contextMessages = relevantMsgs.takeLast(6).toMutableList()
            
            // Replace the last message (which is the current user message) with the augmented prompt if modified
            if (contextMessages.isNotEmpty() && contextMessages.last().id == userMessage.id) {
                // IMPORTANT: If model does NOT support vision, we must strip images from the request
                // Otherwise APIs like DeepSeek/SiliconFlow will return 400 Bad Request
                // However, we should only strip them from the REQUEST, not from the displayed message history.
                // The contextMessages list here is constructed specifically for the API request.
                // But `userMessage` here is a reference to the message object that might be displayed in UI if we are not careful.
                // Wait, `messagesToSend` is a new list, but `contextMessages` contains objects from `_uiState.value.messages`.
                // We need to ensure we don't modify the object in the UI state by accident, but `copy()` does create a new object.
                // The issue is: `streamResponse` uses `userMessage` passed as argument to find the message in `contextMessages`.
                
                val strippedImages = if (model.supportsVision == false) null else userMessage.images
                // We use promptToSend as the content, which might include OCR results, Search results, or the quoted content.
                // Since promptToSend already includes the quoted content (if any), we must set quotedContent to null 
                // to prevent ChatRepository from appending it again.
                contextMessages[contextMessages.lastIndex] = userMessage.copy(
                    content = promptToSend, 
                    quotedContent = null,
                    images = strippedImages
                )
            }
            
            systemMessages + contextMessages
        }

        chatRepository.sendMessageStream(
            messages = messagesToSend,
            model = model,
            temperature = _uiState.value.temperature.toDouble(),
            topP = _uiState.value.topP.toDouble()
        ).collect { chunk ->
            fullContent += chunk.content
            fullReasoningContent += chunk.reasoningContent
            
            if (currentMessageId == null) {
                // First chunk, create message
                val assistantMessage = ChatMessage(
                    role = Role.ASSISTANT, 
                    content = fullContent,
                    reasoningContent = if (fullReasoningContent.isNotEmpty()) fullReasoningContent else null,
                    modelName = model.name
                )
                currentMessageId = assistantMessage.id
                _uiState.update { 
                    it.copy(
                        messages = it.messages + assistantMessage
                    )
                }
            } else {
                // Update existing message
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == currentMessageId) {
                            msg.copy(
                                content = fullContent,
                                reasoningContent = if (fullReasoningContent.isNotEmpty()) fullReasoningContent else null,
                                isSearching = false // Ensure false
                            )
                        } else {
                            msg
                        }
                    }
                    state.copy(
                        messages = updatedMessages
                    )
                }
            }
        }
        
        // Save final assistant message
        if (currentMessageId != null) {
            val finalMsg = _uiState.value.messages.find { it.id == currentMessageId }
            if (finalMsg != null) {
                chatHistoryRepository.saveMessage(finalMsg)
            }
        }
        
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun handleSendError(e: Exception) {
        val rawErrorMsg = if (e is retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                if (errorBody.contains("insufficient_quota") || 
                    errorBody.contains("balance is insufficient") ||
                    errorBody.contains("30001")) {
                    "您的账户余额不足，请充值后重试。"
                } else {
                    "HTTP ${e.code()}: $errorBody"
                }
            } else {
                "HTTP ${e.code()} ${e.message()}"
            }
        } else {
            e.message ?: "未知错误 (${e.javaClass.simpleName})"
        }
        
        _uiState.update { 
            it.copy(
                isLoading = false,
                error = rawErrorMsg
            )
        }
        val errorMessage = ChatMessage(role = Role.SYSTEM, content = "系统提示: $rawErrorMsg")
        _uiState.update { it.copy(messages = it.messages + errorMessage) }
    }
    
    // Legacy method removed or kept private if needed? 
    // performSendMessage was merged into sendMessage


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
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository, settingsRepository, webSearchRepository, chatHistoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
