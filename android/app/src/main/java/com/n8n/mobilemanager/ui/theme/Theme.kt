package com.n8n.mobilemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Neumorphic Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = N8nPrimary,
    onPrimary = Color.White,
    primaryContainer = N8nPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = N8nPrimary,
    
    secondary = N8nAccent,
    onSecondary = N8nSecondary,
    secondaryContainer = N8nAccent.copy(alpha = 0.2f),
    onSecondaryContainer = N8nAccent,
    
    tertiary = N8nPrimaryVariant,
    onTertiary = Color.White,
    
    background = NeuroBackgroundDark,
    onBackground = TextPrimaryDark,
    
    surface = NeuroSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeuroSurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusError.copy(alpha = 0.2f),
    onErrorContainer = StatusError,
    
    outline = TextSecondaryDark.copy(alpha = 0.3f),
    outlineVariant = TextSecondaryDark.copy(alpha = 0.15f)
)

// Neumorphic Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = N8nPrimary,
    onPrimary = Color.White,
    primaryContainer = N8nPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = N8nPrimary,
    
    secondary = N8nSecondary,
    onSecondary = Color.White,
    secondaryContainer = N8nSecondary.copy(alpha = 0.1f),
    onSecondaryContainer = N8nSecondary,
    
    tertiary = N8nAccent,
    onTertiary = Color.White,
    
    background = NeuroBackgroundLight,
    onBackground = TextPrimaryLight,
    
    surface = NeuroSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = NeuroSurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusError.copy(alpha = 0.15f),
    onErrorContainer = StatusError,
    
    outline = TextSecondaryLight.copy(alpha = 0.3f),
    outlineVariant = TextSecondaryLight.copy(alpha = 0.15f)
)

@Composable
fun N8nMobileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled for neumorphic design consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // We disable dynamic colors for neumorphic design as it needs specific
        // color relationships for the shadow effects to work properly
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use the neumorphic background color for status bar
            window.statusBarColor = colorScheme.background.toArgb()
            // Navigation bar matches the background
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
