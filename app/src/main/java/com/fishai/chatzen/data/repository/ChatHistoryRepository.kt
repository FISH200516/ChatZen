package com.fishai.chatzen.data.repository

import com.fishai.chatzen.data.database.dao.ChatDao
import com.fishai.chatzen.data.database.entity.ChatMessageEntity
import com.fishai.chatzen.data.manager.ImageStorageManager
import com.fishai.chatzen.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatHistoryRepository(
    private val chatDao: ChatDao,
    private val imageStorageManager: ImageStorageManager
) {

    suspend fun getAllMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        chatDao.getAllMessages().map { it.toDomain() }
    }

    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        // Save images to local storage and get file paths
        val persistentImages = imageStorageManager.saveImages(message.id, message.images)
        // Update message with persistent image paths
        val messageToSave = message.copy(images = persistentImages)
        chatDao.insertMessage(ChatMessageEntity.fromDomain(messageToSave))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAllMessages()
        imageStorageManager.clearAllImages()
    }
    
    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessage(id)
        imageStorageManager.deleteImages(id)
    }
}
