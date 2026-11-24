package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class AmmoStatusColors(
    val healthy: Color,
    val healthyContainer: Color,
    val low: Color,
    val lowContainer: Color,
    val critical: Color,
    val criticalContainer: Color,
    val subtleOutline: Color,
    val scrimDark: Color
)

val LocalAmmoStatusColors = staticCompositionLocalOf {
    AmmoStatusColors(
        healthy = StockHealthyDark,
        healthyContainer = TacticalSageContainerDark,
        low = StockLowDark,
        lowContainer = BrassContainerDark,
        critical = StockCriticalDark,
        criticalContainer = Color(0xFF3E1818),
        subtleOutline = GunmetalOutlineSubtle,
        scrimDark = Color(0xCC101214)
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = BrassGold,
    onPrimary = Color(0xFF1F1605),
    primaryContainer = BrassContainerDark,
    onPrimaryContainer = BrassGoldBright,
    secondary = CopperAccent,
    onSecondary = Color(0xFF231105),
    secondaryContainer = CopperContainerDark,
    onSecondaryContainer = Color(0xFFF6C6A6),
    tertiary = TacticalSage,
    onTertiary = Color(0xFF0E2010),
    tertiaryContainer = TacticalSageContainerDark,
    onTertiaryContainer = Color(0xFFB7DFBB),
    background = GunmetalBg,
    onBackground = InkOnDark,
    surface = GunmetalSurface,
    onSurface = InkOnDark,
    surfaceVariant = GunmetalSurfaceVariant,
    onSurfaceVariant = InkMutedOnDark,
    surfaceContainerLowest = GunmetalBg,
    surfaceContainerLow = GunmetalSurface,
    surfaceContainer = GunmetalSurfaceVariant,
    surfaceContainerHigh = GunmetalSurfaceHigh,
    surfaceContainerHighest = Color(0xFF343B43),
    outline = GunmetalOutline,
    outlineVariant = GunmetalOutlineSubtle,
    error = StockCriticalDark,
    onError = Color(0xFF2D0909),
    errorContainer = Color(0xFF471919),
    onErrorContainer = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = BrassDeep,
    onPrimary = Color.White,
    primaryContainer = BrassContainerLight,
    onPrimaryContainer = Color(0xFF382600),
    secondary = CopperDeep,
    onSecondary = Color.White,
    secondaryContainer = CopperContainerLight,
    onSecondaryContainer = Color(0xFF3B1A06),
    tertiary = TacticalSageDeep,
    onTertiary = Color.White,
    tertiaryContainer = TacticalSageContainerLight,
    onTertiaryContainer = Color(0xFF102A14),
    background = ParchmentBg,
    onBackground = InkOnLight,
    surface = ParchmentSurface,
    onSurface = InkOnLight,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = InkMutedOnLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = ParchmentBg,
    surfaceContainer = ParchmentSurfaceVariant,
    surfaceContainerHigh = ParchmentSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD8D2C4),
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineSubtle,
    error = StockCriticalLight,
    onError = Color.White,
    errorContainer = Color(0xFFFADAD8),
    onErrorContainer = Color(0xFF5A0A0A)
)

val AmmoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AmmoVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) {
        AmmoStatusColors(
            healthy = StockHealthyDark,
            healthyContainer = TacticalSageContainerDark,
            low = StockLowDark,
            lowContainer = BrassContainerDark,
            critical = StockCriticalDark,
            criticalContainer = Color(0xFF3E1818),
            subtleOutline = GunmetalOutlineSubtle,
            scrimDark = Color(0xD9101214)
        )
    } else {
        AmmoStatusColors(
            healthy = StockHealthyLight,
            healthyContainer = TacticalSageContainerLight,
            low = StockLowLight,
            lowContainer = BrassContainerLight,
            critical = StockCriticalLight,
            criticalContainer = Color(0xFFFADAD8),
            subtleOutline = ParchmentOutlineSubtle,
            scrimDark = Color(0xCC16181C)
        )
    }

    CompositionLocalProvider(LocalAmmoStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AmmoTypography,
            shapes = AmmoShapes,
            content = content
        )
    }
}
