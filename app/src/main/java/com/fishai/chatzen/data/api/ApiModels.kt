package com.fishai.chatzen.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// --- OpenAI Compatible Models ---

@Serializable
data class OpenAIModelListResponse(
    val data: List<OpenAIModel>
)

@Serializable
data class OpenAIModel(
    val id: String,
    val created: Long? = null,
    val owned_by: String? = null
)

@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val stream: Boolean = false,
    @SerialName("response_format") val responseFormat: OpenAIResponseFormat? = null,
    @SerialName("stream_options") val streamOptions: OpenAIStreamOptions? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("reasoning_split") val reasoningSplit: Boolean? = null
)

@Serializable
data class OpenAIResponseFormat(
    val type: String
)

@Serializable
data class OpenAIStreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean
)

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class OpenAIChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@Serializable
data class OpenAIChoice(
    val index: Int = 0,
    val message: OpenAIMessage? = null,
    val delta: OpenAIDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    @SerialName("reasoning_details") val reasoningDetails: List<MiniMaxReasoningDetail>? = null
)

@Serializable
data class MiniMaxReasoningDetail(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

// --- OpenAI Responses API (GPT-5/o3) ---

@Serializable
data class OpenAIResponsesRequest(
    val model: String,
    val input: List<OpenAIMessage>, // Assuming list of messages is accepted as input
    val messages: List<OpenAIMessage>? = null, // Backward compatibility for some providers
    val stream: Boolean = false,
    @SerialName("stream_options") val streamOptions: OpenAIStreamOptions? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null
)

@Serializable
data class OpenAIResponsesStreamResponse(
    val id: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val output: List<OpenAIResponseItem>? = null,
    @SerialName("output_text") val outputText: String? = null,
    val usage: OpenAIUsage? = null,
    val choices: List<OpenAIChoice>? = null
)

@Serializable
data class OpenAIResponseItem(
    val type: String? = null,
    val role: String? = null,
    val content: String? = null // It might be a string or list, but let's try string first or lenient parser
)

// --- Claude Models ---

@Serializable
data class ClaudeChatRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 4096
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ClaudeChatResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val usage: ClaudeUsage? = null
)

@Serializable
data class ClaudeContent(
    val type: String,
    val text: String
)

@Serializable
data class ClaudeUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

// --- Gemini Models ---

@Serializable
data class GeminiChatRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GeminiChatResponse(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsage? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null,
    val index: Int? = null
)

@Serializable
data class GeminiUsage(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)
