package com.n8n.mobilemanager.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.nativeCanvas
import com.n8n.mobilemanager.ui.theme.*

// ============================================
// NEUMORPHIC DESIGN SYSTEM
// ============================================

/**
 * Data class containing all colors needed for neumorphic effects
 */
data class NeumorphicColors(
    val background: Color,
    val surface: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val insetLightShadow: Color,
    val insetDarkShadow: Color,
    val primary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

/**
 * Provides neumorphic colors based on current theme
 * Uses MaterialTheme.colorScheme.background to detect theme instead of isSystemInDarkTheme()
 * This ensures neumorphic components follow the app's theme preference, not system theme
 */
@Composable
fun neumorphicColors(): NeumorphicColors {
    // Detect dark theme by checking if background matches dark theme color
    val isDark = MaterialTheme.colorScheme.background == NeuroBackgroundDark
    
    return if (isDark) {
        NeumorphicColors(
            background = NeuroBackgroundDark,
            surface = NeuroSurfaceDark,
            lightShadow = NeuroLightShadowDark,
            darkShadow = NeuroDarkShadowDark,
            insetLightShadow = NeuroInsetLightShadowDark,
            insetDarkShadow = NeuroInsetDarkShadowDark,
            primary = N8nPrimary,
            onSurface = TextPrimaryDark,
            onSurfaceVariant = TextSecondaryDark
        )
    } else {
        NeumorphicColors(
            background = NeuroBackgroundLight,
            surface = NeuroSurfaceLight,
            lightShadow = NeuroLightShadowLight,
            darkShadow = NeuroDarkShadowLight,
            insetLightShadow = NeuroInsetLightShadowLight,
            insetDarkShadow = NeuroInsetDarkShadowLight,
            primary = N8nPrimary,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight
        )
    }
}

/**
 * Modifier extension for neumorphic raised (convex) shadow effect
 * Creates the characteristic "popping out" look with TWO shadows:
 * - Light shadow on top-left (highlight)
 * - Dark shadow on bottom-right (shadow)
 */
fun Modifier.neumorphicRaised(
    lightShadowColor: Color,
    darkShadowColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp = 20.dp,
    shadowOffset: Dp = 8.dp,
    shadowBlur: Dp = 16.dp
): Modifier = this
    .drawBehind {
        val shadowOffsetPx = shadowOffset.toPx()
        val blurRadiusPx = shadowBlur.toPx()
        val cornerRadiusPx = cornerRadius.toPx()
        
        // Draw dark shadow (bottom-right)
        drawIntoCanvas { canvas ->
            val darkPaint = android.graphics.Paint().apply {
                color = darkShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                shadowOffsetPx,
                shadowOffsetPx,
                size.width + shadowOffsetPx,
                size.height + shadowOffsetPx,
                cornerRadiusPx,
                cornerRadiusPx,
                darkPaint
            )
        }
        
        // Draw light shadow (top-left)
        drawIntoCanvas { canvas ->
            val lightPaint = android.graphics.Paint().apply {
                color = lightShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                -shadowOffsetPx,
                -shadowOffsetPx,
                size.width - shadowOffsetPx,
                size.height - shadowOffsetPx,
                cornerRadiusPx,
                cornerRadiusPx,
                lightPaint
            )
        }
        
        // Draw the main background
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    }

/**
 * Modifier for inset (concave) neumorphic effect
 * Creates the characteristic "pressed in" look with inner shadows
 */
fun Modifier.neumorphicInset(
    lightShadowColor: Color,
    darkShadowColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    shadowBlur: Dp = 8.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .drawBehind {
        val shadowOffsetPx = shadowOffset.toPx()
        val blurRadiusPx = shadowBlur.toPx()
        val cornerRadiusPx = cornerRadius.toPx()
        
        // Draw main background first
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        
        // Inner dark shadow (top-left, simulating depth)
        drawIntoCanvas { canvas ->
            val darkPaint = android.graphics.Paint().apply {
                color = darkShadowColor.copy(alpha = 0.5f).toArgb()
                maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.INNER)
            }
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerRadiusPx,
                cornerRadiusPx,
                darkPaint
            )
        }
    }

// ============================================
// NEUMORPHIC CARD COMPONENT
// ============================================

/**
 * Neumorphic Card - The core container component
 * Provides TRUE neumorphic raised appearance with dual shadows:
 * - Light shadow on top-left (creates highlight)
 * - Dark shadow on bottom-right (creates depth)
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    isPressed: Boolean = false,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    shadowOffset: Dp = 6.dp,
    shadowBlur: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = neumorphicColors()
    val bgColor = backgroundColor ?: colors.background
    
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val actualPressed = isPressed || pressed
    
    // Animate shadow offset for press effect
    val animatedShadowOffset by animateDpAsState(
        targetValue = if (actualPressed) 2.dp else shadowOffset,
        animationSpec = tween(durationMillis = 150),
        label = "shadowOffset"
    )
    
    val animatedBlur by animateDpAsState(
        targetValue = if (actualPressed) 4.dp else shadowBlur,
        animationSpec = tween(durationMillis = 150),
        label = "shadowBlur"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (actualPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .neumorphicRaised(
                lightShadowColor = colors.lightShadow,
                darkShadowColor = colors.darkShadow,
                backgroundColor = bgColor,
                cornerRadius = cornerRadius,
                shadowOffset = animatedShadowOffset,
                shadowBlur = animatedBlur
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

// ============================================
// NEUMORPHIC BUTTON COMPONENTS
// ============================================

/**
 * Neumorphic Button with text and optional icon
 * Uses true neumorphic dual shadow effect
 */
@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val colors = neumorphicColors()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor = if (isPrimary) {
        colors.primary
    } else {
        colors.background
    }
    
    val textColor = if (isPrimary) {
        Color.White
    } else {
        colors.primary
    }
    
    val shadowOffset by animateDpAsState(
        targetValue = if (pressed || !enabled) 2.dp else 5.dp,
        animationSpec = tween(durationMillis = 100),
        label = "buttonShadowOffset"
    )
    
    val shadowBlur by animateDpAsState(
        targetValue = if (pressed || !enabled) 4.dp else 10.dp,
        animationSpec = tween(durationMillis = 100),
        label = "buttonShadowBlur"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )
    
    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.6f
            }
            .neumorphicRaised(
                lightShadowColor = if (isPrimary) colors.primary.copy(alpha = 0.6f) else colors.lightShadow,
                darkShadowColor = if (isPrimary) colors.primary.copy(alpha = 0.4f) else colors.darkShadow,
                backgroundColor = backgroundColor,
                cornerRadius = 14.dp,
                shadowOffset = shadowOffset,
                shadowBlur = shadowBlur
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = textColor
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

/**
 * Generic Neumorphic Button with custom content
 */
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val neumorphColors = neumorphicColors()
    
    val elevation by animateDpAsState(
        targetValue = if (pressed || !enabled) 2.dp else 6.dp,
        animationSpec = tween(durationMillis = 100),
        label = "genericButtonElevation"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(14.dp),
                ambientColor = neumorphColors.darkShadow
            ),
        colors = colors,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        interactionSource = interactionSource,
        content = content
    )
}

// ============================================
// NEUMORPHIC ICON BUTTON
// ============================================

/**
 * Neumorphic circular icon button with true dual shadow effect
 */
@Composable
fun NeumorphicIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    isCircle: Boolean = true
) {
    val colors = neumorphicColors()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    
    val cornerRadius = if (isCircle) size / 2 else 12.dp
    
    val shadowOffset by animateDpAsState(
        targetValue = if (pressed) 2.dp else 4.dp,
        animationSpec = tween(durationMillis = 100),
        label = "iconButtonShadowOffset"
    )
    
    val shadowBlur by animateDpAsState(
        targetValue = if (pressed) 4.dp else 8.dp,
        animationSpec = tween(durationMillis = 100),
        label = "iconButtonShadowBlur"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "iconButtonScale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .neumorphicRaised(
                lightShadowColor = colors.lightShadow,
                darkShadowColor = colors.darkShadow,
                backgroundColor = colors.background,
                cornerRadius = cornerRadius,
                shadowOffset = shadowOffset,
                shadowBlur = shadowBlur
            )
            .clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}

// ============================================
// NEUMORPHIC TEXT FIELD
// ============================================

/**
 * Neumorphic styled text field with true inset appearance
 * Creates a "pressed in" effect for input fields
 */
@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    val colors = neumorphicColors()
    
    Box(
        modifier = modifier
            .neumorphicInset(
                lightShadowColor = colors.lightShadow,
                darkShadowColor = colors.darkShadow,
                backgroundColor = colors.background,
                cornerRadius = 16.dp,
                shadowOffset = 3.dp,
                shadowBlur = 6.dp
            )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    text = placeholder,
                    color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            },
            label = label?.let { 
                { 
                    Text(
                        text = it,
                        color = if (isError) StatusError else colors.onSurfaceVariant
                    ) 
                } 
            },
            leadingIcon = leadingIcon?.let { 
                { 
                    Icon(
                        imageVector = it, 
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                } 
            },
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedBorderColor = if (isError) StatusError else colors.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = StatusError,
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                cursorColor = colors.primary
            ),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            isError = isError,
            singleLine = singleLine,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ============================================
// NEUMORPHIC TOGGLE/SWITCH
// ============================================

/**
 * Neumorphic styled toggle switch
 * Uses custom drawing instead of shadow() for better dark mode support
 */
@Composable
fun NeumorphicToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = neumorphicColors()
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "toggleOffset"
    )
    
    val trackColor by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "toggleColor"
    )
    
    // Track with neumorphic inset effect
    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .neumorphicInset(
                lightShadowColor = colors.lightShadow,
                darkShadowColor = colors.darkShadow,
                backgroundColor = lerp(colors.surface, colors.primary, trackColor),
                cornerRadius = 14.dp,
                shadowOffset = 2.dp,
                shadowBlur = 4.dp
            )
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb with neumorphic raised effect
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .neumorphicRaised(
                    lightShadowColor = if (checked) colors.primary.copy(alpha = 0.4f) else colors.lightShadow,
                    darkShadowColor = if (checked) colors.primary.copy(alpha = 0.6f) else colors.darkShadow,
                    backgroundColor = Color.White,
                    cornerRadius = 12.dp,
                    shadowOffset = 2.dp,
                    shadowBlur = 4.dp
                )
                .clip(CircleShape)
        )
    }
}

/**
 * Helper function to interpolate between colors
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}