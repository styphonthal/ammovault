package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.example.data.AmmoEntry
import com.example.data.LotReliabilityStats
import com.example.data.PriceHistoryEntry
import com.example.data.RangePresets
import com.example.ui.components.BuyTargetDialog
import com.example.ui.components.CaliberPill
import com.example.ui.components.FifoShootFirstBadge
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.LocalHideDollarValues
import com.example.ui.components.LotReliabilityBadge
import com.example.ui.components.PriceHistoryChart
import com.example.ui.components.QuickMarketPriceDialog
import com.example.ui.components.QuickRoundAdjustDialog
import com.example.ui.components.StockStatusChip
import com.example.ui.components.displayCpr
import com.example.ui.components.displayCurrency
import com.example.ui.components.displaySignedCurrency
import com.example.ui.components.formatHistoryTimestamp
import com.example.ui.components.formatPurchaseDate
import com.example.ui.components.formatRounds
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryLarge
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LotDetailScreen(
    entry: AmmoEntry?,
    priceHistory: List<PriceHistoryEntry> = emptyList(),
    caliberDefaultBuyTarget: Double? = null,
    isFifoOldest: Boolean = false,
    reliabilityStats: LotReliabilityStats? = null,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDuplicateClick: (Long) -> Unit,
    onAdjustRounds: (AmmoEntry, Int) -> Unit,
    onSetExactRounds: (AmmoEntry, Int) -> Unit,
    onUpdateMarketPrice: (AmmoEntry, Double, String) -> Unit,
    onUpdateBuyTarget: (AmmoEntry, Double?) -> Unit,
    onCheckAmmoSeek: (AmmoEntry) -> Unit,
    onDeleteEntry: (AmmoEntry) -> Unit,
    modifier: Modifier = Modifier,
    onCheckAmmoSeekUrl: ((AmmoEntry, String) -> Unit)? = null,
    rangeBagNames: List<String> = RangePresets.commonRangeBags,
    defaultRangeBagName: String = RangePresets.commonRangeBags.first(),
    onPackInRangeBag: ((bagName: String, entry: AmmoEntry, packedRounds: Int) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCustomAdjust by remember { mutableStateOf(false) }
    var showPriceEdit by remember { mutableStateOf(false) }
    var showBuyTargetEdit by remember { mutableStateOf(false) }
    var showPackDialog by remember { mutableStateOf(false) }
    var showFullPhoto by remember { mutableStateOf(false) }

    if (entry != null && showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Ammo Entry?") },
            text = {
                Text("Delete ${entry.brand} ${entry.caliber} (${entry.grain} gr ${entry.bulletType}) from your catalog? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteEntry(entry)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_lot_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (entry != null && showCustomAdjust) {
        QuickRoundAdjustDialog(
            entry = entry,
            onDismiss = { showCustomAdjust = false },
            onApplyDelta = { delta ->
                onAdjustRounds(entry, delta)
                showCustomAdjust = false
            },
            onSetExactRounds = { exact ->
                onSetExactRounds(entry, exact)
                showCustomAdjust = false
            }
        )
    }

    if (entry != null && showPriceEdit) {
        QuickMarketPriceDialog(
            entry = entry,
            onDismiss = { showPriceEdit = false },
            onSavePrice = { newPrice, source ->
                onUpdateMarketPrice(entry, newPrice, source)
                showPriceEdit = false
            },
            onCheckAmmoSeek = {
                showPriceEdit = false
                onCheckAmmoSeek(entry)
            },
            onCheckAmmoSeekUrl = onCheckAmmoSeekUrl?.let { callback ->
                { url ->
                    showPriceEdit = false
                    callback(entry, url)
                }
            }
        )
    }

    if (entry != null && showBuyTargetEdit) {
        BuyTargetDialog(
            entry = entry,
            caliberDefaultTarget = caliberDefaultBuyTarget,
            onDismiss = { showBuyTargetEdit = false },
            onSaveTarget = { newTarget ->
                onUpdateBuyTarget(entry, newTarget)
                showBuyTargetEdit = false
            }
        )
    }

    if (entry != null && showPackDialog && onPackInRangeBag != null) {
        PackRangeBagDialog(
            allEntries = listOf(entry),
            bagNames = rangeBagNames,
            initialBagName = defaultRangeBagName,
            initialEntry = entry,
            onDismiss = { showPackDialog = false },
            onConfirmPack = { bagName, packedEntry, rounds ->
                onPackInRangeBag(bagName, packedEntry, rounds)
                showPackDialog = false
            }
        )
    }

    if (entry != null && showFullPhoto && entry.photoUri.isNotBlank()) {
        FullScreenPhotoDialog(
            photoUri = entry.photoUri,
            title = "${entry.brand} ${entry.caliber}",
            onDismiss = { showFullPhoto = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Entry Specification") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (entry != null) {
                        IconButton(
                            onClick = { onDuplicateClick(entry.id) },
                            modifier = Modifier.testTag("detail_duplicate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Duplicate ammo lot"
                            )
                        }
                        IconButton(
                            onClick = { onEditClick(entry.id) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit ammo entry"
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete ammo entry",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (entry == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ammo entry not found",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onNavigateBack) {
                    Text("Return to Catalog")
                }
            }
        } else {
            val formattedUpdated = remember(entry.updatedAt) {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(entry.updatedAt))
            }
            val formattedPurchaseDate = remember(entry.purchaseDate) {
                entry.purchaseDate?.let { formatPurchaseDate(it) }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Identity Card
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CaliberPill(caliber = entry.caliber)
                                StockStatusChip(status = entry.stockStatus)
                            }
                            Text(
                                text = entry.brand,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (entry.productLine.isNotBlank()) {
                                Text(
                                    text = entry.productLine,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isFifoOldest || (reliabilityStats != null && (reliabilityStats.totalRoundsFired > 0 || reliabilityStats.totalMalfunctions > 0))) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isFifoOldest) {
                                        FifoShootFirstBadge()
                                    }
                                    if (reliabilityStats != null && (reliabilityStats.totalRoundsFired > 0 || reliabilityStats.totalMalfunctions > 0)) {
                                        LotReliabilityBadge(stats = reliabilityStats)
                                    }
                                }
                            }
                            if (entry.photoUri.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { showFullPhoto = true }
                                        .testTag("detail_lot_photo")
                                ) {
                                    AsyncImage(
                                        model = entry.photoUri,
                                        contentDescription = "Photo of ${entry.brand} ${entry.caliber}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Live Inventory Control Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "INVENTORY ON HAND",
                                        style = MonoTelemetrySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${formatRounds(entry.currentCount)} Rounds",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(
                                    onClick = { showCustomAdjust = true },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_custom_adjust_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Custom +/-")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onAdjustRounds(entry, -10) },
                                    enabled = entry.currentCount > 0,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_minus_ten")
                                ) {
                                    Text("-10", style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                OutlinedButton(
                                    onClick = { onAdjustRounds(entry, -1) },
                                    enabled = entry.currentCount > 0,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_minus_one")
                                ) {
                                    Text("-1", style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                OutlinedButton(
                                    onClick = { onAdjustRounds(entry, 1) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_plus_one")
                                ) {
                                    Text("+1", style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                OutlinedButton(
                                    onClick = { onAdjustRounds(entry, 10) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_plus_ten")
                                ) {
                                    Text("+10", style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            if (entry.boxSize != 10 && entry.boxSize != 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onAdjustRounds(entry, -entry.boxSize) },
                                        enabled = entry.currentCount > 0,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("detail_minus_box")
                                    ) {
                                        Text("-${entry.boxSize} (1 Box)", style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    OutlinedButton(
                                        onClick = { onAdjustRounds(entry, entry.boxSize) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("detail_plus_box")
                                    ) {
                                        Text("+${entry.boxSize} (1 Box)", style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            Text(
                                text = "Low stock warning at ≤ ${entry.lowStockThreshold} rounds · Box size: ${entry.boxSize} rds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (onPackInRangeBag != null) {
                                OutlinedButton(
                                    onClick = { showPackDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_pack_in_range_bag_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WorkOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pack Lot in Range Bag")
                                }
                            }
                        }
                    }

                    // Market Valuation & Cost Basis Card
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusColors = LocalAmmoStatusColors.current
                        val effectiveBuyTarget = entry.targetBuyPricePerRound ?: caliberDefaultBuyTarget
                        val isAtBuyTarget = effectiveBuyTarget != null &&
                            entry.marketPricePerRound <= effectiveBuyTarget + 0.0001

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
                                Text(
                                    text = "VALUATION & COST BASIS",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { showPriceEdit = true },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_update_price_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Update CPR")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Market Price / Rd",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayCpr(entry.marketPricePerRound),
                                        style = MonoTelemetryLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Market Value",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayCurrency(entry.totalMarketValue),
                                        style = MonoTelemetryLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (entry.hasExplicitCostBasis) "Purchase Cost / Rd" else "Cost Basis / Rd (Est.)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayCpr(entry.effectivePurchaseCpr),
                                        style = MonoTelemetryMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Total Cost Basis",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayCurrency(entry.totalCostBasis),
                                        style = MonoTelemetryMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Gain / Loss",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val gainLoss = entry.unrealizedGainLoss
                                    val pctText = if (LocalHideDollarValues.current) {
                                        ""
                                    } else {
                                        entry.unrealizedGainLossPercent?.let {
                                            val sign = if (it >= 0) "+" else ""
                                            " ($sign${String.format(Locale.US, "%.1f", it)}%)"
                                        } ?: ""
                                    }
                                    val glColor = when {
                                        gainLoss > 0.005 -> statusColors.healthy
                                        gainLoss < -0.005 -> statusColors.low
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(
                                        text = "${displaySignedCurrency(gainLoss)}$pctText",
                                        style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                        color = glColor
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = effectiveBuyTarget?.let {
                                            "Buy Target: ${displayCpr(it)}" +
                                                if (entry.targetBuyPricePerRound == null) " (Caliber)" else ""
                                        } ?: "Buy Target: Not set",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isAtBuyTarget) {
                                        Surface(
                                            color = statusColors.healthyContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "AT OR BELOW BUY TARGET",
                                                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                                color = statusColors.healthy,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showBuyTargetEdit = true },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_buy_target_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PriceCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (entry.targetBuyPricePerRound != null) "Edit Target" else "Set Target")
                                }
                            }

                            Text(
                                text = "Current ${entry.boxSize}-round box value: ${displayCurrency(entry.marketPricePerRound * entry.boxSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onCheckAmmoSeek(entry) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_check_ammoseek_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AmmoSeek.com")
                                }

                                OutlinedButton(
                                    onClick = { onDuplicateClick(entry.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("detail_restock_duplicate_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Duplicate Lot")
                                }
                            }
                        }
                    }

                    // CPR Price History Card
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_price_history_card")
                    ) {
                        val statusColors = LocalAmmoStatusColors.current
                        val earliest = remember(priceHistory) { priceHistory.minByOrNull { it.recordedAt } }
                        val cprChange = earliest?.let { entry.marketPricePerRound - it.pricePerRound }

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
                                Text(
                                    text = "CPR PRICE HISTORY (${priceHistory.size})",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (cprChange != null && abs(cprChange) >= 0.005 && !LocalHideDollarValues.current) {
                                    val sign = if (cprChange > 0) "+" else "-"
                                    val changeColor = if (cprChange > 0) statusColors.low else statusColors.healthy
                                    Text(
                                        text = "$sign${String.format(Locale.US, "$%.2f/rd", abs(cprChange))} vs initial",
                                        style = MonoTelemetrySmall,
                                        color = changeColor
                                    )
                                }
                            }

                            if (priceHistory.isEmpty()) {
                                Text(
                                    text = "No price snapshots recorded yet. Update CPR to begin tracking price trends.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                PriceHistoryChart(history = priceHistory)

                                priceHistory.take(8).forEachIndexed { index, snapshot ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                                text = snapshot.source,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val subtitleParts = buildList {
                                                add(formatHistoryTimestamp(snapshot.recordedAt))
                                                if (snapshot.vendor.isNotBlank() && snapshot.vendor != snapshot.source) {
                                                    add(snapshot.vendor)
                                                }
                                            }
                                            Text(
                                                text = subtitleParts.joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = displayCpr(snapshot.pricePerRound),
                                            style = MonoTelemetryMedium,
                                            color = if (index == 0) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ballistic & Purchase Specification Table
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "BALLISTIC & PURCHASE SPECS",
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            SpecDetailRow(label = "Caliber", value = entry.caliber)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SpecDetailRow(label = "Grain Weight", value = "${entry.grain} gr")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SpecDetailRow(label = "Bullet Type", value = entry.bulletType)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SpecDetailRow(label = "Casing Material", value = entry.casingMaterial)
                            if (entry.storageLocation.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SpecDetailRow(label = "Storage Location", value = entry.storageLocation)
                            }
                            if (entry.vendor.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SpecDetailRow(label = "Vendor / Store", value = entry.vendor)
                            }
                            if (formattedPurchaseDate != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SpecDetailRow(label = "Purchase Date", value = formattedPurchaseDate)
                            }
                            if (entry.lotNumber.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SpecDetailRow(label = "Lot Number", value = entry.lotNumber)
                            }
                            if (entry.upcCode.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SpecDetailRow(label = "UPC Barcode", value = entry.upcCode)
                            }
                            if (entry.notes.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Notes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = entry.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SpecDetailRow(label = "Last Updated", value = formattedUpdated)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SpecDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MonoTelemetryMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
