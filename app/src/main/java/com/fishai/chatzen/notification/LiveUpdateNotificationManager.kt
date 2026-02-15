package com.fishai.chatzen.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.fishai.chatzen.MainActivity
import com.fishai.chatzen.R

enum class ChatGenerationState(val progress: Int) {
    THINKING(25),
    GENERATING(50),
    STREAMING(75),
    COMPLETED(100)
}

object LiveUpdateNotificationManager {
    private const val TAG = "LiveUpdateNotification"
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context
    
    const val CHANNEL_ID = "chat_generation_channel"
    private const val CHANNEL_NAME = "Chat Generation"
    private const val NOTIFICATION_ID = 1001

    private var currentState: ChatGenerationState? = null
    private var currentContentPreview: String = ""
    private var currentModelName: String = ""
    private var currentUserQuestion: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    fun initialize(context: Context, notifManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = notifManager
            appContext = context.applicationContext
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                IMPORTANCE_DEFAULT
            ).apply {
                description = "AI对话生成进度通知"
            }
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Live Update Notification Manager initialized")
        }
    }

    fun isLiveUpdatesSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 36
    }

    @RequiresApi(36)
    fun canPostPromotedNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= 36) {
            try {
                notificationManager.canPostPromotedNotifications()
            } catch (e: NoSuchMethodError) {
                Log.d(TAG, "canPostPromotedNotifications not available: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    fun startGeneration(modelName: String = "", userQuestion: String = "") {
        currentState = ChatGenerationState.THINKING
        currentContentPreview = ""
        currentModelName = modelName
        currentUserQuestion = userQuestion
        if (Build.VERSION.SDK_INT >= 36) {
            updateLiveUpdateNotification(ChatGenerationState.THINKING)
        } else {
            updateLegacyNotification(ChatGenerationState.THINKING)
        }
    }

    fun updateProgress(state: ChatGenerationState, contentPreview: String = "") {
        if (state != currentState || contentPreview.isNotEmpty()) {
            currentState = state
            currentContentPreview = contentPreview
            if (Build.VERSION.SDK_INT >= 36) {
                updateLiveUpdateNotification(state, contentPreview)
            } else {
                updateLegacyNotification(state, contentPreview)
            }
        }
    }

    fun completeGeneration(summary: String = "") {
        currentState = ChatGenerationState.COMPLETED
        val contentText = if (summary.isNotEmpty()) summary else appContext.getString(R.string.notification_completed_content)
        
        if (Build.VERSION.SDK_INT >= 36) {
            val notification = buildLiveUpdateNotification(ChatGenerationState.COMPLETED, contentText)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            val notification = buildLegacyNotification(ChatGenerationState.COMPLETED, contentText)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            dismissNotification()
        }, 8000)
    }

    fun cancelGeneration() {
        dismissNotification()
    }

    fun getNotificationForService(state: ChatGenerationState, contentPreview: String = ""): android.app.Notification {
        return if (Build.VERSION.SDK_INT >= 36) {
            buildLiveUpdateNotification(state, contentPreview)
        } else {
            buildLegacyNotification(state, contentPreview)
        }
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        currentState = null
        currentContentPreview = ""
        currentModelName = ""
        currentUserQuestion = ""
    }

    private fun updateLegacyNotification(state: ChatGenerationState, contentPreview: String = "") {
        val notification = buildLegacyNotification(state, contentPreview)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @RequiresApi(36)
    private fun updateLiveUpdateNotification(state: ChatGenerationState, contentPreview: String = "") {
        val notification = buildLiveUpdateNotification(state, contentPreview)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildLegacyNotification(state: ChatGenerationState, contentPreview: String): android.app.Notification {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content, icon, progress) = getNotificationData(state, contentPreview)

        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(state != ChatGenerationState.COMPLETED)
            .setProgress(100, progress, state == ChatGenerationState.THINKING)
            .build()
    }

    @RequiresApi(36)
    private fun buildLiveUpdateNotification(state: ChatGenerationState, contentPreview: String): android.app.Notification {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content, icon, progress, shortText) = getNotificationData(state, contentPreview)

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(state != ChatGenerationState.COMPLETED)
            .setRequestPromotedOngoing(state != ChatGenerationState.COMPLETED)
            .setShortCriticalText(shortText)
            .setStyle(buildProgressStyle(state, progress))

        Log.d(TAG, "Built Live Update notification for state: $state, progress: $progress")
        
        return builder.build()
    }

    @RequiresApi(36)
    private fun buildProgressStyle(state: ChatGenerationState, progress: Int): NotificationCompat.ProgressStyle {
        val pointColor = Color.valueOf(
            103f / 255f,
            80f / 255f,
            164f / 255f,
            1f
        ).toArgb()
        val segmentColor = Color.valueOf(
            180f / 255f,
            160f / 255f,
            220f / 255f,
            1f
        ).toArgb()

        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressPoints(
                listOf(
                    NotificationCompat.ProgressStyle.Point(25).setColor(pointColor),
                    NotificationCompat.ProgressStyle.Point(50).setColor(pointColor),
                    NotificationCompat.ProgressStyle.Point(75).setColor(pointColor),
                    NotificationCompat.ProgressStyle.Point(100).setColor(pointColor)
                )
            )
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor),
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor),
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor),
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor)
                )
            )

        when (state) {
            ChatGenerationState.THINKING -> {
                progressStyle.setProgressIndeterminate(true)
            }
            else -> {
                progressStyle.setProgress(progress)
            }
        }

        return progressStyle
    }

    private fun getNotificationData(state: ChatGenerationState, contentPreview: String): NotificationData {
        val title = if (currentModelName.isNotEmpty()) currentModelName else appContext.getString(R.string.app_name)
        val formattedQuestion = if (currentUserQuestion.isNotEmpty()) {
            val sanitized = currentUserQuestion.replace("\n", " ")
            if (sanitized.length > 20) sanitized.take(20) + "..." else sanitized
        } else ""

        return when (state) {
            ChatGenerationState.THINKING -> NotificationData(
                title = title,
                content = if (formattedQuestion.isNotEmpty()) "正在思考：$formattedQuestion" else appContext.getString(R.string.notification_thinking_content),
                icon = R.drawable.ic_ai_thinking,
                progress = 0,
                shortText = appContext.getString(R.string.notification_thinking_short)
            )
            ChatGenerationState.GENERATING -> NotificationData(
                title = title,
                content = if (formattedQuestion.isNotEmpty()) "正在深度思考：$formattedQuestion" else appContext.getString(R.string.notification_generating_content),
                icon = R.drawable.ic_ai_generating,
                progress = 50,
                shortText = appContext.getString(R.string.notification_generating_short)
            )
            ChatGenerationState.STREAMING -> {
                val content = if (formattedQuestion.isNotEmpty()) {
                    "正在回答：$formattedQuestion"
                } else if (contentPreview.isNotEmpty()) {
                    if (contentPreview.length > 50) contentPreview.take(50) + "..." else contentPreview
                } else {
                    appContext.getString(R.string.notification_streaming_content)
                }
                NotificationData(
                    title = title,
                    content = content,
                    icon = R.drawable.ic_ai_generating,
                    progress = 75,
                    shortText = appContext.getString(R.string.notification_streaming_short)
                )
            }
            ChatGenerationState.COMPLETED -> NotificationData(
                title = title,
                content = if (formattedQuestion.isNotEmpty()) "回答完成：$formattedQuestion" else contentPreview.ifEmpty { appContext.getString(R.string.notification_completed_content) },
                icon = R.drawable.ic_ai_complete,
                progress = 100,
                shortText = appContext.getString(R.string.notification_completed_short)
            )
        }
    }

    private data class NotificationData(
        val title: String,
        val content: String,
        val icon: Int,
        val progress: Int,
        val shortText: String
    )
}
