package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AmmoEntry
import com.example.data.RangeBagItemWithEntry
import com.example.data.RangePresets
import com.example.data.RangeSession
import com.example.data.RangeSessionAmmoUsage
import com.example.data.RangeSessionWithUsages
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.CaliberBurnForecastChart
import com.example.ui.components.CaliberPill
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.SpecBadge
import com.example.ui.components.displayCurrency
import com.example.ui.components.formatDurationMinutes
import com.example.ui.components.formatRounds
import com.example.ui.components.formatSessionDateTime
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.util.Locale

@Composable
fun RangeLogsScreen(
    uiState: AmmoVaultUiState,
    onSelectRangeBag: (String) -> Unit,
    onPackLotInBag: (bagName: String, entry: AmmoEntry, packedRounds: Int) -> Unit,
    onPackStorageUnitInBag: (bagName: String, storageUnitName: String) -> Unit = { _, _ -> },
    onRemoveBagItem: (Long) -> Unit,
    onClearBag: (String) -> Unit,
    onLogRangeSession: (RangeSession, List<RangeSessionAmmoUsage>, Boolean) -> Unit,
    onDeleteRangeSession: (sessionId: Long, restoreInventory: Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    initialStorageUnitForLog: String? = null,
    onConsumeInitialStorageUnit: () -> Unit = {},
    onOpenStorageDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showPackDialog by remember { mutableStateOf(false) }
    var editingBagItem by remember { mutableStateOf<RangeBagItemWithEntry?>(null) }
    var showLogSessionDialog by remember { mutableStateOf(false) }
    var logFromActiveBag by remember { mutableStateOf(false) }
    var logInitialStorageUnit by remember { mutableStateOf<String?>(null) }
    var deletingSession by remember { mutableStateOf<RangeSessionWithUsages?>(null) }
    var viewingPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedSessionTypeFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialStorageUnitForLog) {
        if (!initialStorageUnitForLog.isNullOrBlank()) {
            logFromActiveBag = false
            logInitialStorageUnit = initialStorageUnitForLog
            showLogSessionDialog = true
            onConsumeInitialStorageUnit()
        }
    }

    if (viewingPhoto != null) {
        FullScreenPhotoDialog(
            photoUri = viewingPhoto!!.first,
            title = viewingPhoto!!.second,
            onDismiss = { viewingPhoto = null }
        )
    }

    if (showPackDialog || editingBagItem != null) {
        PackRangeBagDialog(
            allEntries = uiState.allEntries,
            bagNames = uiState.rangeBagNames,
            initialBagName = editingBagItem?.bagItem?.bagName ?: uiState.selectedRangeBagName,
            initialEntry = editingBagItem?.entry,
            initialPackedRounds = editingBagItem?.bagItem?.packedRounds,
            fifoOldestLotIds = uiState.fifoOldestLotIds,
            storageUnits = uiState.allStorageUnitNames,
            onPackEntireStorageUnit = { bagName, unitName ->
                onPackStorageUnitInBag(bagName, unitName)
                showPackDialog = false
                editingBagItem = null
            },
            onDismiss = {
                showPackDialog = false
                editingBagItem = null
            },
            onConfirmPack = { bagName, entry, rounds ->
                onPackLotInBag(bagName, entry, rounds)
                showPackDialog = false
                editingBagItem = null
            }
        )
    }

    if (showLogSessionDialog) {
        LogRangeSessionDialog(
            allEntries = uiState.allEntries,
            sourceBagName = if (logFromActiveBag) uiState.selectedRangeBagName else null,
            prefilledBagItems = if (logFromActiveBag) uiState.selectedRangeBagItems else emptyList(),
            storageUnits = uiState.allStorageUnitNames,
            initialStorageUnit = logInitialStorageUnit,
            onDismiss = {
                showLogSessionDialog = false
                logInitialStorageUnit = null
            },
            onSaveSession = { session, usages, clearBag ->
                onLogRangeSession(session, usages, clearBag)
                showLogSessionDialog = false
                logInitialStorageUnit = null
            }
        )
    }

    if (deletingSession != null) {
        val target = deletingSession!!
        AlertDialog(
            onDismissRequest = { deletingSession = null },
            title = { Text("Delete Range Log?") },
            text = {
                Text(
                    "Remove the ${target.session.location} session (${formatRounds(target.totalRoundsFired)} rounds fired)? You can also restore those fired rounds back to their linked inventory lots."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRangeSession(target.session.id, true)
                        deletingSession = null
                    },
                    modifier = Modifier.testTag("delete_session_restore_button")
                ) {
                    Text("Delete & Restore Rounds")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { deletingSession = null }
                    ) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            onDeleteRangeSession(target.session.id, false)
                            deletingSession = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_session_only_button")
                    ) { Text("Delete Only") }
                }
            }
        )
    }

    val packedTotalRounds = remember(uiState.selectedRangeBagItems) {
        uiState.selectedRangeBagItems.sumOf { it.bagItem.packedRounds }
    }
    val packedTotalValue = remember(uiState.selectedRangeBagItems) {
        uiState.selectedRangeBagItems.sumOf { it.estimatedValue }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Virtual Range Bag Section
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("range_bag_card")
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "VIRTUAL RANGE BAGS",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = uiState.selectedRangeBagName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (uiState.selectedRangeBagItems.isNotEmpty()) {
                            TextButton(
                                onClick = { onClearBag(uiState.selectedRangeBagName) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_range_bag_button")
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.rangeBagNames.forEach { bagName ->
                            val countInBag = uiState.allRangeBagItems
                                .filter { it.bagItem.bagName.equals(bagName, ignoreCase = true) }
                                .sumOf { it.bagItem.packedRounds }
                            val labelText = if (countInBag > 0) "$bagName ($countInBag)" else bagName
                            FilterChip(
                                selected = uiState.selectedRangeBagName.equals(bagName, ignoreCase = true),
                                onClick = { onSelectRangeBag(bagName) },
                                label = { Text(labelText) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    if (uiState.selectedRangeBagItems.isEmpty()) {
                        Text(
                            text = "No lots packed in ${uiState.selectedRangeBagName}. Pack specific ammo lots & round counts before heading to the range, then log the entire trip in one tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.selectedRangeBagItems.forEach { item ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    onClick = { onEntryClick(item.entry.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("packed_bag_item_${item.bagItem.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CaliberPill(caliber = item.entry.caliber)
                                                Text(
                                                    text = "${item.entry.grain} gr ${item.entry.bulletType}",
                                                    style = MonoTelemetrySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${item.entry.brand} ${item.entry.productLine}".trim(),
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val lotLabel = if (item.entry.lotNumber.isNotBlank()) "Lot ${item.entry.lotNumber} · " else ""
                                            Text(
                                                text = "${lotLabel}${formatRounds(item.bagItem.packedRounds)} packed / ${formatRounds(item.entry.currentCount)} on hand",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingBagItem = item },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit packed amount",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRemoveBagItem(item.bagItem.id) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Remove from bag",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bag Total: ${formatRounds(packedTotalRounds)} rounds (${uiState.selectedRangeBagItems.size} ${if (uiState.selectedRangeBagItems.size == 1) "lot" else "lots"})",
                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = displayCurrency(packedTotalValue),
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPackDialog = true },
                            enabled = uiState.allEntries.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("pack_lot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pack Lot")
                        }
                        Button(
                            onClick = {
                                logFromActiveBag = uiState.selectedRangeBagItems.isNotEmpty()
                                logInitialStorageUnit = null
                                showLogSessionDialog = true
                            },
                            enabled = uiState.allEntries.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .heightIn(min = 48.dp)
                                .testTag("log_from_bag_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.selectedRangeBagItems.isNotEmpty()) "Log Bag Trip" else "Log Session")
                        }
                    }

                    val namedStorageUnits = remember(uiState.storageSummaries) {
                        uiState.storageSummaries.filterNot { it.isUnassigned }
                    }
                    if (namedStorageUnits.isNotEmpty() || onOpenStorageDashboard != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "LOG FROM STORAGE UNIT / AMMO CAN",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (onOpenStorageDashboard != null) {
                                TextButton(
                                    onClick = onOpenStorageDashboard,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("range_manage_storage_units_button")
                                ) {
                                    Text("Manage")
                                }
                            }
                        }
                        if (namedStorageUnits.isEmpty()) {
                            Text(
                                text = "Create named ammo cans or storage shelves to grab them directly for a range day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                namedStorageUnits.forEach { container ->
                                    val label = if (container.totalRounds > 0) {
                                        "${container.locationName} (${formatRounds(container.totalRounds)})"
                                    } else {
                                        "${container.locationName} (Empty)"
                                    }
                                    FilterChip(
                                        selected = false,
                                        enabled = container.totalRounds > 0,
                                        onClick = {
                                            logFromActiveBag = false
                                            logInitialStorageUnit = container.locationName
                                            showLogSessionDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Place,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("range_quick_storage_chip_${container.locationName}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Burn-Rate & Depletion Forecast Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("burn_rate_analytics_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BURN-RATE & DEPLETION ANALYTICS",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RangeMetricMiniBox(
                            label = "30-DAY BURN",
                            value = "${formatRounds(uiState.totalRoundsFiredLast30Days)} rds",
                            modifier = Modifier.weight(1f)
                        )
                        RangeMetricMiniBox(
                            label = "ALL-TIME FIRED",
                            value = "${formatRounds(uiState.totalRoundsFiredAllTime)} rds",
                            modifier = Modifier.weight(1f)
                        )
                        RangeMetricMiniBox(
                            label = "RANGE TIME",
                            value = formatDurationMinutes(uiState.totalRangeMinutesAllTime),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.caliberBurnForecasts.isEmpty()) {
                        Text(
                            text = "Log your first range session to unlock monthly consumption rates, ammo spend burned, and estimated months of supply left per caliber.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CaliberBurnForecastChart(forecasts = uiState.caliberBurnForecasts)
                    }
                }
            }
        }

        // 3. Range Logs Header + Session Type Filter Chips
        val filteredSessions = if (selectedSessionTypeFilter == null) {
            uiState.rangeSessions
        } else {
            uiState.rangeSessions.filter {
                it.session.sessionType.equals(selectedSessionTypeFilter, ignoreCase = true)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Range Logs (${filteredSessions.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Every logged trip automatically deducts rounds from the specific lots used.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            logFromActiveBag = false
                            logInitialStorageUnit = null
                            showLogSessionDialog = true
                        },
                        enabled = uiState.allEntries.isNotEmpty(),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("log_new_session_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Trip")
                    }
                }

                if (uiState.rangeSessions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSessionTypeFilter == null,
                            onClick = { selectedSessionTypeFilter = null },
                            label = { Text("All (${uiState.rangeSessions.size})") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        RangePresets.commonSessionTypes.forEach { type ->
                            FilterChip(
                                selected = selectedSessionTypeFilter.equals(type, ignoreCase = true),
                                onClick = {
                                    selectedSessionTypeFilter = if (selectedSessionTypeFilter == type) null else type
                                },
                                label = { Text(type) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }
        }

        if (filteredSessions.isEmpty()) {
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (selectedSessionTypeFilter != null) {
                                "No $selectedSessionTypeFilter Sessions Yet"
                            } else {
                                "No Range Sessions Logged Yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record date, time, location, session type, distance, MOA, target photos, and exact ammo lots fired.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = filteredSessions, key = { it.session.id }) { sessionWithUsages ->
                RangeSessionLogCard(
                    item = sessionWithUsages,
                    onPhotoClick = { uri ->
                        viewingPhoto = uri to sessionWithUsages.session.location
                    },
                    onDeleteClick = { deletingSession = sessionWithUsages }
                )
            }
        }
    }
}

@Composable
private fun RangeMetricMiniBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeSessionLogCard(
    item: RangeSessionWithUsages,
    onPhotoClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val session = item.session
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("range_log_card_${session.id}")
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
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = session.location.ifBlank { "Range Session" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatSessionDateTime(session.sessionDateTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatDurationMinutes(session.durationMinutes),
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = session.sessionType.ifBlank { "Practice" }.uppercase(Locale.US),
                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (session.rangeBagName.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = session.rangeBagName,
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (session.storageUnitName.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = session.storageUnitName,
                                        style = MonoTelemetrySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                    val precisionParts = buildList {
                        session.distanceYards?.let { add("${it} yd") }
                        session.bestGroupMoa?.let { add(String.format(Locale.US, "%.2f MOA", it)) }
                        session.avgVelocityFps?.let { add("$it fps") }
                    }
                    if (precisionParts.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            precisionParts.forEach { metric ->
                                SpecBadge(text = metric)
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_range_log_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete range log",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val statusColors = LocalAmmoStatusColors.current
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.usages.forEach { usage ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CaliberPill(caliber = usage.caliber)
                                Text(
                                    text = "${usage.brand} · ${usage.grain} gr ${usage.bulletType}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (usage.lotNumber.isNotBlank()) {
                                    Text(
                                        text = "Lot ${usage.lotNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (usage.malfunctionsCount > 0) {
                                    Text(
                                        text = "· ${usage.malfunctionsCount} malf",
                                        style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColors.low
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-${formatRounds(usage.roundsUsed)} rds",
                                style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = displayCurrency(usage.totalAmmoCost),
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (session.notes.isNotBlank()) {
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.photoUri.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onPhotoClick(session.photoUri) }
                        .testTag("range_log_photo_${session.id}")
                ) {
                    AsyncImage(
                        model = session.photoUri,
                        contentDescription = "Range session photo for ${session.location}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Fired: ${formatRounds(item.totalRoundsFired)} rds",
                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ammo Burned: ${displayCurrency(item.totalAmmoCost)}",
                    style = MonoTelemetryLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
