package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AmmoEntry
import com.example.data.AmmoSeekUrlBuilder
import com.example.data.PriceClipboardParser
import com.example.data.PriceUnitMode
import com.example.ui.components.formatCpr
import com.example.ui.components.openBrowserUrl
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetrySmall
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmmoSeekBrowserScreen(
    initialUrl: String,
    caliberLabel: String,
    targetEntry: AmmoEntry?,
    caliberEntries: List<AmmoEntry>,
    caliberDefaultBuyTarget: Double?,
    onNavigateBack: () -> Unit,
    onSaveMarketPrice: (AmmoEntry, Double, String) -> Unit,
    onLaunchError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val statusColors = LocalAmmoStatusColors.current

    val selectableLots = remember(targetEntry, caliberEntries) {
        when {
            targetEntry != null -> listOf(targetEntry)
            else -> caliberEntries
        }
    }
    var activeEntry by remember(targetEntry?.id, caliberEntries) {
        mutableStateOf(targetEntry ?: caliberEntries.firstOrNull())
    }

    var currentUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Docked Price Capture State
    var unitMode by remember(activeEntry?.id) { mutableStateOf(PriceUnitMode.CENTS_PER_ROUND) }
    var priceInput by remember(activeEntry?.id) {
        mutableStateOf(
            activeEntry?.let { entry ->
                PriceClipboardParser.formatValueForMode(entry.marketPricePerRound, PriceUnitMode.CENTS_PER_ROUND, entry.boxSize)
            } ?: ""
        )
    }
    var clipboardFeedback by remember(activeEntry?.id) { mutableStateOf<String?>(null) }

    val boxSize = activeEntry?.boxSize ?: 20
    val parsedCpr = PriceClipboardParser.computeCpr(priceInput, unitMode, boxSize)
    val canSave = activeEntry != null && parsedCpr != null && parsedCpr in 0.0..100.0

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    val entryForLinks = activeEntry
    val grainUrl = remember(entryForLinks, caliberLabel) {
        if (entryForLinks != null) {
            AmmoSeekUrlBuilder.buildSearchUrl(entryForLinks)
        } else {
            AmmoSeekUrlBuilder.buildCaliberSearchUrl(caliberLabel)
        }
    }
    val brandUrl = remember(entryForLinks) {
        entryForLinks?.let { AmmoSeekUrlBuilder.buildBrandSearchUrl(it) }
    }
    val caliberUrl = remember(entryForLinks, caliberLabel) {
        AmmoSeekUrlBuilder.buildCaliberSearchUrl(entryForLinks?.caliber ?: caliberLabel)
    }
    val brandKeyword = remember(entryForLinks?.brand) {
        entryForLinks?.let { AmmoSeekUrlBuilder.normalizeBrandKeyword(it.brand) }.orEmpty()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = entryForLinks?.let { "${it.caliber} · ${it.grain} gr" } ?: caliberLabel,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "AmmoSeek Live Lookup",
                                style = MonoTelemetrySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("browser_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to AmmoVault"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.testTag("browser_reload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reload page"
                            )
                        }
                        IconButton(
                            onClick = { openBrowserUrl(context, currentUrl, onLaunchError) },
                            modifier = Modifier.testTag("browser_open_external_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in external browser"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Quick filter strip for switching AmmoSeek filter scope in-place
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entryForLinks != null && entryForLinks.grain > 0) {
                        FilterChip(
                            selected = currentUrl == grainUrl,
                            onClick = {
                                currentUrl = grainUrl
                                webViewRef?.loadUrl(grainUrl)
                            },
                            label = { Text("${entryForLinks.grain} gr", style = MonoTelemetrySmall) },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                    if (brandUrl != null && brandKeyword.isNotEmpty() && brandUrl != grainUrl) {
                        FilterChip(
                            selected = currentUrl == brandUrl,
                            onClick = {
                                currentUrl = brandUrl
                                webViewRef?.loadUrl(brandUrl)
                            },
                            label = { Text("+ $brandKeyword", style = MonoTelemetrySmall) },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                    FilterChip(
                        selected = currentUrl == caliberUrl,
                        onClick = {
                            currentUrl = caliberUrl
                            webViewRef?.loadUrl(caliberUrl)
                        },
                        label = { Text("All ${entryForLinks?.caliber ?: caliberLabel}", style = MonoTelemetrySmall) },
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (pageProgress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (targetEntry == null && selectableLots.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectableLots.forEach { lot ->
                                FilterChip(
                                    selected = activeEntry?.id == lot.id,
                                    onClick = { activeEntry = lot },
                                    label = {
                                        Text(
                                            text = "${lot.brand} ${lot.grain}gr",
                                            style = MonoTelemetrySmall,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (activeEntry != null) {
                        val lot = activeEntry!!
                        val buyTarget = lot.targetBuyPricePerRound ?: caliberDefaultBuyTarget
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lot.brand} · Now ${formatCpr(lot.marketPricePerRound)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (buyTarget != null) {
                                val atTarget = parsedCpr != null && parsedCpr <= buyTarget + 0.0001
                                Text(
                                    text = "Target: ${formatCpr(buyTarget)}",
                                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (atTarget) statusColors.healthy else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Unit toggle row: ¢/rd (matches AmmoSeek display) | $/rd | Box $
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PriceUnitMode.entries.forEach { mode ->
                                val modeLabel = if (mode == PriceUnitMode.BOX_TOTAL) "Box (${lot.boxSize})" else mode.label
                                FilterChip(
                                    selected = unitMode == mode,
                                    onClick = {
                                        if (unitMode != mode) {
                                            val currentCpr = PriceClipboardParser.computeCpr(priceInput, unitMode, lot.boxSize)
                                            unitMode = mode
                                            if (currentCpr != null) {
                                                priceInput = PriceClipboardParser.formatValueForMode(currentCpr, mode, lot.boxSize)
                                            }
                                            clipboardFeedback = null
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = modeLabel,
                                            style = MonoTelemetrySmall,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .testTag("browser_unit_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val prefixContent: (@Composable () -> Unit)? = if (unitMode != PriceUnitMode.CENTS_PER_ROUND) {
                                { Text("$") }
                            } else {
                                null
                            }
                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { text ->
                                    val cleaned = text.filter { it.isDigit() || it == '.' }
                                    if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 8) {
                                        priceInput = cleaned
                                        clipboardFeedback = null
                                    }
                                },
                                placeholder = {
                                    Text(
                                        when (unitMode) {
                                            PriceUnitMode.CENTS_PER_ROUND -> "e.g. 47.5"
                                            PriceUnitMode.DOLLARS_PER_ROUND -> "e.g. 0.48"
                                            PriceUnitMode.BOX_TOTAL -> "e.g. 28.99"
                                        }
                                    )
                                },
                                prefix = prefixContent,
                                suffix = {
                                    Text(
                                        when (unitMode) {
                                            PriceUnitMode.CENTS_PER_ROUND -> "¢/rd"
                                            PriceUnitMode.DOLLARS_PER_ROUND -> "$/rd"
                                            PriceUnitMode.BOX_TOTAL -> "/box"
                                        }
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text.orEmpty()
                                            val parsed = PriceClipboardParser.parsePriceFromText(clipText, lot.boxSize)
                                            if (parsed != null) {
                                                unitMode = parsed.unitMode
                                                priceInput = parsed.displayInputValue
                                                clipboardFeedback = parsed.summary
                                            } else {
                                                clipboardFeedback = if (clipText.isBlank()) {
                                                    "Copy a price from the page, then tap Paste"
                                                } else {
                                                    "No price found in clipboard"
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("browser_paste_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentPaste,
                                            contentDescription = "Paste copied price"
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browser_price_input")
                            )

                            Button(
                                onClick = {
                                    if (parsedCpr != null) {
                                        onSaveMarketPrice(lot, parsedCpr, "AmmoSeek Check")
                                        onNavigateBack()
                                    }
                                },
                                enabled = canSave,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("browser_save_price_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }
                        }

                        val helperText = clipboardFeedback ?: parsedCpr?.let {
                            "Saves ${formatCpr(it)} (${String.format(Locale.US, "$%.2f", it * lot.boxSize)} / ${lot.boxSize}-rd box)"
                        }
                        if (helperText != null) {
                            Text(
                                text = helperText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (clipboardFeedback != null) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    } else {
                        Text(
                            text = "No lots in $caliberLabel yet. Open in browser or add a lot to track CPR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    createConfiguredWebView(
                        context = ctx,
                        initialUrl = initialUrl,
                        onUrlChanged = { updatedUrl, backAllowed ->
                            currentUrl = updatedUrl
                            canGoBack = backAllowed
                        },
                        onLoadingChanged = { loading ->
                            isLoading = loading
                        },
                        onProgressChanged = { progress ->
                            pageProgress = progress
                        }
                    ).also { created ->
                        webViewRef = created
                    }
                },
                update = { webView ->
                    canGoBack = webView.canGoBack()
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: android.content.Context,
    initialUrl: String,
    onUrlChanged: (String, Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    false
                } else {
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onLoadingChanged(true)
                if (url != null) {
                    onUrlChanged(url, view?.canGoBack() ?: false)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onLoadingChanged(false)
                if (url != null) {
                    onUrlChanged(url, view?.canGoBack() ?: false)
                }
            }
        }
        loadUrl(initialUrl)
    }
}
