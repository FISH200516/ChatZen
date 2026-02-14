package com.fishai.chatzen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.ui.utils.bounceClick
import com.fishai.chatzen.ui.utils.scaleOnPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface

import androidx.compose.ui.res.stringResource
import com.fishai.chatzen.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder

import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    currentModel: ModelInfo?,
    availableModels: List<ModelInfo>,
    customProviderNames: Map<String, String> = emptyMap(),
    favoriteModels: Set<String> = emptySet(),
    onModelSelected: (ModelInfo) -> Unit,
    onToggleFavorite: (ModelInfo) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Row(
        modifier = Modifier
            .bounceClick(onClick = { showBottomSheet = true })
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (currentModel != null) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                // Provider Name (Top)
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    val displayProviderName = if (currentModel.provider == com.fishai.chatzen.data.model.Provider.CUSTOM && currentModel.customProviderId != null) {
                        customProviderNames[currentModel.customProviderId] ?: stringResource(currentModel.provider.displayNameResId)
                    } else {
                        stringResource(currentModel.provider.displayNameResId)
                    }

                    Text(
                        text = displayProviderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Model Name (Bottom, Smaller)
                val cleanName = currentModel.name
                    .replace(Regex("^.*/"), "")
                    .replace(Regex(":[\\w-]+$"), "")
                    .replace(Regex("-\\d{8}$"), "")
                
                Text(
                    text = cleanName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = stringResource(R.string.select_model),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = stringResource(R.string.select_model),
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color(0xFFFAFAFA)
        ) {
            ModelSelectionContent(
                currentModel = currentModel,
                availableModels = availableModels,
                customProviderNames = customProviderNames,
                favoriteModels = favoriteModels,
                onModelSelected = {
                    onModelSelected(it)
                    showBottomSheet = false
                },
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
fun ModelSelectionContent(
    currentModel: ModelInfo?,
    availableModels: List<ModelInfo>,
    customProviderNames: Map<String, String> = emptyMap(),
    favoriteModels: Set<String> = emptySet(),
    onModelSelected: (ModelInfo) -> Unit,
    onToggleFavorite: (ModelInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Group by provider, but for custom provider, group by custom provider ID
        val groupedModels = availableModels.groupBy { model ->
            if (model.provider == com.fishai.chatzen.data.model.Provider.CUSTOM && model.customProviderId != null) {
                // Use a composite key or just the ID to differentiate
                "CUSTOM:${model.customProviderId}"
            } else {
                model.provider.name
            }
        }
        
        if (groupedModels.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_models_configure),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        groupedModels.forEach { (groupKey, models) ->
            item {
                var expanded by remember { mutableStateOf(false) }
                
                // Determine display name for the group
                val defaultCustomName = stringResource(R.string.custom_provider_default_name)
                val displayName = if (groupKey.startsWith("CUSTOM:")) {
                    val customId = groupKey.removePrefix("CUSTOM:")
                    customProviderNames[customId] ?: defaultCustomName
                } else {
                    // Find the provider enum by name
                    val provider = try {
                        com.fishai.chatzen.data.model.Provider.valueOf(groupKey)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    
                    if (provider != null) {
                        stringResource(provider.displayNameResId)
                    } else {
                        groupKey
                    }
                }
                
                // WRAP GROUP IN SURFACE
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(onClick = { expanded = !expanded })
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand)
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                val manufacturerGroups = models.groupBy { it.manufacturer }
                                // Only show subheaders if it's NOT a custom provider group (unless we want to group by manufacturer within custom provider too?)
                                // For custom providers, usually we just list models directly.
                                // But keeping existing logic is fine, let's see.
                                
                                val isCustomGroup = groupKey.startsWith("CUSTOM:")
                                val showSubHeaders = !isCustomGroup && (manufacturerGroups.size > 1 || 
                                        (manufacturerGroups.isNotEmpty() && manufacturerGroups.keys.first() != displayName))

                                if (showSubHeaders) {
                                    manufacturerGroups.forEach { (manufacturer, manuModels) ->
                                        ManufacturerGroup(
                                            manufacturer = manufacturer,
                                            models = manuModels,
                                            currentModel = currentModel,
                                            favoriteModels = favoriteModels,
                                            onModelSelected = onModelSelected,
                                            onToggleFavorite = onToggleFavorite
                                        )
                                    }
                                } else {
                                    models.forEachIndexed { index, model ->
                                        val isFavorite = favoriteModels.contains("${model.provider.name}|${model.id}|${model.customProviderId ?: ""}")
                                        ModelRow(
                                            model = model, 
                                            currentModel = currentModel, 
                                            isFavorite = isFavorite,
                                            showDivider = index < models.lastIndex,
                                            onModelSelected = onModelSelected,
                                            onToggleFavorite = onToggleFavorite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { 
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ManufacturerGroup(
    manufacturer: String,
    models: List<ModelInfo>,
    currentModel: ModelInfo?,
    favoriteModels: Set<String>,
    onModelSelected: (ModelInfo) -> Unit,
    onToggleFavorite: (ModelInfo) -> Unit
) {
    // Default to collapsed unless user manually expands it.
    // If the group contains the currently selected model, we could default to true, 
    // but per user request "default collapsed", we start with false.
    // However, to ensure visibility of current selection, we'll auto-expand if it contains current model.
    val hasSelection = currentModel != null && models.any { it.id == currentModel.id && it.provider == currentModel.provider }
    var expanded by remember { mutableStateOf(hasSelection) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(onClick = { expanded = !expanded })
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = manufacturer,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                models.forEachIndexed { index, model ->
                    val isFavorite = favoriteModels.contains("${model.provider.name}|${model.id}|${model.customProviderId ?: ""}")
                    ModelRow(
                        model = model, 
                        currentModel = currentModel, 
                        isFavorite = isFavorite,
                        showDivider = index < models.lastIndex,
                        onModelSelected = onModelSelected,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModelRow(
    model: ModelInfo,
    currentModel: ModelInfo?,
    isFavorite: Boolean,
    showDivider: Boolean = true,
    onModelSelected: (ModelInfo) -> Unit,
    onToggleFavorite: (ModelInfo) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scaleOnPress(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onModelSelected(model) },
                onLongClick = { onToggleFavorite(model) }
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Visual Tags Row (Above Model Name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                // Reasoning Tag
                Surface(
                    color = if (model.supportsDeepThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "Reasoning",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (model.supportsDeepThinking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Vision Tag
                Surface(
                    color = if (model.supportsVision) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Vision",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (model.supportsVision) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = androidx.compose.ui.graphics.Color(0xFFFFC107), // Gold
                        modifier = Modifier.padding(start = 8.dp).size(16.dp)
                    )
                }
            }
        }

        if (currentModel?.id == model.id && currentModel.provider == model.provider) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
