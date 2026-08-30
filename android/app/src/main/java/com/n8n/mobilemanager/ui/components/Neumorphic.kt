package com.n8n.mobilemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.n8n.mobilemanager.ui.theme.NeuroBackgroundDark
import com.n8n.mobilemanager.ui.theme.NeuroBackgroundLight
import com.n8n.mobilemanager.ui.theme.NeuroDarkShadowDark
import com.n8n.mobilemanager.ui.theme.NeuroDarkShadowLight
import com.n8n.mobilemanager.ui.theme.NeuroInsetDarkShadowDark
import com.n8n.mobilemanager.ui.theme.NeuroInsetDarkShadowLight
import com.n8n.mobilemanager.ui.theme.NeuroInsetLightShadowDark
import com.n8n.mobilemanager.ui.theme.NeuroInsetLightShadowLight
import com.n8n.mobilemanager.ui.theme.NeuroLightShadowDark
import com.n8n.mobilemanager.ui.theme.NeuroLightShadowLight
import com.n8n.mobilemanager.ui.theme.NeuroSurfaceDark
import com.n8n.mobilemanager.ui.theme.NeuroSurfaceLight
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.ui.theme.TextPrimaryDark
import com.n8n.mobilemanager.ui.theme.TextPrimaryLight
import com.n8n.mobilemanager.ui.theme.TextSecondaryDark
import com.n8n.mobilemanager.ui.theme.TextSecondaryLight

/**
 * Compatibility data for screens that still use the historical component API.
 * Values now come from the active Material color scheme.
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

@Composable
fun neumorphicColors(): NeumorphicColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background == NeuroBackgroundDark
    return NeumorphicColors(
        background = scheme.background,
        surface = scheme.surface,
        lightShadow = if (isDark) NeuroLightShadowDark else NeuroLightShadowLight,
        darkShadow = if (isDark) NeuroDarkShadowDark else NeuroDarkShadowLight,
        insetLightShadow = if (isDark) NeuroInsetLightShadowDark else NeuroInsetLightShadowLight,
        insetDarkShadow = if (isDark) NeuroInsetDarkShadowDark else NeuroInsetDarkShadowLight,
        primary = scheme.primary,
        onSurface = scheme.onSurface,
        onSurfaceVariant = scheme.onSurfaceVariant
    )
}

/**
 * Legacy modifier retained for compatibility. It intentionally produces a
 * restrained Material-like surface instead of a high-contrast shadow effect.
 */
fun Modifier.neumorphicRaised(
    lightShadowColor: Color,
    darkShadowColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 2.dp,
    shadowBlur: Dp = 4.dp
): Modifier = this
    .shadow(
        elevation = if (shadowBlur > 0.dp) 1.dp else 0.dp,
        shape = RoundedCornerShape(cornerRadius),
        clip = false
    )
    .background(backgroundColor, RoundedCornerShape(cornerRadius))

fun Modifier.neumorphicInset(
    lightShadowColor: Color,
    darkShadowColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp = 12.dp,
    shadowOffset: Dp = 1.dp,
    shadowBlur: Dp = 2.dp
): Modifier = this.background(backgroundColor, RoundedCornerShape(cornerRadius))

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isPressed: Boolean = false,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    shadowOffset: Dp = 2.dp,
    shadowBlur: Dp = 4.dp,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceColor = backgroundColor ?: MaterialTheme.colorScheme.surface
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(if (onClick != null) 1.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), shape)
            .then(clickableModifier),
        content = content
    )
}

@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(if (icon != null) 8.dp else 0.dp))
        androidx.compose.material3.Text(text = text, style = MaterialTheme.typography.labelLarge)
    }

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            content = content
        )
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = colors,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
fun NeumorphicIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    isCircle: Boolean = true,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(if (size < 48.dp) 48.dp else size)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}

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
    val resolvedLabel = label ?: placeholder.takeIf { it.isNotBlank() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 56.dp),
        label = resolvedLabel?.let { { androidx.compose.material3.Text(it) } },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = isError,
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun NeumorphicToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
