package com.example.deepseekclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDsColors = staticCompositionLocalOf { DsLightColors }

private val LightScheme = lightColorScheme(
    primary = DsLightColors.primary,
    onPrimary = DsLightColors.onPrimary,
    primaryContainer = DsLightColors.bg,
    onPrimaryContainer = DsLightColors.primary,
    background = DsLightColors.bg,
    onBackground = DsLightColors.textPrimary,
    surface = DsLightColors.surface1,
    onSurface = DsLightColors.textPrimary,
    surfaceVariant = DsLightColors.chipBg,
    onSurfaceVariant = DsLightColors.textSecondary,
    outline = DsLightColors.outline,
    outlineVariant = DsLightColors.outline,
    error = DsLightColors.danger,
    onError = Color.White,
    scrim = Color.Black,
)

private val DarkScheme = darkColorScheme(
    primary = DsDarkColors.primary,
    onPrimary = DsDarkColors.onPrimary,
    primaryContainer = DsDarkColors.bg,
    onPrimaryContainer = DsDarkColors.primary,
    background = DsDarkColors.bg,
    onBackground = DsDarkColors.textPrimary,
    surface = DsDarkColors.surface1,
    onSurface = DsDarkColors.textPrimary,
    surfaceVariant = DsDarkColors.chipBg,
    onSurfaceVariant = DsDarkColors.textSecondary,
    outline = DsDarkColors.outline,
    outlineVariant = DsDarkColors.outline,
    error = DsDarkColors.danger,
    onError = Color.White,
    scrim = Color.Black,
)

@Composable
fun DeepSeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val ds = if (darkTheme) DsDarkColors else DsLightColors
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalDsColors provides ds) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DsTypography,
            shapes = DsShapes,
            content = content,
        )
    }
}

object DsTheme {
    val colors: DsColors
        @Composable @ReadOnlyComposable get() = LocalDsColors.current
}
