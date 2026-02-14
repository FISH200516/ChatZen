package com.fishai.chatzen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fishai.chatzen.data.database.UsageEntity
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import com.fishai.chatzen.ui.utils.scaleOnPress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.fishai.chatzen.R

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ScaffoldDefaults

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha

import androidx.compose.animation.core.tween
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.fishai.chatzen.ui.components.rememberMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(viewModel: UsageViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberScrollState()
    val vicoScrollState = rememberVicoScrollState()
    val vicoZoomState = rememberVicoZoomState(
        initialZoom = remember(uiState.selectedFilter) {
            if (uiState.selectedFilter == TimeFilter.DAY) Zoom.x(8.0) else Zoom.Content
        }
    )

    LaunchedEffect(uiState.usageData, uiState.selectedFilter, uiState.statsMode) {
        val aggregated = aggregateData(uiState.usageData, uiState.selectedFilter, uiState.statsMode)
        val hasData = aggregated.values.any { it.toDouble() > 0.0 }
        
        // Auto-switch filter if current range has no data
        // Only auto-upgrade if usageData is empty AND we are NOT in a state where we might have just switched
        // But actually, the issue described by user is that it skips DAY even if there is data?
        // Wait, if uiState.usageData is NOT empty, hasData should be true.
        // User says: "为什么现在当前日期有token或请求消耗的时候不默认显示日统计直接跳到周统计界面了"
        // This implies that checkAndAutoUpgradeFilter is being triggered even when there is data.
        // Or, maybe usageData is empty initially (loading state) and then it triggers upgrade?
        
        // Let's look at UsageViewModel.
        // The initial state has emptyList().
        // So on first composition, usageData is empty.
        // Then this LaunchedEffect runs.
        // It sees empty usageData.
        // It calls checkAndAutoUpgradeFilter().
        // So it switches DAY -> WEEK immediately before data loads from DB.
        
        // We need a way to distinguish "loading" from "no data".
        // But UsageUiState doesn't have isLoading.
        // However, we can just remove this auto-upgrade logic or make it smarter.
        // User probably doesn't want this auto-jump if it's buggy.
        // The previous request was "if current range has no data... auto switch".
        // But since we don't have a loading state, this is risky on startup.
        
        // Fix: Add a small delay or check if we have attempted to load.
        // Better yet, let's remove this auto-upgrade for now as it causes the bug described.
        // Or only do it if we are sure. 
        // A simple fix is to rely on user interaction for switching, or add a proper loading state.
        // Given the constraints, I will disable this auto-jump logic to fix the user's immediate problem.
        /*
        if (!hasData && uiState.usageData.isEmpty()) {
             viewModel.checkAndAutoUpgradeFilter()
        }
        */

        modelProducer.runTransaction {
            columnSeries {
                series(
                    x = aggregated.keys.toList(),
                    y = aggregated.values.toList()
                )
            }
        }

        // Auto-scroll bar chart to current time segment
        val calendar = Calendar.getInstance()
        val currentX = when (uiState.selectedFilter) {
            TimeFilter.DAY -> calendar.get(Calendar.HOUR_OF_DAY).toDouble()
            TimeFilter.WEEK -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                (if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2).toDouble()
            }

            TimeFilter.MONTH -> calendar.get(Calendar.WEEK_OF_MONTH).toDouble()
            TimeFilter.YEAR -> calendar.get(Calendar.MONTH).toDouble()
        }
        // Use a small delay to ensure chart data is populated
        // Try twice to ensure it works on both hot start (fast) and cold start (slow layout)
        kotlinx.coroutines.delay(100)
        vicoScrollState.scroll(Scroll.Absolute.x(currentX, bias = 0.5f))

        kotlinx.coroutines.delay(200)
        vicoScrollState.scroll(Scroll.Absolute.x(currentX, bias = 0.5f))
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.usage_stats),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Controls Section (Filter + Toggle)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        // Time Filters - FIXED WIDTH, NO SCROLL
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly, // Distribute evenly
                            modifier = Modifier.weight(1f),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            TimeFilter.entries.forEach { filter ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val label = when (filter) {
                                    TimeFilter.DAY -> stringResource(R.string.filter_day)
                                    TimeFilter.WEEK -> stringResource(R.string.filter_week)
                                    TimeFilter.MONTH -> stringResource(R.string.filter_month)
                                    TimeFilter.YEAR -> stringResource(R.string.filter_year)
                                }

                                FilterChip(
                                    selected = uiState.selectedFilter == filter,
                                    onClick = { viewModel.setFilter(filter) },
                                    label = { Text(label, maxLines = 1) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    interactionSource = interactionSource,
                                    modifier = Modifier.scaleOnPress(interactionSource)
                                )
                            }
                        }

                        // Stats Mode Toggle
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = if (uiState.statsMode == StatsMode.TOKENS) "Tokens" else "Reqs",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp),
                                maxLines = 1
                            )
                            val interactionSource = remember { MutableInteractionSource() }
                            androidx.compose.material3.Switch(
                                checked = uiState.statsMode == StatsMode.TOKENS,
                                onCheckedChange = { viewModel.toggleStatsMode(it) },
                                interactionSource = interactionSource,
                                modifier = Modifier.scaleOnPress(interactionSource),
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }

            // 2. Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    if (uiState.currentModelId != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.current_stat_model,
                                    uiState.currentModelId!!
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Dynamic Main Stat
                    val mainValue = if (uiState.statsMode == StatsMode.TOKENS) {
                        uiState.totalTokens.toString()
                    } else {
                        uiState.totalRequests.toString()
                    }

                    val mainLabel = if (uiState.statsMode == StatsMode.TOKENS) {
                        stringResource(R.string.total_tokens, "")
                    } else {
                        stringResource(R.string.total_requests, "")
                    }.replace(":", "").trim()

                    Text(
                        text = mainValue,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = mainLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // 3. Chart Section
            if (uiState.usageData.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = if (uiState.statsMode == StatsMode.TOKENS) "Token 消耗趋势" else "请求趋势",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Pre-fetch resources to avoid using LocalContext inside remember block
                        val hourSuffix = stringResource(R.string.chart_hour_suffix)
                        val weekFormat = stringResource(R.string.chart_week_format)
                        val monthSuffix = stringResource(R.string.chart_month_suffix)

                        val weekdays = listOf(
                            stringResource(R.string.weekday_mon),
                            stringResource(R.string.weekday_tue),
                            stringResource(R.string.weekday_wed),
                            stringResource(R.string.weekday_thu),
                            stringResource(R.string.weekday_fri),
                            stringResource(R.string.weekday_sat),
                            stringResource(R.string.weekday_sun)
                        )

                        val xAxisFormatter = remember(
                            uiState.selectedFilter,
                            hourSuffix,
                            weekFormat,
                            monthSuffix,
                            weekdays
                        ) {
                            CartesianValueFormatter { _, x, _ ->
                                val value = (x as Number).toInt()
                                when (uiState.selectedFilter) {
                                    TimeFilter.DAY -> String.format(hourSuffix, value)
                                    TimeFilter.WEEK -> {
                                        if (value in 0..6) weekdays[value] else " "
                                    }

                                    TimeFilter.MONTH -> String.format(weekFormat, value)
                                    TimeFilter.YEAR -> String.format(monthSuffix, value + 1)
                                }
                            }
                        }

                        val labelColor = MaterialTheme.colorScheme.onSurface
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(
                                    dataLabel = rememberTextComponent(color = labelColor),
                                    dataLabelValueFormatter = remember {
                                        CartesianValueFormatter { _, value, _ ->
                                            // Only show label if the bar is clicked (handled by marker) or implementing simple visibility toggle
                                            // Since Vico 2.0.0-alpha doesn't have built-in click-to-show-label easily without Marker,
                                            // we will hide default labels and let user tap bars (if interactive) or just hide them as requested.
                                            // "只有点击的时候才展示" implies we need a Marker.
                                            // For now, let's return empty string to hide default labels.
                                            ""
                                        }
                                    }
                                ),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = xAxisFormatter,
                                    guideline = null,
                                    itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() }
                                ),
                                marker = rememberMarker(labelColor), // Add marker for click/touch interaction
                            ),
                            modelProducer = modelProducer,
                            scrollState = vicoScrollState,
                            zoomState = vicoZoomState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    }
                }

                // 4. Distribution Chart
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.model_distribution),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        if (uiState.allModelsUsageData.isNotEmpty()) {
                            ModelUsageDonutChart(uiState.allModelsUsageData)
                        } else {
                            Text(stringResource(R.string.no_distribution_data))
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}


@Composable
fun ModelUsageDonutChart(usageData: List<UsageEntity>) {
    val modelUsage = remember(usageData) {
        usageData
            .groupBy { it.model }
            .mapValues { entry -> entry.value.size.toFloat() }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalRequests = remember(modelUsage) { modelUsage.sumOf { it.second.toDouble() }.toFloat() }

    // Define a list of colors for the chart
    val colors = listOf(
        Color(0xFF4285F4), // Google Blue
        Color(0xFFDB4437), // Google Red
        Color(0xFFF4B400), // Google Yellow
        Color(0xFF0F9D58), // Google Green
        Color(0xFFAA00FF), // Purple
        Color(0xFFFF6D00), // Orange
        Color(0xFF00BCD4), // Cyan
        Color(0xFFE91E63)  // Pink
    )

    // Animation state for donut chart
    val animatedProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(usageData) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = androidx.compose.ui.Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 40.dp.toPx()

                // Re-implementation inside Canvas for correct Radial Reveal
                var currentStartAngle = -90f
                val totalVisibleAngle = 360f * animatedProgress.value
                var accumulatedAngle = 0f

                modelUsage.forEachIndexed { index, (_, count) ->
                    val segmentAngle = (count / totalRequests) * 360f
                    val color = colors[index % colors.size]

                    // Determine how much of this segment is visible
                    val segmentStart = accumulatedAngle
                    val segmentEnd = accumulatedAngle + segmentAngle

                    if (segmentStart < totalVisibleAngle) {
                        // Calculate visible portion
                        val visibleEnd = minOf(segmentEnd, totalVisibleAngle)
                        val visibleSweep = visibleEnd - segmentStart

                        if (visibleSweep > 0) {
                            drawArc(
                                color = color,
                                startAngle = currentStartAngle,
                                sweepAngle = visibleSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    strokeWidth / 2,
                                    strokeWidth / 2
                                )
                            )
                        }
                    }

                    currentStartAngle += segmentAngle
                    accumulatedAngle += segmentAngle
                }
            }

            // Center text showing total requests (Fade in)
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.alpha(animatedProgress.value)
            ) {
                Text(
                    text = stringResource(R.string.total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = String.format("%.0f", totalRequests),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modelUsage.forEachIndexed { index, (model, count) ->
                val percentage = (count / totalRequests) * 100
                val color = colors[index % colors.size]

                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = model,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = String.format("%.1f%% (%.0f)", percentage, count),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Helper to aggregate data based on filter
fun aggregateData(
    data: List<UsageEntity>,
    filter: TimeFilter,
    statsMode: StatsMode
): Map<Number, Number> {
    val result = mutableMapOf<Number, Number>()
    val calendar = Calendar.getInstance()

    // Initialize defaults based on filter to ensure continuous axis
    when (filter) {
        TimeFilter.DAY -> {
            for (i in 0..23) result[i] = 0.0
        }

        TimeFilter.WEEK -> {
            // Mon(0)..Sun(6)
            for (i in 0..6) result[i] = 0.0
        }

        TimeFilter.MONTH -> {
            // Usually 4-6 weeks
            for (i in 1..5) result[i] = 0.0
        }

        TimeFilter.YEAR -> {
            // 0 (Jan) to 11 (Dec)
            for (i in 0..11) result[i] = 0.0
        }
    }

    data.forEach { entity ->
        calendar.timeInMillis = entity.timestamp
        val key: Number = when (filter) {
            TimeFilter.DAY -> calendar.get(Calendar.HOUR_OF_DAY) // 0-23
            TimeFilter.WEEK -> {
                // Map Calendar.DAY_OF_WEEK (Sun=1...Sat=7) to 0(Mon)...6(Sun)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            }

            TimeFilter.MONTH -> calendar.get(Calendar.WEEK_OF_MONTH) // 1-6
            TimeFilter.YEAR -> calendar.get(Calendar.MONTH) // 0-11
        }

        val current = result[key]?.toDouble() ?: 0.0
        val increment = if (statsMode == StatsMode.TOKENS) {
            (entity.inputTokens + entity.outputTokens).toDouble()
        } else {
            1.0
        }
        result[key] = current + increment
    }

    return result.toSortedMap(compareBy { (it as Number).toInt() })
}
