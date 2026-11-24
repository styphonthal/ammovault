package com.example

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AmmoDatabase
import com.example.data.AmmoSeekUrlBuilder
import com.example.data.RoomAmmoRepository
import com.example.data.StorageContainerSummary
import com.example.data.StorageQrCodec
import com.example.data.VaultBackupManager
import com.example.data.VaultBackupPayload
import com.example.data.VaultPdfReportGenerator
import com.example.data.VaultPreferences
import com.example.data.VaultPrivacySettings
import com.example.ui.AmmoViewModel
import com.example.ui.components.LocalHideDollarValues
import com.example.ui.components.LowThresholdVisualAlertBanner
import com.example.ui.components.VaultBackupDialog
import com.example.ui.components.VaultLockOverlay
import com.example.ui.components.VaultPrivacySettingsDialog
import com.example.ui.screens.AddEditLotScreen
import com.example.ui.screens.AmmoSeekBrowserScreen
import com.example.ui.screens.AppLoadingScreen
import com.example.ui.screens.CaliberOverviewScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.LotDetailScreen
import com.example.ui.screens.MarketStatsScreen
import com.example.ui.screens.RangeLogsScreen
import com.example.ui.screens.RestockScreen
import com.example.ui.screens.StorageDashboardScreen
import com.example.ui.screens.ThresholdConfigScreen
import com.example.ui.screens.UpcScannerScreen
import com.example.ui.theme.AmmoVaultTheme
import com.example.ui.theme.LocalAmmoStatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val ROUTE_CALIBERS = "calibers"
const val ROUTE_CATALOG = "catalog"
const val ROUTE_RANGE_LOGS = "range_logs"
const val ROUTE_RESTOCK = "restock"
const val ROUTE_MARKET = "market"
const val ROUTE_STORAGE = "storage"
const val ROUTE_THRESHOLDS = "thresholds"
const val ROUTE_UPC_SCANNER = "upc_scanner"
const val ROUTE_DETAIL = "detail/{lotId}"
const val ROUTE_ADD_EDIT = "add_edit?lotId={lotId}&caliber={caliber}&duplicate={duplicate}&upc={upc}"
const val ROUTE_AMMOSEEK = "ammoseek?lotId={lotId}&caliber={caliber}&url={url}"

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmmoVaultTheme {
                AmmoVaultApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmmoVaultApp() {
    val activityContext = LocalContext.current
    val hostActivity = activityContext as? FragmentActivity
    val context = activityContext.applicationContext
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        RoomAmmoRepository(AmmoDatabase.getInstance(context).ammoDao())
    }
    val preferences = remember(context) { VaultPreferences(context) }
    val privacySettingsOrNull by preferences.privacySettings.collectAsStateWithLifecycle(initialValue = null)
    val privacySettings = privacySettingsOrNull ?: VaultPrivacySettings()

    val viewModel: AmmoViewModel = viewModel(factory = AmmoViewModel.Factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuthenticateWithDevice = remember(activityContext) {
        BiometricManager.from(activityContext).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var lockErrorMessage by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    fun triggerBiometricUnlock() {
        val activity = hostActivity
        if (activity == null || !canAuthenticateWithDevice) {
            isVaultUnlocked = true
            return
        }
        lockErrorMessage = null
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isVaultUnlocked = true
                    lockErrorMessage = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        lockErrorMessage = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    lockErrorMessage = "Authentication failed. Please try again."
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock AmmoVault")
            .setSubtitle("Verify your identity to access your vault")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(promptInfo)
    }

    var splashMinElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(900L)
        splashMinElapsed = true
    }

    var showBackupDialog by remember { mutableStateOf(false) }
    var pendingImportPayload by remember { mutableStateOf<VaultBackupPayload?>(null) }
    var pendingQrContainer by remember { mutableStateOf<StorageContainerSummary?>(null) }
    var pendingRangeStorageUnit by remember { mutableStateOf<String?>(null) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        activityContext.contentResolver.openOutputStream(uri)?.use { stream ->
                            viewModel.exportBackupZip(stream)
                        } ?: error("Unable to open output ZIP file")
                    }
                }
                result.onSuccess { photoCount ->
                    val photoText = if (photoCount > 0) " + $photoCount photo(s)" else ""
                    viewModel.notifyMessage("Exported full ZIP backup (${uiState.allEntries.size} lots$photoText)")
                    showBackupDialog = false
                }.onFailure { err ->
                    viewModel.notifyMessage("ZIP backup failed: ${err.localizedMessage ?: "I/O error"}")
                }
            }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val snapshot = uiState
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        activityContext.contentResolver.openOutputStream(uri)?.use { stream ->
                            VaultPdfReportGenerator.generateValuationReport(
                                outputStream = stream,
                                entries = snapshot.allEntries,
                                caliberSummaries = snapshot.caliberSummaries,
                                storageSummaries = snapshot.storageSummaries,
                                totalRounds = snapshot.totalRounds,
                                totalCostBasis = snapshot.totalCostBasis,
                                totalMarketValue = snapshot.totalMarketValue,
                                weightedAvgCpr = snapshot.weightedAvgPricePerRound
                            )
                        } ?: error("Unable to open output PDF file")
                    }
                }
                result.onSuccess {
                    viewModel.notifyMessage("Saved Insurance Valuation PDF (${snapshot.allEntries.size} lots)")
                    showBackupDialog = false
                }.onFailure { err ->
                    viewModel.notifyMessage("PDF export failed: ${err.localizedMessage ?: "I/O error"}")
                }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val content = viewModel.exportBackupJson()
                        activityContext.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(content.toByteArray(Charsets.UTF_8))
                        } ?: error("Unable to open output file")
                    }
                }
                result.onSuccess {
                    viewModel.notifyMessage("Exported JSON backup (${uiState.allEntries.size} lots)")
                    showBackupDialog = false
                }.onFailure { err ->
                    viewModel.notifyMessage("Export failed: ${err.localizedMessage ?: "I/O error"}")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val content = viewModel.exportCatalogCsv()
                        activityContext.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(content.toByteArray(Charsets.UTF_8))
                        } ?: error("Unable to open output file")
                    }
                }
                result.onSuccess {
                    viewModel.notifyMessage("Exported CSV spreadsheet (${uiState.allEntries.size} lots)")
                    showBackupDialog = false
                }.onFailure { err ->
                    viewModel.notifyMessage("CSV export failed: ${err.localizedMessage ?: "I/O error"}")
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        activityContext.contentResolver.openInputStream(uri)?.use { stream ->
                            VaultBackupManager.importZipOrJsonBackup(context, stream).getOrThrow()
                        } ?: error("Unable to read selected file")
                    }
                }
                result.onSuccess { payload ->
                    pendingImportPayload = payload
                }.onFailure { err ->
                    viewModel.notifyMessage("Invalid backup file: ${err.localizedMessage ?: "Parse error"}")
                }
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val csvText = activityContext.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        } ?: error("Unable to read CSV file")
                        VaultBackupManager.parseFromCsv(csvText).getOrThrow()
                    }
                }
                result.onSuccess { payload ->
                    pendingImportPayload = payload
                }.onFailure { err ->
                    viewModel.notifyMessage("CSV import error: ${err.localizedMessage ?: "Parse error"}")
                }
            }
        }
    }

    val saveCanQrPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val container = pendingQrContainer
        pendingQrContainer = null
        if (uri != null && container != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = StorageQrCodec.renderPrintableCanLabel(container)
                        try {
                            activityContext.contentResolver.openOutputStream(uri)?.use { stream ->
                                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                                    error("Unable to encode PNG label")
                                }
                            } ?: error("Unable to open output PNG file")
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                result.onSuccess {
                    viewModel.notifyMessage("Saved printable QR label for ${container.locationName}")
                }.onFailure { err ->
                    viewModel.notifyMessage("QR label export failed: ${err.localizedMessage ?: "I/O error"}")
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showPrivacyDialog) {
        VaultPrivacySettingsDialog(
            hideDollarValues = privacySettings.hideDollarValues,
            biometricLockEnabled = privacySettings.biometricLockEnabled,
            canAuthenticateWithDevice = canAuthenticateWithDevice,
            onToggleHideDollars = { enabled ->
                scope.launch { preferences.setHideDollarValues(enabled) }
            },
            onToggleBiometricLock = { enabled ->
                scope.launch {
                    preferences.setBiometricLockEnabled(enabled)
                    isVaultUnlocked = true
                    viewModel.notifyMessage(
                        if (enabled) "Biometric vault lock enabled" else "Biometric vault lock disabled"
                    )
                }
            },
            onLockNowClick = {
                isVaultUnlocked = false
                lockErrorMessage = null
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showBackupDialog) {
        val dateStamp = remember {
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }
        VaultBackupDialog(
            entryCount = uiState.allEntries.size,
            historyCount = uiState.allPriceHistory.size,
            rangeSessionCount = uiState.rangeSessions.size,
            pendingImportPayload = pendingImportPayload,
            onDismiss = {
                pendingImportPayload = null
                showBackupDialog = false
            },
            onExportZipClick = {
                exportZipLauncher.launch("ammovault-full-backup-$dateStamp.zip")
            },
            onExportJsonClick = {
                exportJsonLauncher.launch("ammovault-backup-$dateStamp.json")
            },
            onExportCsvClick = {
                exportCsvLauncher.launch("ammovault-inventory-$dateStamp.csv")
            },
            onExportPdfClick = {
                exportPdfLauncher.launch("ammovault-valuation-report-$dateStamp.pdf")
            },
            onPickImportFileClick = {
                importBackupLauncher.launch(arrayOf("application/zip", "application/json", "text/plain", "*/*"))
            },
            onPickImportCsvClick = {
                importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
            },
            onConfirmImportMerge = {
                pendingImportPayload?.let { payload ->
                    viewModel.importBackup(payload, replaceExisting = false)
                }
                pendingImportPayload = null
                showBackupDialog = false
            },
            onConfirmImportReplace = {
                pendingImportPayload?.let { payload ->
                    viewModel.importBackup(payload, replaceExisting = true)
                }
                pendingImportPayload = null
                showBackupDialog = false
            },
            onCancelPendingImport = {
                pendingImportPayload = null
            }
        )
    }

    val topDestinations = listOf(
        TopLevelDestination(
            route = ROUTE_CALIBERS,
            label = stringResource(id = R.string.nav_calibers),
            selectedIcon = Icons.Filled.Inventory2,
            unselectedIcon = Icons.Outlined.Inventory2,
            testTag = "nav_tab_calibers"
        ),
        TopLevelDestination(
            route = ROUTE_CATALOG,
            label = stringResource(id = R.string.nav_catalog),
            selectedIcon = Icons.AutoMirrored.Filled.ViewList,
            unselectedIcon = Icons.AutoMirrored.Outlined.ViewList,
            testTag = "nav_tab_catalog"
        ),
        TopLevelDestination(
            route = ROUTE_RANGE_LOGS,
            label = stringResource(id = R.string.nav_range_logs),
            selectedIcon = Icons.Filled.EventNote,
            unselectedIcon = Icons.Outlined.EventNote,
            testTag = "nav_tab_range_logs"
        ),
        TopLevelDestination(
            route = ROUTE_RESTOCK,
            label = stringResource(id = R.string.nav_restock),
            selectedIcon = Icons.Filled.ShoppingCart,
            unselectedIcon = Icons.Outlined.ShoppingCart,
            testTag = "nav_tab_restock"
        ),
        TopLevelDestination(
            route = ROUTE_MARKET,
            label = stringResource(id = R.string.nav_market),
            selectedIcon = Icons.Filled.Assessment,
            unselectedIcon = Icons.Outlined.Assessment,
            testTag = "nav_tab_market"
        )
    )

    val isTopLevel = currentRoute in listOf(
        ROUTE_CALIBERS,
        ROUTE_CATALOG,
        ROUTE_RANGE_LOGS,
        ROUTE_RESTOCK,
        ROUTE_MARKET
    )
    val belowMinAlertCount = uiState.belowMinThresholdAlerts.size
    val burnOnlyCount = uiState.burnRateRestockAlerts.count { burn ->
        uiState.belowMinThresholdAlerts.none { it.caliber == burn.caliber }
    }
    val restockAttentionCount = uiState.restockLots.size + belowMinAlertCount + burnOnlyCount

    var dismissedAlertSignature by remember { mutableStateOf<String?>(null) }
    val currentAlertSignature = remember(uiState.belowMinThresholdAlerts) {
        uiState.belowMinThresholdAlerts.joinToString("|") {
            "${it.caliber}:${it.currentRounds}:${it.minThresholdRounds}"
        }
    }
    val showVisualAlertBanner = isTopLevel &&
        uiState.belowMinThresholdAlerts.isNotEmpty() &&
        dismissedAlertSignature != currentAlertSignature

    fun navigateToAdd(caliber: String? = null, upc: String? = null) {
        val encodedCaliber = caliber?.let { android.net.Uri.encode(it) } ?: ""
        val encodedUpc = upc?.let { android.net.Uri.encode(it) } ?: ""
        navController.navigate("add_edit?lotId=0&caliber=$encodedCaliber&duplicate=false&upc=$encodedUpc")
    }

    fun navigateToEdit(lotId: Long) {
        navController.navigate("add_edit?lotId=$lotId&caliber=&duplicate=false&upc=")
    }

    fun navigateToDuplicate(lotId: Long) {
        navController.navigate("add_edit?lotId=$lotId&caliber=&duplicate=true&upc=")
    }

    fun navigateToScanner() {
        navController.navigate(ROUTE_UPC_SCANNER)
    }

    fun navigateToStorageDashboard() {
        navController.navigate(ROUTE_STORAGE)
    }

    fun navigateToThresholdConfig() {
        navController.navigate(ROUTE_THRESHOLDS)
    }

    fun navigateToDetail(lotId: Long) {
        navController.navigate("detail/$lotId")
    }

    fun navigateToAmmoSeek(lotId: Long, caliber: String, url: String) {
        val encodedCaliber = android.net.Uri.encode(caliber)
        val encodedUrl = android.net.Uri.encode(url)
        navController.navigate("ammoseek?lotId=$lotId&caliber=$encodedCaliber&url=$encodedUrl")
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showLoadingPage = uiState.isLoading || !splashMinElapsed || privacySettingsOrNull == null
    val requiresUnlock = !showLoadingPage &&
        privacySettings.biometricLockEnabled &&
        canAuthenticateWithDevice &&
        !isVaultUnlocked

    LaunchedEffect(requiresUnlock) {
        if (requiresUnlock) {
            triggerBiometricUnlock()
        }
    }

    CompositionLocalProvider(LocalHideDollarValues provides privacySettings.hideDollarValues) {
        Crossfade(
            targetState = when {
                showLoadingPage -> 0
                requiresUnlock -> 1
                else -> 2
            },
            animationSpec = tween(durationMillis = 320),
            label = "app_loading_crossfade"
        ) { stage ->
            when (stage) {
                0 -> AppLoadingScreen()
                1 -> VaultLockOverlay(
                    errorMessage = lockErrorMessage,
                    onUnlockClick = { triggerBiometricUnlock() }
                )
                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useNavRail = maxWidth >= 680.dp

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            contentWindowInsets = WindowInsets.statusBars,
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier.navigationBarsPadding()
                                )
                            },
                            topBar = {
                                if (isTopLevel) {
                                    val statusColors = LocalAmmoStatusColors.current
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = when (currentRoute) {
                                                    ROUTE_CATALOG -> "Ammo Catalog"
                                                    ROUTE_RANGE_LOGS -> "Range & Bag"
                                                    ROUTE_RESTOCK -> "Restock"
                                                    ROUTE_MARKET -> "Market Stats"
                                                    else -> stringResource(id = R.string.app_name)
                                                },
                                                style = MaterialTheme.typography.titleLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = { showPrivacyDialog = true },
                                                modifier = Modifier.testTag("top_bar_lock_settings_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (privacySettings.biometricLockEnabled) {
                                                        Icons.Filled.Lock
                                                    } else {
                                                        Icons.Outlined.LockOpen
                                                    },
                                                    contentDescription = "Privacy and vault lock settings",
                                                    tint = if (privacySettings.biometricLockEnabled) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = { navigateToThresholdConfig() },
                                                modifier = Modifier.testTag("top_bar_thresholds_button")
                                            ) {
                                                if (belowMinAlertCount > 0) {
                                                    val hasDepleted = uiState.belowMinThresholdAlerts.any { it.currentRounds <= 0 }
                                                    val alertTint = if (hasDepleted) statusColors.critical else statusColors.low
                                                    BadgedBox(
                                                        badge = {
                                                            Badge(containerColor = alertTint) {
                                                                Text(belowMinAlertCount.toString())
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.NotificationsActive,
                                                            contentDescription = "Minimum stock thresholds ($belowMinAlertCount active alerts)",
                                                            tint = alertTint
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Notifications,
                                                        contentDescription = "Configure minimum stock thresholds",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        preferences.setHideDollarValues(!privacySettings.hideDollarValues)
                                                    }
                                                },
                                                modifier = Modifier.testTag("top_bar_privacy_toggle")
                                            ) {
                                                Icon(
                                                    imageVector = if (privacySettings.hideDollarValues) {
                                                        Icons.Filled.VisibilityOff
                                                    } else {
                                                        Icons.Filled.Visibility
                                                    },
                                                    contentDescription = if (privacySettings.hideDollarValues) {
                                                        "Show dollar values"
                                                    } else {
                                                        "Hide dollar values"
                                                    },
                                                    tint = if (privacySettings.hideDollarValues) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                            IconButton(
                                                onClick = { navigateToScanner() },
                                                modifier = Modifier.testTag("top_bar_scan_upc_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.QrCodeScanner,
                                                    contentDescription = "Scan UPC barcode or ammo can QR"
                                                )
                                            }
                                            IconButton(
                                                onClick = { showBackupDialog = true },
                                                modifier = Modifier.testTag("top_bar_backup_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ImportExport,
                                                    contentDescription = "Backup, export, PDF report, or restore vault"
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }
                            },
                    bottomBar = {
                        if (isTopLevel && !useNavRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                topDestinations.forEach { dest ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopTab(dest.route) },
                                        icon = {
                                            val showBadge = dest.route == ROUTE_RESTOCK && restockAttentionCount > 0
                                            if (showBadge) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge {
                                                            Text(restockAttentionCount.toString())
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            }
                                        },
                                        label = { Text(dest.label) },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (isTopLevel && !useNavRail) {
                            ExtendedFloatingActionButton(
                                onClick = { navigateToAdd(uiState.selectedCaliberFilter) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                icon = {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                },
                                text = { Text(stringResource(id = R.string.action_add_ammo)) },
                                modifier = Modifier.testTag("fab_add_ammo")
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isTopLevel && useNavRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                header = {
                                    FloatingActionButton(
                                        onClick = { navigateToAdd(uiState.selectedCaliberFilter) },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add ammo")
                                    }
                                }
                            ) {
                                topDestinations.forEach { dest ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTopTab(dest.route) },
                                        icon = {
                                            val showBadge = dest.route == ROUTE_RESTOCK && restockAttentionCount > 0
                                            if (showBadge) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge {
                                                            Text(restockAttentionCount.toString())
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            }
                                        },
                                        label = { Text(dest.label) },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                            AnimatedVisibility(
                                visible = showVisualAlertBanner,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                LowThresholdVisualAlertBanner(
                                    alerts = uiState.belowMinThresholdAlerts,
                                    onOpenRestock = { navigateTopTab(ROUTE_RESTOCK) },
                                    onOpenThresholdConfig = { navigateToThresholdConfig() },
                                    onDismiss = { dismissedAlertSignature = currentAlertSignature }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_CALIBERS
                                ) {
                                composable(ROUTE_CALIBERS) {
                                    CaliberOverviewScreen(
                                        uiState = uiState,
                                        onCaliberClick = { caliber ->
                                            viewModel.selectCaliberFilter(caliber)
                                            navigateTopTab(ROUTE_CATALOG)
                                        },
                                        onSetCaliberTarget = { caliber, target, buyTarget, minThreshold ->
                                            viewModel.setCaliberTarget(caliber, target, buyTarget, minThreshold)
                                        },
                                        onRenameCaliber = viewModel::renameCaliber,
                                        onDeleteCaliber = viewModel::deleteCaliberInventory,
                                        onLowStockBannerClick = { navigateTopTab(ROUTE_RESTOCK) },
                                        onOpenStorageDashboard = { navigateToStorageDashboard() },
                                        onOpenThresholdConfig = { navigateToThresholdConfig() },
                                        onOpenMarketStats = { navigateTopTab(ROUTE_MARKET) },
                                        onAddAmmoClick = { navigateToAdd() },
                                        onLoadStarterClick = { viewModel.loadStarterEntries() }
                                    )
                                }

                                composable(ROUTE_THRESHOLDS) {
                                    ThresholdConfigScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onSetMinThreshold = viewModel::setCaliberMinThreshold,
                                        onApplySmartDefaults = viewModel::applyDefaultMinThresholds,
                                        onRemoveThresholdOnlyCaliber = viewModel::removeThresholdOnlyCaliber,
                                        onCheckAmmoSeekCaliber = { caliber ->
                                            navigateToAmmoSeek(
                                                lotId = 0L,
                                                caliber = caliber,
                                                url = AmmoSeekUrlBuilder.buildCaliberSearchUrl(caliber)
                                            )
                                        },
                                        onInspectCaliberInCatalog = { caliber ->
                                            viewModel.selectCaliberFilter(caliber)
                                            navController.popBackStack()
                                            navigateTopTab(ROUTE_CATALOG)
                                        }
                                    )
                                }

                                composable(ROUTE_CATALOG) {
                                    CatalogScreen(
                                        uiState = uiState,
                                        onSearchChange = viewModel::updateSearchQuery,
                                        onSelectCaliber = viewModel::selectCaliberFilter,
                                        onSelectLocation = viewModel::selectLocationFilter,
                                        onToggleLowStock = viewModel::toggleLowStockOnly,
                                        onSelectSort = viewModel::setSortOption,
                                        onAdjustRounds = viewModel::adjustRounds,
                                        onSetExactRounds = viewModel::setRoundsOnHand,
                                        onEntryClick = ::navigateToDetail,
                                        onAddAmmoClick = { navigateToAdd(uiState.selectedCaliberFilter) },
                                        onLoadStarterClick = { viewModel.loadStarterEntries() },
                                        onOpenStorageDashboard = { navigateToStorageDashboard() }
                                    )
                                }

                                composable(ROUTE_STORAGE) {
                                    StorageDashboardScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onOpenLocationInCatalog = { location ->
                                            viewModel.selectCaliberFilter(null)
                                            viewModel.selectLocationFilter(location)
                                            navController.popBackStack()
                                            navigateTopTab(ROUTE_CATALOG)
                                        },
                                        onAddStorageUnit = viewModel::addStorageUnit,
                                        onRenameStorageUnit = viewModel::renameStorageUnit,
                                        onDeleteStorageUnit = viewModel::deleteStorageUnit,
                                        onUseStorageAtRange = { unitName ->
                                            pendingRangeStorageUnit = unitName
                                            navController.popBackStack()
                                            navigateTopTab(ROUTE_RANGE_LOGS)
                                        },
                                        onScanCanQrClick = { navigateToScanner() },
                                        onSaveCanQrPng = { container ->
                                            pendingQrContainer = container
                                            val safeSlug = container.locationName
                                                .lowercase(Locale.US)
                                                .replace(Regex("[^a-z0-9]+"), "-")
                                                .trim('-')
                                                .ifBlank { "ammo-can" }
                                            saveCanQrPngLauncher.launch("ammovault-qr-$safeSlug.png")
                                        }
                                    )
                                }

                                composable(ROUTE_RANGE_LOGS) {
                                    RangeLogsScreen(
                                        uiState = uiState,
                                        onSelectRangeBag = viewModel::selectRangeBag,
                                        onPackLotInBag = viewModel::packLotInRangeBag,
                                        onPackStorageUnitInBag = viewModel::packStorageUnitInRangeBag,
                                        onRemoveBagItem = viewModel::removeRangeBagItem,
                                        onClearBag = viewModel::clearRangeBag,
                                        onLogRangeSession = { session, usages, clearBag ->
                                            viewModel.logRangeSession(
                                                session = session,
                                                usages = usages,
                                                clearSourceBag = clearBag
                                            )
                                        },
                                        onDeleteRangeSession = viewModel::deleteRangeSession,
                                        onEntryClick = ::navigateToDetail,
                                        initialStorageUnitForLog = pendingRangeStorageUnit,
                                        onConsumeInitialStorageUnit = { pendingRangeStorageUnit = null },
                                        onOpenStorageDashboard = { navigateToStorageDashboard() }
                                    )
                                }

                                composable(ROUTE_UPC_SCANNER) {
                                    UpcScannerScreen(
                                        allEntries = uiState.allEntries,
                                        onNavigateBack = { navController.popBackStack() },
                                        onQuickRestockBox = { matched ->
                                            viewModel.adjustRounds(matched, matched.boxSize)
                                        },
                                        onViewEntry = { id ->
                                            navigateToDetail(id)
                                        },
                                        onCreateLotWithUpc = { scannedUpc ->
                                            navigateToAdd(upc = scannedUpc)
                                        },
                                        onOpenStorageLocation = { canLocation ->
                                            viewModel.selectCaliberFilter(null)
                                            viewModel.selectLocationFilter(canLocation)
                                            navController.popBackStack()
                                            navigateTopTab(ROUTE_CATALOG)
                                        }
                                    )
                                }

                                composable(ROUTE_RESTOCK) {
                                    RestockScreen(
                                        uiState = uiState,
                                        onCheckAmmoSeekEntry = { entry ->
                                            navigateToAmmoSeek(
                                                lotId = entry.id,
                                                caliber = entry.caliber,
                                                url = AmmoSeekUrlBuilder.buildSearchUrl(entry)
                                            )
                                        },
                                        onCheckAmmoSeekCaliber = { caliber ->
                                            navigateToAmmoSeek(
                                                lotId = 0L,
                                                caliber = caliber,
                                                url = AmmoSeekUrlBuilder.buildCaliberSearchUrl(caliber)
                                            )
                                        },
                                        onAdjustRounds = viewModel::adjustRounds,
                                        onUpdateEntryBuyTarget = viewModel::updateEntryBuyTarget,
                                        onSetCaliberTarget = { caliber, target, buyTarget, minThreshold ->
                                            viewModel.setCaliberTarget(caliber, target, buyTarget, minThreshold)
                                        },
                                        onRenameCaliber = viewModel::renameCaliber,
                                        onDeleteCaliber = viewModel::deleteCaliberInventory,
                                        onEntryClick = ::navigateToDetail,
                                        onAddAmmoClick = { navigateToAdd() },
                                        onLoadStarterClick = { viewModel.loadStarterEntries() },
                                        onOpenThresholdConfig = { navigateToThresholdConfig() }
                                    )
                                }

                                composable(ROUTE_MARKET) {
                                    MarketStatsScreen(
                                        uiState = uiState,
                                        onUpdateMarketPrice = viewModel::updateMarketPrice,
                                        onCheckAmmoSeek = { entry ->
                                            navigateToAmmoSeek(
                                                lotId = entry.id,
                                                caliber = entry.caliber,
                                                url = AmmoSeekUrlBuilder.buildSearchUrl(entry)
                                            )
                                        },
                                        onCheckAmmoSeekUrl = { entry, customUrl ->
                                            navigateToAmmoSeek(
                                                lotId = entry.id,
                                                caliber = entry.caliber,
                                                url = customUrl
                                            )
                                        },
                                        onEntryClick = ::navigateToDetail,
                                        onAddAmmoClick = { navigateToAdd() },
                                        onLoadStarterClick = { viewModel.loadStarterEntries() },
                                        onOpenRangeLogsClick = { navigateTopTab(ROUTE_RANGE_LOGS) }
                                    )
                                }

                                composable(
                                    route = ROUTE_DETAIL,
                                    arguments = listOf(
                                        navArgument("lotId") { type = NavType.LongType }
                                    )
                                ) { backStackEntry ->
                                    val entryId = backStackEntry.arguments?.getLong("lotId") ?: 0L
                                    val entry by viewModel.observeEntry(entryId)
                                        .collectAsStateWithLifecycle(initialValue = uiState.allEntries.find { it.id == entryId })
                                    val priceHistory by viewModel.observePriceHistory(entryId)
                                        .collectAsStateWithLifecycle(initialValue = uiState.priceHistoryByEntryId[entryId].orEmpty())
                                    val caliberDefaultBuyTarget = entry?.let { current ->
                                        uiState.caliberTargets.find { it.caliber == current.caliber }?.targetBuyPricePerRound
                                    }

                                    LotDetailScreen(
                                        entry = entry,
                                        priceHistory = priceHistory,
                                        caliberDefaultBuyTarget = caliberDefaultBuyTarget,
                                        isFifoOldest = entryId in uiState.fifoOldestLotIds,
                                        reliabilityStats = uiState.lotReliabilityByEntryId[entryId],
                                        onNavigateBack = { navController.popBackStack() },
                                        onEditClick = { id -> navigateToEdit(id) },
                                        onDuplicateClick = { id -> navigateToDuplicate(id) },
                                        onAdjustRounds = viewModel::adjustRounds,
                                        onSetExactRounds = viewModel::setRoundsOnHand,
                                        onUpdateMarketPrice = viewModel::updateMarketPrice,
                                        onUpdateBuyTarget = viewModel::updateEntryBuyTarget,
                                        onCheckAmmoSeek = { target ->
                                            navigateToAmmoSeek(
                                                lotId = target.id,
                                                caliber = target.caliber,
                                                url = AmmoSeekUrlBuilder.buildSearchUrl(target)
                                            )
                                        },
                                        onCheckAmmoSeekUrl = { target, customUrl ->
                                            navigateToAmmoSeek(
                                                lotId = target.id,
                                                caliber = target.caliber,
                                                url = customUrl
                                            )
                                        },
                                        onDeleteEntry = { toDelete ->
                                            viewModel.deleteEntry(toDelete) {
                                                navController.popBackStack()
                                            }
                                        },
                                        rangeBagNames = uiState.rangeBagNames,
                                        defaultRangeBagName = uiState.selectedRangeBagName,
                                        onPackInRangeBag = viewModel::packLotInRangeBag
                                    )
                                }

                                composable(
                                    route = ROUTE_AMMOSEEK,
                                    arguments = listOf(
                                        navArgument("lotId") {
                                            type = NavType.LongType
                                            defaultValue = 0L
                                        },
                                        navArgument("caliber") {
                                            type = NavType.StringType
                                            defaultValue = ""
                                        },
                                        navArgument("url") {
                                            type = NavType.StringType
                                            defaultValue = ""
                                        }
                                    )
                                ) { backStackEntry ->
                                    val lotId = backStackEntry.arguments?.getLong("lotId") ?: 0L
                                    val caliberArg = backStackEntry.arguments?.getString("caliber").orEmpty()
                                    val initialUrl = backStackEntry.arguments?.getString("url").orEmpty()
                                        .ifBlank { AmmoSeekUrlBuilder.buildCaliberSearchUrl(caliberArg) }
                                    val targetEntry = remember(lotId, uiState.allEntries) {
                                        if (lotId > 0L) uiState.allEntries.find { it.id == lotId } else null
                                    }
                                    val effectiveCaliber = targetEntry?.caliber ?: caliberArg
                                    val caliberEntries = remember(effectiveCaliber, uiState.allEntries) {
                                        uiState.allEntries.filter { it.caliber == effectiveCaliber }
                                    }
                                    val caliberDefaultBuyTarget = remember(effectiveCaliber, uiState.caliberTargets) {
                                        uiState.caliberTargets.find { it.caliber == effectiveCaliber }?.targetBuyPricePerRound
                                    }

                                    AmmoSeekBrowserScreen(
                                        initialUrl = initialUrl,
                                        caliberLabel = effectiveCaliber.ifBlank { "AmmoSeek" },
                                        targetEntry = targetEntry,
                                        caliberEntries = caliberEntries,
                                        caliberDefaultBuyTarget = caliberDefaultBuyTarget,
                                        onNavigateBack = { navController.popBackStack() },
                                        onSaveMarketPrice = viewModel::updateMarketPrice,
                                        onLaunchError = viewModel::notifyMessage
                                    )
                                }

                                composable(
                                    route = ROUTE_ADD_EDIT,
                                    arguments = listOf(
                                        navArgument("lotId") {
                                            type = NavType.LongType
                                            defaultValue = 0L
                                        },
                                        navArgument("caliber") {
                                            type = NavType.StringType
                                            defaultValue = ""
                                        },
                                        navArgument("duplicate") {
                                            type = NavType.BoolType
                                            defaultValue = false
                                        },
                                        navArgument("upc") {
                                            type = NavType.StringType
                                            defaultValue = ""
                                        }
                                    )
                                ) { backStackEntry ->
                                    val entryId = backStackEntry.arguments?.getLong("lotId") ?: 0L
                                    val caliberArg = backStackEntry.arguments?.getString("caliber")
                                        ?.takeIf { it.isNotBlank() }
                                    val isDuplicate = backStackEntry.arguments?.getBoolean("duplicate") ?: false
                                    val upcArg = backStackEntry.arguments?.getString("upc")
                                        ?.takeIf { it.isNotBlank() }
                                    val existingEntry = remember(entryId, uiState.allEntries) {
                                        if (entryId > 0L) uiState.allEntries.find { it.id == entryId } else null
                                    }

                                    AddEditLotScreen(
                                        existingEntry = existingEntry,
                                        defaultCaliber = caliberArg,
                                        isDuplicate = isDuplicate,
                                        initialUpc = upcArg,
                                        availableStorageUnits = uiState.allStorageUnitNames,
                                        onScanUpcClick = { navigateToScanner() },
                                        onNavigateBack = { navController.popBackStack() },
                                        onSave = { savedEntry ->
                                            viewModel.saveEntry(savedEntry) {
                                                navController.popBackStack()
                                            }
                                        }
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}
