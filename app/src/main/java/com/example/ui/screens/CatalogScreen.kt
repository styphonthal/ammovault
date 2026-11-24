package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AmmoEntry
import com.example.data.AmmoSortOption
import com.example.data.LotReliabilityStats
import com.example.ui.AmmoVaultUiState
import com.example.ui.components.CaliberPill
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.FifoShootFirstBadge
import com.example.ui.components.LotReliabilityBadge
import com.example.ui.components.QuickRoundAdjustDialog
import com.example.ui.components.SpecBadge
import com.example.ui.components.StockStatusChip
import com.example.ui.components.displayCpr
import com.example.ui.components.displayCurrency
import com.example.ui.components.formatRounds
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall

@Composable
fun CatalogScreen(
    uiState: AmmoVaultUiState,
    onSearchChange: (String) -> Unit,
    onSelectCaliber: (String?) -> Unit,
    onSelectLocation: (String?) -> Unit,
    onToggleLowStock: () -> Unit,
    onSelectSort: (AmmoSortOption) -> Unit,
    onAdjustRounds: (AmmoEntry, Int) -> Unit,
    onSetExactRounds: (AmmoEntry, Int) -> Unit,
    onEntryClick: (Long) -> Unit,
    onAddAmmoClick: () -> Unit,
    onLoadStarterClick: () -> Unit,
    onOpenStorageDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var adjustDialogEntry by remember { mutableStateOf<AmmoEntry?>(null) }

    if (adjustDialogEntry != null) {
        val entry = adjustDialogEntry!!
        QuickRoundAdjustDialog(
            entry = entry,
            onDismiss = { adjustDialogEntry = null },
            onApplyDelta = { delta ->
                onAdjustRounds(entry, delta)
                adjustDialogEntry = null
            },
            onSetExactRounds = { exact ->
                onSetExactRounds(entry, exact)
                adjustDialogEntry = null
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search brand, caliber, grain, can, vendor...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search catalog")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("catalog_search_input")
            )

            Box {
                OutlinedButton(
                    onClick = { sortMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("catalog_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort options",
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    AmmoSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == uiState.sortOption) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == uiState.sortOption) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                onSelectSort(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        if (uiState.allEntries.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.selectedCaliberFilter == null,
                    onClick = { onSelectCaliber(null) },
                    label = { Text("All (${uiState.allEntries.size})") },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("filter_chip_all")
                )

                FilterChip(
                    selected = uiState.showLowStockOnly,
                    onClick = onToggleLowStock,
                    label = { Text("Low Stock") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalAmmoStatusColors.current.lowContainer,
                        selectedLabelColor = LocalAmmoStatusColors.current.low,
                        selectedLeadingIconColor = LocalAmmoStatusColors.current.low
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("filter_chip_low_stock")
                )

                uiState.activeCalibers.forEach { caliber ->
                    FilterChip(
                        selected = uiState.selectedCaliberFilter == caliber,
                        onClick = {
                            onSelectCaliber(if (uiState.selectedCaliberFilter == caliber) null else caliber)
                        },
                        label = { Text(caliber) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_chip_$caliber")
                    )
                }
            }

            if (uiState.activeStorageLocations.isNotEmpty() || onOpenStorageDashboard != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenStorageDashboard != null) {
                        FilterChip(
                            selected = false,
                            onClick = onOpenStorageDashboard,
                            label = { Text("Cans & QR") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("catalog_open_storage_chip")
                        )
                    }
                    uiState.activeStorageLocations.forEach { location ->
                        val isSelected = uiState.selectedLocationFilter == location
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectLocation(if (isSelected) null else location)
                            },
                            label = { Text(location) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("filter_location_$location")
                        )
                    }
                }
            }
        }

        // Content List / Empty States
        when {
            uiState.allEntries.isEmpty() && !uiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
            }
            uiState.filteredEntries.isEmpty() && !uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No matching rifle ammo lots",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try clearing your search, location, or caliber filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            onSearchChange("")
                            onSelectCaliber(null)
                            onSelectLocation(null)
                            if (uiState.showLowStockOnly) onToggleLowStock()
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 680.dp
                    if (isWide) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(items = uiState.filteredEntries, key = { it.id }) { entry ->
                                AmmoEntryCard(
                                    entry = entry,
                                    isFifoOldest = entry.id in uiState.fifoOldestLotIds,
                                    reliabilityStats = uiState.lotReliabilityByEntryId[entry.id],
                                    onCardClick = { onEntryClick(entry.id) },
                                    onQuickAdjust = { delta -> onAdjustRounds(entry, delta) },
                                    onCustomAdjust = { adjustDialogEntry = entry }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = uiState.filteredEntries, key = { it.id }) { entry ->
                                AmmoEntryCard(
                                    entry = entry,
                                    isFifoOldest = entry.id in uiState.fifoOldestLotIds,
                                    reliabilityStats = uiState.lotReliabilityByEntryId[entry.id],
                                    onCardClick = { onEntryClick(entry.id) },
                                    onQuickAdjust = { delta -> onAdjustRounds(entry, delta) },
                                    onCustomAdjust = { adjustDialogEntry = entry }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmmoEntryCard(
    entry: AmmoEntry,
    isFifoOldest: Boolean = false,
    reliabilityStats: LotReliabilityStats? = null,
    onCardClick: () -> Unit,
    onQuickAdjust: (Int) -> Unit,
    onCustomAdjust: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick)
            .testTag("ammo_entry_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Caliber + Stock Status + CPR
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

            // Brand + Product Line + Rounds On Hand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.brand,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.productLine.isNotBlank()) {
                        Text(
                            text = entry.productLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatRounds(entry.currentCount)} rds",
                        style = MonoTelemetryLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = displayCurrency(entry.totalMarketValue),
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ballistic Specs, Location, FIFO & Reliability Strip
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isFifoOldest) {
                    FifoShootFirstBadge()
                }
                if (reliabilityStats != null && (reliabilityStats.totalRoundsFired > 0 || reliabilityStats.totalMalfunctions > 0)) {
                    LotReliabilityBadge(stats = reliabilityStats)
                }
                SpecBadge(text = "${entry.grain} gr")
                SpecBadge(text = entry.bulletType)
                SpecBadge(text = entry.casingMaterial)
                if (entry.storageLocation.isNotBlank()) {
                    SpecBadge(text = entry.storageLocation)
                }
                if (entry.vendor.isNotBlank()) {
                    SpecBadge(text = entry.vendor)
                }
                if (entry.lotNumber.isNotBlank()) {
                    SpecBadge(text = "Lot ${entry.lotNumber}")
                }
                if (entry.hasExplicitCostBasis) {
                    SpecBadge(text = "Paid ${displayCpr(entry.effectivePurchaseCpr)}")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Round Adjustments (+/-10, +/-1, and Exact Adjust)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onQuickAdjust(-10) },
                    enabled = entry.currentCount > 0,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("lot_minus_10_${entry.id}")
                ) {
                    Text(
                        text = "-10",
                        style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { onQuickAdjust(-1) },
                    enabled = entry.currentCount > 0,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .heightIn(min = 48.dp)
                        .testTag("lot_minus_1_${entry.id}")
                ) {
                    Text(
                        text = "-1",
                        style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { onQuickAdjust(1) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .heightIn(min = 48.dp)
                        .testTag("lot_plus_1_${entry.id}")
                ) {
                    Text(
                        text = "+1",
                        style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { onQuickAdjust(10) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("lot_plus_10_${entry.id}")
                ) {
                    Text(
                        text = "+10",
                        style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                FilledTonalIconButton(
                    onClick = onCustomAdjust,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("lot_custom_adjust_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Exact round adjustment",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
