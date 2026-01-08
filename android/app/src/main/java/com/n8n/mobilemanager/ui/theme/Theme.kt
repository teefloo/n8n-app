package com.n8n.mobilemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ==================== Neumorphic Shadow Configuration ====================

data class NeumorphicShadows(
    val lightShadowColor: Color,
    val darkShadowColor: Color,
    val shadowElevation: Dp = 8.dp,
    val shadowBlur: Dp = 16.dp,
    val shadowOffset: Dp = 6.dp
)

data class NeumorphicColors(
    val background: Color,
    val surface: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val pressedBackground: Color
)

val LocalNeumorphicColors = staticCompositionLocalOf {
    NeumorphicColors(
        background = NeumorphBackgroundLight,
        surface = NeumorphSurfaceLight,
        lightShadow = ShadowLightColor,
        darkShadow = ShadowDarkColorLight,
        pressedBackground = NeumorphPressedLight
    )
}

val LightNeumorphicColors = NeumorphicColors(
    background = NeumorphBackgroundLight,
    surface = NeumorphSurfaceLight,
    lightShadow = ShadowLightColor,
    darkShadow = ShadowDarkColorLight,
    pressedBackground = NeumorphPressedLight
)

val DarkNeumorphicColors = NeumorphicColors(
    background = NeumorphBackgroundDark,
    surface = NeumorphSurfaceDark,
    lightShadow = ShadowLightColorDark,
    darkShadow = ShadowDarkColorDark,
    pressedBackground = NeumorphPressedDark
)

// ==================== Color Schemes ====================

private val LightColorScheme = lightColorScheme(
    primary = N8nPrimary,
    onPrimary = Color.White,
    primaryContainer = N8nPrimaryLight.copy(alpha = 0.2f),
    onPrimaryContainer = N8nPrimary,
    
    secondary = N8nAccent,
    onSecondary = Color.White,
    secondaryContainer = N8nAccentMuted.copy(alpha = 0.3f),
    onSecondaryContainer = N8nAccent,
    
    tertiary = StatusInfo,
    onTertiary = Color.White,
    tertiaryContainer = StatusInfoMuted.copy(alpha = 0.3f),
    onTertiaryContainer = StatusInfo,
    
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorMuted.copy(alpha = 0.3f),
    onErrorContainer = StatusError,
    
    background = NeumorphBackgroundLight,
    onBackground = TextPrimaryLight,
    
    surface = NeumorphSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = NeumorphSurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    
    outline = DividerLight,
    outlineVariant = DividerLight.copy(alpha = 0.5f),
    
    inverseSurface = NeumorphSurfaceDark,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = N8nPrimaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = N8nPrimary,
    onPrimary = Color.White,
    primaryContainer = N8nPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = N8nPrimaryLight,
    
    secondary = N8nAccent,
    onSecondary = Color.Black,
    secondaryContainer = N8nAccent.copy(alpha = 0.2f),
    onSecondaryContainer = N8nAccentMuted,
    
    tertiary = StatusInfo,
    onTertiary = Color.White,
    tertiaryContainer = StatusInfo.copy(alpha = 0.2f),
    onTertiaryContainer = StatusInfoMuted,
    
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusError.copy(alpha = 0.2f),
    onErrorContainer = StatusErrorMuted,
    
    background = NeumorphBackgroundDark,
    onBackground = TextPrimaryDark,
    
    surface = NeumorphSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeumorphSurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    
    outline = DividerDark,
    outlineVariant = DividerDark.copy(alpha = 0.5f),
    
    inverseSurface = NeumorphSurfaceLight,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = N8nPrimaryVariant
)

@Composable
fun N8nMobileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val neumorphicColors = if (darkTheme) DarkNeumorphicColors else LightNeumorphicColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalNeumorphicColors provides neumorphicColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = N8nTypography,
            content = content
        )
    }
}

// ==================== Extension Colors ====================
val ColorScheme.statusSuccess: Color
    get() = StatusSuccess

val ColorScheme.statusSuccessMuted: Color
    get() = StatusSuccessMuted

val ColorScheme.statusError: Color
    get() = StatusError

val ColorScheme.statusErrorMuted: Color
    get() = StatusErrorMuted

val ColorScheme.statusWarning: Color
    get() = StatusWarning

val ColorScheme.statusWarningMuted: Color
    get() = StatusWarningMuted

val ColorScheme.statusInfo: Color
    get() = StatusInfo

val ColorScheme.statusInfoMuted: Color
    get() = StatusInfoMuted

val ColorScheme.statusRunning: Color
    get() = StatusRunning

val ColorScheme.statusRunningMuted: Color
    get() = StatusRunningMuted

val ColorScheme.n8nAccent: Color
    get() = N8nAccent

val ColorScheme.n8nAccentMuted: Color
    get() = N8nAccentMuted

// ==================== Neumorphic Helpers ====================
@Composable
fun neumorphicColors(): NeumorphicColors = LocalNeumorphicColors.current
