package com.pocketsarkar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PSNavy,
    onPrimary = PSWhite,
    primaryContainer = PSCream,
    onPrimaryContainer = PSNavy,
    secondary = PSSaffron,
    onSecondary = PSWhite,
    secondaryContainer = PSWhite,
    onSecondaryContainer = PSNavy,
    background = PSCream,
    onBackground = PSTextPrimary,
    surface = PSWhite,
    onSurface = PSTextPrimary,
    surfaceVariant = PSWhite,
    onSurfaceVariant = PSTextSecondary,
    outline = PSBorder,
    error = PSRedFlag,
    onError = PSWhite,
    errorContainer = Color(0xFFFFCDD2),
)

private val DarkColorScheme = darkColorScheme(
    primary = PSWhiteDark,
    onPrimary = PSWhite,
    primaryContainer = PSNavyDark,
    onPrimaryContainer = PSWhite,
    secondary = PSSaffron,
    onSecondary = PSWhite,
    background = PSCreamDark,
    onBackground = PSWhite,
    surface = PSWhiteDark,
    onSurface = PSWhite,
    error = PSRedFlag,
)

@Composable
fun PocketSarkarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PocketSarkarTypography,
        content = content
    )
}
