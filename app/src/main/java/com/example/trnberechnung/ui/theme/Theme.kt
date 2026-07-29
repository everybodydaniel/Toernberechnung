package com.example.trnberechnung.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NauticalPrimary,
    onPrimary = NauticalTextOnPrimary,
    primaryContainer = NauticalPrimaryDark,
    onPrimaryContainer = NauticalTextPrimary,
    secondary = NauticalSecondary,
    onSecondary = NauticalTextOnPrimary,
    secondaryContainer = NauticalSurfaceVariant,
    onSecondaryContainer = NauticalTextPrimary,
    tertiary = NauticalAccentWarm,
    onTertiary = NauticalTextOnPrimary,
    background = NauticalBackground,
    onBackground = NauticalTextPrimary,
    surface = NauticalSurface,
    onSurface = NauticalTextPrimary,
    surfaceVariant = NauticalSurfaceVariant,
    onSurfaceVariant = NauticalTextSecondary,
    error = NauticalNoGo,
    onError = Color.White,
    errorContainer = NauticalNoGoBg,
    onErrorContainer = NauticalNoGo,
    outline = NauticalDivider,
    outlineVariant = NauticalGridLine
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
)

@Composable
fun TörnberechnungTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
