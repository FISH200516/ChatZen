package com.fishai.chatzen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishai.chatzen.R

import androidx.compose.foundation.interaction.MutableInteractionSource
import com.fishai.chatzen.ui.utils.scaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenClawSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var url by remember { mutableStateOf("") }
    
    // Initialize with saved URL
    LaunchedEffect(uiState.openClawWebUiUrl) {
        url = uiState.openClawWebUiUrl ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "便捷网页控制台设置",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "配置常用 API 控制台地址",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "在此处填写您常用的 API 服务商控制台网址（如 OpenAI Dashboard ，硅基流动，火山引擎等）。保存后，在对话页长按发送按钮即可快速跳转，便于查看用量或管理 Key。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Web UI URL") },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val saveInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            onClick = { viewModel.saveOpenClawWebUiUrl(url) },
                            modifier = Modifier.weight(1f).scaleOnPress(saveInteractionSource),
                            interactionSource = saveInteractionSource
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存")
                        }

                        if (!uiState.openClawWebUiUrl.isNullOrEmpty()) {
                            val deleteInteractionSource = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { 
                                    viewModel.clearOpenClawWebUiUrl()
                                    url = ""
                                },
                                modifier = Modifier.weight(1f).scaleOnPress(deleteInteractionSource),
                                interactionSource = deleteInteractionSource,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("清空")
                            }
                        }
                    }
                }
            }
        }
    }
}
