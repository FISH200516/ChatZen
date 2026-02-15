package com.fishai.chatzen.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.fishai.chatzen.R
import com.fishai.chatzen.data.model.ChatMessage
import com.fishai.chatzen.data.model.Role
import com.fishai.chatzen.data.model.WebSearchResult
import com.fishai.chatzen.ui.utils.ContentSegment
import com.fishai.chatzen.ui.utils.hasMarkdownSyntax
import com.fishai.chatzen.ui.utils.scaleOnPress
import com.fishai.chatzen.ui.utils.splitMarkdownContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.LocalDensity
import com.fishai.chatzen.ui.utils.parseInlineMath
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import org.intellij.markdown.ast.getTextInNode

import com.fishai.chatzen.ui.utils.bounceClick

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage, 
    isStreaming: Boolean = false,
    userAvatarUri: String? = null,
    onQuote: (String) -> Unit
) {
    val isUser = message.role == Role.USER
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    
    // Fixed shape logic, ignoring global settings
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else if (message.content == "对话取消") Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Avatar aligned to bottom
    ) {
        // AI Avatar (Left)
        if (!isUser && message.content != "对话取消") {
            ChatAvatar(
                role = message.role,
                modelName = message.modelName,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
        }

        Column(
            horizontalAlignment = if (message.content == "对话取消") Alignment.CenterHorizontally else horizontalAlignment,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Display images outside and above the chat bubble
            if (!message.images.isNullOrEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .widthIn(max = screenWidth * 0.70f)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(message.images) { pathOrBase64 ->
                        val bitmap = remember(pathOrBase64) {
                            try {
                                if (pathOrBase64.startsWith("/")) {
                                    BitmapFactory.decodeFile(pathOrBase64)?.asImageBitmap()
                                } else {
                                    val cleanBase64 = if (pathOrBase64.startsWith("data:")) {
                                        pathOrBase64.substringAfter("base64,")
                                    } else {
                                        pathOrBase64
                                    }
                                    val decodedString = Base64.decode(cleanBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        .asImageBitmap()
                                }
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
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            Box(
                contentAlignment = if (isUser) Alignment.CenterEnd else if (message.content == "对话取消") Alignment.Center else Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = screenWidth * 0.70f)
                        .scaleOnPress(interactionSource)
                        .clip(bubbleShape)
                        .background(if (message.content == "对话取消") Color.Transparent else backgroundColor)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                            onLongClick = { 
                                if (!isUser && message.content != "对话取消") showMenu = true 
                            }
                        )
                        .padding(12.dp)
                ) {
                    if (message.role == Role.SYSTEM) {
                        if (message.content == "对话取消") {
                            Text(
                                text = "对话已取消",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Text(
                                text = "System: ${message.content}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        if (message.isSearching) {
                            Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (message.isOcr) "正在识别图片" else stringResource(R.string.searching_web),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }

                        if (!message.searchResults.isNullOrEmpty()) {
                            WebSearchContentBlock(
                                searchResults = message.searchResults,
                                textColor = textColor,
                                isStreaming = isStreaming
                            )
                        }

                        if (!message.reasoningContent.isNullOrBlank()) {
                            ReasoningContentBlock(
                                reasoningContent = message.reasoningContent,
                                textColor = textColor,
                                isStreaming = isStreaming
                            )
                        }

                        if (!message.ocrContent.isNullOrBlank()) {
                            OcrContentBlock(
                                ocrContent = message.ocrContent,
                                textColor = textColor,
                                isStreaming = isStreaming
                            )
                        }

                        if (!message.quotedContent.isNullOrBlank()) {
                            QuotedContentBlock(
                                quotedContent = message.quotedContent,
                                textColor = textColor
                            )
                        }

                        if (message.content.isNotEmpty()) {
                            CompositionLocalProvider(LocalContentColor provides textColor) {
                                val segments = remember(message.content) {
                                    splitMarkdownContent(message.content)
                                }
        
                                val currentTypography = MaterialTheme.typography
                                
                                val baseTextStyle = currentTypography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 24.sp 
                                )
                                
                                val customTypography = markdownTypography(
                                    h1 = currentTypography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
                                    h2 = currentTypography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
                                    h3 = currentTypography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
                                    h4 = currentTypography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
                                    h5 = currentTypography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
                                    h6 = currentTypography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                                    paragraph = baseTextStyle
                                )
                                
                                val markdownComponents = markdownComponents(
                                    codeFence = {
                                        CodeBlock(
                                            language = it.node.children.find { child -> child.type.toString() == "FENCE_LANG" }?.getTextInNode(it.content)?.toString(),
                                            content = it.node.children.find { child -> child.type.toString() == "CODE_FENCE_CONTENT" }?.getTextInNode(it.content)?.toString() 
                                                ?: it.node.getTextInNode(it.content).toString()
                                                    .substringAfter("\n")
                                                    .substringBeforeLast("```")
                                                    .trim(),
                                            bubbleColor = backgroundColor
                                        )
                                    }
                                )
        
                                segments.forEachIndexed { index, segment ->
                                    androidx.compose.runtime.key(index) {
                                        when (segment) {
                                            is ContentSegment.Text -> {
                                                val contentToRenderFull = segment.content
                                                val hasMarkdown = remember(contentToRenderFull) { hasMarkdownSyntax(contentToRenderFull) }
                                                
                                                val hasTrailingNewlines = contentToRenderFull.endsWith("\n\n")
                                                val contentToRender = if (hasTrailingNewlines) contentToRenderFull.removeSuffix("\n\n") else contentToRenderFull
                                                
                                                if (contentToRender.isNotEmpty()) {
                                                    val density = LocalDensity.current
                                                    val inlineMathRegex = Regex("(?<!\\\\)\\$([^$]+?)(?<!\\\\)\\$")
                                                    val hasInlineMath = remember(contentToRender) { inlineMathRegex.containsMatchIn(contentToRender) }
                                                    
                                                    // 如果有行内公式，且没有复杂的块级Markdown（如代码块、标题等，hasMarkdownSyntax会检测这些），
                                                    // 我们优先尝试解析行内公式。
                                                    // 注意：hasMarkdownSyntax 目前也会检测行内公式。
                                                    // 我们需要一种方式：如果主要是文本+行内公式，就用 BasicText 渲染。
                                                    // 如果包含列表、引用、代码块等复杂结构，还是交给 Markdown 组件。
                                                    // 简单起见，如果检测到行内公式，我们尝试用 InlineTextContent 渲染。
                                                    // 但如果这段文本同时包含 **加粗** 等 Markdown 语法，AnnotatedString 需要处理。
                                                    // 目前 parseInlineMath 只处理了公式，其他文本作为普通文本。
                                                    // 这是一个权衡：为了支持公式，可能牺牲部分 Markdown 样式，除非我们在 parseInlineMath 里也解析 Markdown。
                                                    
                                                    val hasBlockMarkdown = remember(contentToRender) { com.fishai.chatzen.ui.utils.hasBlockMarkdownSyntax(contentToRender) }
                                                    val isLastSegment = index == segments.lastIndex
                                                    
                                                    // 强制在流式输出的最后一段使用简单渲染（纯文本），
                                                    // 避免因为不完整的 Markdown 语法（如未闭合的 ** 或 *）导致布局频繁跳变和闪烁。
                                                    // 只有当段落完全生成并换行后，才会进行 Markdown 渲染。
                                                    val useSimpleRendering = isLastSegment && isStreaming

                                                    if (!useSimpleRendering && hasInlineMath && !hasBlockMarkdown) {
                                                        // 尝试使用 InlineTextContent 渲染
                                                        val inlineContent = remember(contentToRender, baseTextStyle.fontSize, textColor) {
                                                            parseInlineMath(contentToRender, baseTextStyle.fontSize, textColor, density)
                                                        }
                                                        
                                                        BasicText(
                                                            text = inlineContent.annotatedString,
                                                            inlineContent = inlineContent.inlineContent,
                                                            style = baseTextStyle,
                                                            modifier = if (isUser) Modifier else Modifier.fillMaxWidth()
                                                        )
                                                    } else if (!hasMarkdown || useSimpleRendering) {
                                                        Text(
                                                            text = contentToRender,
                                                            modifier = if (isUser) Modifier else Modifier.fillMaxWidth(),
                                                            style = baseTextStyle,
                                                            color = textColor
                                                        )
                                                    } else {
                                                        Markdown(
                                                            content = contentToRender,
                                                            modifier = Modifier, 
                                                            typography = customTypography,
                                                            components = markdownComponents
                                                        )
                                                    }
                                                }
                                                if (hasTrailingNewlines) Spacer(modifier = Modifier.height(12.dp))
                                            }
                                            is ContentSegment.Math -> {
                                                MathView(
                                                    latex = segment.latex,
                                                    textColor = textColor,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp
            ) {
                DropdownMenuItem(
                    text = { Text("复制内容") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("引用追问") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        onQuote(message.content)
                        showMenu = false
                    }
                )
            }
        }
        
        // User Avatar (Right)
        if (isUser) {
            ChatAvatar(
                role = message.role,
                userAvatarUri = userAvatarUri,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun QuotedContentBlock(
    quotedContent: String,
    textColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = { isExpanded = !isExpanded })
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "引用内容",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor.copy(alpha = 0.7f)
            )
        }

        if (isExpanded) {
            Text(
                text = quotedContent,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            
            Spacer(modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(textColor.copy(alpha = 0.2f))
                .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun LoadingIndicatorBubble() {
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = 4.dp, bottomEnd = 16.dp
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
         ChatAvatar(
            role = Role.ASSISTANT,
            modelName = "AI",
            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
        )

        Column(
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.70f)
                .clip(bubbleShape)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            TypingIndicator(
                dotColor = textColor.copy(alpha = 0.6f),
                dotSize = 8.dp,
                range = 10.dp
            )
        }
    }
}

@Composable
fun WebSearchContentBlock(
    searchResults: List<WebSearchResult>,
    textColor: Color,
    isStreaming: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isStreaming) {
        if (!isStreaming) {
            isExpanded = false
        }
    }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = { isExpanded = !isExpanded })
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.web_search_results),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor.copy(alpha = 0.7f)
            )
        }

        if (isExpanded) {
            searchResults.forEachIndexed { index, result ->
                Column(modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)) {
                    Text(
                        text = "${index + 1}. ${result.title}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = result.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.6f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(textColor.copy(alpha = 0.2f))
                .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun ReasoningContentBlock(
    reasoningContent: String,
    textColor: Color,
    isStreaming: Boolean
) {
    var isExpanded by remember { mutableStateOf(isStreaming) }

    LaunchedEffect(isStreaming) {
        if (!isStreaming) {
            isExpanded = false
        }
    }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = { isExpanded = !isExpanded })
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.deep_thinking_process),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor.copy(alpha = 0.7f)
            )
        }

        if (isExpanded) {
            Text(
                text = reasoningContent,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            
            Spacer(modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(textColor.copy(alpha = 0.2f))
                .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun OcrContentBlock(
    ocrContent: String,
    textColor: Color,
    isStreaming: Boolean
) {
    // Default to collapsed unless it's currently streaming
    var isExpanded by remember { mutableStateOf(isStreaming) }

    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            isExpanded = true
        } else {
            // Auto-collapse when streaming finishes
            isExpanded = false
        }
    }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = { isExpanded = !isExpanded })
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "图片分析过程",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor.copy(alpha = 0.7f)
            )
        }

        if (isExpanded) {
            Text(
                text = ocrContent,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            
            Spacer(modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(textColor.copy(alpha = 0.2f))
                .padding(bottom = 8.dp)
            )
        }
    }
}
