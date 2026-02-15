package com.fishai.chatzen.ui.utils

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import ru.noties.jlatexmath.JLatexMathDrawable
import com.fishai.chatzen.ui.components.InlineMathView

sealed class ContentSegment {
    data class Text(val content: String) : ContentSegment()
    data class Math(val latex: String) : ContentSegment()
}

fun splitMarkdownContent(content: String): List<ContentSegment> {
    if (content.isEmpty()) return emptyList()

    val segments = mutableListOf<ContentSegment>()
    var i = 0
    var start = 0
    
    // States
    val STATE_TEXT = 0
    val STATE_CODE = 1
    val STATE_MATH_DISPLAY = 2 // \[ ... \]
    val STATE_MATH_DOLLAR = 3  // $$ ... $$
    
    var state = STATE_TEXT
    
    while (i < content.length) {
        when (state) {
            STATE_TEXT -> {
                if (content.startsWith("```", i)) {
                    state = STATE_CODE
                    i += 3
                } else if (content.startsWith("\\[", i)) {
                    // Flush text before math
                    if (i > start) {
                        segments.add(ContentSegment.Text(content.substring(start, i)))
                    }
                    start = i // Include delimiters in math segment for now? No, we strip them.
                    // Actually, let's just mark start of math content
                    start = i + 2
                    state = STATE_MATH_DISPLAY
                    i += 2
                } else if (content.startsWith("$$", i)) {
                    // Flush text before math
                    if (i > start) {
                        segments.add(ContentSegment.Text(content.substring(start, i)))
                    }
                    start = i + 2
                    state = STATE_MATH_DOLLAR
                    i += 2
                } else if (content.startsWith("\n\n", i)) {
                    // Split text segment on double newline to allow incremental rendering of paragraphs
                    if (i >= start) {
                        // Include the newlines in the segment so UI knows to add spacing
                        segments.add(ContentSegment.Text(content.substring(start, i + 2)))
                    }
                    i += 2
                    start = i
                } else {
                    i++
                }
            }
            STATE_CODE -> {
                if (content.startsWith("```", i)) {
                    state = STATE_TEXT
                    i += 3
                } else {
                    i++
                }
            }
            STATE_MATH_DISPLAY -> {
                if (content.startsWith("\\]", i)) {
                    val latex = content.substring(start, i).trim()
                    segments.add(ContentSegment.Math(latex))
                    i += 2
                    start = i
                    state = STATE_TEXT
                } else {
                    i++
                }
            }
            STATE_MATH_DOLLAR -> {
                if (content.startsWith("$$", i)) {
                    val latex = content.substring(start, i).trim()
                    segments.add(ContentSegment.Math(latex))
                    i += 2
                    start = i
                    state = STATE_TEXT
                } else {
                    i++
                }
            }
        }
    }
    
    // Handle remaining content
    if (start < content.length) {
        val remaining = content.substring(start)
        if (state == STATE_TEXT || state == STATE_CODE) {
            segments.add(ContentSegment.Text(remaining))
        } else {
            // Unclosed math block, treat as text with delimiters
            segments.add(ContentSegment.Text(remaining))
        }
    }
    
    return segments
}

fun hasMarkdownSyntax(content: String): Boolean {
    // Check for common Markdown indicators
    // Code blocks
    if (content.contains("```") || content.contains("`")) return true
    
    // Headers (at start of line)
    if (content.contains(Regex("(?m)^#{1,6}\\s"))) return true
    
    // Lists (at start of line)
    if (content.contains(Regex("(?m)^[\\*\\-\\+]\\s"))) return true
    if (content.contains(Regex("(?m)^\\d+\\.\\s"))) return true
    
    // Quotes
    if (content.contains(Regex("(?m)^>\\s"))) return true
    
    // Links and Images
    if (content.contains(Regex("\\[.*\\]\\(.*\\)"))) return true
    
    // Bold and Italic (simplified check, might have false positives but safe)
    // We look for pairs of * or _
    if (content.contains(Regex("[\\*_]{1,3}.+[\\*_]{1,3}"))) return true
    
    // Tables
    if (content.contains("|") && content.contains(Regex("(?m)^\\|.*\\|$"))) return true
    
    // Math
    if (content.contains("\\[") || content.contains("$$")) return true
    
    // Inline Math
    if (content.contains(Regex("(?<!\\\\)\\$"))) return true
    
    // Check for Block Markdown (Headers, Lists, Quotes, Tables) which usually cause layout shifts
    if (hasBlockMarkdownSyntax(content)) return true

    return false
}

fun hasBlockMarkdownSyntax(content: String): Boolean {
    // Code blocks
    if (content.contains("```")) return true
    
    // Headers (at start of line)
    if (content.contains(Regex("(?m)^#{1,6}\\s"))) return true
    
    // Lists (at start of line)
    if (content.contains(Regex("(?m)^[\\*\\-\\+]\\s"))) return true
    if (content.contains(Regex("(?m)^\\d+\\.\\s"))) return true
    
    // Quotes
    if (content.contains(Regex("(?m)^>\\s"))) return true
    
    // Tables
    if (content.contains("|") && content.contains(Regex("(?m)^\\|.*\\|$"))) return true
    
    // Math Block
    if (content.contains("\\[") || content.contains("$$")) return true
    
    return false
}

data class InlineMathContent(
    val annotatedString: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

fun parseInlineMath(
    content: String,
    fontSize: TextUnit,
    textColor: androidx.compose.ui.graphics.Color,
    density: Density
): InlineMathContent {
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    val parts = mutableListOf<String>()
    val isMathPart = mutableListOf<Boolean>()
    
    // Simple split by $...$ (non-greedy, lookbehind to avoid escaped \$)
    // Using simple regex for now, might need improvement for complex cases
    // Note: Kotlin Regex split keeps delimiters if using lookaround, but easier to find match ranges
    
    val regex = Regex("(?<!\\\\)\\$([^$]+?)(?<!\\\\)\\$")
    var lastIndex = 0
    
    regex.findAll(content).forEach { matchResult ->
        // Add text before math
        if (matchResult.range.first > lastIndex) {
            parts.add(content.substring(lastIndex, matchResult.range.first))
            isMathPart.add(false)
        }
        
        // Add math content (without $)
        parts.add(matchResult.groupValues[1])
        isMathPart.add(true)
        
        lastIndex = matchResult.range.last + 1
    }
    
    // Add remaining text
    if (lastIndex < content.length) {
        parts.add(content.substring(lastIndex))
        isMathPart.add(false)
    }
    
    val annotatedString = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (isMathPart[index]) {
                val id = "math_$index"
                val latex = part
                
                try {
                    val textSizePx = with(density) { fontSize.toPx() }
                    
                    // Pre-calculate drawable size
                    val drawable = JLatexMathDrawable.builder(latex)
                        .textSize(textSizePx)
                        .color(textColor.toArgb())
                        .align(JLatexMathDrawable.ALIGN_CENTER)
                        .build()
                        
                    val widthSp = with(density) { drawable.intrinsicWidth.toSp() }
                    val heightSp = with(density) { drawable.intrinsicHeight.toSp() }
                    
                    appendInlineContent(id, "[Formula]")
                    
                    inlineContentMap[id] = InlineTextContent(
                        Placeholder(
                            width = widthSp,
                            height = heightSp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        InlineMathView(drawable)
                    }
                } catch (e: Exception) {
                    // Fallback to text if math parsing fails
                    append("$$part$")
                }
            } else {
                append(part)
            }
        }
    }
    
    return InlineMathContent(annotatedString, inlineContentMap)
}
