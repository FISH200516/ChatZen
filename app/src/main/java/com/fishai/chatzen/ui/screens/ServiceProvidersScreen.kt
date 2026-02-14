package com.fishai.chatzen.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fishai.chatzen.R
import com.fishai.chatzen.data.model.CustomProvider
import com.fishai.chatzen.data.model.ModelInfo
import com.fishai.chatzen.data.model.Provider
import com.fishai.chatzen.ui.utils.bounceClick
import com.fishai.chatzen.ui.utils.scaleOnPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceProvidersListScreen(
    viewModel: SettingsViewModel,
    onNavigateToDetail: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddCustomProviderDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, host, key, path ->
                viewModel.addCustomProvider(name, host, key, path)
                showDialog = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.providers_title),
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
        Box(modifier = Modifier.padding(padding)) {
            ProviderList(
                providers = Provider.values().toList(),
                customProviders = uiState.customProviders,
                onProviderClick = { 
                    viewModel.selectProvider(it)
                    onNavigateToDetail(false)
                },
                onCustomProviderClick = {
                    viewModel.selectCustomProvider(it)
                    onNavigateToDetail(true)
                },
                onAddCustomProvider = {
                    showDialog = true
                }
            )
        }
    }
}

@Composable
private fun TestAllConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认全部测试") },
        text = { Text("确认后会通过发送图片的方式检测是否支持视觉，模型数量大的时候可能会造成大量token消耗，测试结果也未必准确仅供参考") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProviderListScreen(
    viewModel: SettingsViewModel,
    onNavigateToDetail: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Provider.CUSTOM.displayNameResId),
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
        },
        floatingActionButton = {
            var showDialog by remember { mutableStateOf(false) }
            if (showDialog) {
                AddCustomProviderDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { name, host, key, path ->
                        viewModel.addCustomProvider(name, host, key, path)
                        showDialog = false
                    }
                )
            }
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_custom_provider))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            CustomProviderList(
                providers = uiState.customProviders,
                onDelete = { viewModel.deleteCustomProvider(it) },
                onClick = { 
                    viewModel.selectCustomProvider(it)
                    onNavigateToDetail()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // We expect selectedProvider to be set before navigating here
    val provider = uiState.selectedProvider ?: return

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(provider.displayNameResId),
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
        Box(modifier = Modifier.padding(padding)) {
            val currentModels = uiState.availableModels[provider] ?: emptyList()
            ProviderDetail(
                provider = provider,
                apiKey = uiState.apiKeys[provider] ?: "",
                availableModels = currentModels,
                testStatuses = uiState.modelTestStatuses,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onSave = { apiKey -> viewModel.fetchAndSaveModels(provider, apiKey) },
                onTestAll = { viewModel.testAllModelsVisionCapability(currentModels) },
                onEnableAll = { viewModel.enableAllModels(provider) },
                onToggleVision = { viewModel.toggleVisionSupport(it) },
                onTestVision = { model ->
                    viewModel.testVisionCapability(model) { success, result ->
                         android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onToggleEnabled = { viewModel.toggleModelEnabled(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProviderDetailScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // We expect selectedCustomProvider to be set before navigating here
    val provider = uiState.selectedCustomProvider ?: return
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除自定义服务商") },
            text = { Text("确定要删除该自定义服务商吗？删除后所有相关配置和模型都将丢失且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomProvider(provider.id)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val currentModels = uiState.availableModels[Provider.CUSTOM]?.filter { it.customProviderId == provider.id } ?: emptyList()
            CustomProviderDetail(
                provider = provider,
                availableModels = currentModels,
                testStatuses = uiState.modelTestStatuses,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onSave = { updatedProvider -> 
                    viewModel.updateCustomProvider(updatedProvider)
                },
                onFetchModels = { viewModel.fetchAndSaveCustomModels(it) },
                onTestAll = { viewModel.testAllModelsVisionCapability(currentModels) },
                onEnableAll = { viewModel.enableAllModels(Provider.CUSTOM, provider.id) },
                onToggleVision = { viewModel.toggleVisionSupport(it) },
                onTestVision = { model ->
                    viewModel.testVisionCapability(model) { success, result ->
                         android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onToggleEnabled = { viewModel.toggleModelEnabled(it) }
            )
        }
    }
}

@Composable
fun CustomProviderList(
    providers: List<CustomProvider>,
    onDelete: (String) -> Unit,
    onClick: (CustomProvider) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (providers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.add_custom_provider_hint), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        items(providers) { provider ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = { onClick(provider) }),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDelete(provider.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text(stringResource(R.string.host_label, provider.baseUrl), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.path_label, provider.chatEndpoint), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { 
            Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp)) 
        }
    }
}

@Composable
fun AddCustomProviderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    // Default to chat completions
    var path by remember { mutableStateOf("/chat/completions") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_provider)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.provider_name)) }, singleLine = true)
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(stringResource(R.string.host_hint)) }, singleLine = true)
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text(stringResource(R.string.api_key)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                
                OutlinedTextField(
                    value = path, 
                    onValueChange = { path = it }, 
                    label = { Text(stringResource(R.string.api_path)) }, 
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, host, key, path) },
                enabled = name.isNotBlank() && host.isNotBlank() && key.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}


@Composable
fun ProviderList(
    providers: List<Provider>,
    customProviders: List<CustomProvider> = emptyList(),
    onProviderClick: (Provider) -> Unit,
    onCustomProviderClick: (CustomProvider) -> Unit = {},
    onAddCustomProvider: () -> Unit = {}
) {
    val officialProviders = remember {
        listOf(
            Provider.OPENAI,
            Provider.CLAUDE,
            Provider.GEMINI,
            Provider.DEEPSEEK,
            Provider.MOONSHOT,
            Provider.MIMO,
            Provider.MINIMAX,
            Provider.ZHIPU,
            Provider.GROK
        )
    }

    val thirdPartyProviders = remember {
        listOf(
            Provider.SILICONFLOW,
            Provider.VOLCENGINE,
            Provider.ALIYUN,
            Provider.NVIDIA
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Official Model Services Group
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "官方模型服务",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        officialProviders.forEachIndexed { index, provider ->
                            if (providers.contains(provider)) {
                                ProviderItem(
                                    provider = provider,
                                    onClick = { onProviderClick(provider) },
                                    showDivider = index < officialProviders.filter { providers.contains(it) }.lastIndex
                                )
                            }
                        }
                    }
                }
            }
        }

        // Third-party Aggregated Model Services Group
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "第三方聚合模型服务",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        thirdPartyProviders.forEachIndexed { index, provider ->
                            if (providers.contains(provider)) {
                                ProviderItem(
                                    provider = provider,
                                    onClick = { onProviderClick(provider) },
                                    showDivider = index < thirdPartyProviders.filter { providers.contains(it) }.lastIndex
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Model Services Group
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "自定义模型服务商",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Added custom providers
                        customProviders.forEach { customProvider ->
                            CustomProviderItem(
                                customProvider = customProvider,
                                onClick = { onCustomProviderClick(customProvider) },
                                showDivider = true // Always show divider as the "Add" button follows
                            )
                        }
                        
                        // Persistent "Add Custom Provider" entry
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.add_custom_provider), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                            supportingContent = { Text("添加OpenAI Api兼容格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddCustomProvider() },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }

        item { 
            Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp)) 
        }
    }
}

@Composable
fun CustomProviderItem(
    customProvider: CustomProvider,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(customProvider.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { 
                Text(
                    customProvider.baseUrl, 
                    style = MaterialTheme.typography.bodySmall
                ) 
            },
            trailingContent = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ProviderItem(
    provider: Provider,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(provider.displayNameResId), style = MaterialTheme.typography.titleMedium) },
            supportingContent = { 
                Text(
                    if (provider == Provider.CUSTOM) stringResource(R.string.custom_provider_desc) else provider.baseUrl, 
                    style = MaterialTheme.typography.bodySmall
                ) 
            },
            trailingContent = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ProviderDetail(
    provider: Provider,
    apiKey: String,
    availableModels: List<ModelInfo>,
    testStatuses: Map<String, TestStatus> = emptyMap(),
    isLoading: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onTestAll: () -> Unit,
    onEnableAll: () -> Unit,
    onToggleVision: (ModelInfo) -> Unit,
    onTestVision: (ModelInfo) -> Unit,
    onToggleEnabled: (ModelInfo) -> Unit
) {
    var currentKey by remember(apiKey) { mutableStateOf(apiKey) }
    var showTestDialog by remember { mutableStateOf(false) }

    if (showTestDialog) {
        TestAllConfirmationDialog(
            onDismiss = { showTestDialog = false },
            onConfirm = {
                onTestAll()
                showTestDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = currentKey,
            onValueChange = { currentKey = it },
            label = { Text(stringResource(R.string.api_key)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        if (provider.websiteUrl.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { uriHandler.openUri(provider.websiteUrl) }
                ) {
                    Text(stringResource(R.string.get_api_key), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        val buttonInteractionSource = remember { MutableInteractionSource() }
        Button(
            onClick = { onSave(currentKey) },
            modifier = Modifier.fillMaxWidth().scaleOnPress(buttonInteractionSource),
            enabled = !isLoading && currentKey.isNotBlank(),
            interactionSource = buttonInteractionSource
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.padding(8.dp))
                Text(stringResource(R.string.verifying_and_fetching))
            } else {
                Text(stringResource(R.string.save_and_fetch))
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.available_models, availableModels.size),
                style = MaterialTheme.typography.titleMedium
            )
            if (availableModels.isNotEmpty()) {
                Row {
                    TextButton(onClick = onEnableAll) {
                        Text("开启全部")
                    }
                    TextButton(onClick = { showTestDialog = true }) {
                        Text(stringResource(R.string.test_all))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (availableModels.isEmpty() && !isLoading) {
            Text(stringResource(R.string.no_models_found), style = MaterialTheme.typography.bodyMedium)
        } else {
            // Custom Comparator for Manufacturer Sorting
    val manufacturerPriority = listOf(
        "OpenAI",
        "Anthropic", 
        "Google",
        "DeepSeek",
        "Zhipu AI",   // 提前
        "MiniMax",    // 提前
        "xAI",        // Grok
        "NVIDIA",     // 提前
        "Alibaba", "Qwen", // 提前
        "Meta",
        "Microsoft",
        "Mistral",
        "01.AI",
        "ByteDance", "Doubao",
        "Baichuan",
        "Xiaomi"
    )

            val comparator = Comparator<String> { a, b ->
                val indexA = manufacturerPriority.indexOfFirst { a.contains(it, ignoreCase = true) }
                val indexB = manufacturerPriority.indexOfFirst { b.contains(it, ignoreCase = true) }

                when {
                    indexA != -1 && indexB != -1 -> indexA - indexB
                    indexA != -1 -> -1 // A is in priority list, so it comes first
                    indexB != -1 -> 1  // B is in priority list, so it comes first
                    else -> a.compareTo(b) // Fallback to alphabetical
                }
            }

            val groupedModels = availableModels.groupBy { it.manufacturer }.toSortedMap(comparator)
            val expandedManufacturers = remember { mutableStateListOf<String>() }
            
            // Auto-expand the first group if it's a priority manufacturer
            LaunchedEffect(groupedModels) {
                if (expandedManufacturers.isEmpty() && groupedModels.isNotEmpty()) {
                    val firstKey = groupedModels.firstKey()
                    if (manufacturerPriority.any { firstKey.contains(it, ignoreCase = true) }) {
                        expandedManufacturers.add(firstKey)
                    }
                }
            }
            
            groupedModels.forEach { (manufacturer, models) ->
                val isExpanded = expandedManufacturers.contains(manufacturer)
                
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateContentSize()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(onClick = {
                                    if (isExpanded) {
                                        expandedManufacturers.remove(manufacturer)
                                    } else {
                                        expandedManufacturers.add(manufacturer)
                                    }
                                })
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$manufacturer (${models.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            ) {
                                models.forEachIndexed { index, model ->
                                    ModelItem(
                                        model = model,
                                        testStatus = testStatuses["${model.provider.name}|${model.customProviderId ?: ""}|${model.id}"],
                                        onToggleVision = { onToggleVision(model) },
                                        onTestVision = { onTestVision(model) },
                                        onToggleEnabled = { onToggleEnabled(model) },
                                        showDivider = index < models.size - 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp))
    }
}

@Composable
fun CustomProviderDetail(
    provider: CustomProvider,
    availableModels: List<ModelInfo>,
    testStatuses: Map<String, TestStatus> = emptyMap(),
    isLoading: Boolean,
    error: String?,
    onSave: (CustomProvider) -> Unit,
    onFetchModels: (CustomProvider) -> Unit,
    onTestAll: () -> Unit,
    onEnableAll: () -> Unit,
    onToggleVision: (ModelInfo) -> Unit,
    onTestVision: (ModelInfo) -> Unit,
    onToggleEnabled: (ModelInfo) -> Unit
) {
    var name by remember(provider) { mutableStateOf(provider.name) }
    var baseUrl by remember(provider) { mutableStateOf(provider.baseUrl) }
    var apiKey by remember(provider) { mutableStateOf(provider.apiKey) }
    var chatEndpoint by remember(provider) { mutableStateOf(provider.chatEndpoint) }
    var showTestDialog by remember { mutableStateOf(false) }

    if (showTestDialog) {
        TestAllConfirmationDialog(
            onDismiss = { showTestDialog = false },
            onConfirm = {
                onTestAll()
                showTestDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.provider_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text(stringResource(R.string.base_url)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.api_key)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = chatEndpoint,
            onValueChange = { chatEndpoint = it },
            label = { Text(stringResource(R.string.chat_endpoint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    onSave(provider.copy(name = name, baseUrl = baseUrl, apiKey = apiKey, chatEndpoint = chatEndpoint)) 
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
            
            Button(
                onClick = { 
                    val updated = provider.copy(name = name, baseUrl = baseUrl, apiKey = apiKey, chatEndpoint = chatEndpoint)
                    onSave(updated)
                    onFetchModels(updated) 
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.fetch_models))
                }
            }
        }

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.available_models, availableModels.size),
                style = MaterialTheme.typography.titleMedium
            )
            if (availableModels.isNotEmpty()) {
                Row {
                    TextButton(onClick = onEnableAll) {
                        Text("开启全部")
                    }
                    TextButton(onClick = { showTestDialog = true }) {
                        Text(stringResource(R.string.test_all))
                    }
                }
            }
        }

        if (availableModels.isEmpty() && !isLoading) {
            Text(stringResource(R.string.no_models_found), style = MaterialTheme.typography.bodyMedium)
        } else {
            val groupedModels = availableModels.groupBy { it.manufacturer }.toSortedMap()
            val expandedManufacturers = remember { mutableStateListOf<String>() }
            
            groupedModels.forEach { (manufacturer, models) ->
                val isExpanded = expandedManufacturers.contains(manufacturer)
                
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateContentSize()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isExpanded) {
                                        expandedManufacturers.remove(manufacturer)
                                    } else {
                                        expandedManufacturers.add(manufacturer)
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$manufacturer (${models.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            ) {
                                models.forEachIndexed { index, model ->
                                    ModelItem(
                                        model = model,
                                        testStatus = testStatuses["${model.provider.name}|${model.customProviderId ?: ""}|${model.id}"],
                                        onToggleVision = { onToggleVision(model) },
                                        onTestVision = { onTestVision(model) },
                                        onToggleEnabled = { onToggleEnabled(model) },
                                        showDivider = index < models.size - 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp))
    }
}

@Composable
fun ModelItem(
    model: ModelInfo,
    testStatus: TestStatus? = null,
    onToggleVision: () -> Unit,
    onTestVision: () -> Unit,
    onToggleEnabled: () -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .bounceClick(onClick = { onToggleVision() })
            ) {
                Text(text = model.simpleName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    // Reasoning
                    Icon(
                        imageVector = if (model.supportsDeepThinking) Icons.Default.CheckCircle else Icons.Default.Info, 
                        contentDescription = null, 
                        tint = if (model.supportsDeepThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reasoning", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (model.supportsDeepThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Vision
                    Icon(
                        imageVector = if (model.supportsVision) Icons.Default.CheckCircle else Icons.Default.Info, 
                        contentDescription = null,
                        tint = if (model.supportsVision) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vision", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (model.supportsVision) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    
                    if (testStatus is TestStatus.Failure) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = testStatus.error.take(20) + "...", // Truncate error
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTestVision) {
                    when (testStatus) {
                        is TestStatus.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        is TestStatus.Success -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary)
                        }
                        else -> { // Null or Failure (allow retry)
                             Icon(Icons.Default.PlayArrow, contentDescription = "Test", tint = if (testStatus is TestStatus.Failure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Switch(
                    checked = model.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}
