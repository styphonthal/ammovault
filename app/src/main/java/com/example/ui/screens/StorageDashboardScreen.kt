package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.StarterRiflePresets
import com.example.data.StorageContainerSummary
import com.example.data.StorageQrCodec
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.CaliberPill
import com.example.ui.components.displayCurrency
import com.example.ui.components.formatRounds
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDashboardScreen(
    uiState: AmmoVaultUiState,
    onNavigateBack: () -> Unit,
    onOpenLocationInCatalog: (String) -> Unit,
    onAddStorageUnit: (String) -> Unit,
    onRenameStorageUnit: (oldName: String, newName: String) -> Unit,
    onDeleteStorageUnit: (String) -> Unit,
    onUseStorageAtRange: (String) -> Unit,
    onScanCanQrClick: () -> Unit,
    onSaveCanQrPng: (StorageContainerSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    var qrPreviewContainer by remember { mutableStateOf<StorageContainerSummary?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var renamingContainer by remember { mutableStateOf<StorageContainerSummary?>(null) }
    var deletingContainer by remember { mutableStateOf<StorageContainerSummary?>(null) }

    if (showAddDialog) {
        AddStorageUnitDialog(
            existingNames = uiState.allStorageUnitNames,
            onDismiss = { showAddDialog = false },
            onConfirmAdd = { newName ->
                onAddStorageUnit(newName)
                showAddDialog = false
            }
        )
    }

    if (renamingContainer != null) {
        val target = renamingContainer!!
        RenameStorageUnitDialog(
            container = target,
            existingNames = uiState.allStorageUnitNames,
            onDismiss = { renamingContainer = null },
            onConfirmRename = { newName ->
                onRenameStorageUnit(target.locationName, newName)
                renamingContainer = null
            }
        )
    }

    if (deletingContainer != null) {
        val target = deletingContainer!!
        DeleteStorageUnitDialog(
            container = target,
            onDismiss = { deletingContainer = null },
            onConfirmDelete = {
                onDeleteStorageUnit(target.locationName)
                deletingContainer = null
            }
        )
    }

    if (qrPreviewContainer != null) {
        val container = qrPreviewContainer!!
        StorageQrLabelDialog(
            container = container,
            onDismiss = { qrPreviewContainer = null },
            onSavePngClick = {
                onSaveCanQrPng(container)
                qrPreviewContainer = null
            }
        )
    }

    val namedContainers = remember(uiState.storageSummaries) {
        uiState.storageSummaries.filterNot { it.isUnassigned }
    }
    val totalWeightLbs = remember(uiState.storageSummaries) {
        uiState.storageSummaries.sumOf { it.estimatedWeightLbs }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Ammo Cans & Storage") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("storage_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("storage_add_unit_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add storage unit"
                        )
                    }
                    IconButton(
                        onClick = onScanCanQrClick,
                        modifier = Modifier.testTag("storage_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan ammo can QR label"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_hero_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "STORAGE CONTAINERS & WEIGHT ESTIMATE",
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "${namedContainers.size} Container${if (namedContainers.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${formatRounds(uiState.totalRounds)} total rounds · ${displayCurrency(uiState.totalMarketValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "~%.1f lbs", totalWeightLbs),
                                    style = MonoTelemetryLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Est. Ammo Weight",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("storage_add_unit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Storage")
                            }
                            OutlinedButton(
                                onClick = onScanCanQrClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("storage_open_scanner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Can QR")
                            }
                        }
                    }
                }
            }

            if (uiState.storageSummaries.isEmpty()) {
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
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Storage Containers Yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap 'Add Storage' above to create named ammo cans, safes, or shelves—then assign lots, print QR labels, or take specific cans to your range days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.storageSummaries,
                    key = { "${it.isUnassigned}_${it.locationName}" }
                ) { container ->
                    StorageContainerCard(
                        container = container,
                        onInspectLots = {
                            if (!container.isUnassigned) {
                                onOpenLocationInCatalog(container.locationName)
                            }
                        },
                        onRenameClick = { renamingContainer = container },
                        onDeleteClick = { deletingContainer = container },
                        onUseAtRangeClick = { onUseStorageAtRange(container.locationName) },
                        onShowQrLabel = { qrPreviewContainer = container }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageContainerCard(
    container: StorageContainerSummary,
    onInspectLots: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUseAtRangeClick: () -> Unit,
    onShowQrLabel: () -> Unit
) {
    val statusColors = LocalAmmoStatusColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storage_container_card_${container.locationName}")
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
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = if (container.isUnassigned) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Column {
                        Text(
                            text = container.locationName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${container.lotCount} lot${if (container.lotCount == 1) "" else "s"} · ${formatRounds(container.totalRounds)} rds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayCurrency(container.totalMarketValue),
                        style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Scale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "~%.1f lbs", container.estimatedWeightLbs),
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (container.calibers.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    container.calibers.forEach { cal ->
                        CaliberPill(caliber = cal)
                    }
                    if (container.lowStockLotCount > 0) {
                        Surface(
                            color = statusColors.lowContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${container.lowStockLotCount} LOW",
                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColors.low,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else if (!container.isUnassigned) {
                Text(
                    text = "Empty storage unit — assign lots to it when adding or editing ammo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!container.isUnassigned) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onInspectLots,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("storage_inspect_${container.locationName}")
                    ) {
                        Text("Inspect", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onShowQrLabel,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("storage_qr_button_${container.locationName}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QR Label", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = onRenameClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("storage_rename_${container.locationName}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Rename ${container.locationName}"
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("storage_delete_${container.locationName}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete ${container.locationName}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Button(
                    onClick = onUseAtRangeClick,
                    enabled = container.totalRounds > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("storage_use_at_range_${container.locationName}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (container.totalRounds > 0) {
                            "Take to Range Day / Event"
                        } else {
                            "Add Lots to Use at Range Day"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddStorageUnitDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirmAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()
    val alreadyExists = existingNames.any { it.equals(trimmed, ignoreCase = true) }
    val suggestedPresets = remember(existingNames) {
        StarterRiflePresets.commonStorageLocations.filter { preset ->
            existingNames.none { it.equals(preset, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ammo Storage Unit") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create a named ammo can, safe shelf, or storage bin so you can assign lots, print QR labels, and bring it to range days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Storage Unit Name") },
                    placeholder = { Text("e.g., 50 Cal Can #1, Safe Top Shelf") },
                    singleLine = true,
                    isError = alreadyExists,
                    supportingText = if (alreadyExists) {
                        { Text("A storage unit with this name already exists.") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_storage_unit_input")
                )
                if (suggestedPresets.isNotEmpty()) {
                    Text(
                        text = "Quick Suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedPresets.forEach { preset ->
                            FilterChip(
                                selected = trimmed.equals(preset, ignoreCase = true),
                                onClick = { name = preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmAdd(trimmed) },
                enabled = trimmed.isNotEmpty() && !alreadyExists,
                modifier = Modifier.testTag("confirm_add_storage_unit_button")
            ) {
                Text("Add Unit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameStorageUnitDialog(
    container: StorageContainerSummary,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    var newName by remember(container.locationName) { mutableStateOf(container.locationName) }
    val trimmed = newName.trim()
    val unchanged = trimmed == container.locationName
    val willMerge = !unchanged &&
        trimmed.isNotEmpty() &&
        existingNames.any { it.equals(trimmed, ignoreCase = true) && !it.equals(container.locationName, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Storage Unit") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Renaming \"${container.locationName}\" will automatically update all ${container.lotCount} assigned lot${if (container.lotCount == 1) "" else "s"} and linked range logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Storage Unit Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_storage_unit_input")
                )
                if (willMerge) {
                    Text(
                        text = "Note: \"$trimmed\" already exists. Saving will merge all lots from \"${container.locationName}\" into \"$trimmed\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRename(trimmed) },
                enabled = trimmed.isNotEmpty() && !unchanged,
                modifier = Modifier.testTag("confirm_rename_storage_unit_button")
            ) {
                Text(if (willMerge) "Merge & Rename" else "Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteStorageUnitDialog(
    container: StorageContainerSummary,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Storage Unit?") },
        text = {
            val lotMessage = if (container.lotCount > 0) {
                "The ${container.lotCount} lot${if (container.lotCount == 1) "" else "s"} (${formatRounds(container.totalRounds)} rds) inside will NOT be deleted—they will be moved to \"Unassigned Storage\"."
            } else {
                "This empty storage unit will be removed from your vault."
            }
            Text("Remove \"${container.locationName}\"? $lotMessage")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_storage_unit_button")
            ) {
                Text("Delete Unit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StorageQrLabelDialog(
    container: StorageContainerSummary,
    onDismiss: () -> Unit,
    onSavePngClick: () -> Unit
) {
    val qrBitmap = remember(container.locationName) {
        StorageQrCodec.generateQrBitmap(
            payload = StorageQrCodec.encodeCanQr(container.locationName),
            sizePx = 420
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Printable Can QR Label") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color.White,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(2.dp, Color(0xFF1C1F24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = container.locationName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1C1F24)
                        )
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR code for ${container.locationName}",
                            modifier = Modifier
                                .size(190.dp)
                                .background(Color.White)
                        )
                        Text(
                            text = "${formatRounds(container.totalRounds)} rds · ${container.lotCount} lots · ${String.format(Locale.US, "~%.1f lbs", container.estimatedWeightLbs)}",
                            style = MonoTelemetrySmall,
                            color = Color(0xFF3A414D)
                        )
                        if (container.calibers.isNotEmpty()) {
                            Text(
                                text = container.calibers.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555D6B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "Save and print this label to tape onto your ammo can or shelf. Scan it anytime with AmmoVault's scanner to inspect its exact contents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSavePngClick,
                modifier = Modifier.testTag("save_qr_label_png_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save PNG Label")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
