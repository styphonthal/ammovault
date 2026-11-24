package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CaliberThresholdConfigItem
import com.example.data.StarterRiflePresets
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.CaliberPill
import com.example.ui.components.formatRounds
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall

private enum class ThresholdFilterMode(val label: String) {
    ALL("All Types"),
    BELOW_MIN("Below Min"),
    MONITORED("Monitored")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThresholdConfigScreen(
    uiState: AmmoVaultUiState,
    onNavigateBack: () -> Unit,
    onSetMinThreshold: (String, Int) -> Unit,
    onApplySmartDefaults: () -> Unit,
    onRemoveThresholdOnlyCaliber: (String) -> Unit,
    onCheckAmmoSeekCaliber: (String) -> Unit,
    onInspectCaliberInCatalog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMode by remember { mutableStateOf(ThresholdFilterMode.ALL) }
    var showAddTypeDialog by remember { mutableStateOf(false) }

    val allItems = uiState.thresholdConfigItems
    val belowCount = uiState.belowMinThresholdAlerts.size
    val monitoredCount = remember(allItems) { allItems.count { it.isAlertEnabled } }
    val unconfiguredCount = remember(allItems) { allItems.count { !it.isAlertEnabled } }
    val totalDeficitRounds = remember(uiState.belowMinThresholdAlerts) {
        uiState.belowMinThresholdAlerts.sumOf { it.roundsDeficit }
    }

    val filteredItems = remember(allItems, filterMode) {
        when (filterMode) {
            ThresholdFilterMode.ALL -> allItems
            ThresholdFilterMode.BELOW_MIN -> allItems.filter { it.isBelowMinThreshold }
            ThresholdFilterMode.MONITORED -> allItems.filter { it.isAlertEnabled }
        }
    }

    if (showAddTypeDialog) {
        AddThresholdTypeDialog(
            existingCalibers = allItems.map { it.caliber }.toSet(),
            onDismiss = { showAddTypeDialog = false },
            onConfirm = { caliber, threshold ->
                onSetMinThreshold(caliber, threshold)
                showAddTypeDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.thresholds_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("thresholds_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showAddTypeDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("thresholds_add_type_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.thresholds_add_type))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ThresholdSummaryHeroCard(
                        totalTypes = allItems.size,
                        monitoredCount = monitoredCount,
                        belowMinCount = belowCount,
                        totalDeficitRounds = totalDeficitRounds,
                        unconfiguredCount = unconfiguredCount,
                        onApplySmartDefaults = onApplySmartDefaults
                    )
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterMode == ThresholdFilterMode.ALL,
                            onClick = { filterMode = ThresholdFilterMode.ALL },
                            label = { Text("All (${allItems.size})") },
                            modifier = Modifier.testTag("threshold_filter_all")
                        )
                        FilterChip(
                            selected = filterMode == ThresholdFilterMode.BELOW_MIN,
                            onClick = { filterMode = ThresholdFilterMode.BELOW_MIN },
                            label = { Text("Below Min ($belowCount)") },
                            modifier = Modifier.testTag("threshold_filter_below_min")
                        )
                        FilterChip(
                            selected = filterMode == ThresholdFilterMode.MONITORED,
                            onClick = { filterMode = ThresholdFilterMode.MONITORED },
                            label = { Text("Monitored ($monitoredCount)") },
                            modifier = Modifier.testTag("threshold_filter_monitored")
                        )
                    }
                }

                if (filteredItems.isEmpty()) {
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
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (filterMode == ThresholdFilterMode.BELOW_MIN) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = if (filterMode == ThresholdFilterMode.BELOW_MIN) {
                                        LocalAmmoStatusColors.current.healthy
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = when (filterMode) {
                                        ThresholdFilterMode.BELOW_MIN -> "No Ammunition Types Below Minimum"
                                        ThresholdFilterMode.MONITORED -> "No Minimum Thresholds Enabled Yet"
                                        ThresholdFilterMode.ALL -> "No Ammunition Types Configured"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (filterMode) {
                                        ThresholdFilterMode.BELOW_MIN -> "All monitored ammunition types are currently at or above their minimum round thresholds."
                                        ThresholdFilterMode.MONITORED -> "Set a minimum round threshold below or tap \"Apply Smart Defaults\" to activate visual alerts."
                                        ThresholdFilterMode.ALL -> "Add an ammunition type here to pre-configure low-stock thresholds even before adding inventory lots."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (filterMode == ThresholdFilterMode.ALL) {
                                    Button(
                                        onClick = { showAddTypeDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Ammunition Type")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredItems,
                        key = { "threshold_${it.caliber}" }
                    ) { item ->
                        ThresholdConfigCard(
                            item = item,
                            onSaveThreshold = { newMin -> onSetMinThreshold(item.caliber, newMin) },
                            onRemovePreconfigured = if (item.lotCount == 0) {
                                { onRemoveThresholdOnlyCaliber(item.caliber) }
                            } else {
                                null
                            },
                            onCheckAmmoSeek = { onCheckAmmoSeekCaliber(item.caliber) },
                            onInspectLots = if (item.lotCount > 0) {
                                { onInspectCaliberInCatalog(item.caliber) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThresholdSummaryHeroCard(
    totalTypes: Int,
    monitoredCount: Int,
    belowMinCount: Int,
    totalDeficitRounds: Int,
    unconfiguredCount: Int,
    onApplySmartDefaults: () -> Unit
) {
    val statusColors = LocalAmmoStatusColors.current
    val hasAlerts = belowMinCount > 0
    val accentColor = if (hasAlerts) statusColors.low else statusColors.healthy
    val badgeBg = if (hasAlerts) statusColors.lowContainer else statusColors.healthyContainer

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (hasAlerts) 1.5.dp else 1.dp,
            color = if (hasAlerts) accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("threshold_summary_hero_card")
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
                    text = "LOW-STOCK ALERT MONITOR",
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (hasAlerts) Icons.Filled.NotificationsActive else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (hasAlerts) "$belowMinCount BELOW MIN" else "ALL CLEAR",
                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.thresholds_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThresholdMetricPill(
                    label = "MONITORED",
                    value = "$monitoredCount / $totalTypes",
                    modifier = Modifier.weight(1f)
                )
                ThresholdMetricPill(
                    label = "BELOW MIN",
                    value = belowMinCount.toString(),
                    highlightColor = if (hasAlerts) accentColor else null,
                    modifier = Modifier.weight(1f)
                )
                ThresholdMetricPill(
                    label = "DEFICIT",
                    value = if (totalDeficitRounds > 0) "${formatRounds(totalDeficitRounds)} rds" else "0 rds",
                    highlightColor = if (totalDeficitRounds > 0) accentColor else null,
                    modifier = Modifier.weight(1f)
                )
            }

            if (unconfiguredCount > 0) {
                OutlinedButton(
                    onClick = onApplySmartDefaults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("thresholds_apply_defaults_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Smart Defaults ($unconfiguredCount unconfigured)")
                }
            }
        }
    }
}

@Composable
private fun ThresholdMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightColor: androidx.compose.ui.graphics.Color? = null
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThresholdConfigCard(
    item: CaliberThresholdConfigItem,
    onSaveThreshold: (Int) -> Unit,
    onRemovePreconfigured: (() -> Unit)?,
    onCheckAmmoSeek: () -> Unit,
    onInspectLots: (() -> Unit)?
) {
    val statusColors = LocalAmmoStatusColors.current
    var thresholdInput by remember(item.caliber, item.minThresholdRounds) {
        mutableStateOf(item.minThresholdRounds.toString())
    }
    val parsedInput = thresholdInput.toIntOrNull()
    val isValid = parsedInput != null && parsedInput in 0..100_000
    val isDirty = parsedInput != null && parsedInput != item.minThresholdRounds

    val isDepletedAlert = item.isBelowMinThreshold && item.currentRounds <= 0
    val statusFg = when {
        isDepletedAlert -> statusColors.critical
        item.isBelowMinThreshold -> statusColors.low
        item.isAlertEnabled -> statusColors.healthy
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusBg = when {
        isDepletedAlert -> statusColors.criticalContainer
        item.isBelowMinThreshold -> statusColors.lowContainer
        item.isAlertEnabled -> statusColors.healthyContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val statusLabel = when {
        item.isBelowMinThreshold -> "BELOW MIN (-${formatRounds(item.roundsDeficit)} rds)"
        item.isAlertEnabled -> "ABOVE MIN"
        else -> "ALERT OFF"
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (item.isBelowMinThreshold) 1.5.dp else 1.dp,
            color = if (item.isBelowMinThreshold) statusFg.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("threshold_card_${item.caliber}")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CaliberPill(caliber = item.caliber)
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (item.isBelowMinThreshold) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = statusFg,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = statusLabel,
                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                color = statusFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (onRemovePreconfigured != null) {
                    IconButton(
                        onClick = onRemovePreconfigured,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("threshold_remove_${item.caliber}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove threshold configuration for ${item.caliber}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = "${formatRounds(item.currentRounds)} rds on hand",
                        style = MonoTelemetryLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.lotCount} lot${if (item.lotCount == 1) "" else "s"} · Par target: ${formatRounds(item.targetRounds)} rds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.isAlertEnabled) {
                            "${formatRounds(item.minThresholdRounds)} rds"
                        } else {
                            "Off"
                        },
                        style = MonoTelemetryMedium,
                        color = if (item.isBelowMinThreshold) statusFg else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Min Threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ThresholdGaugeBar(
                currentRounds = item.currentRounds,
                minThresholdRounds = item.minThresholdRounds,
                targetRounds = item.targetRounds,
                isBelowMin = item.isBelowMinThreshold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { text ->
                        if (text.length <= 6) {
                            thresholdInput = text.filter(Char::isDigit)
                        }
                    },
                    label = { Text("Minimum Threshold") },
                    suffix = { Text("rds") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = thresholdInput.isNotEmpty() && !isValid,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("threshold_input_${item.caliber}")
                )

                Button(
                    onClick = {
                        if (isValid && parsedInput != null) {
                            onSaveThreshold(parsedInput)
                        }
                    },
                    enabled = isValid && isDirty,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("threshold_save_${item.caliber}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(-50, -10, 10, 50).forEach { delta ->
                    val label = if (delta > 0) "+$delta" else "$delta"
                    val nextValue = (item.minThresholdRounds + delta).coerceAtLeast(0)
                    OutlinedButton(
                        onClick = {
                            thresholdInput = nextValue.toString()
                            onSaveThreshold(nextValue)
                        },
                        enabled = !(delta < 0 && item.minThresholdRounds == 0),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("threshold_step_${item.caliber}_$delta")
                    ) {
                        Text(text = label, style = MonoTelemetrySmall, maxLines = 1)
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0, 100, 250, 500, 1000).forEach { preset ->
                    FilterChip(
                        selected = item.minThresholdRounds == preset,
                        onClick = {
                            thresholdInput = preset.toString()
                            onSaveThreshold(preset)
                        },
                        label = {
                            Text(
                                text = if (preset == 0) "Off" else formatRounds(preset),
                                style = MonoTelemetrySmall
                            )
                        },
                        modifier = Modifier.testTag("threshold_preset_${item.caliber}_$preset")
                    )
                }
            }

            if (item.isBelowMinThreshold || onInspectLots != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onInspectLots != null) {
                        TextButton(
                            onClick = onInspectLots,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("View ${item.lotCount} Lot${if (item.lotCount == 1) "" else "s"}")
                        }
                    } else {
                        Text(
                            text = "Pre-configured (no lots yet)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.isBelowMinThreshold) {
                        OutlinedButton(
                            onClick = onCheckAmmoSeek,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("threshold_ammoseek_${item.caliber}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restock on AmmoSeek")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThresholdGaugeBar(
    currentRounds: Int,
    minThresholdRounds: Int,
    targetRounds: Int,
    isBelowMin: Boolean
) {
    val statusColors = LocalAmmoStatusColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val markerColor = MaterialTheme.colorScheme.onSurface
    val fillColor = when {
        isBelowMin && currentRounds <= 0 -> statusColors.critical
        isBelowMin -> statusColors.low
        minThresholdRounds > 0 -> statusColors.healthy
        else -> MaterialTheme.colorScheme.primary
    }

    val maxScale = maxOf(
        targetRounds,
        minThresholdRounds * 2,
        currentRounds,
        100
    ).toFloat()
    val stockRatio = (currentRounds.toFloat() / maxScale).coerceIn(0f, 1f)
    val minRatio = if (minThresholdRounds > 0) {
        (minThresholdRounds.toFloat() / maxScale).coerceIn(0.02f, 0.98f)
    } else {
        null
    }

    val animatedStock by animateFloatAsState(
        targetValue = stockRatio,
        animationSpec = tween(durationMillis = 320),
        label = "threshold_gauge_fill"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
            if (animatedStock > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset.Zero,
                    size = Size(width = size.width * animatedStock, height = size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
            if (minRatio != null) {
                val x = size.width * minRatio
                drawLine(
                    color = markerColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0 rds",
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (minThresholdRounds > 0) {
                Text(
                    text = "| Min: ${formatRounds(minThresholdRounds)} rds",
                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isBelowMin) fillColor else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${formatRounds(maxScale.toInt())} rds",
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddThresholdTypeDialog(
    existingCalibers: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val suggestedPresets = remember(existingCalibers) {
        StarterRiflePresets.commonCalibers.filter { preset ->
            existingCalibers.none { it.equals(preset, ignoreCase = true) }
        }
    }
    var caliberInput by remember { mutableStateOf("") }
    var thresholdInput by remember { mutableStateOf("200") }

    val trimmedCaliber = caliberInput.trim()
    val parsedThreshold = thresholdInput.toIntOrNull()
    val isValid = trimmedCaliber.isNotEmpty() && parsedThreshold != null && parsedThreshold in 1..100_000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Ammunition Threshold",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pre-configure a minimum round threshold for any cartridge or ammunition type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = caliberInput,
                    onValueChange = { caliberInput = it },
                    label = { Text("Ammunition Type / Caliber") },
                    placeholder = { Text("e.g., 5.56 NATO, .308 Win, 9mm") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_threshold_caliber_input")
                )

                if (suggestedPresets.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestedPresets.take(8).forEach { preset ->
                            FilterChip(
                                selected = caliberInput.equals(preset, ignoreCase = true),
                                onClick = {
                                    caliberInput = preset
                                    thresholdInput = StarterRiflePresets
                                        .defaultMinThresholdForCaliber(preset)
                                        .toString()
                                },
                                label = { Text(preset, style = MonoTelemetrySmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { text ->
                        if (text.length <= 6) {
                            thresholdInput = text.filter(Char::isDigit)
                        }
                    },
                    label = { Text("Minimum Threshold") },
                    suffix = { Text("rds") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = thresholdInput.isNotEmpty() && (parsedThreshold == null || parsedThreshold !in 1..100_000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_threshold_value_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && parsedThreshold != null) {
                        onConfirm(trimmedCaliber, parsedThreshold)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("add_threshold_confirm_button")
            ) {
                Text("Add Threshold")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
