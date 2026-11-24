package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AmmoEntry
import com.example.data.StorageQrCodec
import com.example.data.UpcCatalog
import com.example.ui.components.CaliberPill
import com.example.ui.components.displayCpr
import com.example.ui.components.formatRounds
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcScannerScreen(
    allEntries: List<AmmoEntry>,
    onNavigateBack: () -> Unit,
    onQuickRestockBox: (AmmoEntry) -> Unit,
    onViewEntry: (Long) -> Unit,
    onCreateLotWithUpc: (String) -> Unit,
    onOpenStorageLocation: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var scannedCode by remember { mutableStateOf("") }
    var scannedCanLocation by remember { mutableStateOf<String?>(null) }
    var manualUpcInput by remember { mutableStateOf("") }
    var isScanningActive by remember { mutableStateOf(true) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            cameraError = "Camera permission denied. You can still enter or paste any box UPC manually below."
        } else {
            cameraError = null
        }
    }

    val activeUpc = remember(scannedCode) { UpcCatalog.normalizeUpc(scannedCode) }
    val matchingEntries = remember(activeUpc, allEntries) {
        UpcCatalog.findMatchingEntries(activeUpc, allEntries)
    }
    val builtInMatch = remember(activeUpc) {
        UpcCatalog.lookupBuiltIn(activeUpc)
    }
    val matchedCanEntries = remember(scannedCanLocation, allEntries) {
        val loc = scannedCanLocation ?: return@remember emptyList()
        allEntries.filter { it.storageLocation.trim().equals(loc, ignoreCase = true) }
    }
    val statusColors = LocalAmmoStatusColors.current

    fun submitCode(raw: String) {
        val decodedCan = StorageQrCodec.decodeCanQr(raw)
        if (decodedCan != null) {
            scannedCanLocation = decodedCan
            scannedCode = ""
            isScanningActive = false
            return
        }
        val normalized = UpcCatalog.normalizeUpc(raw)
        if (normalized.isNotEmpty()) {
            scannedCanLocation = null
            scannedCode = normalized
            manualUpcInput = normalized
            isScanningActive = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("UPC & Can QR Scanner") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("scanner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (activeUpc.isNotEmpty() || scannedCanLocation != null) {
                        IconButton(
                            onClick = {
                                scannedCode = ""
                                scannedCanLocation = null
                                manualUpcInput = ""
                                isScanningActive = true
                            },
                            modifier = Modifier.testTag("scanner_rescan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Scan another box or can QR"
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
                // Camera viewfinder card
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (hasCameraPermission && cameraError == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(Color.Black)
                        ) {
                            UpcCameraPreview(
                                isScanningActive = isScanningActive,
                                onBarcodeDetected = { raw -> submitCode(raw) },
                                onCameraError = { err -> cameraError = err },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Targeting reticle overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.76f)
                                    .height(106.dp)
                                    .background(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Surface(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (activeUpc.isNotEmpty()) {
                                            statusColors.healthy
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) {}
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.68f),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = when {
                                        scannedCanLocation != null -> "Can QR: $scannedCanLocation"
                                        isScanningActive -> "Align box UPC or Ammo Can QR inside frame"
                                        else -> "Locked UPC: $activeUpc · Tap Scan Again"
                                    },
                                    style = MonoTelemetrySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = if (cameraError != null) "Camera Unavailable" else "Point & Scan Ammo Box UPCs",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = cameraError
                                    ?: "Enable camera access to scan UPC-A / EAN-13 box barcodes on-device. No internet required.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    cameraError = null
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("grant_camera_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Camera Scanner")
                            }
                        }
                    }
                }

                // Manual UPC entry & quick-lookup fallback
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "MANUAL UPC LOOKUP",
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualUpcInput,
                                onValueChange = { text ->
                                    val digits = text.filter(Char::isDigit)
                                    if (digits.length <= 14) manualUpcInput = digits
                                },
                                label = { Text("12-digit UPC / EAN") },
                                placeholder = { Text("e.g., 741569010115") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { submitCode(manualUpcInput) }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_upc_input")
                            )
                            Button(
                                onClick = { submitCode(manualUpcInput) },
                                enabled = manualUpcInput.length >= 6,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("lookup_manual_upc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Look up UPC"
                                )
                            }
                        }

                        Text(
                            text = "Quick Sample UPCs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickSamples = listOf(
                                "PMC 5.56" to "741569010115",
                                "Fed .308 168gr" to "029465089986",
                                "Hornady 6.5 CM" to "090255815009",
                                "CCI .22 LR" to "076683000309"
                            )
                            quickSamples.forEach { (label, code) ->
                                AssistChip(
                                    onClick = { submitCode(code) },
                                    label = { Text(label) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }

                // Ammo Can QR Result Card
                if (scannedCanLocation != null) {
                    val canName = scannedCanLocation!!
                    val totalCanRounds = matchedCanEntries.sumOf { it.currentCount }
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, statusColors.healthy.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_can_qr_result_card")
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AMMO CAN QR DETECTED",
                                        style = MonoTelemetrySmall,
                                        color = statusColors.healthy
                                    )
                                    Text(
                                        text = canName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        scannedCanLocation = null
                                        isScanningActive = true
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text("Scan Again") }
                            }
                            Text(
                                text = "${matchedCanEntries.size} lot(s) · ${formatRounds(totalCanRounds)} rounds stored inside.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (onOpenStorageLocation != null) {
                                Button(
                                    onClick = { onOpenStorageLocation(canName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("scanner_open_can_button")
                                ) {
                                    Text("Open \"$canName\" Contents")
                                }
                            }
                        }
                    }
                }

                // Scan Results Section
                if (activeUpc.isNotEmpty()) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_result_card")
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
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = statusColors.healthy,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "SCANNED BARCODE",
                                            style = MonoTelemetrySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = activeUpc,
                                            style = MonoTelemetryMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        scannedCode = ""
                                        isScanningActive = true
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Scan Again")
                                }
                            }

                            if (matchingEntries.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "IN YOUR VAULT (${matchingEntries.size} ${if (matchingEntries.size == 1) "LOT" else "LOTS"})",
                                    style = MonoTelemetrySmall,
                                    color = statusColors.healthy
                                )
                                matchingEntries.forEach { matched ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CaliberPill(caliber = matched.caliber)
                                                Text(
                                                    text = "${formatRounds(matched.currentCount)} rds · ${displayCpr(matched.marketPricePerRound)}",
                                                    style = MonoTelemetrySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${matched.brand} ${matched.productLine} · ${matched.grain} gr ${matched.bulletType}".trim(),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onQuickRestockBox(matched) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("scanner_restock_box_${matched.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("+${matched.boxSize} rds (1 Box)")
                                                }
                                                OutlinedButton(
                                                    onClick = { onViewEntry(matched.id) },
                                                    modifier = Modifier
                                                        .heightIn(min = 48.dp)
                                                        .testTag("scanner_view_lot_${matched.id}")
                                                ) {
                                                    Text("View Lot")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (builtInMatch != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "FACTORY UPC SPEC MATCH",
                                    style = MonoTelemetrySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${builtInMatch.brand} ${builtInMatch.productLine} · ${builtInMatch.caliber} · ${builtInMatch.grain} gr ${builtInMatch.bulletType} (${builtInMatch.boxSize} rd box)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else if (matchingEntries.isEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "Not in built-in catalog yet. Save it once as a new lot and AmmoVault will automatically recognize this barcode on every future scan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onCreateLotWithUpc(activeUpc) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("scanner_add_lot_with_upc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        builtInMatch != null && matchingEntries.isEmpty() -> "Add Pre-Filled Lot"
                                        matchingEntries.isNotEmpty() -> "Add Another Lot with This UPC"
                                        else -> "Create New Lot with UPC"
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun UpcCameraPreview(
    isScanningActive: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val activeScanRef = remember { mutableStateOf(isScanningActive) }
    activeScanRef.value = isScanningActive

    DisposableEffect(lifecycleOwner, previewView) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        val barcodeScanner = BarcodeScanning.getClient(scannerOptions)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener(
            {
                runCatching {
                    val provider = cameraProviderFuture.get()
                    boundProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && activeScanRef.value) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                                    if (!value.isNullOrBlank() && activeScanRef.value) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            onBarcodeDetected(value)
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }.onFailure { err ->
                    onCameraError(err.localizedMessage ?: "No back camera available on this device.")
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            runCatching { boundProvider?.unbindAll() }
            runCatching { barcodeScanner.close() }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
