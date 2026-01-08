package com.n8n.mobilemanager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.theme.*

// ==================== Neumorphic Modifier Extensions ====================

fun Modifier.neumorphicShadow(
    lightShadowColor: Color,
    darkShadowColor: Color,
    shadowRadius: Dp = 12.dp,
    shadowOffset: Dp = 6.dp,
    cornerRadius: Dp = 20.dp,
    isPressed: Boolean = false
): Modifier = this
    .drawBehind {
        val actualLightShadow = if (isPressed) darkShadowColor else lightShadowColor
        val actualDarkShadow = if (isPressed) lightShadowColor else darkShadowColor
        val offsetPx = shadowOffset.toPx()
        val radiusPx = shadowRadius.toPx()
        
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            
            // Light shadow (top-left)
            frameworkPaint.color = actualLightShadow.toArgb()
            frameworkPaint.setShadowLayer(
                radiusPx,
                -offsetPx,
                -offsetPx,
                actualLightShadow.toArgb()
            )
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadius.toPx(), cornerRadius.toPx(),
                paint
            )
            
            // Dark shadow (bottom-right)
            frameworkPaint.color = actualDarkShadow.toArgb()
            frameworkPaint.setShadowLayer(
                radiusPx,
                offsetPx,
                offsetPx,
                actualDarkShadow.toArgb()
            )
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadius.toPx(), cornerRadius.toPx(),
                paint
            )
        }
    }

// ==================== Neumorphic Card Component ====================

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isPressed: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val neumorphColors = neumorphicColors()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val actualPressed = isPressed || pressed
    
    val animatedOffset by animateDpAsState(
        targetValue = if (actualPressed) 2.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shadow_offset"
    )
    
    Box(
        modifier = modifier
            .neumorphicShadow(
                lightShadowColor = neumorphColors.lightShadow,
                darkShadowColor = neumorphColors.darkShadow,
                shadowOffset = animatedOffset,
                cornerRadius = cornerRadius,
                isPressed = actualPressed
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(neumorphColors.surface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        Column(content = content)
    }
}

// ==================== Neumorphic Button ====================

@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isPrimary: Boolean = true
) {
    val neumorphColors = neumorphicColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonColor = if (isPrimary) N8nPrimary else neumorphColors.surface
    val textColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = modifier
            .height(56.dp)
            .neumorphicShadow(
                lightShadowColor = if (isPrimary) N8nPrimaryLight.copy(alpha = 0.5f) else neumorphColors.lightShadow,
                darkShadowColor = if (isPrimary) N8nPrimaryVariant else neumorphColors.darkShadow,
                shadowOffset = if (isPressed && enabled) 2.dp else 5.dp,
                cornerRadius = 16.dp,
                isPressed = isPressed && enabled
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPrimary) {
                    Brush.linearGradient(
                        colors = listOf(N8nPrimaryLight, N8nPrimary)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(neumorphColors.surface, neumorphColors.surface)
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f)
            )
        }
    }
}

// ==================== Neumorphic Icon Button ====================

@Composable
fun NeumorphicIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val neumorphColors = neumorphicColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .size(size)
            .neumorphicShadow(
                lightShadowColor = neumorphColors.lightShadow,
                darkShadowColor = neumorphColors.darkShadow,
                shadowOffset = if (isPressed) 2.dp else 4.dp,
                cornerRadius = size / 2,
                isPressed = isPressed
            )
            .clip(CircleShape)
            .background(neumorphColors.surface)
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
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ==================== Status Indicator ====================

@Composable
fun StatusIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (isOnline) StatusSuccess else StatusError,
        animationSpec = tween(300),
        label = "status_color"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((12 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.3f))
            )
        }
        // Inner solid
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ==================== Execution Status Chip ====================

@Composable
fun ExecutionStatusChip(
    status: ExecutionStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon) = when (status) {
        ExecutionStatus.SUCCESS -> Triple(StatusSuccessMuted.copy(alpha = 0.4f), StatusSuccess, Icons.Filled.CheckCircle)
        ExecutionStatus.ERROR -> Triple(StatusErrorMuted.copy(alpha = 0.4f), StatusError, Icons.Filled.Error)
        ExecutionStatus.RUNNING -> Triple(StatusRunningMuted.copy(alpha = 0.4f), StatusRunning, Icons.Filled.PlayCircle)
        ExecutionStatus.WAITING -> Triple(StatusWarningMuted.copy(alpha = 0.4f), StatusWarning, Icons.Filled.Schedule)
        ExecutionStatus.CANCELED -> Triple(StatusWarningMuted.copy(alpha = 0.4f), StatusWarning, Icons.Filled.Cancel)
        ExecutionStatus.CRASHED -> Triple(StatusErrorMuted.copy(alpha = 0.4f), StatusError, Icons.Filled.Warning)
        ExecutionStatus.NEW -> Triple(StatusInfoMuted.copy(alpha = 0.4f), StatusInfo, Icons.Filled.FiberNew)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (status == ExecutionStatus.RUNNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==================== Neumorphic Stats Card ====================

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Neumorphic icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .neumorphicShadow(
                            lightShadowColor = neumorphColors.lightShadow,
                            darkShadowColor = neumorphColors.darkShadow,
                            shadowOffset = 3.dp,
                            shadowRadius = 6.dp,
                            cornerRadius = 14.dp
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    iconTint.copy(alpha = 0.15f),
                                    iconTint.copy(alpha = 0.25f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==================== Neumorphic Workflow Card ====================

@Composable
fun WorkflowCard(
    name: String,
    isActive: Boolean,
    lastExecutionStatus: ExecutionStatus?,
    nodesCount: Int,
    modifier: Modifier = Modifier,
    isToggling: Boolean = false,
    onToggleActive: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = if (!isToggling) onClick else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator with neumorphic effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .neumorphicShadow(
                        lightShadowColor = neumorphColors.lightShadow,
                        darkShadowColor = neumorphColors.darkShadow,
                        shadowOffset = 3.dp,
                        cornerRadius = 14.dp
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isActive) {
                            Brush.linearGradient(
                                colors = listOf(
                                    N8nPrimary.copy(alpha = 0.15f),
                                    N8nPrimary.copy(alpha = 0.25f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    neumorphColors.surface,
                                    neumorphColors.surface
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isToggling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = N8nPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.PlayCircle else Icons.Outlined.PauseCircle,
                        contentDescription = null,
                        tint = if (isActive) N8nPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Workflow info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nodes count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$nodesCount nodes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    lastExecutionStatus?.let {
                        ExecutionStatusChip(status = it)
                    }
                }
            }
            
            // Neumorphic toggle
            NeumorphicToggle(
                checked = isActive,
                onCheckedChange = onToggleActive,
                enabled = !isToggling
            )
        }
    }
}

// ==================== Neumorphic Toggle ====================

@Composable
fun NeumorphicToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val neumorphColors = neumorphicColors()
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "thumb_offset"
    )
    
    val trackColor by animateColorAsState(
        targetValue = if (checked) N8nPrimary.copy(alpha = 0.3f) else neumorphColors.pressedBackground,
        animationSpec = tween(200),
        label = "track_color"
    )
    
    val thumbColor by animateColorAsState(
        targetValue = if (checked) N8nPrimary else neumorphColors.surface,
        animationSpec = tween(200),
        label = "thumb_color"
    )
    
    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .shadow(
                    elevation = if (checked) 4.dp else 2.dp,
                    shape = CircleShape,
                    spotColor = if (checked) N8nPrimary.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.1f)
                )
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

// ==================== Neumorphic Execution Card ====================

@Composable
fun ExecutionCard(
    workflowName: String,
    status: ExecutionStatus,
    startedAt: String,
    duration: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    NeumorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExecutionStatusChip(status = status)
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = workflowName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = startedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            duration?.let {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ==================== Neumorphic Credential Card ====================

@Composable
fun CredentialCard(
    name: String,
    type: String,
    lastUpdated: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    NeumorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val neumorphColors = neumorphicColors()
            
            // Neumorphic icon container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .neumorphicShadow(
                        lightShadowColor = neumorphColors.lightShadow,
                        darkShadowColor = neumorphColors.darkShadow,
                        shadowOffset = 3.dp,
                        cornerRadius = 13.dp
                    )
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                N8nAccent.copy(alpha = 0.15f),
                                N8nAccent.copy(alpha = 0.25f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = N8nAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastUpdated,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==================== Neumorphic Empty State ====================

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val neumorphColors = neumorphicColors()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Neumorphic icon container
        Box(
            modifier = Modifier
                .size(96.dp)
                .neumorphicShadow(
                    lightShadowColor = neumorphColors.lightShadow,
                    darkShadowColor = neumorphColors.darkShadow,
                    shadowOffset = 6.dp,
                    cornerRadius = 48.dp
                )
                .clip(CircleShape)
                .background(neumorphColors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        action?.let {
            Spacer(modifier = Modifier.height(24.dp))
            it()
        }
    }
}

// ==================== Neumorphic Loading State ====================

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Chargement..."
) {
    val neumorphColors = neumorphicColors()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .neumorphicShadow(
                    lightShadowColor = neumorphColors.lightShadow,
                    darkShadowColor = neumorphColors.darkShadow,
                    shadowOffset = 5.dp,
                    cornerRadius = 40.dp
                )
                .clip(CircleShape)
                .background(neumorphColors.surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = N8nPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Gradient Button (Legacy Support) ====================

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    NeumorphicButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        icon = icon,
        isPrimary = true
    )
}

// ==================== Neumorphic Text Field ====================

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    val neumorphColors = neumorphicColors()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .neumorphicShadow(
                lightShadowColor = neumorphColors.darkShadow.copy(alpha = 0.3f),
                darkShadowColor = neumorphColors.lightShadow,
                shadowOffset = 3.dp,
                shadowRadius = 8.dp,
                cornerRadius = 16.dp,
                isPressed = true
            )
            .clip(RoundedCornerShape(16.dp))
            .background(neumorphColors.pressedBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = singleLine,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(12.dp))
                it()
            }
        }
    }
}
