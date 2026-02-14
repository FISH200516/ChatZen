package com.fishai.chatzen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import com.fishai.chatzen.R
import com.fishai.chatzen.ui.utils.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToServiceProviders: () -> Unit,
    onNavigateToWebSearchSettings: () -> Unit,
    onNavigateToOpenClawSettings: () -> Unit,
    onNavigateToOcrSettings: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsGroup(title = "AI 服务配置") {
                    SettingsItem(
                        title = stringResource(R.string.select_service_provider),
                        icon = Icons.Default.Cloud,
                        onClick = onNavigateToServiceProviders
                    )
                    SettingsItem(
                        title = stringResource(R.string.select_web_search_service),
                        icon = Icons.Default.Search,
                        onClick = onNavigateToWebSearchSettings
                    )
                    SettingsItem(
                        title = "OCR / 视觉辅助设置",
                        icon = Icons.Default.Image,
                        onClick = onNavigateToOcrSettings
                    )
                }
            }

            item {
                SettingsGroup(title = "应用设置") {
                    SettingsItem(
                        title = "外观设置 (Theme & UI)",
                        icon = Icons.Default.Palette,
                        onClick = onNavigateToThemeSettings
                    )
                    SettingsItem(
                        title = "配置便捷网页控制台",
                        icon = Icons.Default.Language,
                        onClick = onNavigateToOpenClawSettings
                    )
                }
            }

            item {
                SettingsGroup(title = "其他") {
                    SettingsItem(
                        title = "关于 ChatZen",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = null)
    }
}
