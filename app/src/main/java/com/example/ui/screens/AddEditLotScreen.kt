package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AmmoEntry
import com.example.data.StarterRiflePresets
import com.example.data.UpcCatalog
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.PhotoAttachmentPickerCard
import com.example.ui.components.formatCpr
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatPurchaseDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLotScreen(
    existingEntry: AmmoEntry?,
    defaultCaliber: String? = null,
    isDuplicate: Boolean = false,
    initialUpc: String? = null,
    availableStorageUnits: List<String> = emptyList(),
    onScanUpcClick: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onSave: (AmmoEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEdit = existingEntry != null && !isDuplicate
    val normalizedInitialUpc = remember(initialUpc) {
        initialUpc?.let { UpcCatalog.normalizeUpc(it) }.orEmpty()
    }
    val upcPreset = remember(normalizedInitialUpc, existingEntry) {
        if (existingEntry == null && normalizedInitialUpc.isNotEmpty()) {
            UpcCatalog.lookupBuiltIn(normalizedInitialUpc)
        } else {
            null
        }
    }

    var caliber by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.caliber ?: upcPreset?.caliber ?: defaultCaliber ?: "5.56 NATO")
    }
    var brand by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.brand ?: upcPreset?.brand ?: "")
    }
    var productLine by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.productLine ?: upcPreset?.productLine ?: "")
    }
    var grainText by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.grain?.toString() ?: upcPreset?.grain?.toString() ?: "")
    }
    var bulletType by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.bulletType ?: upcPreset?.bulletType ?: "FMJ")
    }
    var casingMaterial by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.casingMaterial ?: upcPreset?.casingMaterial ?: "Brass")
    }
    var roundsText by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.currentCount?.toString() ?: upcPreset?.boxSize?.toString() ?: "20")
    }
    var boxSizeText by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.boxSize?.toString() ?: upcPreset?.boxSize?.toString() ?: "20")
    }
    var lowThresholdText by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(existingEntry?.lowStockThreshold?.toString() ?: "60")
    }
    var priceInputModeBox by remember { mutableStateOf(false) }
    var cprText by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(
            if (existingEntry != null) String.format(Locale.US, "%.2f", existingEntry.marketPricePerRound) else ""
        )
    }
    var boxPriceText by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(
            if (existingEntry != null) {
                String.format(Locale.US, "%.2f", existingEntry.marketPricePerRound * existingEntry.boxSize)
            } else {
                ""
            }
        )
    }
    var purchaseCprText by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(
            existingEntry?.costBasis?.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    var purchaseBoxPriceText by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(
            existingEntry?.costBasis?.takeIf { it > 0.0 }?.let {
                String.format(Locale.US, "%.2f", it * existingEntry.boxSize)
            } ?: ""
        )
    }
    var buyTargetCprText by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(
            existingEntry?.targetBuyPricePerRound?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    var vendor by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(existingEntry?.vendor ?: "")
    }
    var storageLocation by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(existingEntry?.storageLocation ?: "")
    }
    var purchaseDate by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(
            if (isDuplicate) System.currentTimeMillis() else existingEntry?.purchaseDate
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var lotNumber by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(if (isDuplicate) "" else existingEntry?.lotNumber ?: "")
    }
    var upcCode by remember(existingEntry?.id, isDuplicate, normalizedInitialUpc) {
        mutableStateOf(existingEntry?.upcCode ?: normalizedInitialUpc)
    }
    var photoUri by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(if (isDuplicate) "" else existingEntry?.photoUri ?: "")
    }
    var showFullPhoto by remember { mutableStateOf(false) }
    var notes by remember(existingEntry?.id, isDuplicate) {
        mutableStateOf(existingEntry?.notes ?: "")
    }
    var attemptedSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(existingEntry, isDuplicate) {
        if (existingEntry != null) {
            caliber = existingEntry.caliber
            brand = existingEntry.brand
            productLine = existingEntry.productLine
            grainText = existingEntry.grain.toString()
            bulletType = existingEntry.bulletType
            casingMaterial = existingEntry.casingMaterial
            roundsText = existingEntry.currentCount.toString()
            boxSizeText = existingEntry.boxSize.toString()
            lowThresholdText = existingEntry.lowStockThreshold.toString()
            cprText = String.format(Locale.US, "%.2f", existingEntry.marketPricePerRound)
            boxPriceText = String.format(Locale.US, "%.2f", existingEntry.marketPricePerRound * existingEntry.boxSize)
            purchaseCprText = existingEntry.costBasis.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            purchaseBoxPriceText = existingEntry.costBasis.takeIf { it > 0.0 }?.let {
                String.format(Locale.US, "%.2f", it * existingEntry.boxSize)
            } ?: ""
            buyTargetCprText = existingEntry.targetBuyPricePerRound?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            vendor = existingEntry.vendor
            storageLocation = existingEntry.storageLocation
            purchaseDate = if (isDuplicate) System.currentTimeMillis() else existingEntry.purchaseDate
            lotNumber = if (isDuplicate) "" else existingEntry.lotNumber
            upcCode = existingEntry.upcCode
            photoUri = if (isDuplicate) "" else existingEntry.photoUri
            notes = existingEntry.notes
        }
    }

    val parsedGrain = grainText.toIntOrNull()
    val parsedRounds = roundsText.toIntOrNull()
    val parsedBoxSize = (boxSizeText.toIntOrNull() ?: 20).coerceAtLeast(1)
    val parsedLowThreshold = (lowThresholdText.toIntOrNull() ?: 60).coerceAtLeast(0)
    val computedCpr: Double? = if (priceInputModeBox) {
        boxPriceText.toDoubleOrNull()?.let { it / parsedBoxSize.toDouble() }
    } else {
        cprText.toDoubleOrNull()
    }
    val computedPurchaseCpr: Double? = if (priceInputModeBox) {
        if (purchaseBoxPriceText.isBlank()) null else purchaseBoxPriceText.toDoubleOrNull()?.let { it / parsedBoxSize.toDouble() }
    } else {
        if (purchaseCprText.isBlank()) null else purchaseCprText.toDoubleOrNull()
    }
    val parsedBuyTargetCpr: Double? = if (buyTargetCprText.isBlank()) null else buyTargetCprText.toDoubleOrNull()

    val isCaliberValid = caliber.trim().isNotEmpty()
    val isBrandValid = brand.trim().isNotEmpty()
    val isGrainValid = parsedGrain != null && parsedGrain in 15..1000
    val isBulletValid = bulletType.trim().isNotEmpty()
    val isRoundsValid = parsedRounds != null && parsedRounds >= 0
    val isPriceValid = computedCpr != null && computedCpr >= 0.0

    val canSave = isCaliberValid && isBrandValid && isGrainValid && isBulletValid && isRoundsValid && isPriceValid

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = purchaseDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchaseDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirm_purchase_date_button")
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showFullPhoto && photoUri.isNotBlank()) {
        FullScreenPhotoDialog(
            photoUri = photoUri,
            title = "${brand.ifBlank { caliber }} Photo",
            onDismiss = { showFullPhoto = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isDuplicate -> "Duplicate / Restock Load"
                            isEdit -> "Edit Rifle Ammo Lot"
                            else -> "Catalog Rifle Ammo"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_edit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Caliber Section
                FormSectionCard(title = "1. Cartridge & Caliber") {
                    Text(
                        text = "Quick Select Caliber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StarterRiflePresets.commonCalibers.forEach { preset ->
                            FilterChip(
                                selected = caliber.equals(preset, ignoreCase = true),
                                onClick = { caliber = preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = caliber,
                        onValueChange = { caliber = it },
                        label = { Text("Caliber *") },
                        placeholder = { Text("e.g., .308 Win, 5.56 NATO, 6.5 Creedmoor") },
                        singleLine = true,
                        isError = attemptedSubmit && !isCaliberValid,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_caliber")
                    )
                }

                // 2. Brand & Ballistics Section
                FormSectionCard(title = "2. Brand & Ballistic Specs") {
                    Text(
                        text = "Popular Brands",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StarterRiflePresets.commonBrands.forEach { preset ->
                            FilterChip(
                                selected = brand.equals(preset, ignoreCase = true),
                                onClick = { brand = preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand / Manufacturer *") },
                        placeholder = { Text("e.g., Federal Premium, Hornady, PMC") },
                        singleLine = true,
                        isError = attemptedSubmit && !isBrandValid,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_brand")
                    )

                    OutlinedTextField(
                        value = productLine,
                        onValueChange = { productLine = it },
                        label = { Text("Product Line / Load Name (Optional)") },
                        placeholder = { Text("e.g., Gold Medal Match, X-TAC M193") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_product_line")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = grainText,
                            onValueChange = { if (it.length <= 4) grainText = it.filter(Char::isDigit) },
                            label = { Text("Grain Weight *") },
                            placeholder = { Text("168") },
                            suffix = { Text("gr") },
                            singleLine = true,
                            isError = attemptedSubmit && !isGrainValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_grain_weight")
                        )

                        OutlinedTextField(
                            value = bulletType,
                            onValueChange = { bulletType = it },
                            label = { Text("Bullet Type *") },
                            placeholder = { Text("BTHP") },
                            singleLine = true,
                            isError = attemptedSubmit && !isBulletValid,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_bullet_type")
                        )
                    }

                    Text(
                        text = "Bullet Type Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StarterRiflePresets.commonBulletTypes.forEach { preset ->
                            FilterChip(
                                selected = bulletType.equals(preset, ignoreCase = true),
                                onClick = { bulletType = preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    Text(
                        text = "Casing Material",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StarterRiflePresets.commonCasingMaterials.forEach { preset ->
                            FilterChip(
                                selected = casingMaterial.equals(preset, ignoreCase = true),
                                onClick = { casingMaterial = preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                // 3. Inventory & Market Price Section
                FormSectionCard(title = "3. Inventory & Market Pricing") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = roundsText,
                            onValueChange = { if (it.length <= 6) roundsText = it.filter(Char::isDigit) },
                            label = { Text("Rounds on Hand *") },
                            suffix = { Text("rds") },
                            singleLine = true,
                            isError = attemptedSubmit && !isRoundsValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_rounds_on_hand")
                        )
                        OutlinedTextField(
                            value = boxSizeText,
                            onValueChange = { if (it.length <= 4) boxSizeText = it.filter(Char::isDigit) },
                            label = { Text("Box Size") },
                            suffix = { Text("rds") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("input_box_size")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StarterRiflePresets.commonBoxSizes.forEach { size ->
                            FilterChip(
                                selected = boxSizeText == size.toString(),
                                onClick = { boxSizeText = size.toString() },
                                label = { Text("$size rd box") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = lowThresholdText,
                        onValueChange = { if (it.length <= 5) lowThresholdText = it.filter(Char::isDigit) },
                        label = { Text("Low Stock Alert Threshold") },
                        suffix = { Text("rds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_low_threshold")
                    )

                    Text(
                        text = "Market Price Entry Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val toggleModifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                        if (!priceInputModeBox) {
                            Button(
                                onClick = { priceInputModeBox = false },
                                modifier = toggleModifier.testTag("price_mode_per_round")
                            ) { Text("Price / Round") }
                            OutlinedButton(
                                onClick = { priceInputModeBox = true },
                                modifier = toggleModifier.testTag("price_mode_per_box")
                            ) { Text("Price / Box") }
                        } else {
                            OutlinedButton(
                                onClick = { priceInputModeBox = false },
                                modifier = toggleModifier.testTag("price_mode_per_round")
                            ) { Text("Price / Round") }
                            Button(
                                onClick = { priceInputModeBox = true },
                                modifier = toggleModifier.testTag("price_mode_per_box")
                            ) { Text("Price / Box") }
                        }
                    }

                    if (!priceInputModeBox) {
                        OutlinedTextField(
                            value = cprText,
                            onValueChange = { text ->
                                val cleaned = text.filter { it.isDigit() || it == '.' }
                                if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 7) {
                                    cprText = cleaned
                                }
                            },
                            label = { Text("Current Market Price Per Round *") },
                            placeholder = { Text("0.55") },
                            prefix = { Text("$") },
                            suffix = { Text("/ rd") },
                            singleLine = true,
                            isError = attemptedSubmit && !isPriceValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_market_cpr")
                        )
                    } else {
                        OutlinedTextField(
                            value = boxPriceText,
                            onValueChange = { text ->
                                val cleaned = text.filter { it.isDigit() || it == '.' }
                                if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 8) {
                                    boxPriceText = cleaned
                                }
                            },
                            label = { Text("Current Market Price Per Box ($parsedBoxSize rds) *") },
                            placeholder = { Text("24.99") },
                            prefix = { Text("$") },
                            suffix = { Text("/ box") },
                            singleLine = true,
                            isError = attemptedSubmit && !isPriceValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_market_box_price")
                        )
                    }

                    if (!priceInputModeBox) {
                        OutlinedTextField(
                            value = purchaseCprText,
                            onValueChange = { text ->
                                val cleaned = text.filter { it.isDigit() || it == '.' }
                                if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 7) {
                                    purchaseCprText = cleaned
                                }
                            },
                            label = { Text("Purchase Cost Basis / Rd (Optional)") },
                            placeholder = { Text("Defaults to market CPR") },
                            prefix = { Text("$") },
                            suffix = { Text("/ rd") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_purchase_cpr")
                        )
                    } else {
                        OutlinedTextField(
                            value = purchaseBoxPriceText,
                            onValueChange = { text ->
                                val cleaned = text.filter { it.isDigit() || it == '.' }
                                if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 8) {
                                    purchaseBoxPriceText = cleaned
                                }
                            },
                            label = { Text("Purchase Cost Paid / Box ($parsedBoxSize rds, Optional)") },
                            placeholder = { Text("Defaults to market box price") },
                            prefix = { Text("$") },
                            suffix = { Text("/ box") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_purchase_box_price")
                        )
                    }

                    OutlinedTextField(
                        value = buyTargetCprText,
                        onValueChange = { text ->
                            val cleaned = text.filter { it.isDigit() || it == '.' }
                            if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 7) {
                                buyTargetCprText = cleaned
                            }
                        },
                        label = { Text("Buy Target CPR (Optional Restock Alert)") },
                        placeholder = { Text("e.g., 0.45") },
                        prefix = { Text("$") },
                        suffix = { Text("/ rd") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_buy_target_cpr")
                    )

                    if (computedCpr != null && parsedRounds != null) {
                        val costPreview = computedPurchaseCpr?.let {
                            " · Cost Basis: ${formatCurrency(it * parsedRounds)}"
                        } ?: ""
                        Text(
                            text = "Calculated: ${formatCpr(computedCpr)} · Lot Value: ${formatCurrency(computedCpr * parsedRounds)}$costPreview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 4. Purchase, Storage, Lot Tracking & Notes
                FormSectionCard(title = "4. Purchase, Storage & Rifle Notes (Optional)") {
                    Text(
                        text = "Storage Locations / Ammo Cans",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val storageUnitOptions = remember(availableStorageUnits) {
                        (availableStorageUnits + StarterRiflePresets.commonStorageLocations)
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinctBy { it.lowercase() }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        storageUnitOptions.forEach { preset ->
                            FilterChip(
                                selected = storageLocation.equals(preset, ignoreCase = true),
                                onClick = {
                                    storageLocation = if (storageLocation.equals(preset, ignoreCase = true)) "" else preset
                                },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = storageLocation,
                        onValueChange = { storageLocation = it },
                        label = { Text("Storage Location / Ammo Can") },
                        placeholder = { Text("e.g., Safe Top Shelf, Ammo Can #1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_storage_location")
                    )

                    Text(
                        text = "Common Vendors",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StarterRiflePresets.commonVendors.forEach { preset ->
                            FilterChip(
                                selected = vendor.equals(preset, ignoreCase = true),
                                onClick = { vendor = if (vendor.equals(preset, ignoreCase = true)) "" else preset },
                                label = { Text(preset) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = vendor,
                        onValueChange = { vendor = it },
                        label = { Text("Vendor / Store") },
                        placeholder = { Text("e.g., MidwayUSA, SGAmmo, Local Gun Shop") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_vendor")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("input_purchase_date")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = purchaseDate?.let { "Purchased: ${formatPurchaseDate(it)}" }
                                    ?: "Select Purchase Date"
                            )
                        }
                        if (purchaseDate != null) {
                            IconButton(
                                onClick = { purchaseDate = null },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_purchase_date")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear purchase date"
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = lotNumber,
                        onValueChange = { lotNumber = it },
                        label = { Text("Manufacturer Lot Number") },
                        placeholder = { Text("e.g., F308GM1-112") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lot_number")
                    )

                    val scanTrailing: (@Composable () -> Unit)? = if (onScanUpcClick != null) {
                        {
                            IconButton(
                                onClick = onScanUpcClick,
                                modifier = Modifier.testTag("input_scan_upc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan box UPC barcode"
                                )
                            }
                        }
                    } else {
                        null
                    }
                    OutlinedTextField(
                        value = upcCode,
                        onValueChange = { text ->
                            val digits = text.filter(Char::isDigit)
                            if (digits.length <= 14) upcCode = digits
                        },
                        label = { Text("Box UPC / EAN Barcode") },
                        placeholder = { Text("e.g., 741569010115") },
                        trailingIcon = scanTrailing,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_upc_code")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Ballistic / Rifle Zero Notes") },
                        placeholder = { Text("e.g., 100 yd zero on 18\" SPR, 2710 fps avg") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notes")
                    )

                    PhotoAttachmentPickerCard(
                        photoUri = photoUri,
                        title = "Box / Lot Label / Receipt Photo",
                        subtitle = "Attach an optional photo stored privately on-device",
                        onPhotoChanged = { photoUri = it },
                        onPhotoTap = { showFullPhoto = true },
                        testTagPrefix = "lot_photo"
                    )
                }

                Button(
                    onClick = {
                        attemptedSubmit = true
                        if (canSave && parsedGrain != null && parsedRounds != null && computedCpr != null) {
                            val newEntry = AmmoEntry(
                                id = if (isEdit) existingEntry!!.id else 0L,
                                caliber = caliber.trim(),
                                brand = brand.trim(),
                                productLine = productLine.trim(),
                                grain = parsedGrain,
                                bulletType = bulletType.trim(),
                                currentCount = parsedRounds,
                                boxSize = parsedBoxSize,
                                lowStockThreshold = parsedLowThreshold,
                                marketPricePerRound = computedCpr,
                                costBasis = (computedPurchaseCpr ?: 0.0).coerceAtLeast(0.0),
                                targetBuyPricePerRound = parsedBuyTargetCpr,
                                casingMaterial = casingMaterial.trim(),
                                lotNumber = lotNumber.trim(),
                                upcCode = UpcCatalog.normalizeUpc(upcCode),
                                photoUri = photoUri.trim(),
                                vendor = vendor.trim(),
                                storageLocation = storageLocation.trim(),
                                purchaseDate = purchaseDate,
                                notes = notes.trim(),
                                createdAt = if (isEdit) existingEntry!!.createdAt else System.currentTimeMillis()
                            )
                            onSave(newEntry)
                        }
                    },
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("save_ammo_lot_button")
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isDuplicate -> "Save New Lot"
                            isEdit -> "Update Ammo Entry"
                            else -> "Save to Vault"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
