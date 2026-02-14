package com.fishai.chatzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fishai.chatzen.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    // State for Custom Color Bottom Sheet
    var showColorPickerSheet by remember { mutableStateOf(false) }
    // We need to know which color we are editing: "THEME", "USER_BUBBLE", "AI_BUBBLE"
    var activeColorTarget by remember { mutableStateOf<String?>(null) }

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Copy image to app storage to ensure persistence
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = java.io.File(context.filesDir, "user_avatar.jpg")
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.updateUserAvatarUri(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to original URI if copy fails (though unlikely to work after restart)
                viewModel.updateUserAvatarUri(uri.toString())
            }
        }
    }

    // Preset Colors
    val presetColors = remember {
        listOf(
            Color(0xFFD32F2F), // Red
            Color(0xFFC2185B), // Pink
            Color(0xFF7B1FA2), // Purple
            Color(0xFF512DA8), // Deep Purple
            Color(0xFF303F9F), // Indigo
            Color(0xFF1976D2), // Blue
            Color(0xFF00796B), // Teal
            Color(0xFF388E3C), // Green
            Color(0xFFF57C00), // Orange
        )
    }

    // Helper to open sheet
    fun openColorSheet(target: String) {
        activeColorTarget = target
        showColorPickerSheet = true
    }

    if (showColorPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorPickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when(activeColorTarget) {
                        "THEME" -> "自定义主题色"
                        "USER_BUBBLE" -> "自定义用户气泡颜色"
                        "AI_BUBBLE" -> "自定义AI气泡颜色"
                        else -> "选择颜色"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                val currentColor = when(activeColorTarget) {
                    "THEME" -> uiState.customThemeColor
                    "USER_BUBBLE" -> uiState.userBubbleColor ?: 0xFF2196F3.toInt()
                    "AI_BUBBLE" -> uiState.aiBubbleColor ?: 0xFFE0E0E0.toInt()
                    else -> 0
                }

                RgbColorSelector(
                    colorInt = currentColor,
                    onColorChange = { color ->
                        when(activeColorTarget) {
                            "THEME" -> {
                                viewModel.updateCustomThemeColor(color)
                                viewModel.updateThemeMode("CUSTOM")
                            }
                            "USER_BUBBLE" -> viewModel.updateUserBubbleColor(color)
                            "AI_BUBBLE" -> viewModel.updateAiBubbleColor(color)
                        }
                    }
                )
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Global Theme Color
            ThemeSectionCard(
                title = "全局主题色",
                cornerRadius = uiState.globalCornerRadius.dp
            ) {
                val isCustom = uiState.themeMode == "CUSTOM"
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text("动态取色 (Material You)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = !isCustom,
                        onCheckedChange = { useDynamic ->
                            viewModel.updateThemeMode(if (useDynamic) "DYNAMIC" else "CUSTOM") 
                        }
                    )
                }

                // Color Grid
                if (isCustom) {
                    ColorGrid(
                        colors = presetColors,
                        selectedColor = if (uiState.themeMode == "CUSTOM") Color(uiState.customThemeColor) else null,
                        onColorSelected = { color ->
                            viewModel.updateCustomThemeColor(color.toArgb())
                            viewModel.updateThemeMode("CUSTOM")
                        },
                        onCustomClick = { openColorSheet("THEME") }
                    )
                }
            }

            // 2. Bubble Colors
            ThemeSectionCard(
                title = "气泡颜色",
                cornerRadius = uiState.globalCornerRadius.dp
            ) {
                // User Bubble
                Text("用户气泡", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                val userBubbleCustom = uiState.userBubbleColor != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text("自定义颜色", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = userBubbleCustom,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                viewModel.updateUserBubbleColor(Color(0xFF2196F3).toArgb()) 
                            } else {
                                viewModel.updateUserBubbleColor(null)
                            }
                        }
                    )
                }
                
                if (userBubbleCustom) {
                     ColorGrid(
                        colors = presetColors,
                        selectedColor = uiState.userBubbleColor?.let { Color(it) },
                        onColorSelected = { color -> viewModel.updateUserBubbleColor(color.toArgb()) },
                        onCustomClick = { openColorSheet("USER_BUBBLE") }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                // AI Bubble
                Text("AI 气泡", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                val aiBubbleCustom = uiState.aiBubbleColor != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text("自定义颜色", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = aiBubbleCustom,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val defaultStartColor = if (isDark) Color(0xFF424242) else Color(0xFFE0E0E0)
                                viewModel.updateAiBubbleColor(defaultStartColor.toArgb())
                            } else {
                                viewModel.updateAiBubbleColor(null)
                            }
                        }
                    )
                }
                
                if (aiBubbleCustom) {
                    ColorGrid(
                        colors = presetColors,
                        selectedColor = uiState.aiBubbleColor?.let { Color(it) },
                        onColorSelected = { color -> viewModel.updateAiBubbleColor(color.toArgb()) },
                        onCustomClick = { openColorSheet("AI_BUBBLE") }
                    )
                }
            }

            // 3. Corner Radius
            ThemeSectionCard(
                title = "圆角设置",
                cornerRadius = uiState.globalCornerRadius.dp
            ) {
                Text(
                    text = "全局圆角大小: ${uiState.globalCornerRadius.toInt()}dp",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = uiState.globalCornerRadius,
                    onValueChange = { viewModel.updateGlobalCornerRadius(it) },
                    valueRange = 4f..32f,
                    steps = 27
                )
                // Removed Preview Box as requested
            }

            // 4. Avatar Settings
            ThemeSectionCard(
                title = "头像设置",
                cornerRadius = uiState.globalCornerRadius.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar Preview
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.userAvatarUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uiState.userAvatarUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                "I",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column {
                        Button(
                            onClick = { 
                                avatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("上传头像")
                        }
                        
                        if (uiState.userAvatarUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.updateUserAvatarUri(null) }
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("恢复默认")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSectionCard(
    title: String,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius), // Expressive roundness
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            content()
        }
    }
}

@Composable
fun ColorGrid(
    colors: List<Color>,
    selectedColor: Color?,
    onColorSelected: (Color) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1: 5 colors
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f)) // Centering spacer
            colors.take(5).forEach { color ->
                ColorOption(
                    color = color,
                    isSelected = selectedColor == color, // Note: Exact match might fail if alpha differs, but presets should match
                    onClick = { onColorSelected(color) }
                )
            }
            Spacer(modifier = Modifier.weight(1f)) // Centering spacer
        }

        // Row 2: 4 colors + Custom
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f)) // Centering spacer
            colors.drop(5).take(4).forEach { color ->
                ColorOption(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorSelected(color) }
                )
            }
            // Custom Button
            CustomColorButton(onClick = onCustomClick)
            Spacer(modifier = Modifier.weight(1f)) // Centering spacer
        }
    }
}

@Composable
fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) 
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CustomColorButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = "Custom Color",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun RgbColorSelector(
    colorInt: Int,
    onColorChange: (Int) -> Unit
) {
    val color = Color(colorInt)
    
    // Components
    val red = color.red
    val green = color.green
    val blue = color.blue

    Column {
        // Preview Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = "#${Integer.toHexString(colorInt).uppercase().takeLast(6)}",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Sliders
        ColorSliderItem(label = "R", value = red, color = Color.Red) { newR ->
            onColorChange(Color(newR, green, blue).toArgb())
        }
        Spacer(modifier = Modifier.height(16.dp))
        ColorSliderItem(label = "G", value = green, color = Color.Green) { newG ->
            onColorChange(Color(red, newG, blue).toArgb())
        }
        Spacer(modifier = Modifier.height(16.dp))
        ColorSliderItem(label = "B", value = blue, color = Color.Blue) { newB ->
            onColorChange(Color(red, green, newB).toArgb())
        }
    }
}

@Composable
fun ColorSliderItem(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
        Text(
            text = "${(value * 255).toInt()}",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
