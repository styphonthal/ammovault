package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AmmoEntry
import com.example.data.RangeBagItemWithEntry
import com.example.data.RangePresets
import com.example.data.RangeSession
import com.example.data.RangeSessionAmmoUsage
import com.example.ui.components.CaliberPill
import com.example.ui.components.FifoShootFirstBadge
import com.example.ui.components.PhotoAttachmentPickerCard
import com.example.ui.components.displayCurrency
import com.example.ui.components.formatDurationMinutes
import com.example.ui.components.formatRounds
import com.example.ui.components.formatSessionDateOnly
import com.example.ui.components.formatSessionTimeOnly
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.util.Calendar
import java.util.TimeZone

@Composable
fun PackRangeBagDialog(
    allEntries: List<AmmoEntry>,
    bagNames: List<String>,
    initialBagName: String,
    initialEntry: AmmoEntry? = null,
    initialPackedRounds: Int? = null,
    fifoOldestLotIds: Set<Long> = emptySet(),
    storageUnits: List<String> = emptyList(),
    onPackEntireStorageUnit: ((bagName: String, storageUnitName: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirmPack: (bagName: String, entry: AmmoEntry, packedRounds: Int) -> Unit
) {
    var selectedBagName by remember(initialBagName) { mutableStateOf(initialBagName) }
    var selectedStorageFilter by remember(initialEntry) { mutableStateOf<String?>(null) }
    val visibleEntries = remember(allEntries, selectedStorageFilter) {
        if (selectedStorageFilter.isNullOrBlank()) {
            allEntries
        } else {
            allEntries.filter { it.storageLocation.trim().equals(selectedStorageFilter, ignoreCase = true) }
        }
    }
    var selectedEntryId by remember(initialEntry, allEntries) {
        mutableLongStateOf(initialEntry?.id ?: allEntries.firstOrNull()?.id ?: 0L)
    }
    val selectedEntry = remember(selectedEntryId, visibleEntries, allEntries) {
        visibleEntries.find { it.id == selectedEntryId } ?: allEntries.find { it.id == selectedEntryId }
    }
    var roundsText by remember(selectedEntryId, initialPackedRounds) {
        val defaultCount = initialPackedRounds
            ?: selectedEntry?.let { minOf(it.currentCount, (it.boxSize * 2).coerceAtLeast(20)) }
            ?: 20
        mutableStateOf(defaultCount.coerceAtLeast(1).toString())
    }

    val parsedRounds = roundsText.toIntOrNull() ?: 0
    val statusColors = LocalAmmoStatusColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pack Ammo in Range Bag") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Range Bag",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bagNames.forEach { bag ->
                        FilterChip(
                            selected = selectedBagName.equals(bag, ignoreCase = true),
                            onClick = { selectedBagName = bag },
                            label = { Text(bag) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = selectedBagName,
                    onValueChange = { selectedBagName = it },
                    label = { Text("Range Bag Name") },
                    placeholder = { Text("e.g., Primary Range Bag, Match Pack") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_bag_name_input")
                )

                if (allEntries.isEmpty()) {
                    Text(
                        text = "Add ammo lots to your catalog first before packing a range bag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (storageUnits.isNotEmpty()) {
                        Text(
                            text = "Filter by Storage Unit / Ammo Can",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedStorageFilter == null,
                                onClick = { selectedStorageFilter = null },
                                label = { Text("All Units") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                            storageUnits.forEach { unit ->
                                val unitRounds = allEntries
                                    .filter { it.storageLocation.trim().equals(unit, ignoreCase = true) }
                                    .sumOf { it.currentCount }
                                FilterChip(
                                    selected = selectedStorageFilter.equals(unit, ignoreCase = true),
                                    onClick = {
                                        selectedStorageFilter = if (selectedStorageFilter.equals(unit, ignoreCase = true)) {
                                            null
                                        } else {
                                            unit
                                        }
                                        val filteredNow = if (selectedStorageFilter == null) {
                                            allEntries
                                        } else {
                                            allEntries.filter {
                                                it.storageLocation.trim().equals(selectedStorageFilter, ignoreCase = true)
                                            }
                                        }
                                        if (filteredNow.isNotEmpty() && filteredNow.none { it.id == selectedEntryId }) {
                                            val first = filteredNow.first()
                                            selectedEntryId = first.id
                                            val suggested = minOf(first.currentCount, (first.boxSize * 2).coerceAtLeast(20))
                                            roundsText = suggested.coerceAtLeast(1).toString()
                                        }
                                    },
                                    label = { Text(if (unitRounds > 0) "$unit ($unitRounds)" else unit) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }

                        val activeUnit = selectedStorageFilter
                        if (activeUnit != null && onPackEntireStorageUnit != null) {
                            val inStockUnitLots = visibleEntries.filter { it.currentCount > 0 }
                            if (inStockUnitLots.isNotEmpty()) {
                                val totalUnitRounds = inStockUnitLots.sumOf { it.currentCount }
                                OutlinedButton(
                                    onClick = {
                                        if (selectedBagName.trim().isNotEmpty()) {
                                            onPackEntireStorageUnit(selectedBagName.trim(), activeUnit)
                                        }
                                    },
                                    enabled = selectedBagName.trim().isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("pack_entire_storage_unit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pack All ${inStockUnitLots.size} Lot(s) (${formatRounds(totalUnitRounds)} rds)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Select Specific Lot",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (visibleEntries.isEmpty()) {
                        Text(
                            text = "No lots are currently assigned to \"${selectedStorageFilter.orEmpty()}\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            visibleEntries.forEach { entry ->
                                val isSelected = entry.id == selectedEntryId
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    onClick = {
                                        selectedEntryId = entry.id
                                        val suggested = minOf(entry.currentCount, (entry.boxSize * 2).coerceAtLeast(20))
                                        roundsText = suggested.coerceAtLeast(1).toString()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pack_lot_option_${entry.id}")
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
                                            Text(
                                                text = "${entry.caliber} · ${entry.brand} ${entry.productLine}".trim(),
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val lotSuffix = if (entry.lotNumber.isNotBlank()) " · Lot ${entry.lotNumber}" else ""
                                            val storageSuffix = if (entry.storageLocation.isNotBlank()) " · ${entry.storageLocation}" else ""
                                            Text(
                                                text = "${entry.grain} gr ${entry.bulletType}$lotSuffix$storageSuffix",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (entry.id in fifoOldestLotIds) {
                                                FifoShootFirstBadge()
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${formatRounds(entry.currentCount)} rds",
                                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedEntry != null) {
                        OutlinedTextField(
                            value = roundsText,
                            onValueChange = { text ->
                                val digits = text.filter(Char::isDigit)
                                if (digits.length <= 5) roundsText = digits
                            },
                            label = { Text("Rounds to Pack") },
                            suffix = { Text("rds") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pack_rounds_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { roundsText = (parsedRounds + selectedEntry.boxSize).toString() },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("+1 Box (${selectedEntry.boxSize})") }
                            OutlinedButton(
                                onClick = { roundsText = (parsedRounds + 50).toString() },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("+50") }
                            if (selectedEntry.currentCount > 0) {
                                OutlinedButton(
                                    onClick = { roundsText = selectedEntry.currentCount.toString() },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text("All (${selectedEntry.currentCount})") }
                            }
                        }

                        if (parsedRounds > selectedEntry.currentCount) {
                            Text(
                                text = "Note: Exceeds ${selectedEntry.currentCount} rounds currently on hand in this lot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColors.low
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entry = selectedEntry ?: return@Button
                    if (selectedBagName.trim().isNotEmpty() && parsedRounds > 0) {
                        onConfirmPack(selectedBagName.trim(), entry, parsedRounds)
                    }
                },
                enabled = selectedEntry != null && selectedBagName.trim().isNotEmpty() && parsedRounds > 0,
                modifier = Modifier.testTag("confirm_pack_range_bag_button")
            ) {
                Text("Pack Lot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogRangeSessionDialog(
    allEntries: List<AmmoEntry>,
    sourceBagName: String?,
    prefilledBagItems: List<RangeBagItemWithEntry>,
    storageUnits: List<String> = emptyList(),
    initialStorageUnit: String? = null,
    onDismiss: () -> Unit,
    onSaveSession: (session: RangeSession, usages: List<RangeSessionAmmoUsage>, clearSourceBag: Boolean) -> Unit
) {
    val initialCal = remember { Calendar.getInstance() }
    var sessionYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var sessionMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var sessionDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var sessionHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var sessionMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAddLotSheet by remember { mutableStateOf(false) }

    var location by remember { mutableStateOf(RangePresets.commonRangeLocations.first()) }
    var sessionType by remember { mutableStateOf(RangePresets.commonSessionTypes.first()) }
    var selectedStorageUnit by remember(initialStorageUnit) { mutableStateOf(initialStorageUnit.orEmpty()) }
    var durationText by remember { mutableStateOf("60") }
    var distanceText by remember { mutableStateOf("") }
    var bestGroupMoaText by remember { mutableStateOf("") }
    var avgVelocityText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    var clearSourceBag by remember(sourceBagName) { mutableStateOf(!sourceBagName.isNullOrBlank()) }

    val selectedLotRounds = remember(prefilledBagItems, initialStorageUnit) {
        mutableStateMapOf<Long, String>().apply {
            if (prefilledBagItems.isNotEmpty()) {
                prefilledBagItems.forEach { item ->
                    put(item.entry.id, item.bagItem.packedRounds.coerceAtLeast(1).toString())
                }
            } else if (!initialStorageUnit.isNullOrBlank()) {
                val matchingUnitLots = allEntries.filter {
                    it.storageLocation.trim().equals(initialStorageUnit.trim(), ignoreCase = true)
                }
                val inStockLots = matchingUnitLots.filter { it.currentCount > 0 }.ifEmpty { matchingUnitLots }
                inStockLots.forEach { lot ->
                    val defaultRds = minOf(lot.currentCount.coerceAtLeast(1), (lot.boxSize * 2).coerceAtLeast(20))
                    put(lot.id, defaultRds.toString())
                }
            } else {
                allEntries.firstOrNull()?.let { first ->
                    put(first.id, minOf(first.currentCount.coerceAtLeast(1), first.boxSize.coerceAtLeast(20)).toString())
                }
            }
        }
    }
    val selectedLotMalfunctions = remember { mutableStateMapOf<Long, Int>() }

    val sessionMillis = remember(sessionYear, sessionMonth, sessionDay, sessionHour, sessionMinute) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, sessionYear)
            set(Calendar.MONTH, sessionMonth)
            set(Calendar.DAY_OF_MONTH, sessionDay)
            set(Calendar.HOUR_OF_DAY, sessionHour)
            set(Calendar.MINUTE, sessionMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    if (showDatePicker) {
        val utcInit = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(sessionYear, sessionMonth, sessionDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcInit)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtc ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = pickedUtc
                            }
                            sessionYear = utcCal.get(Calendar.YEAR)
                            sessionMonth = utcCal.get(Calendar.MONTH)
                            sessionDay = utcCal.get(Calendar.DAY_OF_MONTH)
                        }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = sessionHour,
            initialMinute = sessionMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Session Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionHour = timePickerState.hour
                        sessionMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    val availableToAdd = remember(allEntries, selectedLotRounds.keys.toSet()) {
        allEntries.filter { it.id !in selectedLotRounds.keys }
    }

    if (showAddLotSheet) {
        var addLotStorageFilter by remember {
            mutableStateOf<String?>(selectedStorageUnit.takeIf { it.isNotBlank() })
        }
        val filteredAvailable = remember(availableToAdd, addLotStorageFilter) {
            if (addLotStorageFilter.isNullOrBlank()) {
                availableToAdd
            } else {
                availableToAdd.filter {
                    it.storageLocation.trim().equals(addLotStorageFilter, ignoreCase = true)
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showAddLotSheet = false },
            title = { Text("Add Ammo Lot to Session") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (storageUnits.isNotEmpty() && availableToAdd.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = addLotStorageFilter == null,
                                onClick = { addLotStorageFilter = null },
                                label = { Text("All") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                            storageUnits.forEach { unit ->
                                FilterChip(
                                    selected = addLotStorageFilter.equals(unit, ignoreCase = true),
                                    onClick = {
                                        addLotStorageFilter = if (addLotStorageFilter.equals(unit, ignoreCase = true)) {
                                            null
                                        } else {
                                            unit
                                        }
                                    },
                                    label = { Text(unit) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }

                    if (filteredAvailable.isEmpty()) {
                        Text(
                            text = if (availableToAdd.isEmpty()) {
                                "All catalog lots are already included in this session."
                            } else {
                                "No remaining lots in \"${addLotStorageFilter.orEmpty()}\"."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredAvailable.forEach { entry ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                onClick = {
                                    val defaultRds = minOf(entry.currentCount.coerceAtLeast(1), entry.boxSize.coerceAtLeast(20))
                                    selectedLotRounds[entry.id] = defaultRds.toString()
                                    showAddLotSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.caliber} · ${entry.brand} ${entry.productLine}".trim(),
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val unitLabel = if (entry.storageLocation.isNotBlank()) " · ${entry.storageLocation}" else ""
                                        Text(
                                            text = "${entry.grain} gr ${entry.bulletType}$unitLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${formatRounds(entry.currentCount)} rds",
                                        style = MonoTelemetrySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddLotSheet = false }) { Text("Close") }
            }
        )
    }

    val entriesById = remember(allEntries) { allEntries.associateBy { it.id } }
    val draftUsages = selectedLotRounds.entries.mapNotNull { (entryId, rdsText) ->
        val entry = entriesById[entryId] ?: return@mapNotNull null
        val rds = rdsText.toIntOrNull() ?: 0
        if (rds <= 0) return@mapNotNull null
        RangeSessionAmmoUsage(
            entryId = entry.id,
            caliber = entry.caliber,
            brand = entry.brand,
            grain = entry.grain,
            bulletType = entry.bulletType,
            lotNumber = entry.lotNumber,
            roundsUsed = rds,
            costPerRoundSnapshot = entry.effectivePurchaseCpr,
            malfunctionsCount = (selectedLotMalfunctions[entryId] ?: 0).coerceAtLeast(0)
        )
    }
    val totalSessionRounds = draftUsages.sumOf { it.roundsUsed }
    val totalSessionCost = draftUsages.sumOf { it.totalAmmoCost }
    val parsedDuration = (durationText.toIntOrNull() ?: 60).coerceIn(5, 1440)
    val parsedDistance = distanceText.toIntOrNull()?.takeIf { it > 0 }
    val parsedMoa = bestGroupMoaText.toDoubleOrNull()?.takeIf { it > 0.0 }
    val parsedVelocity = avgVelocityText.toIntOrNull()?.takeIf { it > 0 }
    val canSubmit = draftUsages.isNotEmpty() && location.trim().isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Range Session",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!sourceBagName.isNullOrBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WorkOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Loaded from $sourceBagName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (selectedStorageUnit.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Storage Unit: $selectedStorageUnit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date & Time Row
                    Text(
                        text = "DATE & TIME",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .heightIn(min = 48.dp)
                                .testTag("range_session_date_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatSessionDateOnly(sessionMillis),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(0.85f)
                                .heightIn(min = 48.dp)
                                .testTag("range_session_time_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(formatSessionTimeOnly(sessionMillis))
                        }
                    }

                    // Location
                    Text(
                        text = "RANGE LOCATION",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RangePresets.commonRangeLocations.forEach { preset ->
                            FilterChip(
                                selected = location.equals(preset, ignoreCase = true),
                                onClick = { location = preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Range Name *") },
                        placeholder = { Text("e.g., Oak Ridge 300yd Range") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("range_session_location_input")
                    )

                    // Session Type Tags
                    Text(
                        text = "SESSION TYPE",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RangePresets.commonSessionTypes.forEach { type ->
                            FilterChip(
                                selected = sessionType.equals(type, ignoreCase = true),
                                onClick = { sessionType = type },
                                label = { Text(type) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("session_type_chip_$type")
                            )
                        }
                    }

                    // Duration
                    Text(
                        text = "SESSION LENGTH",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RangePresets.commonDurationsMinutes.forEach { mins ->
                            FilterChip(
                                selected = durationText == mins.toString(),
                                onClick = { durationText = mins.toString() },
                                label = { Text(formatDurationMinutes(mins)) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { text ->
                            val digits = text.filter(Char::isDigit)
                            if (digits.length <= 4) durationText = digits
                        },
                        label = { Text("Duration (Minutes)") },
                        suffix = { Text("min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("range_session_duration_input")
                    )

                    // Optional Rifle Precision Telemetry
                    Text(
                        text = "PRECISION METRICS (OPTIONAL)",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = { text ->
                                val digits = text.filter(Char::isDigit)
                                if (digits.length <= 4) distanceText = digits
                            },
                            label = { Text("Dist") },
                            placeholder = { Text("100") },
                            suffix = { Text("yd") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("range_session_distance_input")
                        )
                        OutlinedTextField(
                            value = bestGroupMoaText,
                            onValueChange = { text ->
                                val cleaned = text.filter { it.isDigit() || it == '.' }
                                if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 5) {
                                    bestGroupMoaText = cleaned
                                }
                            },
                            label = { Text("Group") },
                            placeholder = { Text("0.85") },
                            suffix = { Text("MOA") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("range_session_moa_input")
                        )
                        OutlinedTextField(
                            value = avgVelocityText,
                            onValueChange = { text ->
                                val digits = text.filter(Char::isDigit)
                                if (digits.length <= 4) avgVelocityText = digits
                            },
                            label = { Text("Vel") },
                            placeholder = { Text("2710") },
                            suffix = { Text("fps") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("range_session_fps_input")
                        )
                    }

                    // Ammo Storage Unit / Can Used for Range Day
                    if (storageUnits.isNotEmpty()) {
                        Text(
                            text = "AMMO STORAGE UNIT / CAN USED (OPTIONAL)",
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedStorageUnit.isBlank(),
                                onClick = { selectedStorageUnit = "" },
                                label = { Text("None") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("session_storage_unit_none")
                            )
                            storageUnits.forEach { unit ->
                                val unitRounds = allEntries
                                    .filter { it.storageLocation.trim().equals(unit, ignoreCase = true) }
                                    .sumOf { it.currentCount }
                                FilterChip(
                                    selected = selectedStorageUnit.equals(unit, ignoreCase = true),
                                    onClick = {
                                        selectedStorageUnit = if (selectedStorageUnit.equals(unit, ignoreCase = true)) {
                                            ""
                                        } else {
                                            unit
                                        }
                                    },
                                    label = { Text(if (unitRounds > 0) "$unit ($unitRounds)" else unit) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("session_storage_unit_chip_$unit")
                                )
                            }
                        }

                        if (selectedStorageUnit.isNotBlank()) {
                            val matchingInStockLots = allEntries.filter {
                                it.storageLocation.trim().equals(selectedStorageUnit, ignoreCase = true) && it.currentCount > 0
                            }
                            if (matchingInStockLots.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedLotRounds.clear()
                                            matchingInStockLots.forEach { lot ->
                                                val defaultRds = minOf(lot.currentCount.coerceAtLeast(1), (lot.boxSize * 2).coerceAtLeast(20))
                                                selectedLotRounds[lot.id] = defaultRds.toString()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("session_load_storage_lots_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Inventory2,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Load Can Lots (${matchingInStockLots.size})",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            matchingInStockLots.forEach { lot ->
                                                if (!selectedLotRounds.containsKey(lot.id)) {
                                                    val defaultRds = minOf(lot.currentCount.coerceAtLeast(1), (lot.boxSize * 2).coerceAtLeast(20))
                                                    selectedLotRounds[lot.id] = defaultRds.toString()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("session_append_storage_lots_button")
                                    ) {
                                        Text("+ Append")
                                    }
                                }
                            } else {
                                Text(
                                    text = "No in-stock lots are currently inside \"$selectedStorageUnit\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Ammo / Lot / Caliber Used
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AMMO & LOTS FIRED",
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showAddLotSheet = true },
                            enabled = availableToAdd.isNotEmpty(),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("range_session_add_lot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Lot")
                        }
                    }

                    if (selectedLotRounds.isEmpty()) {
                        Text(
                            text = "Tap '+ Add Lot' to pick the specific ammo lots you shot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        selectedLotRounds.keys.toList().forEach { entryId ->
                            val entry = entriesById[entryId] ?: return@forEach
                            val rdsStr = selectedLotRounds[entryId] ?: "0"
                            val currentVal = rdsStr.toIntOrNull() ?: 0

                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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
                                                CaliberPill(caliber = entry.caliber)
                                                Text(
                                                    text = "${entry.grain} gr ${entry.bulletType}",
                                                    style = MonoTelemetrySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${entry.brand} ${entry.productLine}".trim(),
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val lotInfo = if (entry.lotNumber.isNotBlank()) "Lot ${entry.lotNumber} · " else ""
                                            val storageInfo = if (entry.storageLocation.isNotBlank()) " · ${entry.storageLocation}" else ""
                                            Text(
                                                text = "$lotInfo${formatRounds(entry.currentCount)} rds on hand$storageInfo",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedLotRounds.remove(entryId) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Remove lot from session",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = rdsStr,
                                            onValueChange = { text ->
                                                val digits = text.filter(Char::isDigit)
                                                if (digits.length <= 5) selectedLotRounds[entryId] = digits
                                            },
                                            label = { Text("Rounds Fired") },
                                            suffix = { Text("rds") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("session_rounds_input_$entryId")
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                selectedLotRounds[entryId] =
                                                    (currentVal + entry.boxSize).toString()
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("+${entry.boxSize}")
                                        }
                                    }

                                    // Per-lot Malfunction / Dud counter
                                    val malfCount = selectedLotMalfunctions[entryId] ?: 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Malfunctions / Duds",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedLotMalfunctions[entryId] = (malfCount - 1).coerceAtLeast(0)
                                                },
                                                enabled = malfCount > 0,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("session_malf_minus_$entryId")
                                            ) { Text("-") }
                                            Text(
                                                text = malfCount.toString(),
                                                style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (malfCount > 0) {
                                                    LocalAmmoStatusColors.current.low
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    selectedLotMalfunctions[entryId] = malfCount + 1
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("session_malf_plus_$entryId")
                                            ) { Text("+") }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!sourceBagName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = clearSourceBag,
                                onCheckedChange = { clearSourceBag = it },
                                modifier = Modifier.testTag("clear_bag_after_log_checkbox")
                            )
                            Text(
                                text = "Empty \"$sourceBagName\" after logging session",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Session Notes (Zero, Groups, Weather)") },
                        placeholder = { Text("e.g., Confirmed 100yd zero, 0.85 MOA 5-shot group") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("range_session_notes_input")
                    )

                    PhotoAttachmentPickerCard(
                        photoUri = photoUri,
                        title = "Target / Range Photo (Optional)",
                        subtitle = "Attach a target group or range bench photo via Photo Picker",
                        onPhotoChanged = { photoUri = it },
                        testTagPrefix = "range_session_photo"
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${formatRounds(totalSessionRounds)} rds deducted",
                            style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Est. ammo burned: ${displayCurrency(totalSessionCost)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (canSubmit) {
                                val session = RangeSession(
                                    sessionDateTime = sessionMillis,
                                    location = location.trim(),
                                    durationMinutes = parsedDuration,
                                    rangeBagName = sourceBagName.orEmpty(),
                                    storageUnitName = selectedStorageUnit.trim(),
                                    sessionType = sessionType,
                                    distanceYards = parsedDistance,
                                    bestGroupMoa = parsedMoa,
                                    avgVelocityFps = parsedVelocity,
                                    notes = notes.trim(),
                                    photoUri = photoUri
                                )
                                onSaveSession(session, draftUsages, clearSourceBag && !sourceBagName.isNullOrBlank())
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("confirm_log_range_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Trip")
                    }
                }
            }
        }
    }
}
