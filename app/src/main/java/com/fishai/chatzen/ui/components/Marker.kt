package com.fishai.chatzen.ui.components

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape

@Composable
fun rememberMarker(
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    bubbleColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    guidelineColor: Color = MaterialTheme.colorScheme.outlineVariant
): CartesianMarker {
    
    // 1. Label Component (Bubble)
    val label = rememberTextComponent(
        color = labelColor,
        typeface = Typeface.MONOSPACE,
        padding = insets(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
        background = rememberShapeComponent(
            shape = CorneredShape.Pill,
            fill = fill(bubbleColor),
            strokeFill = fill(MaterialTheme.colorScheme.outline),
            strokeThickness = 1.dp,
        ),
    )

    // 2. Indicator (Dot on data point)
    val indicator = rememberShapeComponent(
        shape = CorneredShape.Pill,
        fill = fill(indicatorColor),
    )

    // 3. Guideline (Vertical line)
    val guideline = rememberLineComponent(
        fill = fill(guidelineColor),
        thickness = 2.dp,
    )

    return remember(label, indicator, guideline) {
        object : DefaultCartesianMarker(
            label = label,
            labelPosition = LabelPosition.Top,
            indicator = { indicator },
            indicatorSizeDp = 6f,
            guideline = guideline,
        ) {
            // Customization if needed
        }
    }
}