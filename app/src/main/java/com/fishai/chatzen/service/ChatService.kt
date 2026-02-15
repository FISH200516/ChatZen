package com.fishai.chatzen.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.fishai.chatzen.notification.ChatGenerationState
import com.fishai.chatzen.notification.LiveUpdateNotificationManager

class ChatService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground immediately with a default notification
        // The actual content updates will be handled by LiveUpdateNotificationManager calls from the Manager
        val notification = LiveUpdateNotificationManager.getNotificationForService(
            ChatGenerationState.THINKING, 
            "正在思考..."
        )
        try {
            startForeground(1001, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return START_NOT_STICKY
    }
}
