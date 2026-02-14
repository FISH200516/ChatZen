package com.fishai.chatzen.data.repository

import com.fishai.chatzen.data.api.ClaudeChatRequest
import com.fishai.chatzen.data.api.ClaudeMessage
import com.fishai.chatzen.data.api.GeminiChatRequest
import com.fishai.chatzen.data.api.GeminiContent
import com.fishai.chatzen.data.api.GeminiPart
import com.fishai.chatzen.data.api.OpenAIChatRequest
import com.fishai.chatzen.data.api.GeminiInlineData
import com.fishai.chatzen.data.api.OpenAIMessage
import com.fishai.chatzen.data.api.RetrofitClient
import com.fishai.chatzen.data.model.ChatMessage
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.data.model.Provider
import com.fishai.chatzen.data.model.Role
import kotlinx.coroutines.flow.first

import com.fishai.chatzen.data.api.OpenAIChoice
import com.fishai.chatzen.data.api.OpenAIModelListResponse
import com.fishai.chatzen.data.api.OpenAIStreamResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import android.util.Base64

import com.fishai.chatzen.data.api.OpenAIStreamOptions
import com.fishai.chatzen.data.api.OpenAIResponsesRequest
import com.fishai.chatzen.data.api.OpenAIResponsesStreamResponse

data class StreamChunk(
    val content: String = "",
    val reasoningContent: String = "",
    val isFinished: Boolean = false
)

class ChatRepository(
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun parseImageString(imageString: String): Pair<String, String> {
        if (imageString.startsWith("/")) {
            // It's a file path
            return try {
                val bytes = File(imageString).readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "image/jpeg" to base64
            } catch (e: Exception) {
                e.printStackTrace()
                "image/jpeg" to ""
            }
        }

        return if (imageString.startsWith("data:")) {
            val mimeType = imageString.substringAfter("data:").substringBefore(";")
            val base64 = imageString.substringAfter("base64,")
            mimeType to base64
        } else {
            "image/jpeg" to imageString
        }
    }

    suspend fun sendMessage(messages: List<ChatMessage>, model: ModelInfo): String {
        // Fallback for non-streaming calls if needed, or remove if we fully switch to streaming
        // For now, keeping implementation but it might not be used if UI switches to stream
        val apiKey = if (model.provider == Provider.CUSTOM) {
            val customProviders = settingsRepository.getCustomProviders().first()
            val customProvider = customProviders.find { it.id == model.customProviderId }
            customProvider?.apiKey ?: throw IllegalStateException("Custom provider configuration not found")
        } else {
            settingsRepository.getApiKey(model.provider).first()?.trim()
                ?: throw IllegalStateException("Please set API Key for ${model.provider.name} in Settings")
        }

        return when (model.provider) {
            Provider.OPENAI, Provider.DEEPSEEK, Provider.SILICONFLOW, Provider.VOLCENGINE, Provider.MIMO, Provider.MINIMAX, Provider.ZHIPU, Provider.ALIYUN, Provider.NVIDIA, Provider.MOONSHOT, Provider.GROK, Provider.CUSTOM -> {
                // Use streaming implementation to ensure consistency and avoid 400 errors with some providers (e.g. Volcengine)
                // that might behave differently with non-streaming requests involving images.
                val sb = StringBuilder()
                sendMessageStream(messages, model).collect { chunk ->
                    sb.append(chunk.content)
                }
                sb.toString()
            }
            Provider.CLAUDE -> {
                sendClaude(messages, model, apiKey)
            }
            Provider.GEMINI -> {
                sendGemini(messages, model, apiKey)
            }
        }
    }

    fun sendMessageStream(
        messages: List<ChatMessage>, 
        model: ModelInfo,
        temperature: Double? = null,
        topP: Double? = null
    ): Flow<StreamChunk> = flow {
        val apiKey = if (model.provider == Provider.CUSTOM) {
            val customProviders = settingsRepository.getCustomProviders().first()
            val customProvider = customProviders.find { it.id == model.customProviderId }
            customProvider?.apiKey ?: throw IllegalStateException("Custom provider configuration not found")
        } else {
            settingsRepository.getApiKey(model.provider).first()?.trim()
                ?: throw IllegalStateException("Please set API Key for ${model.provider.name} in Settings")
        }

        when (model.provider) {
            Provider.OPENAI, Provider.DEEPSEEK, Provider.SILICONFLOW, Provider.VOLCENGINE, Provider.MIMO, Provider.MINIMAX, Provider.ZHIPU, Provider.ALIYUN, Provider.NVIDIA, Provider.MOONSHOT, Provider.GROK, Provider.CUSTOM -> {
                val openAIMessages = messages.map {
                    val actualContent = if (it.quotedContent != null) "> ${it.quotedContent}\n\n${it.content}" else it.content

                    val contentElement = if (it.images.isNullOrEmpty()) {
                        JsonPrimitive(actualContent)
                    } else {
                        buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", actualContent)
                            })
                            it.images.forEach { imageString ->
                                add(buildJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") {
                                        val url = if (imageString.startsWith("/")) {
                                            try {
                                                val bytes = File(imageString).readBytes()
                                                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                                "data:image/jpeg;base64,$base64"
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        } else if (imageString.startsWith("data:")) {
                                            imageString 
                                        } else {
                                            "data:image/jpeg;base64,$imageString"
                                        }
                                        put("url", url)
                                    }
                                })
                            }
                        }
                    }

                    OpenAIMessage(
                        role = when (it.role) {
                            Role.USER -> "user"
                            Role.ASSISTANT -> "assistant"
                            Role.SYSTEM -> "system"
                        },
                        content = contentElement
                    )
                }

                var isResponsesApi = false
                if (model.provider == Provider.CUSTOM) {
                    val customProviders = settingsRepository.getCustomProviders().first()
                    val customProvider = customProviders.find { it.id == model.customProviderId }
                    if (customProvider != null && customProvider.chatEndpoint.contains("/responses")) {
                        isResponsesApi = true
                    }
                }

                var isThinkingInContent = false

                val call = if (isResponsesApi) {
                    val customProviders = settingsRepository.getCustomProviders().first()
                    val customProvider = customProviders.find { it.id == model.customProviderId }!!
                    val baseUrl = customProvider.baseUrl.trimEnd('/')
                    val path = customProvider.chatEndpoint.trimStart('/')
                    val url = "$baseUrl/$path"
                    
                    val request = OpenAIResponsesRequest(
                        model = model.id,
                        input = openAIMessages,
                        messages = openAIMessages,
                        stream = true,
                        streamOptions = OpenAIStreamOptions(includeUsage = true),
                        temperature = temperature,
                        topP = topP
                    )
                    RetrofitClient.openAIApi.responsesUrlStream(url, "Bearer $apiKey", request)
                } else {
                    val request = OpenAIChatRequest(
                        model = model.id,
                        messages = openAIMessages,
                        stream = true,
                        streamOptions = OpenAIStreamOptions(includeUsage = true),
                        temperature = temperature,
                        topP = topP,
                        reasoningSplit = if (model.provider == Provider.MINIMAX) true else null
                    )
                    
                    if (model.provider == Provider.OPENAI) {
                        RetrofitClient.openAIApi.chatCompletionStream("Bearer $apiKey", request)
                    } else if (model.provider == Provider.CUSTOM) {
                        val customProviders = settingsRepository.getCustomProviders().first()
                        val customProvider = customProviders.find { it.id == model.customProviderId }
                            ?: throw IllegalStateException("Custom provider not found")
                        val baseUrl = customProvider.baseUrl.trimEnd('/')
                        val path = customProvider.chatEndpoint.trimStart('/')
                        val url = "$baseUrl/$path"
                        RetrofitClient.openAIApi.chatCompletionUrlStream(url, "Bearer $apiKey", request)
                    } else {
                        val url = "${model.provider.baseUrl}chat/completions"
                        RetrofitClient.openAIApi.chatCompletionUrlStream(url, "Bearer $apiKey", request)
                    }
                }

                val response = call.execute()
                if (response.isSuccessful) {
                    val source = response.body()?.source()
                    if (source != null) {
                        while (!source.exhausted()) {
                            // Use peek to check if there is data available without blocking indefinitely if connection stalls
                            // However, standard readUtf8Line is usually blocking until line break or end of stream.
                            // The issue might be OkHttp buffering. 
                            // We should not wait for full buffer.
                            
                            val line = source.readUtf8Line()
                            if (line != null) {
                                if (line.startsWith("data:")) {
                                    val data = line.removePrefix("data:").trim()
                                    if (data == "[DONE]") break
                                    
                                    try {
                                        if (isResponsesApi) {
                                            val chunk = json.decodeFromString<OpenAIResponsesStreamResponse>(data)
                                            
                                            // Log usage if present
                                            chunk.usage?.let { usage ->
                                                usageRepository.logUsage(model.provider.name, model.id, usage.promptTokens, usage.completionTokens)
                                            }

                                            val content = chunk.outputText 
                                                ?: chunk.output?.firstOrNull()?.content
                                                ?: chunk.choices?.firstOrNull()?.delta?.content
                                                
                                            if (content != null) {
                                                emit(StreamChunk(content = content))
                                            }
                                        } else {
                                            val chunk = json.decodeFromString<OpenAIStreamResponse>(data)
                                            
                                            // Log usage if present
                                            chunk.usage?.let { usage ->
                                                usageRepository.logUsage(model.provider.name, model.id, usage.promptTokens, usage.completionTokens)
                                            }
        
                                            val delta = chunk.choices.firstOrNull()?.delta
                                            if (delta != null) {
                                                // Handle standard reasoning content (DeepSeek/R1) and MiniMax reasoning details
                                                var reasoning = delta.reasoningContent 
                                                    ?: delta.reasoningDetails?.joinToString("") { it.text ?: "" } 
                                                    ?: ""
                                                
                                                var content = delta.content ?: ""
                                                
                                                // Handle models (like MiniMax via NVIDIA) that put reasoning inside <think> tags in content
                                                // Simple state machine to extract <think>...</think> content from 'content' stream to 'reasoning'
                                                if (content.isNotEmpty()) {
                                                    // Check for start tag
                                                    if (content.contains("<think>")) {
                                                        isThinkingInContent = true
                                                        val parts = content.split("<think>", limit = 2)
                                                        // Part before tag is content
                                                        content = parts[0]
                                                        // Part after tag is reasoning (if any)
                                                        if (parts.size > 1) {
                                                            // Check if end tag is also in this chunk (rare but possible)
                                                            if (parts[1].contains("</think>")) {
                                                                val subParts = parts[1].split("</think>", limit = 2)
                                                                reasoning += subParts[0]
                                                                content += subParts[1]
                                                                isThinkingInContent = false
                                                            } else {
                                                                reasoning += parts[1]
                                                            }
                                                        }
                                                    } 
                                                    // Check for end tag if we are currently thinking
                                                    else if (isThinkingInContent) {
                                                        if (content.contains("</think>")) {
                                                            val parts = content.split("</think>", limit = 2)
                                                            reasoning += parts[0]
                                                            content = parts[1]
                                                            isThinkingInContent = false
                                                        } else {
                                                            // All content is actually reasoning
                                                            reasoning += content
                                                            content = ""
                                                        }
                                                    }
                                                }

                                                // If reasoning content is detected, mark the model as reasoning-capable
                                                if (reasoning.isNotEmpty()) {
                                                    settingsRepository.markModelAsReasoning(model.id)
                                                }
                                                
                                                emit(StreamChunk(
                                                    content = content,
                                                    reasoningContent = reasoning
                                                ))
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore parse errors for partial chunks or keep-alive
                                        println("Error parsing stream chunk: ${e.message}")
                                        println("Chunk data: $data")
                                    }
                                }
                            }
                        }
                    }
                } else {
                     throw Exception("HTTP ${response.code()} ${response.message()}")
                }
            }
            // Fallback for others (non-streaming simulation)
            else -> {
                val fullResponse = sendMessage(messages, model)
                emit(StreamChunk(content = fullResponse, isFinished = true))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun sendClaude(messages: List<ChatMessage>, model: ModelInfo, apiKey: String): String {
        val claudeMessages = messages.filter { it.role != Role.SYSTEM }.map {
            val actualContent = if (it.quotedContent != null) "> ${it.quotedContent}\n\n${it.content}" else it.content
            
            val contentElement = if (it.images.isNullOrEmpty()) {
                JsonPrimitive(actualContent)
            } else {
                buildJsonArray {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", actualContent)
                    })
                    it.images.forEach { imageString ->
                        val (mimeType, cleanBase64) = parseImageString(imageString)
                        add(buildJsonObject {
                            put("type", "image")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", mimeType)
                                put("data", cleanBase64)
                            }
                        })
                    }
                }
            }

            ClaudeMessage(
                role = if (it.role == Role.USER) "user" else "assistant",
                content = contentElement
            )
        }
        // Claude system prompt is top-level parameter, but for simplicity ignoring system for now or need to extract it
        
        val request = ClaudeChatRequest(model = model.id, messages = claudeMessages)
        val response = RetrofitClient.claudeApi.chatCompletion(apiKey, request)
        
        response.usage?.let {
            usageRepository.logUsage(model.provider.name, model.id, it.inputTokens, it.outputTokens)
        }

        return response.content.firstOrNull()?.text ?: ""
    }

    private suspend fun sendGemini(messages: List<ChatMessage>, model: ModelInfo, apiKey: String): String {
        val geminiContents = messages.map {
            val actualContent = if (it.quotedContent != null) "> ${it.quotedContent}\n\n${it.content}" else it.content
            
            val parts = if (it.images.isNullOrEmpty()) {
                listOf(GeminiPart(text = actualContent))
            } else {
                val list = mutableListOf<GeminiPart>()
                list.add(GeminiPart(text = actualContent))
                it.images.forEach { imageString ->
                    val (mimeType, cleanBase64) = parseImageString(imageString)
                    list.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = cleanBase64)))
                }
                list
            }
            
            GeminiContent(
                role = if (it.role == Role.USER) "user" else "model",
                parts = parts
            )
        }
        val request = GeminiChatRequest(contents = geminiContents)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.id}:generateContent?key=$apiKey"
        val response = RetrofitClient.geminiApi.chatCompletion(url, request)
        
        response.usageMetadata?.let {
            usageRepository.logUsage(model.provider.name, model.id, it.promptTokenCount, it.candidatesTokenCount)
        }

        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
    }
}
