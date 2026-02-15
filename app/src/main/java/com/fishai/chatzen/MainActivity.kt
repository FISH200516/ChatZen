package com.fishai.chatzen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.fishai.chatzen.data.database.AppDatabase
import com.fishai.chatzen.data.manager.ImageStorageManager
import com.fishai.chatzen.data.repository.ChatHistoryRepository
import com.fishai.chatzen.data.repository.ChatRepository
import com.fishai.chatzen.data.repository.SettingsRepository
import com.fishai.chatzen.data.repository.UsageRepository
import com.fishai.chatzen.data.repository.WebSearchRepository
import com.fishai.chatzen.manager.ChatGenerationManager
import com.fishai.chatzen.ui.navigation.ChatZenApp
import com.fishai.chatzen.ui.screens.ChatViewModel
import com.fishai.chatzen.ui.screens.ChatViewModelFactory
import com.fishai.chatzen.ui.screens.SettingsViewModel
import com.fishai.chatzen.ui.screens.SettingsViewModelFactory
import com.fishai.chatzen.ui.screens.UsageViewModel
import com.fishai.chatzen.ui.screens.UsageViewModelFactory
import com.fishai.chatzen.ui.theme.ChatZenTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "chatzen-db"
        )
        .fallbackToDestructiveMigration()
        .build()

        val settingsRepository = SettingsRepository(applicationContext)
        val usageRepository = UsageRepository(database.usageDao())
        val imageStorageManager = ImageStorageManager(applicationContext)
        val chatHistoryRepository = ChatHistoryRepository(database.chatDao(), imageStorageManager)
        val chatRepository = ChatRepository(settingsRepository, usageRepository)
        val webSearchRepository = WebSearchRepository(settingsRepository)
        val chatGenerationManager = ChatGenerationManager(
            application,
            chatRepository,
            webSearchRepository,
            chatHistoryRepository
        )

        val settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(settingsRepository)
        )[SettingsViewModel::class.java]

        val chatViewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(chatRepository, settingsRepository, webSearchRepository, chatHistoryRepository, application, chatGenerationManager)
        )[ChatViewModel::class.java]
        
        val usageViewModel = ViewModelProvider(
            this,
            UsageViewModelFactory(usageRepository, settingsRepository)
        )[UsageViewModel::class.java]

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()
            
            ChatZenTheme(
                themeMode = settingsState.themeMode,
                customThemeColor = settingsState.customThemeColor,
                userBubbleColor = settingsState.userBubbleColor,
                aiBubbleColor = settingsState.aiBubbleColor,
                globalCornerRadius = settingsState.globalCornerRadius
            ) {
                ChatZenApp(
                    chatViewModel = chatViewModel,
                    settingsViewModel = settingsViewModel,
                    usageViewModel = usageViewModel
                )
            }
        }
    }
}
