package com.fishai.chatzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import com.fishai.chatzen.ui.utils.scaleOnPress
import com.fishai.chatzen.ui.utils.bounceClick
import com.fishai.chatzen.data.model.ChatMessage
import com.fishai.chatzen.data.model.Role
import com.fishai.chatzen.data.model.WebSearchResult
import com.fishai.chatzen.ui.components.ModelSelector
import com.fishai.chatzen.ui.components.ChatBubble
import com.fishai.chatzen.ui.components.LoadingIndicatorBubble
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import com.fishai.chatzen.R

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.TextFieldDefaults
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.markdownTypography
import com.fishai.chatzen.ui.components.CodeBlock
import androidx.compose.runtime.CompositionLocalProvider
import org.intellij.markdown.ast.getTextInNode
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.sp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.layout.ContentScale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onSettingsClick: () -> Unit,
    onNavigateToWebView: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // --- Auto-scroll Logic ---
    var userScrolled by remember { mutableStateOf(false) }
    var isUserDragging by remember { mutableStateOf(false) }

    // Detect user drag to distinguish user scroll from programmatic scroll
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                isUserDragging = true
            }
        }
    }

    // Detect scroll end and update userScrolled state
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { inProgress ->
                if (!inProgress && isUserDragging) {
                    // User finished scrolling (drag + fling)
                    // With reverseLayout, the bottom is index 0
                    val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                    val isAtBottom = firstVisibleItem != null &&
                            firstVisibleItem.index == 0 &&
                            firstVisibleItem.offset <= 100 // Tolerance
                    
                    userScrolled = !isAtBottom
                    isUserDragging = false
                }
            }
    }

    // Auto-scroll when new message arrives or streaming updates
    val messages = uiState.messages
    LaunchedEffect(messages.lastOrNull()?.content, messages.size, uiState.isLoading) {
        if (messages.isNotEmpty()) {
            val isLastMessageByUser = messages.last().role == Role.USER
            
            if (isLastMessageByUser) {
                // Always scroll to bottom for user messages
                userScrolled = false
                listState.animateScrollToItem(0)
            } else if (!userScrolled && !isUserDragging && (uiState.isLoading || messages.last().role == Role.ASSISTANT)) {
                // If streaming or finished (and user hasn't scrolled up), keep scrolling to bottom
                listState.scrollToItem(0)
            }
        }
    }
    // --- End Auto-scroll Logic ---

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val imageString = "data:$mimeType;base64,$base64"
                    viewModel.addImage(imageString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(uiState.isInputExpanded) {
        if (uiState.isInputExpanded) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.chat_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ModelSelector(
                            currentModel = uiState.currentModel,
                            availableModels = uiState.availableModels.filter { it.enabled },
                            customProviderNames = uiState.customProviderNames,
                            favoriteModels = uiState.favoriteModels,
                            onModelSelected = { viewModel.selectModel(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startNewChat() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.new_chat)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.setInputExpanded(false)
                    focusManager.clearFocus()
                }
        ) {
            AnimatedContent(
                targetState = uiState.messages.isEmpty(),
                transitionSpec = {
                    if (targetState) {
                        // Showing greeting (Empty state)
                        (fadeIn(animationSpec = tween(300)) +
                                slideInVertically(animationSpec = tween(300)) { it / 2 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(300))
                            )
                    } else {
                        // Showing chat list
                        (fadeIn(animationSpec = tween(300)) +
                                slideInVertically(animationSpec = tween(300)) { it / 4 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(300)) +
                                        slideOutVertically(animationSpec = tween(300)) { it / 4 }
                            )
                    }
                },
                label = "ChatContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    val greetings = remember {
                        listOf(
                            R.string.greeting_1,
                            R.string.greeting_2,
                            R.string.greeting_3,
                            R.string.greeting_4,
                            R.string.greeting_5,
                            R.string.greeting_6,
                            R.string.greeting_7
                        )
                    }
                    val greetingRes = remember { greetings.random() }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = stringResource(R.string.ai_assistant),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(1.5f)
                            )
                        }
                        Text(
                            text = stringResource(greetingRes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        if (!uiState.hasConfiguredApiKey) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_api_key),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onSettingsClick) {
                                Text(stringResource(R.string.go_to_settings))
                            }
                        } else {
                            // Suggestion Chips
                            val sampleQuestions = remember {
                                listOf(
                                    "为什么地球是圆的？",
                                    "量子纠缠到底是什么？",
                                    "时间旅行在理论上可行吗？",
                                    "为什么天空是蓝色的？",
                                    "黑洞的另一端是什么？",
                                    "梦是如何产生的？",
                                    "为什么猫咪喜欢钻盒子？",
                                    "极光是怎么形成的？",
                                    "植物有痛觉吗？",
                                    "如果不睡觉会发生什么？"
                                )
                            }
                            val randomQuestions = remember { sampleQuestions.shuffled().take(3) }

                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Use FlowRow if available, otherwise Column for simplicity
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                randomQuestions.forEach { question ->
                                    Surface(
                                        modifier = Modifier.bounceClick(onClick = { viewModel.sendMessage(question) }),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, 
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Text(
                                            text = question,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = if (uiState.isInputExpanded) 140.dp else 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Anchor item for auto-scrolling (at the bottom visually)
                        item { Spacer(modifier = Modifier.height(1.dp)) }

                        // Loading indicator (above anchor)
                        if (uiState.isLoading && (uiState.messages.isEmpty() || uiState.messages.last().role != Role.ASSISTANT)) {
                            item {
                                LoadingIndicatorBubble()
                            }
                        }

                        // Messages (reversed)
                        // Note: uiState.messages.asReversed() is a view, not a copy.
                        // We use a key to ensure proper recomposition when messages update.
                        itemsIndexed(
                            items = uiState.messages.asReversed(),
                            key = { _, message -> message.id } // Assuming ChatMessage has a unique ID
                        ) { index, message ->
                            // index 0 is the last message in the original list
                            val isStreaming = uiState.isLoading && index == 0
                            ChatBubble(
                                message = message,
                                isStreaming = isStreaming,
                                userAvatarUri = uiState.userAvatarUri,
                                onQuote = { content ->
                                    viewModel.setQuotedMessage(content)
                                }
                            )
                        }

                        // Top spacer (at the top visually)
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.isInputExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAFAFA),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp, // No shadow between input and nav bar
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Action Buttons Row (Deep Thinking & Web Search)

                        // Action Buttons Row (Deep Thinking & Web Search)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Deep Thinking Button
                            val currentModel = uiState.currentModel
                            val isDeepThinkingSupported = currentModel?.supportsDeepThinking == true

                            Surface(
                                onClick = { viewModel.toggleDeepThinking() },
                                enabled = isDeepThinkingSupported,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDeepThinkingSupported) {
                                        if (uiState.isDeepThinkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    }
                                ),
                                color = if (isDeepThinkingSupported && uiState.isDeepThinkingEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isDeepThinkingSupported) {
                                            if (uiState.isDeepThinkingEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.deep_thinking),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDeepThinkingSupported) {
                                            if (uiState.isDeepThinkingEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                }
                            }

                            // Web Search Button
                            val isWebSearchEnabled = uiState.isWebSearchEnabled

                            Surface(
                                onClick = { viewModel.toggleWebSearch() },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isWebSearchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                color = if (isWebSearchEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isWebSearchEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.web_search),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isWebSearchEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Image Upload Button
                            val isVisionSupported = uiState.currentModel?.supportsVision == true
                            val isImageUploadEnabled = isVisionSupported || uiState.ocrModel != null

                            Surface(
                                onClick = { 
                                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = isImageUploadEnabled,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isImageUploadEnabled) {
                                        if (uiState.selectedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    }
                                ),
                                color = if (isImageUploadEnabled && uiState.selectedImages.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isImageUploadEnabled) {
                                            if (uiState.selectedImages.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.image_upload),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isImageUploadEnabled) {
                                            if (uiState.selectedImages.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                }
                            }
                            
                            // AI Personality Button
                            Surface(
                                onClick = { viewModel.setModelSettingsVisible(true) },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (uiState.systemPrompt.isNotEmpty() || uiState.temperature != 0.7f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                color = if (uiState.systemPrompt.isNotEmpty() || uiState.temperature != 0.7f) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (uiState.systemPrompt.isNotEmpty() || uiState.temperature != 0.7f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.ai_personality),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (uiState.systemPrompt.isNotEmpty() || uiState.temperature != 0.7f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAFAFA),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Column {
                                    // Image Preview Area
                                    if (uiState.selectedImages.isNotEmpty()) {
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            itemsIndexed(uiState.selectedImages) { index, base64 ->
                                                Box {
                                                    val bitmap = remember(base64) {
                                                        try {
                                                            val cleanBase64 = if (base64.startsWith("data:")) {
                                                                base64.substringAfter("base64,")
                                                            } else {
                                                                base64
                                                            }
                                                            val decodedString = Base64.decode(cleanBase64, Base64.DEFAULT)
                                                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                                                .asImageBitmap()
                                                        } catch (e: Exception) {
                                                            null
                                                        }
                                                    }
                                                    
                                                    if (bitmap != null) {
                                                        Image(
                                                            bitmap = bitmap,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .size(60.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                    
                                                    // Remove Button
                                                    IconButton(
                                                        onClick = { viewModel.removeImage(index) },
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .align(Alignment.TopEnd)
                                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }

                                    // Quoted Message Display inside the input bubble
                                    if (uiState.quotedMessage != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    end = 8.dp,
                                                    top = 8.dp,
                                                    bottom = 4.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = uiState.quotedMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { viewModel.clearQuotedMessage() },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear quote",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(stringResource(R.string.input_placeholder)) },
                                        shape = RoundedCornerShape(24.dp),
                                        maxLines = 4,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent
                                        ),
                                        trailingIcon = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                // Quick Switch Button
                                                Box {
                                                    var showFavoritesMenu by remember {
                                                        mutableStateOf(
                                                            false
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        showFavoritesMenu = true
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = stringResource(R.string.favorite_models),
                                                            tint = if (uiState.favoriteModels.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.5f
                                                            )
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = showFavoritesMenu,
                                                        onDismissRequest = {
                                                            showFavoritesMenu = false
                                                        },
                                                        shape = RoundedCornerShape(20.dp),
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        tonalElevation = 8.dp
                                                    ) {
                                                        val favoritesList =
                                                            uiState.availableModels.filter {
                                                                uiState.favoriteModels.contains("${it.provider.name}|${it.id}|${it.customProviderId ?: ""}")
                                                            }

                                                        if (favoritesList.isEmpty()) {
                                                            DropdownMenuItem(
                                                                text = { Text(stringResource(R.string.no_favorites)) },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Default.StarBorder,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                },
                                                                onClick = {
                                                                    showFavoritesMenu = false
                                                                }
                                                            )
                                                        } else {
                                                            // Add a title for the section
                                                            DropdownMenuItem(
                                                                text = { 
                                                                    Text(
                                                                        text = stringResource(R.string.favorite_models),
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    ) 
                                                                },
                                                                onClick = {},
                                                                enabled = false
                                                            )
                                                            
                                                            favoritesList.forEach { model ->
                                                                val isSelected = model.id == uiState.currentModel?.id && 
                                                                                model.provider == uiState.currentModel?.provider
                                                                
                                                                DropdownMenuItem(
                                                                    text = { 
                                                                        Text(
                                                                            text = model.name,
                                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                                        ) 
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = Icons.Default.SmartToy,
                                                                            contentDescription = null,
                                                                            modifier = Modifier.size(20.dp),
                                                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                                        )
                                                                    },
                                                                    trailingIcon = if (isSelected) {
                                                                        {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Check,
                                                                                contentDescription = "Selected",
                                                                                modifier = Modifier.size(20.dp),
                                                                                tint = MaterialTheme.colorScheme.primary
                                                                            )
                                                                        }
                                                                    } else null,
                                                                    onClick = {
                                                                        viewModel.selectModel(model)
                                                                        showFavoritesMenu = false
                                                                    },
                                                                    colors = androidx.compose.material3.MenuDefaults.itemColors(
                                                                        textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Send Button (always visible)
                                                val sendInteractionSource =
                                                    remember { MutableInteractionSource() }
                                                val isSendEnabled =
                                                    inputText.isNotEmpty() && !uiState.isLoading
                                                val openClawUrl = uiState.openClawWebUiUrl

                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .combinedClickable(
                                                            interactionSource = sendInteractionSource,
                                                            indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp),
                                                            enabled = true,
                                                            onClick = {
                                                                if (uiState.isLoading) {
                                                                    viewModel.stopGeneration()
                                                                } else if (isSendEnabled) {
                                                                    viewModel.sendMessage(inputText)
                                                                    inputText = ""
                                                                    viewModel.setInputExpanded(false)
                                                                }
                                                            }
                                                        )
                                                        .scaleOnPress(sendInteractionSource),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (uiState.isLoading) Icons.Default.Stop else Icons.Default.Send,
                                                        contentDescription = stringResource(if (uiState.isLoading) R.string.stop_generation else R.string.send_message),
                                                        tint = if (isSendEnabled || uiState.isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.38f
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isModelSettingsVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setModelSettingsVisible(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp) // Extra padding for navigation bar
            ) {
                Text(
                    text = stringResource(R.string.model_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // System Prompt
                Text(
                    text = stringResource(R.string.system_prompt_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = uiState.systemPrompt,
                    onValueChange = { viewModel.updateSystemPrompt(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    placeholder = { Text(stringResource(R.string.system_prompt_hint)) },
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.temperature_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format(stringResource(R.string.value_format), uiState.temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = uiState.temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    valueRange = 0f..2f,
                    steps = 19 // 0.1 increments
                )
                Text(
                    text = "保守 (0.0) — 创造性 (1.0) — 疯狂 (2.0)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Top P
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.top_p_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format(stringResource(R.string.value_format), uiState.topP),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = uiState.topP,
                    onValueChange = { viewModel.updateTopP(it) },
                    valueRange = 0f..1f,
                    steps = 9 // 0.1 increments
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.updateSystemPrompt("")
                            viewModel.updateTemperature(0.7f)
                            viewModel.updateTopP(1.0f)
                        }
                    ) {
                        Text(stringResource(R.string.reset_default))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.setModelSettingsVisible(false) }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}



