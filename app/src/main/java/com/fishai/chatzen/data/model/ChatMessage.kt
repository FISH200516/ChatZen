package com.fishai.chatzen.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val images: List<String>? = null,
    val reasoningContent: String? = null,
    val ocrContent: String? = null,
    val searchResults: List<WebSearchResult>? = null,
    val isSearching: Boolean = false,
    val isOcr: Boolean = false,
    val quotedContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String? = null
)

@Serializable
enum class Role {
    USER, ASSISTANT, SYSTEM
}
