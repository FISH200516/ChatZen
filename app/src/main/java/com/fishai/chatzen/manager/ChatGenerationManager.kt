package com.fishai.chatzen.manager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fishai.chatzen.data.model.ChatMessage
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.data.model.Role
import com.fishai.chatzen.data.model.WebSearchResult
import com.fishai.chatzen.data.repository.ChatHistoryRepository
import com.fishai.chatzen.data.repository.ChatRepository
import com.fishai.chatzen.data.repository.WebSearchRepository
import com.fishai.chatzen.notification.ChatGenerationState
import com.fishai.chatzen.notification.LiveUpdateNotificationManager
import com.fishai.chatzen.service.ChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val systemPrompt: String = "",
    val isWebSearchEnabled: Boolean = false,
    val ocrModel: ModelInfo? = null
)

data class GenerationState(
    val status: ChatGenerationState = ChatGenerationState.THINKING,
    val messageId: String? = null,
    val content: String = "",
    val reasoningContent: String? = null,
    val error: String? = null,
    val isSearching: Boolean = false,
    val isOcr: Boolean = false,
    val ocrContent: String = "",
    val searchResults: List<WebSearchResult> = emptyList()
)

class ChatGenerationManager(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val webSearchRepository: WebSearchRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) {
    private val _generationState = MutableStateFlow(GenerationState(status = ChatGenerationState.COMPLETED))
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    fun startGeneration(
        userMessage: ChatMessage,
        contextMessages: List<ChatMessage>,
        model: ModelInfo,
        config: GenerationConfig
    ) {
        stopGeneration() // Cancel any existing generation

        // Start Foreground Service
        startForegroundService()

        currentJob = managerScope.launch {
            try {
                // Initialize State
                val assistantMessageId = UUID.randomUUID().toString()
                _generationState.value = GenerationState(
                    status = ChatGenerationState.THINKING,
                    messageId = assistantMessageId,
                    isSearching = false
                )
                
                // Save initial placeholder to DB
                val placeholderMsg = ChatMessage(
                    id = assistantMessageId,
                    role = Role.ASSISTANT,
                    content = "",
                    modelName = model.name,
                    isSearching = true
                )
                chatHistoryRepository.saveMessage(placeholderMsg)
                
                LiveUpdateNotificationManager.startGeneration(model.name, userMessage.content)

                var promptToSend = userMessage.content
                if (userMessage.quotedContent != null) {
                    promptToSend = "> ${userMessage.quotedContent}\n\n$promptToSend"
                }

                // 1. OCR Logic
                if (model.supportsVision == false && config.ocrModel != null && !userMessage.images.isNullOrEmpty()) {
                    _generationState.update { it.copy(isOcr = true, status = ChatGenerationState.THINKING) }
                    
                    try {
                        val ocrMessage = ChatMessage(
                            role = Role.USER,
                            content = "请详细描述这张图片的内容。如果图片中包含文字，请将文字完整转录出来。",
                            images = userMessage.images
                        )
                        
                        var ocrFullContent = ""
                        chatRepository.sendMessageStream(listOf(ocrMessage), config.ocrModel).collect { chunk ->
                            ocrFullContent += chunk.content
                            _generationState.update { it.copy(ocrContent = ocrFullContent) }
                            
                            // Update DB with OCR progress? Maybe not necessary for every chunk, but good for persistence
                            // We don't save OCR content to the main message content yet
                        }
                        
                        promptToSend = "[图片视觉分析结果]:\n$ocrFullContent\n\n用户问题: $promptToSend"
                        _generationState.update { it.copy(isOcr = false) }
                        
                    } catch (e: Exception) {
                        Log.e("ChatGenerationManager", "OCR Failed", e)
                        // Continue without OCR or fail? Continue with error log usually
                        _generationState.update { it.copy(isOcr = false) }
                    }
                }

                // 2. Web Search Logic
                if (config.isWebSearchEnabled) {
                    _generationState.update { it.copy(isSearching = true, status = ChatGenerationState.THINKING) }
                    
                    // Update DB to show searching state
                    chatHistoryRepository.saveMessage(placeholderMsg.copy(isSearching = true))
                    
                    try {
                        val searchResults = webSearchRepository.search(promptToSend)
                        _generationState.update { it.copy(searchResults = searchResults, isSearching = false) }
                        
                        // Save search results to DB
                        chatHistoryRepository.saveMessage(placeholderMsg.copy(isSearching = false, searchResults = searchResults))

                        if (searchResults.isNotEmpty()) {
                            val sb = StringBuilder()
                            sb.append("Current Date: ${java.time.LocalDate.now()}\n")
                            sb.append("User Query Context: $promptToSend\n") 
                            sb.append("Web Search Results:\n")
                            searchResults.forEachIndexed { index, result ->
                                sb.append("${index + 1}. Title: ${result.title}\n   URL: ${result.url}\n   Snippet: ${result.snippet}\n\n")
                            }
                            sb.append("Please answer the user's query based on the search results above.")
                            promptToSend = sb.toString()
                        }
                    } catch (e: Exception) {
                        Log.e("ChatGenerationManager", "Web Search Failed", e)
                        _generationState.update { it.copy(isSearching = false) }
                    }
                }

                // 3. Chat Streaming
                _generationState.update { it.copy(status = ChatGenerationState.GENERATING) }
                LiveUpdateNotificationManager.updateProgress(ChatGenerationState.GENERATING)
                
                // Prepare messages for API
                // We need to construct the list of messages to send.
                // The contextMessages passed in should be the history (excluding the current user message if it's new, but typically we pass history + current user message).
                // Let's assume contextMessages includes the history. We need to append the modified user message.
                
                val messagesToSend = contextMessages.toMutableList()
                
                // Handle System Prompt
                val systemMessages = if (config.systemPrompt.isNotBlank()) {
                    listOf(ChatMessage(role = Role.SYSTEM, content = config.systemPrompt))
                } else {
                    messagesToSend.filter { it.role == Role.SYSTEM }
                }
                
                val historyMessages = messagesToSend.filter { it.role != Role.SYSTEM }.takeLast(6).toMutableList()
                
                // If the last message is the user message we just sent, we need to replace it with the modified prompt
                // But wait, the `userMessage` argument is the one we just sent.
                // We should append the `userMessage` (with modified content) to the history.
                
                val strippedImages = if (model.supportsVision == false) null else userMessage.images
                
                // Construct the final message object for API
                val finalUserMessageForApi = userMessage.copy(
                    content = promptToSend,
                    quotedContent = null, // Already merged
                    images = strippedImages
                )
                
                val finalMessagesToSend = systemMessages + historyMessages + finalUserMessageForApi

                var fullContent = ""
                var fullReasoning = ""
                var hasStartedStreaming = false

                chatRepository.sendMessageStream(
                    messages = finalMessagesToSend,
                    model = model,
                    temperature = config.temperature.toDouble(),
                    topP = config.topP.toDouble()
                ).collect { chunk ->
                    fullContent += chunk.content
                    fullReasoning += chunk.reasoningContent
                    
                    if (!hasStartedStreaming && fullContent.isNotEmpty()) {
                        hasStartedStreaming = true
                        _generationState.update { it.copy(status = ChatGenerationState.STREAMING) }
                    }
                    
                    if (hasStartedStreaming) {
                        LiveUpdateNotificationManager.updateProgress(ChatGenerationState.STREAMING, fullContent)
                    }

                    _generationState.update { 
                        it.copy(
                            content = fullContent, 
                            reasoningContent = if (fullReasoning.isNotEmpty()) fullReasoning else null
                        ) 
                    }
                    
                    // Throttle DB updates? For now, update every chunk is safe enough for Room usually, 
                    // but maybe optimization is needed later.
                    // We update the message in DB so if app crashes, we have partial result.
                    val updatedMsg = placeholderMsg.copy(
                        content = fullContent,
                        reasoningContent = if (fullReasoning.isNotEmpty()) fullReasoning else null,
                        isSearching = false,
                        isOcr = false,
                        searchResults = _generationState.value.searchResults,
                        ocrContent = _generationState.value.ocrContent
                    )
                    chatHistoryRepository.saveMessage(updatedMsg)
                }

                // Complete
                _generationState.update { it.copy(status = ChatGenerationState.COMPLETED) }
                LiveUpdateNotificationManager.completeGeneration(fullContent.take(50))

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("ChatGenerationManager", "Generation Error", e)
                    _generationState.update { it.copy(error = e.message, status = ChatGenerationState.COMPLETED) }
                    
                    // Save error message to DB
                    val errorMsg = ChatMessage(
                        role = Role.SYSTEM, 
                        content = "生成失败: ${e.message}",
                        id = UUID.randomUUID().toString()
                    )
                    chatHistoryRepository.saveMessage(errorMsg)
                }
            } finally {
                stopForegroundService()
            }
        }
    }

    fun stopGeneration() {
        currentJob?.cancel()
        currentJob = null
        _generationState.update { it.copy(status = ChatGenerationState.COMPLETED) }
        LiveUpdateNotificationManager.cancelGeneration()
        stopForegroundService()
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(application, ChatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(application, ChatService::class.java)
            application.stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
