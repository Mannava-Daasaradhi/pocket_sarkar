package com.pocketsarkar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary             = Saffron40,
    onPrimary           = NeutralLight,
    primaryContainer    = Saffron80,
    secondary           = IndiaGreen40,
    onSecondary         = NeutralLight,
    secondaryContainer  = IndiaGreen80,
    tertiary            = PeacockBlue40,
    tertiaryContainer   = PeacockBlue80,
    background          = NeutralLight,
    surface             = NeutralLight,
    surfaceVariant      = SurfaceVariant,
    error               = RiskHigh,
    errorContainer      = androidx.compose.ui.graphics.Color(0xFFFFCDD2),
)

private val DarkColorScheme = darkColorScheme(
    primary             = Saffron80,
    primaryContainer    = SaffronDark,
    secondary           = IndiaGreen80,
    secondaryContainer  = IndiaGreenDark,
    tertiary            = PeacockBlue80,
    background          = NeutralDark,
    surface             = NeutralDark,
    error               = androidx.compose.ui.graphics.Color(0xFFEF9A9A),
    errorContainer      = androidx.compose.ui.graphics.Color(0xFFB71C1C),
)

@Composable
fun PocketSarkarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled â€” we want consistent India-palette branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Kept for future option â€” currently off
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PocketSarkarTypography,
        content = content
    )
}




