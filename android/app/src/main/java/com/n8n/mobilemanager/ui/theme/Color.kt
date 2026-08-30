package com.n8n.mobilemanager.ui.theme

import androidx.compose.ui.graphics.Color

// n8n brand and semantic accents. The primary brand orange is intentionally
// kept separate from the accessible Material primary used for text and buttons.
val N8nBrandOrange = Color(0xFFFF6D5A)
val N8nPrimary = Color(0xFFB93826)
val N8nPrimaryVariant = Color(0xFF8F271B)
val N8nSecondary = Color(0xFF006B60)
val N8nAccent = Color(0xFF00A896)

// Status colors are shared by the dashboard, workflow lists, and execution detail.
val StatusSuccess = Color(0xFF16794A)
val StatusError = Color(0xFFBA1A1A)
val StatusWarning = Color(0xFF8A5200)
val StatusInfo = Color(0xFF345DA8)
val StatusRunning = Color(0xFF6750A4)

// Legacy names remain available to keep existing screens source-compatible.
// They now point at Material-like neutral surfaces instead of a shadow canvas.
val NeuroBackgroundLight = Color(0xFFF8F8FA)
val NeuroSurfaceLight = Color(0xFFFFFBFF)
val NeuroLightShadowLight = Color(0xFFFFFFFF)
val NeuroDarkShadowLight = Color(0xFFDEDDE2)
val TextPrimaryLight = Color(0xFF1B1B1F)
val TextSecondaryLight = Color(0xFF5F5E66)

val NeuroBackgroundDark = Color(0xFF121216)
val NeuroSurfaceDark = Color(0xFF1C1B20)
val NeuroLightShadowDark = Color(0xFF35343A)
val NeuroDarkShadowDark = Color(0xFF09090C)
val TextPrimaryDark = Color(0xFFE6E1E6)
val TextSecondaryDark = Color(0xFFC9C4CC)

val NeuroInsetLightShadowLight = Color(0xFFE4E2E7)
val NeuroInsetDarkShadowLight = Color(0xFFFFFFFF)
val NeuroInsetLightShadowDark = Color(0xFF0E0E12)
val NeuroInsetDarkShadowDark = Color(0xFF35343A)

val NeuroAccentSuccess = StatusSuccess
val NeuroAccentError = StatusError
val NeuroAccentWarning = StatusWarning
val NeuroAccentRunning = StatusRunning

val BackgroundLight = NeuroBackgroundLight
val SurfaceLight = NeuroSurfaceLight
val BackgroundDark = NeuroBackgroundDark
val SurfaceDark = NeuroSurfaceDark
val Background = BackgroundLight
val N8nPrimaryLight = N8nPrimary

object NeumorphicDimensions {
    const val CardShadowOffset = 2f
    const val CardShadowBlur = 4f
    const val CardCornerRadius = 16f
    const val SmallShadowOffset = 1f
    const val SmallShadowBlur = 2f
    const val SmallCornerRadius = 12f
    const val PressedShadowOffset = 0f
    const val PressedShadowBlur = 0f
    const val LargeShadowOffset = 3f
    const val LargeShadowBlur = 6f
    const val LargeCornerRadius = 20f
    const val IconContainerSize = 48f
    const val IconContainerRadius = 12f
    const val ToggleWidth = 52f
    const val ToggleHeight = 32f
    const val ToggleThumbSize = 24f
}
