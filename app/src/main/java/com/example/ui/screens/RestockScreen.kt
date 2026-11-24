package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AmmoEntry
import com.example.data.CaliberBurnForecast
import com.example.data.CaliberInventorySummary
import com.example.data.CaliberThresholdConfigItem
import com.example.data.RestockLotItem
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.BuyTargetDialog
import com.example.ui.components.CaliberPill
import com.example.ui.components.CaliberProgressBar
import com.example.ui.components.CaliberTargetDialog
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.SpecBadge
import com.example.ui.components.StockStatusChip
import com.example.ui.components.displayCpr
import com.example.ui.components.displayCurrency
import com.example.ui.components.formatRounds
import com.example.ui.components.formatRunwayMonths
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.util.Locale
import kotlin.math.abs

@Composable
fun RestockScreen(
    uiState: AmmoVaultUiState,
    onCheckAmmoSeekEntry: (AmmoEntry) -> Unit,
    onCheckAmmoSeekCaliber: (String) -> Unit,
    onAdjustRounds: (AmmoEntry, Int) -> Unit,
    onUpdateEntryBuyTarget: (AmmoEntry, Double?) -> Unit,
    onSetCaliberTarget: (String, Int, Double?, Int) -> Unit,
    onRenameCaliber: (String, String) -> Unit,
    onDeleteCaliber: (String) -> Unit,
    onEntryClick: (Long) -> Unit,
    onAddAmmoClick: () -> Unit,
    onLoadStarterClick: () -> Unit,
    onOpenThresholdConfig: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var editingLotBuyTarget by remember { mutableStateOf<AmmoEntry?>(null) }
    var editingCaliberTarget by remember { mutableStateOf<CaliberInventorySummary?>(null) }

    if (editingLotBuyTarget != null) {
        val entry = editingLotBuyTarget!!
        val caliberDefault = uiState.caliberTargets.find { it.caliber == entry.caliber }?.targetBuyPricePerRound
        BuyTargetDialog(
            entry = entry,
            caliberDefaultTarget = caliberDefault,
            onDismiss = { editingLotBuyTarget = null },
            onSaveTarget = { newTarget ->
                onUpdateEntryBuyTarget(entry, newTarget)
                editingLotBuyTarget = null
            }
        )
    }

    if (editingCaliberTarget != null) {
        val summary = editingCaliberTarget!!
        CaliberTargetDialog(
            caliber = summary.caliber,
            currentTarget = summary.targetRounds,
            currentBuyTarget = summary.targetBuyPricePerRound,
            currentMinThreshold = summary.minThresholdRounds,
            lotCount = summary.lotCount,
            onDismiss = { editingCaliberTarget = null },
            onConfirm = { newTargetRounds, newBuyTarget, newMinThreshold ->
                onSetCaliberTarget(summary.caliber, newTargetRounds, newBuyTarget, newMinThreshold)
                editingCaliberTarget = null
            },
            onRenameCaliber = { newName ->
                onRenameCaliber(summary.caliber, newName)
                editingCaliberTarget = null
            },
            onDeleteCaliber = {
                onDeleteCaliber(summary.caliber)
                editingCaliberTarget = null
            }
        )
    }

    if (uiState.allEntries.isEmpty() && uiState.belowMinThresholdAlerts.isEmpty() && !uiState.isLoading) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                EmptyVaultState(
                    title = stringResource(id = R.string.empty_title),
                    subtitle = stringResource(id = R.string.empty_subtitle),
                    onAddClick = onAddAmmoClick,
                    onLoadStarterClick = onLoadStarterClick
                )
            }
        }
        return
    }

    val dealReadyCount = remember(uiState.restockLots) {
        uiState.restockLots.count { it.isAtOrBelowBuyTarget }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RestockSummaryHeroCard(
                lowLotCount = uiState.restockLots.size,
                underParCaliberCount = uiState.underParCalibers.size,
                belowMinThresholdCount = uiState.belowMinThresholdAlerts.size,
                estimatedLotRestockCost = uiState.estimatedRestockCost,
                dealReadyCount = dealReadyCount,
                onOpenThresholdConfig = onOpenThresholdConfig
            )
        }

        if (uiState.belowMinThresholdAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "Below Minimum Threshold (${uiState.belowMinThresholdAlerts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "These ammunition types dropped below your configured minimum round thresholds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(items = uiState.belowMinThresholdAlerts, key = { "min_${it.caliber}" }) { alert ->
                BelowMinThresholdAlertCard(
                    item = alert,
                    onCheckAmmoSeek = { onCheckAmmoSeekCaliber(alert.caliber) },
                    onOpenThresholdConfig = onOpenThresholdConfig
                )
            }
        }

        if (uiState.burnRateRestockAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "Burn-Rate Depletion Alerts (${uiState.burnRateRestockAlerts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Based on your Range Logs, these calibers are projected to run low within ~2.5 months.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(items = uiState.burnRateRestockAlerts, key = { "burn_${it.caliber}" }) { alert ->
                BurnRateRestockAlertCard(
                    forecast = alert,
                    onCheckAmmoSeek = { onCheckAmmoSeekCaliber(alert.caliber) }
                )
            }
        }

        if (uiState.restockLots.isEmpty() &&
            uiState.underParCalibers.isEmpty() &&
            uiState.burnRateRestockAlerts.isEmpty() &&
            uiState.belowMinThresholdAlerts.isEmpty()
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restock_all_healthy_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = LocalAmmoStatusColors.current.healthy,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "All Rifle Stock Above Par",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Every cataloged lot and caliber meets your target thresholds. You can still configure buy-target CPR benchmarks on calibers or individual lots.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.restockLots.isNotEmpty()) {
            item {
                Text(
                    text = "Low-Stock Lots (${uiState.restockLots.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Compare current CPR to your buy target, launch AmmoSeek, or log a +1 box restock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(items = uiState.restockLots, key = { "lot_${it.entry.id}" }) { item ->
                RestockLotCard(
                    item = item,
                    onCardClick = { onEntryClick(item.entry.id) },
                    onCheckAmmoSeek = { onCheckAmmoSeekEntry(item.entry) },
                    onEditBuyTarget = { editingLotBuyTarget = item.entry },
                    onQuickAddBox = { onAdjustRounds(item.entry, item.entry.boxSize) }
                )
            }
        }

        if (uiState.underParCalibers.isNotEmpty()) {
            item {
                Text(
                    text = "Under-Par Calibers (${uiState.underParCalibers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "Caliber totals below your configured round par levels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(items = uiState.underParCalibers, key = { "cal_${it.caliber}" }) { summary ->
                UnderParCaliberCard(
                    summary = summary,
                    burnForecast = uiState.burnForecastByCaliber[summary.caliber],
                    onCheckAmmoSeekCaliber = { onCheckAmmoSeekCaliber(summary.caliber) },
                    onEditCaliberTarget = { editingCaliberTarget = summary }
                )
            }
        }
    }
}

@Composable
private fun BurnRateRestockAlertCard(
    forecast: CaliberBurnForecast,
    onCheckAmmoSeek: () -> Unit
) {
    val statusColors = LocalAmmoStatusColors.current
    val isUrgent = (forecast.estimatedMonthsRemaining ?: 99.0) < 1.5
    val fg = if (isUrgent) statusColors.critical else statusColors.low
    val bg = if (isUrgent) statusColors.criticalContainer else statusColors.lowContainer

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("burn_restock_alert_${forecast.caliber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaliberPill(caliber = forecast.caliber)
                    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = formatRunwayMonths(forecast.estimatedMonthsRemaining),
                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                            color = fg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "Burning ~${forecast.estimatedMonthlyBurnRate.toInt()} rds/mo · ${formatRounds(forecast.currentRoundsOnHand)} rds left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onCheckAmmoSeek,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("AmmoSeek")
            }
        }
    }
}

@Composable
private fun BelowMinThresholdAlertCard(
    item: CaliberThresholdConfigItem,
    onCheckAmmoSeek: () -> Unit,
    onOpenThresholdConfig: (() -> Unit)?
) {
    val statusColors = LocalAmmoStatusColors.current
    val isDepleted = item.currentRounds <= 0
    val accentColor = if (isDepleted) statusColors.critical else statusColors.low
    val badgeBg = if (isDepleted) statusColors.criticalContainer else statusColors.lowContainer

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("restock_min_alert_${item.caliber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaliberPill(caliber = item.caliber)
                    Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "BELOW MIN (-${formatRounds(item.roundsDeficit)} rds)",
                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Text(
                text = "On hand: ${formatRounds(item.currentRounds)} rds · Minimum threshold: ${formatRounds(item.minThresholdRounds)} rds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCheckAmmoSeek,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AmmoSeek")
                }
                if (onOpenThresholdConfig != null) {
                    OutlinedButton(
                        onClick = onOpenThresholdConfig,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Min")
                    }
                }
            }
        }
    }
}

@Composable
private fun RestockSummaryHeroCard(
    lowLotCount: Int,
    underParCaliberCount: Int,
    belowMinThresholdCount: Int,
    estimatedLotRestockCost: Double,
    dealReadyCount: Int,
    onOpenThresholdConfig: (() -> Unit)?
) {
    val statusColors = LocalAmmoStatusColors.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("restock_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESTOCK QUEUE & BUY TARGETS",
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (dealReadyCount > 0) {
                    Surface(
                        color = statusColors.healthyContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$dealReadyCount AT BUY TARGET",
                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColors.healthy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        text = "$lowLotCount Lot${if (lowLotCount == 1) "" else "s"} Low",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$belowMinThresholdCount below min · $underParCaliberCount below par",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (belowMinThresholdCount > 0) statusColors.low else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayCurrency(estimatedLotRestockCost),
                        style = MonoTelemetryLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Est. Lot Restock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onOpenThresholdConfig != null) {
                OutlinedButton(
                    onClick = onOpenThresholdConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("restock_configure_thresholds_button")
                ) {
                    Icon(
                        imageVector = if (belowMinThresholdCount > 0) Icons.Filled.NotificationsActive else Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Minimum Thresholds")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestockLotCard(
    item: RestockLotItem,
    onCardClick: () -> Unit,
    onCheckAmmoSeek: () -> Unit,
    onEditBuyTarget: () -> Unit,
    onQuickAddBox: () -> Unit
) {
    val entry = item.entry
    val statusColors = LocalAmmoStatusColors.current

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("restock_lot_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaliberPill(caliber = entry.caliber)
                    StockStatusChip(status = entry.stockStatus)
                }
                Text(
                    text = displayCpr(entry.marketPricePerRound),
                    style = MonoTelemetryMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${entry.brand} ${entry.productLine}".trim(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.grain} gr ${entry.bulletType} · On hand: ${formatRounds(entry.currentCount)} / ≤ ${entry.lowStockThreshold} rds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${item.boxesSuggested} box${if (item.boxesSuggested == 1) "" else "es"}",
                        style = MonoTelemetryMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = displayCurrency(item.estimatedRestockCost),
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.effectiveBuyTargetCpr != null) {
                    val diff = entry.marketPricePerRound - item.effectiveBuyTargetCpr
                    val isDeal = item.isAtOrBelowBuyTarget
                    Surface(
                        color = if (isDeal) statusColors.healthyContainer else statusColors.lowContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isDeal) {
                                "BUY TARGET MET (≤ ${displayCpr(item.effectiveBuyTargetCpr)})"
                            } else {
                                "TARGET ${displayCpr(item.effectiveBuyTargetCpr)} (+${String.format(Locale.US, "$%.2f", abs(diff))})"
                            },
                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDeal) statusColors.healthy else statusColors.low,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    SpecBadge(text = "No Buy Target Set")
                }

                if (entry.storageLocation.isNotBlank()) {
                    SpecBadge(text = entry.storageLocation)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCheckAmmoSeek,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("restock_ammoseek_button_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AmmoSeek", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onEditBuyTarget,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("restock_buy_target_button_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PriceCheck,
                        contentDescription = "Set buy target",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target", maxLines = 1)
                }

                Button(
                    onClick = onQuickAddBox,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("restock_add_box_button_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+${entry.boxSize}", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun UnderParCaliberCard(
    summary: CaliberInventorySummary,
    burnForecast: CaliberBurnForecast? = null,
    onCheckAmmoSeekCaliber: () -> Unit,
    onEditCaliberTarget: () -> Unit
) {
    val statusColors = LocalAmmoStatusColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("restock_caliber_card_${summary.caliber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaliberPill(caliber = summary.caliber)
                    StockStatusChip(status = summary.status)
                }
                Text(
                    text = "Need ${formatRounds(summary.roundsToTarget)} rds",
                    style = MonoTelemetryMedium,
                    color = statusColors.low
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatRounds(summary.totalRounds)} / ${formatRounds(summary.targetRounds)} rds par",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "~${displayCurrency(summary.estimatedRestockCost)} est.",
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (burnForecast != null) {
                Text(
                    text = "Burn rate: ${burnForecast.estimatedMonthlyBurnRate.toInt()} rds/mo · ${formatRunwayMonths(burnForecast.estimatedMonthsRemaining)}",
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
                val buyTargetText = summary.targetBuyPricePerRound?.let {
                    "Buy target: ${displayCpr(it)}"
                } ?: "No caliber buy target"
                Text(
                    text = buyTargetText,
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onEditCaliberTarget,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Targets")
                    }
                    OutlinedButton(
                        onClick = onCheckAmmoSeekCaliber,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("restock_caliber_ammoseek_${summary.caliber}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AmmoSeek")
                    }
                }
            }
        }
    }
}
