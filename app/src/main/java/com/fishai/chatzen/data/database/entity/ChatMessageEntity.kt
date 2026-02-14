package com.fishai.chatzen.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fishai.chatzen.data.model.ChatMessage
import com.fishai.chatzen.data.model.Role
import com.fishai.chatzen.data.model.WebSearchResult

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val role: Role,
    val content: String,
    val images: List<String>? = null,
    val reasoningContent: String? = null,
    val ocrContent: String? = null,
    val searchResults: List<WebSearchResult>? = null,
    val isSearching: Boolean = false,
    val timestamp: Long,
    val modelName: String? = null
) {
    fun toDomain(): ChatMessage {
        return ChatMessage(
            id = id,
            role = role,
            content = content,
            images = images,
            reasoningContent = reasoningContent,
            ocrContent = ocrContent,
            searchResults = searchResults,
            isSearching = isSearching,
            timestamp = timestamp,
            modelName = modelName
        )
    }

    companion object {
        fun fromDomain(message: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                role = message.role,
                content = message.content,
                images = message.images,
                reasoningContent = message.reasoningContent,
                ocrContent = message.ocrContent,
                searchResults = message.searchResults,
                isSearching = message.isSearching,
                timestamp = message.timestamp,
                modelName = message.modelName
            )
        }
    }
}
