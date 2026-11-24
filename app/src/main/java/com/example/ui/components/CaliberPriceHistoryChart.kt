package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CaliberPricePoint
import com.example.data.CaliberPriceSeries
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

private data class ChartLineSeries(
    val id: String,
    val label: String,
    val color: Color,
    val points: List<CaliberPricePoint>
)

private data class PlottedVertex(
    val seriesLabel: String,
    val color: Color,
    val point: CaliberPricePoint,
    val xFraction: Float,
    val yFraction: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaliberPriceHistoryChartCard(
    seriesList: List<CaliberPriceSeries>,
    modifier: Modifier = Modifier,
    onOpenMarketStats: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val hideDollars = LocalHideDollarValues.current
    val statusColors = LocalAmmoStatusColors.current

    val seriesPalette = remember(isDark, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary) {
        if (isDark) {
            listOf(
                Color(0xFFD4A34A), // Brass Gold
                Color(0xFFC87D4B), // Copper Accent
                Color(0xFF6D9F71), // Tactical Sage
                Color(0xFF64B5F6), // Steel Sky
                Color(0xFFCE93D8), // Ballistic Violet
                Color(0xFFFF8A65)  // Tracer Coral
            )
        } else {
            listOf(
                Color(0xFF9E711D), // Brass Deep
                Color(0xFF915229), // Copper Deep
                Color(0xFF3A6640), // Tactical Sage Deep
                Color(0xFF1F6E9C), // Deep Steel Blue
                Color(0xFF7B4397), // Deep Plum
                Color(0xFFC44D2D)  // Rust Accent
            )
        }
    }

    val availableCalibers = remember(seriesList) { seriesList.map { it.caliber } }
    var isAllMode by remember { mutableStateOf(false) }
    var selectedCaliber by remember { mutableStateOf<String?>(null) }
    var inspectedVertex by remember { mutableStateOf<PlottedVertex?>(null) }

    // Default to the first caliber with historical snapshots, and keep selection valid if DB changes
    LaunchedEffect(availableCalibers) {
        if (availableCalibers.isEmpty()) {
            selectedCaliber = null
            isAllMode = false
        } else if (!isAllMode && (selectedCaliber == null || selectedCaliber !in availableCalibers)) {
            selectedCaliber = seriesList
                .maxByOrNull { it.snapshotCount }
                ?.caliber
                ?: availableCalibers.first()
        }
        inspectedVertex = null
    }

    val activeSingleSeries = remember(seriesList, selectedCaliber, isAllMode) {
        if (isAllMode) null else seriesList.find { it.caliber == selectedCaliber } ?: seriesList.firstOrNull()
    }

    val plottedLines = remember(seriesList, activeSingleSeries, isAllMode, seriesPalette) {
        if (seriesList.isEmpty()) {
            emptyList()
        } else if (isAllMode) {
            seriesList.mapIndexed { idx, series ->
                ChartLineSeries(
                    id = series.caliber,
                    label = series.caliber,
                    color = seriesPalette[idx % seriesPalette.size],
                    points = series.comparisonPoints
                )
            }
        } else if (activeSingleSeries != null) {
            if (activeSingleSeries.lotLines.size > 1) {
                activeSingleSeries.lotLines.mapIndexed { idx, lotLine ->
                    ChartLineSeries(
                        id = "${activeSingleSeries.caliber}_${lotLine.entryId}",
                        label = lotLine.label,
                        color = seriesPalette[idx % seriesPalette.size],
                        points = lotLine.points
                    )
                }
            } else {
                listOf(
                    ChartLineSeries(
                        id = activeSingleSeries.caliber,
                        label = activeSingleSeries.lotLines.firstOrNull()?.label ?: activeSingleSeries.caliber,
                        color = seriesPalette.first(),
                        points = activeSingleSeries.points
                    )
                )
            }
        } else {
            emptyList()
        }
    }

    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(isAllMode, selectedCaliber, plottedLines.size) {
        inspectedVertex = null
        revealAnim.snapTo(0f)
        revealAnim.animateTo(1f, animationSpec = tween(durationMillis = 320))
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_price_history_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = stringResource(id = R.string.price_chart_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(id = R.string.price_chart_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(id = R.string.price_chart_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onOpenMarketStats != null) {
                    TextButton(
                        onClick = onOpenMarketStats,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("price_chart_open_market_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.price_chart_open_market),
                            maxLines = 1
                        )
                    }
                }
            }

            if (seriesList.isEmpty() || plottedLines.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.price_chart_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            // Caliber Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (seriesList.size > 1) {
                    FilterChip(
                        selected = isAllMode,
                        onClick = { isAllMode = true },
                        label = {
                            Text(
                                text = stringResource(id = R.string.price_chart_all_calibers),
                                maxLines = 1
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("price_chart_caliber_chip_all")
                    )
                }
                seriesList.forEach { series ->
                    val isSelected = !isAllMode && activeSingleSeries?.caliber == series.caliber
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            isAllMode = false
                            selectedCaliber = series.caliber
                        },
                        label = { Text(text = series.caliber, maxLines = 1) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("price_chart_caliber_chip_${series.caliber}")
                    )
                }
            }

            // Telemetry Summary Row (Single Caliber vs All Calibers)
            if (activeSingleSeries != null && !isAllMode) {
                SingleCaliberTelemetryStrip(
                    series = activeSingleSeries,
                    hideDollars = hideDollars
                )
            } else {
                val allPrices = plottedLines.flatMap { it.points }.map { it.pricePerRound }
                val overallMin = allPrices.minOrNull() ?: 0.0
                val overallMax = allPrices.maxOrNull() ?: 0.0
                val totalSnapshots = seriesList.sumOf { it.snapshotCount }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${seriesList.size} calibers · $totalSnapshots snapshots",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Range: ${displayCpr(overallMin)} – ${displayCpr(overallMax)}",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Interactive Snapshot Inspector Pill (shown when user taps/drags chart)
            if (inspectedVertex != null) {
                val active = inspectedVertex!!
                val formattedDate = remember(active.point.recordedAt) {
                    SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(active.point.recordedAt))
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, active.color.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("price_chart_inspector_tooltip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(active.color)
                            )
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayCpr(active.point.pricePerRound),
                                        style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formattedDate,
                                        style = MonoTelemetrySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                val detailText = buildList {
                                    add(if (isAllMode) active.point.caliber else active.point.lotLabel)
                                    if (active.point.source.isNotBlank()) add(active.point.source)
                                    if (active.point.vendor.isNotBlank() && active.point.vendor != active.point.source) {
                                        add(active.point.vendor)
                                    }
                                }.joinToString(" · ")
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { inspectedVertex = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear selected data point",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Line Chart Canvas
            val buyTargetForChart = if (!isAllMode) activeSingleSeries?.targetBuyPricePerRound else null
            InteractivePriceLineCanvas(
                lines = plottedLines,
                buyTargetCpr = buyTargetForChart,
                hideDollars = hideDollars,
                revealFraction = revealAnim.value,
                inspectedVertex = inspectedVertex,
                onInspectVertex = { inspectedVertex = it },
                targetLineColor = statusColors.healthy
            )

            // Series Legend (when showing multiple lots or multiple calibers)
            if (plottedLines.size > 1) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    plottedLines.forEach { line ->
                        val lastCpr = line.points.lastOrNull()?.pricePerRound
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = isAllMode) {
                                    if (isAllMode) {
                                        isAllMode = false
                                        selectedCaliber = line.label
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(line.color)
                                )
                                Text(
                                    text = line.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (lastCpr != null) {
                                    Text(
                                        text = displayCpr(lastCpr),
                                        style = MonoTelemetrySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(id = R.string.price_chart_touch_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SingleCaliberTelemetryStrip(
    series: CaliberPriceSeries,
    hideDollars: Boolean
) {
    val statusColors = LocalAmmoStatusColors.current
    val delta = series.priceDelta
    val hasTrend = series.comparisonPoints.size >= 2 && abs(delta) >= 0.005
    // Lower ammo price is good (healthy green), higher ammo price is amber/low
    val trendColor = when {
        !hasTrend -> MaterialTheme.colorScheme.onSurfaceVariant
        delta < 0 -> statusColors.healthy
        else -> statusColors.low
    }
    val trendIcon = when {
        !hasTrend -> Icons.AutoMirrored.Filled.TrendingFlat
        delta < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayCpr(series.latestPricePerRound),
                    style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!hideDollars && series.comparisonPoints.size >= 2) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (hasTrend) {
                            if (delta < 0) statusColors.healthyContainer else statusColors.lowContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(13.dp)
                            )
                            val sign = if (delta > 0) "+" else if (delta < 0) "-" else ""
                            val pctText = series.priceDeltaPercent?.let { pct ->
                                val pctSign = if (pct > 0) "+" else ""
                                " ($pctSign${String.format(Locale.US, "%.1f", pct)}%)"
                            } ?: ""
                            Text(
                                text = "$sign${String.format(Locale.US, "$%.2f", abs(delta))}$pctText",
                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                color = trendColor
                            )
                        }
                    }
                }
            }
            Text(
                text = "Low ${displayCpr(series.minPricePerRound)} · High ${displayCpr(series.maxPricePerRound)} · ${series.snapshotCount} pts",
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (series.targetBuyPricePerRound != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColors.healthyContainer,
                border = BorderStroke(1.dp, statusColors.healthy.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "Target ${displayCpr(series.targetBuyPricePerRound)}",
                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColors.healthy,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractivePriceLineCanvas(
    lines: List<ChartLineSeries>,
    buyTargetCpr: Double?,
    hideDollars: Boolean,
    revealFraction: Float,
    inspectedVertex: PlottedVertex?,
    onInspectVertex: (PlottedVertex?) -> Unit,
    targetLineColor: Color
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceBg = MaterialTheme.colorScheme.surfaceContainerLow
    val pointCenterBg = MaterialTheme.colorScheme.surface
    val crosshairColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)

    val axisLabelStyle = remember(axisTextColor) {
        MonoTelemetrySmall.copy(fontSize = 10.sp, color = axisTextColor)
    }
    val targetLabelStyle = remember(targetLineColor) {
        MonoTelemetrySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = targetLineColor)
    }

    val allPoints = remember(lines) { lines.flatMap { it.points } }
    val minTime = remember(allPoints) { allPoints.minOfOrNull { it.recordedAt } ?: 0L }
    val maxTime = remember(allPoints) { allPoints.maxOfOrNull { it.recordedAt } ?: 0L }
    val timeSpan = (maxTime - minTime).coerceAtLeast(0L)

    val priceDomain = remember(allPoints, buyTargetCpr) {
        val values = buildList {
            allPoints.forEach { add(it.pricePerRound) }
            if (buyTargetCpr != null && buyTargetCpr > 0.0) add(buyTargetCpr)
        }
        val rawMin = values.minOrNull() ?: 0.0
        val rawMax = values.maxOrNull() ?: 1.0
        val diff = rawMax - rawMin
        if (diff < 0.02) {
            val pad = (rawMax * 0.12).coerceAtLeast(0.05)
            val low = (rawMin - pad).coerceAtLeast(0.0)
            val high = rawMax + pad
            low to high
        } else {
            val pad = diff * 0.14
            val low = (rawMin - pad).coerceAtLeast(0.0)
            val high = rawMax + pad
            low to high
        }
    }
    val minYPrice = priceDomain.first
    val maxYPrice = priceDomain.second
    val priceSpan = (maxYPrice - minYPrice).coerceAtLeast(0.001)

    // Precompute normalized vertices (0f..1f) for both drawing and touch hit-testing
    val verticesByLine = remember(lines, minTime, timeSpan, minYPrice, priceSpan) {
        lines.map { line ->
            val count = line.points.size
            line to line.points.mapIndexed { idx, pt ->
                val xFrac = when {
                    count <= 1 && timeSpan <= 0L -> 0.5f
                    timeSpan > 0L -> ((pt.recordedAt - minTime).toDouble() / timeSpan.toDouble()).toFloat().coerceIn(0f, 1f)
                    else -> (idx.toFloat() / (count - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                }
                val yFrac = ((pt.pricePerRound - minYPrice) / priceSpan).toFloat().coerceIn(0f, 1f)
                PlottedVertex(
                    seriesLabel = line.label,
                    color = line.color,
                    point = pt,
                    xFraction = xFrac,
                    yFraction = yFrac
                )
            }
        }
    }
    val flatVertices = remember(verticesByLine) { verticesByLine.flatMap { it.second } }

    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.US) }
    val xTickLabels = remember(minTime, maxTime, timeSpan) {
        if (minTime <= 0L) {
            emptyList()
        } else if (timeSpan <= 60_000L) {
            listOf(0.5f to dateFormatter.format(Date(minTime)))
        } else {
            val midTime = minTime + timeSpan / 2L
            listOf(
                0f to dateFormatter.format(Date(minTime)),
                0.5f to dateFormatter.format(Date(midTime)),
                1f to dateFormatter.format(Date(maxTime))
            )
        }
    }

    var canvasWidthPx by remember { mutableFloatStateOf(1f) }
    var canvasHeightPx by remember { mutableFloatStateOf(1f) }

    val leftInsetPx = with(density) { 48.dp.toPx() }
    val rightInsetPx = with(density) { 14.dp.toPx() }
    val topInsetPx = with(density) { 16.dp.toPx() }
    val bottomInsetPx = with(density) { 26.dp.toPx() }

    fun findNearestVertex(touchOffset: Offset, preferXOnly: Boolean): PlottedVertex? {
        if (flatVertices.isEmpty()) return null
        val plotW = (canvasWidthPx - leftInsetPx - rightInsetPx).coerceAtLeast(1f)
        val plotH = (canvasHeightPx - topInsetPx - bottomInsetPx).coerceAtLeast(1f)
        return flatVertices.minByOrNull { vertex ->
            val vx = leftInsetPx + vertex.xFraction * plotW
            val vy = topInsetPx + (1f - vertex.yFraction) * plotH
            if (preferXOnly) {
                abs(vx - touchOffset.x)
            } else {
                hypot(vx - touchOffset.x, vy - touchOffset.y)
            }
        }
    }

    val chartAccessibilityDesc = remember(lines, hideDollars) {
        val summary = lines.joinToString(", ") { line ->
            val latest = line.points.lastOrNull()?.pricePerRound
            val latestStr = if (latest == null) "" else if (hideDollars) "price hidden" else String.format(Locale.US, "$%.2f per round", latest)
            "${line.label} (${line.points.size} points, latest $latestStr)"
        }
        "Price history per round line chart showing $summary"
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(198.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceBg)
            .onSizeChanged { size ->
                canvasWidthPx = size.width.toFloat().coerceAtLeast(1f)
                canvasHeightPx = size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(flatVertices, canvasWidthPx, canvasHeightPx) {
                detectTapGestures { offset ->
                    onInspectVertex(findNearestVertex(offset, preferXOnly = false))
                }
            }
            .pointerInput(flatVertices, canvasWidthPx, canvasHeightPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onInspectVertex(findNearestVertex(offset, preferXOnly = true))
                    },
                    onHorizontalDrag = { change, _ ->
                        onInspectVertex(findNearestVertex(change.position, preferXOnly = true))
                    }
                )
            }
            .semantics { contentDescription = chartAccessibilityDesc }
            .testTag("price_chart_canvas")
    ) {
        val plotLeft = leftInsetPx
        val plotRight = (size.width - rightInsetPx).coerceAtLeast(plotLeft + 1f)
        val plotTop = topInsetPx
        val plotBottom = (size.height - bottomInsetPx).coerceAtLeast(plotTop + 1f)
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        // 1. Horizontal Y-Axis Gridlines & Labels (Top, Mid, Bottom)
        val gridSteps = 2 // 3 lines: 0%, 50%, 100%
        for (step in 0..gridSteps) {
            val frac = step.toFloat() / gridSteps.toFloat()
            val y = plotBottom - frac * plotHeight
            val priceAtStep = minYPrice + frac * priceSpan

            drawLine(
                color = gridLineColor.copy(alpha = if (step == 0) 0.75f else 0.45f),
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = if (step == 0) 1.5f else 1.2f,
                pathEffect = if (step == 0) null else dashEffect
            )

            val yLabel = if (hideDollars) "••••" else String.format(Locale.US, "$%.2f", priceAtStep)
            val measured = textMeasurer.measure(text = yLabel, style = axisLabelStyle)
            val textX = (plotLeft - measured.size.width - 6.dp.toPx()).coerceAtLeast(4.dp.toPx())
            val textY = (y - measured.size.height / 2f).coerceIn(2f, size.height - measured.size.height - 2f)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(textX, textY)
            )
        }

        // 2. X-Axis Date Labels
        xTickLabels.forEach { (xFrac, label) ->
            val measured = textMeasurer.measure(text = label, style = axisLabelStyle)
            val rawX = plotLeft + xFrac * plotWidth
            val textX = when {
                xFrac <= 0.05f -> plotLeft
                xFrac >= 0.95f -> (plotRight - measured.size.width).coerceAtLeast(plotLeft)
                else -> (rawX - measured.size.width / 2f).coerceIn(plotLeft, (plotRight - measured.size.width).coerceAtLeast(plotLeft))
            }
            val textY = plotBottom + 6.dp.toPx()
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(textX, textY)
            )
        }

        // 3. Optional Buy Target Reference Line (Single-Caliber Mode)
        if (buyTargetCpr != null && buyTargetCpr in minYPrice..maxYPrice) {
            val targetFrac = ((buyTargetCpr - minYPrice) / priceSpan).toFloat().coerceIn(0f, 1f)
            val targetY = plotBottom - targetFrac * plotHeight
            drawLine(
                color = targetLineColor.copy(alpha = 0.85f),
                start = Offset(plotLeft, targetY),
                end = Offset(plotRight, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
            if (!hideDollars) {
                val targetText = String.format(Locale.US, "Buy ≤ $%.2f", buyTargetCpr)
                val measuredTarget = textMeasurer.measure(text = targetText, style = targetLabelStyle)
                val labelY = (targetY - measuredTarget.size.height - 2.dp.toPx()).coerceAtLeast(2.dp.toPx())
                drawText(
                    textLayoutResult = measuredTarget,
                    topLeft = Offset(
                        x = (plotRight - measuredTarget.size.width - 2.dp.toPx()).coerceAtLeast(plotLeft),
                        y = labelY
                    )
                )
            }
        }

        // 4. Series Area Fill (Single-Line) & Crisp Polyline Curves (Animated Reveal)
        val clipRightEdge = plotLeft + (plotWidth + 14.dp.toPx()) * revealFraction.coerceIn(0f, 1f)
        clipRect(
            left = plotLeft - 12.dp.toPx(),
            top = 0f,
            right = clipRightEdge,
            bottom = size.height
        ) {
            val isSingleLine = verticesByLine.size == 1
            verticesByLine.forEach { (line, vertices) ->
                if (vertices.isEmpty()) return@forEach
                val offsets = vertices.map { v ->
                    Offset(
                        x = plotLeft + v.xFraction * plotWidth,
                        y = plotBottom - v.yFraction * plotHeight
                    )
                }

                if (offsets.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(offsets.first().x, offsets.first().y)
                        for (i in 1 until offsets.size) {
                            lineTo(offsets[i].x, offsets[i].y)
                        }
                    }

                    if (isSingleLine) {
                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(offsets.last().x, plotBottom)
                            lineTo(offsets.first().x, plotBottom)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    line.color.copy(alpha = 0.28f),
                                    line.color.copy(alpha = 0.02f)
                                ),
                                startY = plotTop,
                                endY = plotBottom
                            )
                        )
                    }

                    drawPath(
                        path = linePath,
                        color = line.color,
                        style = Stroke(
                            width = if (isSingleLine) 3.dp.toPx() else 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Data Point Nodes
                val outerRadius = if (isSingleLine) 4.5.dp.toPx() else 3.8.dp.toPx()
                val innerRadius = if (isSingleLine) 2.2.dp.toPx() else 1.8.dp.toPx()
                offsets.forEach { pt ->
                    drawCircle(color = line.color, radius = outerRadius, center = pt)
                    drawCircle(color = pointCenterBg, radius = innerRadius, center = pt)
                }
            }
        }

        // 5. Active Inspected Point Crosshair & Halo
        if (inspectedVertex != null) {
            val activeX = plotLeft + inspectedVertex.xFraction * plotWidth
            val activeY = plotBottom - inspectedVertex.yFraction * plotHeight
            drawLine(
                color = crosshairColor,
                start = Offset(activeX, plotTop),
                end = Offset(activeX, plotBottom),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )
            drawCircle(
                color = inspectedVertex.color.copy(alpha = 0.24f),
                radius = 11.dp.toPx(),
                center = Offset(activeX, activeY)
            )
            drawCircle(
                color = inspectedVertex.color,
                radius = 5.5.dp.toPx(),
                center = Offset(activeX, activeY)
            )
            drawCircle(
                color = pointCenterBg,
                radius = 2.5.dp.toPx(),
                center = Offset(activeX, activeY)
            )
        }
    }
}
