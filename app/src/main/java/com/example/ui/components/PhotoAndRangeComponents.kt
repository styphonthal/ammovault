package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.CaliberBurnForecast
import com.example.data.VaultPhotoManager
import com.example.ui.theme.LocalAmmoStatusColors
import com.example.ui.theme.MonoTelemetryMedium
import com.example.ui.theme.MonoTelemetrySmall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatSessionDateTime(millis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(millis))
}

fun formatSessionDateOnly(millis: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}

fun formatSessionTimeOnly(millis: Long): String {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
}

fun formatDurationMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hrs = safe / 60
    val mins = safe % 60
    return when {
        hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
        hrs > 0 -> "${hrs}h"
        else -> "${mins}m"
    }
}

fun formatRunwayMonths(months: Double?): String {
    if (months == null) return "—"
    return when {
        months < 0.25 -> "< 1 wk left"
        months < 1.0 -> "~${(months * 4.3).toInt().coerceAtLeast(1)} wks left"
        months >= 24.0 -> "24+ mo left"
        else -> String.format(Locale.US, "~%.1f mo left", months)
    }
}

/**
 * Zero-permission photo picker card backed by Android Photo Picker (PickVisualMedia).
 * Selected images are copied into app-private internal storage so they remain valid across restarts.
 */
@Composable
fun PhotoAttachmentPickerCard(
    photoUri: String,
    title: String,
    subtitle: String,
    onPhotoChanged: (String) -> Unit,
    onPhotoTap: ((String) -> Unit)? = null,
    testTagPrefix: String = "photo_picker",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            scope.launch {
                val persisted = withContext(Dispatchers.IO) {
                    VaultPhotoManager.persistPickedPhoto(context, pickedUri)
                }
                if (persisted != null) {
                    if (photoUri.isNotBlank() && photoUri != persisted) {
                        withContext(Dispatchers.IO) {
                            VaultPhotoManager.deleteManagedPhoto(photoUri)
                        }
                    }
                    onPhotoChanged(persisted)
                }
            }
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (photoUri.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(enabled = onPhotoTap != null) {
                            onPhotoTap?.invoke(photoUri)
                        }
                        .testTag("${testTagPrefix}_preview")
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (onPhotoTap != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomIn,
                                contentDescription = "View full photo",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("${testTagPrefix}_replace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Photo")
                    }
                    TextButton(
                        onClick = {
                            val previous = photoUri
                            onPhotoChanged("")
                            scope.launch(Dispatchers.IO) {
                                VaultPhotoManager.deleteManagedPhoto(previous)
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("${testTagPrefix}_remove_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Photo from Library")
                }
            }
        }
    }
}

@Composable
fun FullScreenPhotoDialog(
    photoUri: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss)
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            )
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .testTag("close_fullscreen_photo_button")
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close full-screen photo",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Horizontal burn-rate and depletion forecast bars by caliber derived from Range Logs.
 */
@Composable
fun CaliberBurnForecastChart(
    forecasts: List<CaliberBurnForecast>,
    modifier: Modifier = Modifier
) {
    if (forecasts.isEmpty()) return

    val statusColors = LocalAmmoStatusColors.current
    val maxBurn = forecasts.maxOf { it.estimatedMonthlyBurnRate }.coerceAtLeast(1.0)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        forecasts.forEach { item ->
            val fraction = (item.estimatedMonthlyBurnRate / maxBurn).toFloat().coerceIn(0.06f, 1f)
            val runwayColor = when {
                item.estimatedMonthsRemaining == null -> MaterialTheme.colorScheme.onSurfaceVariant
                item.estimatedMonthsRemaining < 1.5 -> statusColors.critical
                item.estimatedMonthsRemaining < 3.5 -> statusColors.low
                else -> statusColors.healthy
            }
            val runwayContainer = when {
                item.estimatedMonthsRemaining == null -> MaterialTheme.colorScheme.surfaceContainerHigh
                item.estimatedMonthsRemaining < 1.5 -> statusColors.criticalContainer
                item.estimatedMonthsRemaining < 3.5 -> statusColors.lowContainer
                else -> statusColors.healthyContainer
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.caliber,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.estimatedMonthlyBurnRate.toInt()} rds/mo",
                            style = MonoTelemetrySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = runwayContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formatRunwayMonths(item.estimatedMonthsRemaining),
                            style = MonoTelemetrySmall.copy(fontWeight = FontWeight.Bold),
                            color = runwayColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    val radius = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
                    drawRoundRect(
                        color = runwayColor,
                        size = Size(width = size.width * fraction, height = size.height),
                        cornerRadius = radius
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatRounds(item.totalRoundsFired)} fired (${item.sessionCount} ${if (item.sessionCount == 1) "trip" else "trips"}) · ${displayCurrency(item.totalAmmoCostBurned)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatRounds(item.currentRoundsOnHand)} left",
                        style = MonoTelemetryMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
