package com.fishai.chatzen.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fishai.chatzen.R
import com.fishai.chatzen.ui.components.ModelSelectionContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Filter only vision supported models
    val visionModels = uiState.availableModels.mapValues { (_, models) ->
        models.filter { it.supportsVision }
    }.values.flatten()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OCR / 视觉辅助模型",
                        style = MaterialTheme.typography.titleLarge,
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
        Surface(modifier = Modifier.padding(padding)) {
            ModelSelectionContent(
                currentModel = uiState.ocrModel,
                availableModels = visionModels,
                customProviderNames = uiState.customProviders.associate { it.id to it.name },
                favoriteModels = emptySet(), 
                onModelSelected = { viewModel.selectOcrModel(it) },
                onToggleFavorite = { /* No favorite toggling here for now */ }
            )
        }
    }
}
