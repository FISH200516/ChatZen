package com.fishai.chatzen.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    range: Dp = 12.dp,
    animationDuration: Int = 1000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Typing")
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val rangePx = with(density) { range.toPx() }

    val offset1 by animateOffset(infiniteTransition, 0, rangePx, animationDuration)
    val offset2 by animateOffset(infiniteTransition, 150, rangePx, animationDuration)
    val offset3 by animateOffset(infiniteTransition, 300, rangePx, animationDuration)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDot(dotSize, dotColor, offset1)
        TypingDot(dotSize, dotColor, offset2)
        TypingDot(dotSize, dotColor, offset3)
    }
}

@Composable
private fun animateOffset(
    transition: InfiniteTransition,
    delay: Int,
    range: Float,
    duration: Int
): androidx.compose.runtime.State<Float> {
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration
                0f at 0 with LinearEasing
                -range at (duration * 0.3f).toInt() with FastOutSlowInEasing
                0f at (duration * 0.6f).toInt() with FastOutSlowInEasing
                0f at duration
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delay)
        ),
        label = "offset"
    )
}

@Composable
private fun TypingDot(
    size: Dp,
    color: Color,
    offset: Float
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { translationY = offset }
            .background(color = color, shape = CircleShape)
    )
}
