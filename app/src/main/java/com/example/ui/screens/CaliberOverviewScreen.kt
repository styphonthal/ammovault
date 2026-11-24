package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CaliberInventorySummary
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.CaliberPill
import com.example.ui.components.CaliberPriceHistoryChartCard
import com.example.ui.components.CaliberProgressBar
import com.example.ui.components.CaliberTargetDialog
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.StockStatusChip
import com.example.ui.components.displayCpr
import com.example.ui.components.displayCurrency
import com.example.ui.components.formatRounds
import java.util.Locale
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall

@Composable
fun CaliberOverviewScreen(
    uiState: AmmoVaultUiState,
    onCaliberClick: (String) -> Unit,
    onSetCaliberTarget: (String, Int, Double?, Int) -> Unit,
    onRenameCaliber: (String, String) -> Unit,
    onDeleteCaliber: (String) -> Unit,
    onLowStockBannerClick: () -> Unit = {},
    onOpenStorageDashboard: (() -> Unit)? = null,
    onOpenThresholdConfig: (() -> Unit)? = null,
    onOpenMarketStats: (() -> Unit)? = null,
    onAddAmmoClick: () -> Unit,
    onLoadStarterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetEditCaliber by remember { mutableStateOf<CaliberInventorySummary?>(null) }

    if (targetEditCaliber != null) {
        val summary = targetEditCaliber!!
        CaliberTargetDialog(
            caliber = summary.caliber,
            currentTarget = summary.targetRounds,
            currentBuyTarget = summary.targetBuyPricePerRound,
            currentMinThreshold = summary.minThresholdRounds,
            lotCount = summary.lotCount,
            onDismiss = { targetEditCaliber = null },
            onConfirm = { newTarget, newBuyTarget, newMinThreshold ->
                onSetCaliberTarget(summary.caliber, newTarget, newBuyTarget, newMinThreshold)
                targetEditCaliber = null
            },
            onRenameCaliber = { newName ->
                onRenameCaliber(summary.caliber, newName)
                targetEditCaliber = null
            },
            onDeleteCaliber = {
                onDeleteCaliber(summary.caliber)
                targetEditCaliber = null
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        if (uiState.allEntries.isEmpty() && !uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ArmoryHeroBanner(
                        uiState = uiState,
                        onLowStockBannerClick = onLowStockBannerClick
                    )
                }
                if (onOpenThresholdConfig != null) {
                    item {
                        ThresholdQuickAccessCard(
                            uiState = uiState,
                            onClick = onOpenThresholdConfig
                        )
                    }
                }
                item {
                    EmptyVaultState(
                        title = stringResource(id = R.string.empty_title),
                        subtitle = stringResource(id = R.string.empty_subtitle),
                        onAddClick = onAddAmmoClick,
                        onLoadStarterClick = onLoadStarterClick
                    )
                }
            }
        } else if (isWide) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    ArmoryHeroBanner(
                        uiState = uiState,
                        onLowStockBannerClick = onLowStockBannerClick
                    )
                }
                if (onOpenStorageDashboard != null) {
                    item(span = { GridItemSpan(if (onOpenThresholdConfig != null) 1 else 2) }) {
                        StorageQuickAccessCard(
                            uiState = uiState,
                            onClick = onOpenStorageDashboard
                        )
                    }
                }
                if (onOpenThresholdConfig != null) {
                    item(span = { GridItemSpan(if (onOpenStorageDashboard != null) 1 else 2) }) {
                        ThresholdQuickAccessCard(
                            uiState = uiState,
                            onClick = onOpenThresholdConfig
                        )
                    }
                }
                item(span = { GridItemSpan(2) }) {
                    CaliberPriceHistoryChartCard(
                        seriesList = uiState.caliberPriceSeries,
                        onOpenMarketStats = onOpenMarketStats
                    )
                }
                item(span = { GridItemSpan(2) }) {
                    SectionHeaderRow(
                        caliberCount = uiState.caliberSummaries.size,
                        lowStockCount = uiState.lowStockLotCount
                    )
                }
                items(
                    items = uiState.caliberSummaries,
                    key = { it.caliber }
                ) { summary ->
                    CaliberSummaryCard(
                        summary = summary,
                        onClick = { onCaliberClick(summary.caliber) },
                        onEditTarget = { targetEditCaliber = summary }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ArmoryHeroBanner(
                        uiState = uiState,
                        onLowStockBannerClick = onLowStockBannerClick
                    )
                }
                if (onOpenThresholdConfig != null) {
                    item {
                        ThresholdQuickAccessCard(
                            uiState = uiState,
                            onClick = onOpenThresholdConfig
                        )
                    }
                }
                if (onOpenStorageDashboard != null) {
                    item {
                        StorageQuickAccessCard(
                            uiState = uiState,
                            onClick = onOpenStorageDashboard
                        )
                    }
                }
                item {
                    CaliberPriceHistoryChartCard(
                        seriesList = uiState.caliberPriceSeries,
                        onOpenMarketStats = onOpenMarketStats
                    )
                }
                item {
                    SectionHeaderRow(
                        caliberCount = uiState.caliberSummaries.size,
                        lowStockCount = uiState.lowStockLotCount
                    )
                }
                items(
                    items = uiState.caliberSummaries,
                    key = { it.caliber }
                ) { summary ->
                    CaliberSummaryCard(
                        summary = summary,
                        onClick = { onCaliberClick(summary.caliber) },
                        onEditTarget = { targetEditCaliber = summary }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdQuickAccessCard(
    uiState: AmmoVaultUiState,
    onClick: () -> Unit
) {
    val statusColors = LocalAmmoStatusColors.current
    val belowCount = uiState.belowMinThresholdAlerts.size
    val monitoredCount = remember(uiState.thresholdConfigItems) {
        uiState.thresholdConfigItems.count { it.isAlertEnabled }
    }
    val hasAlerts = belowCount > 0
    val accentColor = if (hasAlerts) statusColors.low else MaterialTheme.colorScheme.primary
    val iconContainer = if (hasAlerts) statusColors.lowContainer else MaterialTheme.colorScheme.primaryContainer

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(
            width = if (hasAlerts) 1.5.dp else 1.dp,
            color = if (hasAlerts) accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_threshold_config_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    color = iconContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasAlerts) Icons.Filled.NotificationsActive else Icons.Filled.Tune,
                            contentDescription = "Minimum stock thresholds",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Minimum Stock Thresholds",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasAlerts) {
                            "$belowCount ammo type${if (belowCount == 1) "" else "s"} below minimum · $monitoredCount monitored"
                        } else {
                            "$monitoredCount ammo type${if (monitoredCount == 1) "" else "s"} monitored · Tap to configure"
                        },
                        style = MonoTelemetrySmall,
                        color = if (hasAlerts) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open minimum stock thresholds",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StorageQuickAccessCard(
    uiState: AmmoVaultUiState,
    onClick: () -> Unit
) {
    val namedContainers = remember(uiState.storageSummaries) {
        uiState.storageSummaries.count { !it.isUnassigned }
    }
    val totalWeightLbs = remember(uiState.storageSummaries) {
        uiState.storageSummaries.sumOf { it.estimatedWeightLbs }
    }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_storage_dashboard_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = "Ammo cans and QR labels",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Ammo Cans & QR Labels",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$namedContainers can(s)/safe(s) · ~${String.format(Locale.US, "%.1f", totalWeightLbs)} lbs est.",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Ammo Cans & Storage",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ArmoryHeroBanner(
    uiState: AmmoVaultUiState,
    onLowStockBannerClick: () -> Unit = {}
) {
    val statusColors = LocalAmmoStatusColors.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("armory_hero_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 168.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_armory_banner),
                contentDescription = stringResource(id = R.string.banner_content_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                statusColors.scrimDark.copy(alpha = 0.65f),
                                statusColors.scrimDark.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RIFLE AMMO VAULT",
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${formatRounds(uiState.totalRounds)} Rounds",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                    if (uiState.lowStockLotCount > 0) {
                        Surface(
                            color = statusColors.lowContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, statusColors.low.copy(alpha = 0.45f)),
                            onClick = onLowStockBannerClick,
                            modifier = Modifier.testTag("hero_low_stock_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = "Low stock lots",
                                    tint = statusColors.low,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${uiState.lowStockLotCount} Low",
                                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColors.low
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroStatPill(
                        label = "EST. VALUE",
                        value = displayCurrency(uiState.totalMarketValue),
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "AVG CPR",
                        value = displayCpr(uiState.weightedAvgPricePerRound),
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "CALIBERS",
                        value = uiState.caliberSummaries.size.toString(),
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MonoTelemetryMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    caliberCount: Int,
    lowStockCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Inventory by Caliber ($caliberCount)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (lowStockCount > 0) {
            Text(
                text = "$lowStockCount lot(s) below par",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAmmoStatusColors.current.low
            )
        }
    }
}

@Composable
private fun CaliberSummaryCard(
    summary: CaliberInventorySummary,
    onClick: () -> Unit,
    onEditTarget: () -> Unit
) {
    val statusColors = LocalAmmoStatusColors.current
    val cprRangeText = if (kotlin.math.abs(summary.maxPricePerRound - summary.minPricePerRound) < 0.01) {
        displayCpr(summary.weightedAvgPricePerRound)
    } else {
        "${displayCpr(summary.minPricePerRound)} – ${displayCpr(summary.maxPricePerRound)}"
    }
    val alertAccent = if (summary.totalRounds <= 0) statusColors.critical else statusColors.low

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (summary.isBelowMinThreshold) 1.5.dp else 1.dp,
            color = if (summary.isBelowMinThreshold) alertAccent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("caliber_card_${summary.caliber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CaliberPill(caliber = summary.caliber)
                    StockStatusChip(status = summary.status)
                }
                IconButton(
                    onClick = onEditTarget,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_target_${summary.caliber}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Set target rounds for ${summary.caliber}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (summary.isBelowMinThreshold) {
                Surface(
                    color = if (summary.totalRounds <= 0) statusColors.criticalContainer else statusColors.lowContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caliber_below_min_alert_${summary.caliber}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Below minimum threshold",
                            tint = alertAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BELOW MIN (${formatRounds(summary.minThresholdRounds)} rds) · Need ${formatRounds(summary.roundsBelowMinThreshold)} rds",
                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                            color = alertAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${formatRounds(summary.totalRounds)} rds",
                        style = MonoTelemetryLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val minSuffix = if (summary.minThresholdRounds > 0) {
                        " · Min: ${formatRounds(summary.minThresholdRounds)}"
                    } else {
                        ""
                    }
                    Text(
                        text = "Par: ${formatRounds(summary.targetRounds)} rds$minSuffix · ${summary.lotCount} lot${if (summary.lotCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayCurrency(summary.totalMarketValue),
                        style = MonoTelemetryMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = cprRangeText,
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            CaliberProgressBar(
                progress = summary.progressFraction,
                status = summary.status,
                minThresholdFraction = summary.minThresholdFraction
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buyTargetSuffix = summary.targetBuyPricePerRound?.let {
                    " · Buy ≤ ${displayCpr(it)}"
                } ?: ""
                Text(
                    text = "Grains: ${summary.grainWeights.joinToString(", ") { "${it}gr" }}$buyTargetSuffix",
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Inspect Lots")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
