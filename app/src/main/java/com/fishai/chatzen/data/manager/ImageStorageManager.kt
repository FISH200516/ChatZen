package com.fishai.chatzen.data.manager

import android.content.Context
import android.util.Base64
import java.io.File

class ImageStorageManager(private val context: Context) {
    
    fun saveImages(messageId: String, images: List<String>?): List<String>? {
        if (images.isNullOrEmpty()) return null
        
        val savedPaths = mutableListOf<String>()
        val directory = File(context.filesDir, "chat_images/$messageId")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        images.forEachIndexed { index, imageData ->
            // If it's already a file path, just keep it
            if (imageData.startsWith("/")) {
                savedPaths.add(imageData)
                return@forEachIndexed
            }

            try {
                // Handle Base64 string
                val cleanBase64 = if (imageData.startsWith("data:")) {
                    imageData.substringAfter("base64,")
                } else {
                    imageData
                }
                
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val file = File(directory, "img_$index.jpg")
                file.writeBytes(bytes)
                savedPaths.add(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                // Skip failed images but continue
            }
        }
        return if (savedPaths.isEmpty()) null else savedPaths
    }
    
    fun deleteImages(messageId: String) {
        val directory = File(context.filesDir, "chat_images/$messageId")
        if (directory.exists()) {
            directory.deleteRecursively()
        }
    }
    
    fun clearAllImages() {
        val directory = File(context.filesDir, "chat_images")
        if (directory.exists()) {
            directory.deleteRecursively()
        }
    }
}
