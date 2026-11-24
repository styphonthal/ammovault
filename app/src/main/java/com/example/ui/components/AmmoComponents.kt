package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AmmoEntry
import com.example.data.AmmoSeekUrlBuilder
import com.example.data.CaliberInventorySummary
import com.example.data.CaliberThresholdConfigItem
import com.example.data.ParsedPriceResult
import com.example.data.PriceClipboardParser
import com.example.data.PriceHistoryEntry
import com.example.data.PriceUnitMode
import com.example.data.StockStatus
import com.example.data.VaultBackupPayload
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

fun formatSignedCurrency(amount: Double): String {
    val prefix = if (amount >= 0.0) "+" else "-"
    return "$prefix${formatCurrency(abs(amount))}"
}

fun formatCpr(pricePerRound: Double): String {
    return String.format(Locale.US, "$%.2f/rd", pricePerRound)
}

fun formatRounds(rounds: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(rounds)
}

fun formatPurchaseDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(epochMillis))
}

fun formatHistoryTimestamp(epochMillis: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(epochMillis))
}

fun openAmmoSeekForEntry(
    context: Context,
    entry: AmmoEntry,
    onLaunchError: (String) -> Unit = {}
) {
    val url = AmmoSeekUrlBuilder.buildSearchUrl(entry)
    openBrowserUrl(context, url, onLaunchError)
}

fun openAmmoSeekWithBrand(
    context: Context,
    entry: AmmoEntry,
    onLaunchError: (String) -> Unit = {}
) {
    val url = AmmoSeekUrlBuilder.buildBrandSearchUrl(entry)
    openBrowserUrl(context, url, onLaunchError)
}

fun openAmmoSeekForCaliber(
    context: Context,
    caliber: String,
    onLaunchError: (String) -> Unit = {}
) {
    val url = AmmoSeekUrlBuilder.buildCaliberSearchUrl(caliber)
    openBrowserUrl(context, url, onLaunchError)
}

fun openBrowserUrl(
    context: Context,
    url: String,
    onLaunchError: (String) -> Unit = {}
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        onLaunchError("No browser available to open AmmoSeek")
    }
}

@Composable
fun CaliberPill(
    caliber: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Text(
            text = caliber,
            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StockStatusChip(
    status: StockStatus,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAmmoStatusColors.current
    val (label, fg, bg) = when (status) {
        StockStatus.HEALTHY -> Triple("IN STOCK", statusColors.healthy, statusColors.healthyContainer)
        StockStatus.LOW -> Triple("LOW STOCK", statusColors.low, statusColors.lowContainer)
        StockStatus.CRITICAL -> Triple("OUT / CRIT", statusColors.critical, statusColors.criticalContainer)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (status != StockStatus.HEALTHY) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(fg)
                )
            }
            Text(
                text = label,
                style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                color = fg
            )
        }
    }
}

@Composable
fun SpecBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MonoTelemetrySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CaliberProgressBar(
    progress: Float,
    status: StockStatus,
    modifier: Modifier = Modifier,
    minThresholdFraction: Float? = null
) {
    val statusColors = LocalAmmoStatusColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val markerColor = MaterialTheme.colorScheme.onSurface
    val barColor = when (status) {
        StockStatus.HEALTHY -> statusColors.healthy
        StockStatus.LOW -> statusColors.low
        StockStatus.CRITICAL -> statusColors.critical
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "caliber_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
        if (animatedProgress > 0f) {
            drawRoundRect(
                color = barColor,
                topLeft = Offset.Zero,
                size = Size(width = size.width * animatedProgress, height = size.height),
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
        }
        if (minThresholdFraction != null) {
            val markerX = size.width * minThresholdFraction.coerceIn(0.02f, 0.98f)
            drawLine(
                color = markerColor.copy(alpha = 0.85f),
                start = Offset(markerX, 0f),
                end = Offset(markerX, size.height),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}

@Composable
fun LowThresholdVisualAlertBanner(
    alerts: List<CaliberThresholdConfigItem>,
    onOpenRestock: () -> Unit,
    onOpenThresholdConfig: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return
    val statusColors = LocalAmmoStatusColors.current
    val hasDepleted = alerts.any { it.currentRounds <= 0 }
    val accentColor = if (hasDepleted) statusColors.critical else statusColors.low
    val containerColor = if (hasDepleted) statusColors.criticalContainer else statusColors.lowContainer

    val summaryText = remember(alerts) {
        val visible = alerts.take(3).joinToString(" · ") { item ->
            "${item.caliber}: ${formatRounds(item.currentRounds)}/${formatRounds(item.minThresholdRounds)}"
        }
        if (alerts.size > 3) "$visible (+${alerts.size - 3} more)" else visible
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("low_threshold_alert_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = stringResource(id = R.string.low_threshold_alert_title),
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Below Minimum (${alerts.size} ${if (alerts.size == 1) "type" else "types"})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("low_threshold_alert_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss low inventory alert banner",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = summaryText,
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenRestock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("low_threshold_alert_restock_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.action_restock))
                }

                OutlinedButton(
                    onClick = onOpenThresholdConfig,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("low_threshold_alert_config_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.action_configure_thresholds),
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun CaliberValueBarChart(
    summaries: List<CaliberInventorySummary>,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return
    val maxVal = summaries.maxOf { it.totalMarketValue }.coerceAtLeast(1.0)
    val sorted = remember(summaries) { summaries.sortedByDescending { it.totalMarketValue } }
    val barPalette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sorted.forEachIndexed { index, summary ->
            val ratio = (summary.totalMarketValue / maxVal).toFloat().coerceIn(0.03f, 1f)
            val color = barPalette[index % barPalette.size]
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.caliber,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${displayCurrency(summary.totalMarketValue)} · ${formatRounds(summary.totalRounds)} rds",
                        style = MonoTelemetrySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = trackColor,
                        size = size,
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                    drawRoundRect(
                        color = color,
                        size = Size(width = size.width * ratio, height = size.height),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyVaultState(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit,
    onLoadStarterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_vault),
            contentDescription = stringResource(id = R.string.empty_vault_content_desc),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(164.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAddClick,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("empty_add_ammo_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.action_add_ammo))
        }
        if (onLoadStarterClick != null) {
            OutlinedButton(
                onClick = onLoadStarterClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("load_starter_lots_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.load_sample_catalog))
            }
        }
    }
}

@Composable
fun CaliberTargetDialog(
    caliber: String,
    currentTarget: Int,
    currentBuyTarget: Double? = null,
    currentMinThreshold: Int = 0,
    lotCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double?, Int) -> Unit,
    onRenameCaliber: (String) -> Unit,
    onDeleteCaliber: () -> Unit
) {
    var targetInput by remember(caliber, currentTarget) { mutableStateOf(currentTarget.toString()) }
    var minThresholdInput by remember(caliber, currentMinThreshold) {
        mutableStateOf(if (currentMinThreshold > 0) currentMinThreshold.toString() else "0")
    }
    var buyTargetInput by remember(caliber, currentBuyTarget) {
        mutableStateOf(currentBuyTarget?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var renameInput by remember(caliber) { mutableStateOf(caliber) }
    var confirmDelete by remember(caliber) { mutableStateOf(false) }

    val parsedTarget = targetInput.toIntOrNull()
    val isTargetValid = parsedTarget != null && parsedTarget in 1..100_000
    val parsedMinThreshold = if (minThresholdInput.isBlank()) 0 else minThresholdInput.toIntOrNull()
    val isMinThresholdValid = parsedMinThreshold != null && parsedMinThreshold in 0..100_000
    val parsedBuyTarget = if (buyTargetInput.isBlank()) null else buyTargetInput.toDoubleOrNull()
    val isBuyTargetValid = buyTargetInput.isBlank() || (parsedBuyTarget != null && parsedBuyTarget >= 0.0 && parsedBuyTarget <= 100.0)
    val trimmedRename = renameInput.trim()
    val isRenameValid = trimmedRename.isNotEmpty() && trimmedRename != caliber

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage Caliber · $caliber",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set par round target, minimum low-stock alert threshold, optional buy-target CPR, or rename/remove this caliber.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { if (it.length <= 6) targetInput = it.filter(Char::isDigit) },
                    label = { Text("Par Target Rounds") },
                    suffix = { Text("rds") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = targetInput.isNotEmpty() && !isTargetValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caliber_target_input")
                )

                OutlinedTextField(
                    value = minThresholdInput,
                    onValueChange = { if (it.length <= 6) minThresholdInput = it.filter(Char::isDigit) },
                    label = { Text("Minimum Alert Threshold (0 = Off)") },
                    suffix = { Text("rds") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isMinThresholdValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caliber_min_threshold_input")
                )

                OutlinedTextField(
                    value = buyTargetInput,
                    onValueChange = { text ->
                        val cleaned = text.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 7) {
                            buyTargetInput = cleaned
                        }
                    },
                    label = { Text("Buy Target CPR (Optional)") },
                    placeholder = { Text("e.g., 0.45") },
                    prefix = { Text("$") },
                    suffix = { Text("/ rd") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isBuyTargetValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caliber_buy_target_input")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Rename Caliber") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("caliber_rename_input")
                    )
                    OutlinedButton(
                        onClick = { if (isRenameValid) onRenameCaliber(trimmedRename) },
                        enabled = isRenameValid,
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("caliber_rename_button")
                    ) {
                        Text("Rename")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (!confirmDelete) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("caliber_delete_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All $caliber ($lotCount lot${if (lotCount == 1) "" else "s"})")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { confirmDelete = false },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Keep")
                        }
                        Button(
                            onClick = onDeleteCaliber,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("caliber_delete_confirm_button")
                        ) {
                            Text("Confirm Delete")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTargetValid && isMinThresholdValid && isBuyTargetValid && parsedTarget != null && parsedMinThreshold != null) {
                        onConfirm(parsedTarget, parsedBuyTarget, parsedMinThreshold)
                    }
                },
                enabled = isTargetValid && isMinThresholdValid && isBuyTargetValid,
                modifier = Modifier.testTag("caliber_target_save_button")
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun QuickRoundAdjustDialog(
    entry: AmmoEntry,
    onDismiss: () -> Unit,
    onApplyDelta: (Int) -> Unit,
    onSetExactRounds: (Int) -> Unit = { exact -> onApplyDelta(exact - entry.currentCount) }
) {
    var setExactMode by remember { mutableStateOf(false) }
    var isSubtract by remember { mutableStateOf(true) }
    var deltaText by remember { mutableStateOf("10") }
    var exactTotalText by remember(entry.id, entry.currentCount) {
        mutableStateOf(entry.currentCount.toString())
    }

    val deltaAmount = deltaText.toIntOrNull()
    val signedDelta = if (setExactMode) {
        val target = exactTotalText.toIntOrNull()
        if (target != null && target >= 0) target - entry.currentCount else null
    } else {
        if (deltaAmount != null && deltaAmount > 0) {
            if (isSubtract) -deltaAmount else deltaAmount
        } else {
            null
        }
    }

    val resultingRounds = signedDelta?.let { (entry.currentCount + it).coerceAtLeast(0) }
    val canApply = signedDelta != null && (setExactMode || (deltaAmount != null && deltaAmount > 0))

    val presets = remember(entry.boxSize) {
        listOf(1, 5, 10, entry.boxSize).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust Rounds",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${entry.brand} ${entry.productLine}".trim() + " · ${entry.caliber} (${entry.grain} gr ${entry.bulletType})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val previewText = if (resultingRounds != null) {
                    "Current: ${formatRounds(entry.currentCount)} → ${formatRounds(resultingRounds)} rds"
                } else {
                    "Current: ${formatRounds(entry.currentCount)} rds"
                }
                Text(
                    text = previewText,
                    style = MonoTelemetryMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Top mode selector: Adjust (+/-) vs Exact Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modeModifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                    if (!setExactMode) {
                        Button(
                            onClick = { setExactMode = false },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = modeModifier.testTag("adjust_tab_delta")
                        ) { Text("By Amount") }
                        OutlinedButton(
                            onClick = { setExactMode = true },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = modeModifier.testTag("adjust_tab_exact")
                        ) { Text("Exact Total") }
                    } else {
                        OutlinedButton(
                            onClick = { setExactMode = false },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = modeModifier.testTag("adjust_tab_delta")
                        ) { Text("By Amount") }
                        Button(
                            onClick = { setExactMode = true },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = modeModifier.testTag("adjust_tab_exact")
                        ) { Text("Exact Total") }
                    }
                }

                if (!setExactMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipModifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                        if (isSubtract) {
                            Button(
                                onClick = { isSubtract = true },
                                modifier = chipModifier.testTag("adjust_mode_range")
                            ) { Text("Used (-)") }
                            OutlinedButton(
                                onClick = { isSubtract = false },
                                modifier = chipModifier.testTag("adjust_mode_restock")
                            ) { Text("Restock (+)") }
                        } else {
                            OutlinedButton(
                                onClick = { isSubtract = true },
                                modifier = chipModifier.testTag("adjust_mode_range")
                            ) { Text("Used (-)") }
                            Button(
                                onClick = { isSubtract = false },
                                modifier = chipModifier.testTag("adjust_mode_restock")
                            ) { Text("Restock (+)") }
                        }
                    }

                    // Quick increment presets down to 1 bullet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            OutlinedButton(
                                onClick = { deltaText = preset.toString() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("adjust_preset_$preset")
                            ) {
                                Text(
                                    text = if (preset == entry.boxSize && preset !in listOf(1, 5, 10)) "Box($preset)" else "$preset",
                                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { if (it.length <= 5) deltaText = it.filter(Char::isDigit) },
                        label = { Text(if (isSubtract) "Rounds Expended" else "Rounds Added") },
                        suffix = { Text("rds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adjust_rounds_input")
                    )
                } else {
                    // Exact Total stepper down to the single bullet
                    val currentExact = exactTotalText.toIntOrNull() ?: entry.currentCount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-10, -1, 1, 10).forEach { step ->
                            OutlinedButton(
                                onClick = {
                                    exactTotalText = (currentExact + step).coerceAtLeast(0).toString()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("exact_step_$step")
                            ) {
                                Text(
                                    text = if (step > 0) "+$step" else "$step",
                                    style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = exactTotalText,
                        onValueChange = { if (it.length <= 6) exactTotalText = it.filter(Char::isDigit) },
                        label = { Text("Exact Rounds On Hand") },
                        suffix = { Text("rds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("adjust_exact_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (setExactMode) {
                        val target = exactTotalText.toIntOrNull()
                        if (target != null && target >= 0) {
                            onSetExactRounds(target)
                        }
                    } else if (canApply && signedDelta != null) {
                        onApplyDelta(signedDelta)
                    }
                },
                enabled = canApply,
                modifier = Modifier.testTag("adjust_rounds_confirm_button")
            ) {
                Text("Apply")
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
fun QuickMarketPriceDialog(
    entry: AmmoEntry,
    onDismiss: () -> Unit,
    onSavePrice: (Double, String) -> Unit,
    onCheckAmmoSeek: (() -> Unit)? = null,
    onCheckAmmoSeekUrl: ((String) -> Unit)? = null
) {
    var unitMode by remember(entry.id) { mutableStateOf(PriceUnitMode.DOLLARS_PER_ROUND) }
    var priceInput by remember(entry.id) {
        mutableStateOf(String.format(Locale.US, "%.2f", entry.marketPricePerRound))
    }
    var selectedSource by remember(entry.id) {
        mutableStateOf("AmmoSeek Check")
    }
    var clipboardFeedback by remember(entry.id) { mutableStateOf<String?>(null) }

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val parsedCpr = PriceClipboardParser.computeCpr(priceInput, unitMode, entry.boxSize)
    val isValid = parsedCpr != null && parsedCpr >= 0.0 && parsedCpr <= 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Market Price",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${entry.brand} · ${entry.caliber} ${entry.grain} gr ${entry.bulletType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (onCheckAmmoSeek != null) {
                    val context = LocalContext.current
                    val brandKeyword = remember(entry.brand) {
                        AmmoSeekUrlBuilder.normalizeBrandKeyword(entry.brand)
                    }
                    OutlinedButton(
                        onClick = onCheckAmmoSeek,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("dialog_check_ammoseek_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val lookupLabel = if (entry.grain > 0) {
                            "AmmoSeek: ${entry.caliber} ${entry.grain}gr"
                        } else {
                            "Look Up Live Price on AmmoSeek.com"
                        }
                        Text(lookupLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (brandKeyword.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val url = AmmoSeekUrlBuilder.buildBrandSearchUrl(entry)
                                    if (onCheckAmmoSeekUrl != null) {
                                        onCheckAmmoSeekUrl(url)
                                    } else {
                                        openAmmoSeekWithBrand(context, entry)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_check_ammoseek_brand_button")
                            ) {
                                Text(
                                    text = "+ Filter: $brandKeyword",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                val url = AmmoSeekUrlBuilder.buildCaliberSearchUrl(entry.caliber)
                                if (onCheckAmmoSeekUrl != null) {
                                    onCheckAmmoSeekUrl(url)
                                } else {
                                    openAmmoSeekForCaliber(context, entry.caliber)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dialog_check_ammoseek_caliber_button")
                        ) {
                            Text(
                                text = "All ${entry.caliber}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Unit switcher: $/rd | ¢/rd | Box $
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PriceUnitMode.entries.forEach { mode ->
                        val label = if (mode == PriceUnitMode.BOX_TOTAL) "Box (${entry.boxSize})" else mode.label
                        FilterChip(
                            selected = unitMode == mode,
                            onClick = {
                                if (unitMode != mode) {
                                    val currentCpr = PriceClipboardParser.computeCpr(priceInput, unitMode, entry.boxSize)
                                    unitMode = mode
                                    if (currentCpr != null) {
                                        priceInput = PriceClipboardParser.formatValueForMode(currentCpr, mode, entry.boxSize)
                                    }
                                    clipboardFeedback = null
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MonoTelemetrySmall
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quick_price_unit_${mode.name.lowercase()}")
                        )
                    }
                }

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
                    label = {
                        Text(
                            when (unitMode) {
                                PriceUnitMode.DOLLARS_PER_ROUND -> "Market Price Per Round"
                                PriceUnitMode.CENTS_PER_ROUND -> "Cents Per Round (¢/rd)"
                                PriceUnitMode.BOX_TOTAL -> "Price Per ${entry.boxSize}-Rd Box"
                            }
                        )
                    },
                    prefix = prefixContent,
                    suffix = {
                        Text(
                            when (unitMode) {
                                PriceUnitMode.DOLLARS_PER_ROUND -> "/ rd"
                                PriceUnitMode.CENTS_PER_ROUND -> "¢/rd"
                                PriceUnitMode.BOX_TOTAL -> "/ box"
                            }
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text.orEmpty()
                                val parsed: ParsedPriceResult? = PriceClipboardParser.parsePriceFromText(
                                    rawText = clipText,
                                    boxSize = entry.boxSize
                                )
                                if (parsed != null) {
                                    unitMode = parsed.unitMode
                                    priceInput = parsed.displayInputValue
                                    clipboardFeedback = parsed.summary
                                } else {
                                    clipboardFeedback = if (clipText.isBlank()) {
                                        "Clipboard is empty — copy a price on AmmoSeek first"
                                    } else {
                                        "No price found in copied text"
                                    }
                                }
                            },
                            modifier = Modifier.testTag("quick_price_paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = "Paste copied price from clipboard"
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceInput.isNotEmpty() && !isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_price_input")
                )

                if (clipboardFeedback != null) {
                    Text(
                        text = clipboardFeedback!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("AmmoSeek Check", "Market Update").forEach { sourceOption ->
                        FilterChip(
                            selected = selectedSource == sourceOption,
                            onClick = { selectedSource = sourceOption },
                            label = { Text(sourceOption, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                if (isValid && parsedCpr != null) {
                    Text(
                        text = "CPR: ${formatCpr(parsedCpr)} · Box of ${entry.boxSize}: ${formatCurrency(parsedCpr * entry.boxSize)} · Lot: ${formatCurrency(parsedCpr * entry.currentCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid && parsedCpr != null) onSavePrice(parsedCpr, selectedSource) },
                enabled = isValid,
                modifier = Modifier.testTag("quick_price_save_button")
            ) {
                Text("Save Snapshot")
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
fun PriceHistoryChart(
    history: List<PriceHistoryEntry>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return
    val chronological = remember(history) { history.sortedBy { it.recordedAt } }
    val prices = chronological.map { it.pricePerRound }
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val pointInnerColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Low: ${displayCpr(minPrice)}",
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "High: ${displayCpr(maxPrice)}",
                style = MonoTelemetrySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            val midY = size.height / 2f
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.5f
            )

            val span = (maxPrice - minPrice).takeIf { it > 0.0001 } ?: 1.0
            val points = chronological.mapIndexed { index, item ->
                val x = if (chronological.size == 1) {
                    size.width / 2f
                } else {
                    (index.toFloat() / (chronological.size - 1).toFloat()) * size.width
                }
                val normalizedY = if (maxPrice - minPrice <= 0.0001) {
                    0.5f
                } else {
                    ((item.pricePerRound - minPrice) / span).toFloat()
                }
                val y = size.height - (normalizedY * size.height)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            points.forEach { pt ->
                drawCircle(color = lineColor, radius = 9f, center = pt)
                drawCircle(color = pointInnerColor, radius = 4.5f, center = pt)
            }
        }
    }
}

@Composable
fun BuyTargetDialog(
    entry: AmmoEntry,
    caliberDefaultTarget: Double?,
    onDismiss: () -> Unit,
    onSaveTarget: (Double?) -> Unit
) {
    var targetInput by remember(entry.id, entry.targetBuyPricePerRound) {
        mutableStateOf(
            entry.targetBuyPricePerRound?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    val parsed = if (targetInput.isBlank()) null else targetInput.toDoubleOrNull()
    val isValid = targetInput.isBlank() || (parsed != null && parsed >= 0.0 && parsed <= 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Lot Buy Target CPR",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${entry.brand} · ${entry.caliber} ${entry.grain} gr ${entry.bulletType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Current Market: ${formatCpr(entry.marketPricePerRound)}" +
                        (caliberDefaultTarget?.let { " · Caliber Default: ${formatCpr(it)}" } ?: ""),
                    style = MonoTelemetrySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { text ->
                        val cleaned = text.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1 && cleaned.length <= 7) {
                            targetInput = cleaned
                        }
                    },
                    label = { Text("Target Buy Price / Rd (Blank = Caliber Default)") },
                    placeholder = { Text("e.g., 0.45") },
                    prefix = { Text("$") },
                    suffix = { Text("/ rd") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lot_buy_target_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onSaveTarget(parsed) },
                enabled = isValid,
                modifier = Modifier.testTag("lot_buy_target_save_button")
            ) {
                Text("Save Target")
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
fun VaultBackupDialog(
    entryCount: Int,
    historyCount: Int,
    rangeSessionCount: Int = 0,
    pendingImportPayload: VaultBackupPayload?,
    onDismiss: () -> Unit,
    onExportZipClick: () -> Unit = {},
    onExportJsonClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportPdfClick: () -> Unit = {},
    onPickImportFileClick: () -> Unit,
    onPickImportCsvClick: () -> Unit = {},
    onConfirmImportMerge: () -> Unit,
    onConfirmImportReplace: () -> Unit,
    onCancelPendingImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (pendingImportPayload != null) "Confirm Vault Import" else "Backup, Export & Reports",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pendingImportPayload == null) {
                    Text(
                        text = "Storing $entryCount lot(s), $rangeSessionCount range log(s), and $historyCount price snapshot(s) locally on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onExportZipClick,
                        enabled = entryCount > 0,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("export_zip_button")
                    ) {
                        Icon(imageVector = Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Full Backup w/ Photos (.zip)")
                    }

                    OutlinedButton(
                        onClick = onExportPdfClick,
                        enabled = entryCount > 0,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insurance Valuation Report (.pdf)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportJsonClick,
                            enabled = entryCount > 0,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("export_json_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON")
                        }
                        OutlinedButton(
                            onClick = onExportCsvClick,
                            enabled = entryCount > 0,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(imageVector = Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedButton(
                        onClick = onPickImportFileClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("import_json_button")
                    ) {
                        Icon(imageVector = Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Backup (.zip / .json)")
                    }

                    OutlinedButton(
                        onClick = onPickImportCsvClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("import_csv_button")
                    ) {
                        Icon(imageVector = Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Spreadsheet (.csv)")
                    }
                } else {
                    val photoNote = if (pendingImportPayload.restoredPhotosCount > 0) {
                        ", ${pendingImportPayload.restoredPhotosCount} photo(s)"
                    } else {
                        ""
                    }
                    val rangeNote = if (pendingImportPayload.rangeSessions.isNotEmpty()) {
                        ", ${pendingImportPayload.rangeSessions.size} range log(s)"
                    } else {
                        ""
                    }
                    Text(
                        text = "Ready to import ${pendingImportPayload.entries.size} lot(s), " +
                            "${pendingImportPayload.targets.size} caliber target(s), " +
                            "${pendingImportPayload.history.size} price snapshot(s)$rangeNote$photoNote.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Merge keeps your existing lots and appends imported ones. Replace wipes current data first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onConfirmImportMerge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("import_merge_button")
                    ) {
                        Text("Merge with Current Vault")
                    }

                    OutlinedButton(
                        onClick = onConfirmImportReplace,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("import_replace_button")
                    ) {
                        Text("Replace Current Vault")
                    }

                    TextButton(
                        onClick = onCancelPendingImport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }
                }
            }
        },
        confirmButton = {
            if (pendingImportPayload == null) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
