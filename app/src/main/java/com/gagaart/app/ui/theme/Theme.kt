package com.gagaart.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GagaPrimary,
    onPrimary = GagaSurfaceLight,
    primaryContainer = GagaPrimary.copy(alpha = 0.15f),
    secondary = GagaSecondary,
    onSecondary = GagaSurfaceLight,
    tertiary = GagaAccent,
    background = GagaBackgroundLight,
    onBackground = GagaOnSurfaceLight,
    surface = GagaSurfaceLight,
    onSurface = GagaOnSurfaceLight,
    surfaceVariant = GagaCardLight,
    onSurfaceVariant = GagaOnSurfaceLight.copy(alpha = 0.7f)
)

private val DarkColorScheme = darkColorScheme(
    primary = GagaPrimary,
    onPrimary = GagaOnSurfaceDark,
    primaryContainer = GagaPrimary.copy(alpha = 0.2f),
    secondary = GagaAccent,
    onSecondary = GagaOnSurfaceDark,
    tertiary = GagaAccent,
    background = GagaBackgroundDark,
    onBackground = GagaOnSurfaceDark,
    surface = GagaSurfaceDark,
    onSurface = GagaOnSurfaceDark,
    surfaceVariant = GagaCardDark,
    onSurfaceVariant = GagaOnSurfaceDark.copy(alpha = 0.7f)
)

@Composable
fun GagaArtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GagaTypography,
        content = content
    )
}
