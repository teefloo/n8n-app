package com.n8n.mobilemanager.ui.theme

import androidx.compose.ui.graphics.Color

// n8n Brand Colors
val N8nPrimary = Color(0xFFFF6D5A)
val N8nPrimaryVariant = Color(0xFFE85A48)
val N8nSecondary = Color(0xFF1A1A2E)
val N8nAccent = Color(0xFF00D9C0)

// Status Colors
val StatusSuccess = Color(0xFF10B981)
val StatusError = Color(0xFFEF4444)
val StatusWarning = Color(0xFFF59E0B)
val StatusInfo = Color(0xFF3B82F6)
val StatusRunning = Color(0xFF8B5CF6)

// ============================================
// NEUMORPHIC DESIGN SYSTEM - Light Theme
// ============================================

// Light Theme - Main Background (the "canvas")
val NeuroBackgroundLight = Color(0xFFE8EDF2)  // Soft grayish-blue
val NeuroSurfaceLight = Color(0xFFE8EDF2)     // Same as background for seamless look

// Light Theme - Shadows
val NeuroLightShadowLight = Color(0xFFFFFFFF)  // White highlight (top-left)
val NeuroDarkShadowLight = Color(0xFFBEC8D1)   // Darker shadow (bottom-right)

// Light Theme - Text
val TextPrimaryLight = Color(0xFF2D3748)    // Dark gray for readability
val TextSecondaryLight = Color(0xFF718096)  // Medium gray

// ============================================
// NEUMORPHIC DESIGN SYSTEM - Dark Theme
// ============================================

// Dark Theme - Main Background
val NeuroBackgroundDark = Color(0xFF2D3142)   // Deep blue-gray
val NeuroSurfaceDark = Color(0xFF2D3142)      // Same as background

// Dark Theme - Shadows
val NeuroLightShadowDark = Color(0xFF3D4355)  // Lighter shade (subtle highlight)
val NeuroDarkShadowDark = Color(0xFF1D212F)   // Darker shade (deep shadow)

// Dark Theme - Text
val TextPrimaryDark = Color(0xFFF7FAFC)     // Almost white
val TextSecondaryDark = Color(0xFFA0AEC0)   // Light gray

// ============================================
// NEUMORPHIC COMPONENT COLORS
// ============================================

// Pressed/Inset state colors
val NeuroInsetLightShadowLight = Color(0xFFD1D9E6)
val NeuroInsetDarkShadowLight = Color(0xFFFFFFFF)

val NeuroInsetLightShadowDark = Color(0xFF23273A)
val NeuroInsetDarkShadowDark = Color(0xFF373D4F)

// Accent gradients for cards with status
val NeuroAccentSuccess = Color(0xFF10B981)
val NeuroAccentError = Color(0xFFEF4444)
val NeuroAccentWarning = Color(0xFFF59E0B)
val NeuroAccentRunning = Color(0xFF8B5CF6)

// ============================================
// LEGACY COMPATIBILITY (keep for existing code)
// ============================================

val BackgroundLight = NeuroBackgroundLight
val SurfaceLight = NeuroSurfaceLight
val BackgroundDark = NeuroBackgroundDark
val SurfaceDark = NeuroSurfaceDark

// Compatibility / Aliases
val Background = BackgroundLight
val N8nPrimaryLight = N8nPrimaryVariant

// ============================================
// NEUMORPHIC SHADOW DIMENSIONS (Standardized)
// ============================================

// Standard shadow sizes for consistent neumorphic design
object NeumorphicDimensions {
    // Card shadows (main containers)
    const val CardShadowOffset = 6f      // dp
    const val CardShadowBlur = 12f       // dp
    const val CardCornerRadius = 20f     // dp
    
    // Small elements (buttons, icons)
    const val SmallShadowOffset = 4f     // dp
    const val SmallShadowBlur = 8f       // dp
    const val SmallCornerRadius = 14f    // dp
    
    // Pressed/inset states
    const val PressedShadowOffset = 2f   // dp
    const val PressedShadowBlur = 4f     // dp
    
    // Large elements (bottom nav, headers)
    const val LargeShadowOffset = 8f     // dp
    const val LargeShadowBlur = 16f      // dp
    const val LargeCornerRadius = 24f    // dp
    
    // Icon containers
    const val IconContainerSize = 44f    // dp
    const val IconContainerRadius = 12f  // dp
    
    // Toggle/Switch
    const val ToggleWidth = 52f          // dp
    const val ToggleHeight = 28f         // dp
    const val ToggleThumbSize = 24f      // dp
}
