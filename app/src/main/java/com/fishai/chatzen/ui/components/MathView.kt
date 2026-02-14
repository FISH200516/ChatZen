package com.fishai.chatzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.noties.jlatexmath.JLatexMathDrawable

@Composable
fun InlineMathView(drawable: JLatexMathDrawable) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { view ->
            view.setImageDrawable(drawable)
        },
        modifier = Modifier.wrapContentSize()
    )
}

@Composable
fun MathView(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val headerColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Formula",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
             IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(latex))
                    isCopied = true
                    coroutineScope.launch {
                        delay(2000)
                        isCopied = false
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (isCopied) "Copied" else "Copy latex",
                    modifier = Modifier.size(14.dp),
                    tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
             AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        adjustViewBounds = true
                    }
                },
                update = { view ->
                    try {
                        val drawable = JLatexMathDrawable.builder(latex)
                            .textSize(50f)
                            .color(textColor.toArgb())
                            .background(android.graphics.Color.TRANSPARENT)
                            .align(JLatexMathDrawable.ALIGN_CENTER)
                            .build()
                        view.setImageDrawable(drawable)
                    } catch (e: Exception) {
                        val drawable = JLatexMathDrawable.builder("\\text{Error}")
                            .textSize(50f)
                            .color(android.graphics.Color.RED)
                            .align(JLatexMathDrawable.ALIGN_CENTER)
                            .build()
                        view.setImageDrawable(drawable)
                    }
                },
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}
