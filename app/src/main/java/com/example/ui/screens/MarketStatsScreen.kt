package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AmmoEntry
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.CaliberBurnForecastChart
import com.example.ui.components.CaliberPill
import com.example.ui.components.CaliberPriceHistoryChartCard
import com.example.ui.components.CaliberValueBarChart
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.LocalHideDollarValues
import com.example.ui.components.QuickMarketPriceDialog
import com.example.ui.components.displayCpr
import com.example.ui.components.displayCurrency
import com.example.ui.components.displaySignedCurrency
import com.example.ui.components.formatHistoryTimestamp
import com.example.ui.components.formatRounds
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import kotlin.math.abs

@Composable
fun MarketStatsScreen(
    uiState: AmmoVaultUiState,
    onUpdateMarketPrice: (AmmoEntry, Double, String) -> Unit,
    onCheckAmmoSeek: (AmmoEntry) -> Unit,
    onEntryClick: (Long) -> Unit,
    onAddAmmoClick: () -> Unit,
    onLoadStarterClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckAmmoSeekUrl: ((AmmoEntry, String) -> Unit)? = null,
    onOpenRangeLogsClick: (() -> Unit)? = null
) {
    var priceEditEntry by remember { mutableStateOf<AmmoEntry?>(null) }

    if (priceEditEntry != null) {
        val entry = priceEditEntry!!
        QuickMarketPriceDialog(
            entry = entry,
            onDismiss = { priceEditEntry = null },
            onSavePrice = { newPrice, source ->
                onUpdateMarketPrice(entry, newPrice, source)
                priceEditEntry = null
            },
            onCheckAmmoSeek = {
                priceEditEntry = null
                onCheckAmmoSeek(entry)
            },
            onCheckAmmoSeekUrl = onCheckAmmoSeekUrl?.let { callback ->
                { url ->
                    priceEditEntry = null
                    callback(entry, url)
                }
            }
        )
    }

    if (uiState.allEntries.isEmpty() && !uiState.isLoading) {
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

    val bestValueEntry = remember(uiState.allEntries) {
        uiState.allEntries.minByOrNull { it.marketPricePerRound }
    }
    val premiumEntry = remember(uiState.allEntries) {
        uiState.allEntries.maxByOrNull { it.marketPricePerRound }
    }
    val entriesByValue = remember(uiState.allEntries) {
        uiState.allEntries.sortedByDescending { it.totalMarketValue }
    }
    val entriesById = remember(uiState.allEntries) {
        uiState.allEntries.associateBy { it.id }
    }
    val recentHistory = remember(uiState.allPriceHistory) {
        uiState.allPriceHistory.take(10)
    }
    val statusColors = LocalAmmoStatusColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "TOTAL MARKET VALUATION",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = displayCurrency(uiState.totalMarketValue),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = displayCpr(uiState.weightedAvgPricePerRound),
                                style = MonoTelemetryLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Weighted Avg CPR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TOTAL COST BASIS",
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayCurrency(uiState.totalCostBasis),
                                style = MonoTelemetryMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "UNREALIZED GAIN / LOSS",
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val netDelta = uiState.totalUnrealizedGainLoss
                            val pct = if (uiState.totalCostBasis > 0.005) {
                                (netDelta / uiState.totalCostBasis) * 100.0
                            } else {
                                null
                            }
                            val pctSuffix = if (LocalHideDollarValues.current) {
                                ""
                            } else {
                                pct?.let {
                                    val sign = if (it >= 0) "+" else ""
                                    " ($sign${String.format(Locale.US, "%.1f", it)}%)"
                                } ?: ""
                            }
                            val deltaColor = when {
                                netDelta > 0.005 -> statusColors.healthy
                                netDelta < -0.005 -> statusColors.low
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = "${displaySignedCurrency(netDelta)}$pctSuffix",
                                style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                                color = deltaColor
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (bestValueEntry != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LOWEST CPR",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = displayCpr(bestValueEntry.marketPricePerRound),
                                    style = MonoTelemetryMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "${bestValueEntry.caliber} · ${bestValueEntry.brand}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (premiumEntry != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HIGHEST CPR",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = displayCpr(premiumEntry.marketPricePerRound),
                                    style = MonoTelemetryMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${premiumEntry.caliber} · ${premiumEntry.brand}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            CaliberPriceHistoryChartCard(
                seriesList = uiState.caliberPriceSeries
            )
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Value Distribution by Caliber",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CaliberValueBarChart(summaries = uiState.caliberSummaries)
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_burn_forecast_card")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Consumption & Depletion Forecast",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.totalRoundsFiredAllTime > 0) {
                                    "${formatRounds(uiState.totalRoundsFiredLast30Days)} rds fired last 30d · ${displayCurrency(uiState.totalAmmoCostBurnedAllTime)} total burned"
                                } else {
                                    "Derived automatically from your Range Logs."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.caliberBurnForecasts.isEmpty()) {
                        Text(
                            text = "Log a trip in the Range tab to track monthly burn rate and estimated months of ammo left per caliber.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onOpenRangeLogsClick != null) {
                            OutlinedButton(
                                onClick = onOpenRangeLogsClick,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("market_open_range_logs_button")
                            ) {
                                Text("Open Range Logs & Bags")
                            }
                        }
                    } else {
                        CaliberBurnForecastChart(forecasts = uiState.caliberBurnForecasts)
                    }
                }
            }
        }

        item {
            Text(
                text = "Current Market Price Ledger",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Check live pricing on AmmoSeek.com or tap CPR to log a new price snapshot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(items = entriesByValue, key = { it.id }) { entry ->
            val historyForEntry = uiState.priceHistoryByEntryId[entry.id].orEmpty()
            val earliestPrice = historyForEntry.minByOrNull { it.recordedAt }?.pricePerRound
            val priceDelta = if (historyForEntry.size >= 2 && earliestPrice != null) {
                entry.marketPricePerRound - earliestPrice
            } else {
                null
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                onClick = { onEntryClick(entry.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_lot_card_${entry.id}")
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
                            CaliberPill(caliber = entry.caliber)
                            Text(
                                text = "${entry.grain} gr ${entry.bulletType}",
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${entry.brand} ${entry.productLine}".trim(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatRounds(entry.currentCount)} rds · ${displayCurrency(entry.totalMarketValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (priceDelta != null && abs(priceDelta) >= 0.005 && !LocalHideDollarValues.current) {
                                val sign = if (priceDelta > 0) "+" else "-"
                                val deltaColor = if (priceDelta > 0) statusColors.low else statusColors.healthy
                                Text(
                                    text = "$sign${String.format(Locale.US, "$%.2f", abs(priceDelta))}",
                                    style = MonoTelemetrySmall,
                                    color = deltaColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onCheckAmmoSeek(entry) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("check_ammoseek_button_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Check ${entry.brand} ${entry.caliber} on AmmoSeek",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        OutlinedButton(
                            onClick = { priceEditEntry = entry },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("update_cpr_button_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit market price",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayCpr(entry.marketPricePerRound),
                                style = MonoTelemetrySmall
                            )
                        }
                    }
                }
            }
        }

        if (recentHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Price History Ledger",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "Recent CPR snapshots recorded from purchases, edits, and AmmoSeek checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_price_history_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentHistory.forEachIndexed { index, snapshot ->
                            val linkedEntry = entriesById[snapshot.entryId]
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = linkedEntry?.let { "${it.caliber} · ${it.brand}" } ?: "Entry #${snapshot.entryId}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val metaParts = buildList {
                                        add(snapshot.source)
                                        if (snapshot.vendor.isNotBlank() && snapshot.vendor != snapshot.source) {
                                            add(snapshot.vendor)
                                        }
                                        add(formatHistoryTimestamp(snapshot.recordedAt))
                                    }
                                    Text(
                                        text = metaParts.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayCpr(snapshot.pricePerRound),
                                    style = MonoTelemetryMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
