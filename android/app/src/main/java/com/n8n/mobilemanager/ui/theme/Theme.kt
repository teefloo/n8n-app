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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF690F05),
    primaryContainer = Color(0xFF8F271B),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFF50D9C8),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF73F8E5),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF36275D),
    background = NeuroBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = NeuroSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF4A454A),
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF958F95),
    outlineVariant = Color(0xFF4A454A)
)

private val LightColorScheme = lightColorScheme(
    primary = N8nPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410002),
    secondary = N8nSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFAAEEE4),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color.White,
    background = NeuroBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = NeuroSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE8E2E5),
    onSurfaceVariant = TextSecondaryLight,
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7A7579),
    outlineVariant = Color(0xFFCBC5C9)
)

@Composable
fun N8nMobileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
