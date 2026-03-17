package com.wiom.csp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Extra brand colors not covered by Material3 ──────────────────────

@Immutable
data class WiomExtraColors(
    val restore: Color,
    val netbox: Color,
    val positive: Color,
    val warning: Color,
    val negative: Color
)

val LocalWiomColors = staticCompositionLocalOf {
    WiomExtraColors(
        restore = AccentRestore,
        netbox = AccentGold,
        positive = Positive,
        warning = Warning,
        negative = Negative
    )
}

// ── Material3 color schemes ──────────────────────────────────────────

val WiomDarkColors: ColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = TextPrimary,
    primaryContainer = BrandSecondary,
    onPrimaryContainer = TextPrimary,
    secondary = AccentRestore,
    onSecondary = TextPrimary,
    secondaryContainer = BrandSecondary,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentGold,
    onTertiary = TextPrimary,
    tertiaryContainer = BrandSecondary,
    onTertiaryContainer = TextPrimary,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = BrandSecondary,
    onSurfaceVariant = TextSecondary,
    error = Negative,
    onError = TextPrimary,
    errorContainer = Negative.copy(alpha = 0.2f),
    onErrorContainer = Negative,
    outline = TextSecondary.copy(alpha = 0.4f),
    outlineVariant = TextSecondary.copy(alpha = 0.2f),
    inverseSurface = SurfaceLight,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = BrandPrimary,
    scrim = Color.Black
)

val WiomLightColors: ColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = BrandSecondary,
    secondary = AccentRestore,
    onSecondary = Color.White,
    secondaryContainer = AccentRestore.copy(alpha = 0.12f),
    onSecondaryContainer = BrandSecondary,
    tertiary = AccentGold,
    onTertiary = Color.White,
    tertiaryContainer = AccentGold.copy(alpha = 0.12f),
    onTertiaryContainer = BrandSecondary,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF0EDE8),
    onSurfaceVariant = TextSecondaryLight,
    error = Negative,
    onError = Color.White,
    errorContainer = Negative.copy(alpha = 0.1f),
    onErrorContainer = Negative,
    outline = TextSecondaryLight.copy(alpha = 0.4f),
    outlineVariant = TextSecondaryLight.copy(alpha = 0.2f),
    inverseSurface = SurfaceDark,
    inverseOnSurface = TextPrimary,
    inversePrimary = BrandPrimary,
    scrim = Color.Black
)

// ── Theme composable ─────────────────────────────────────────────────

@Composable
fun WiomCspTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WiomDarkColors else WiomLightColors

    val wiomExtraColors = WiomExtraColors(
        restore = AccentRestore,
        netbox = AccentGold,
        positive = Positive,
        warning = Warning,
        negative = Negative
    )

    CompositionLocalProvider(LocalWiomColors provides wiomExtraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WiomTypography,
            content = content
        )
    }
}
