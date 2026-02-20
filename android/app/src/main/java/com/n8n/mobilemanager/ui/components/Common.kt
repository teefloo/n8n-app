package com.n8n.mobilemanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.theme.*

// ============================================
// EMPTY STATE COMPONENT
// ============================================

/**
 * Neumorphic styled empty state for when there's no content
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = neumorphicColors()
    
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with neumorphic effect - use neumorphicRaised instead of shadow()
        Box(
            modifier = Modifier
                .size(80.dp)
                .neumorphicRaised(
                    lightShadowColor = colors.lightShadow,
                    darkShadowColor = colors.darkShadow,
                    backgroundColor = colors.surface,
                    cornerRadius = 40.dp,
                    shadowOffset = NeumorphicDimensions.CardShadowOffset.dp,
                    shadowBlur = NeumorphicDimensions.CardShadowBlur.dp
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

// ============================================
// LOADING STATE COMPONENT
// ============================================

/**
 * Neumorphic styled loading indicator
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    val colors = neumorphicColors()
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neumorphic loading container - use neumorphicRaised instead of shadow()
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .neumorphicRaised(
                        lightShadowColor = colors.lightShadow,
                        darkShadowColor = colors.darkShadow,
                        backgroundColor = colors.surface,
                        cornerRadius = 32.dp,
                        shadowOffset = NeumorphicDimensions.CardShadowOffset.dp,
                        shadowBlur = NeumorphicDimensions.CardShadowBlur.dp
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = colors.primary,
                    strokeWidth = 3.dp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

// ============================================
// EXECUTION STATUS CHIP - NEUMORPHIC STYLE
// ============================================

/**
 * Returns status color based on execution status
 */
@Composable
fun getStatusColor(status: ExecutionStatus): Color {
    return when (status) {
        ExecutionStatus.SUCCESS -> StatusSuccess
        ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> StatusError
        ExecutionStatus.RUNNING -> StatusRunning
        ExecutionStatus.WAITING, ExecutionStatus.QUEUED -> StatusWarning
        ExecutionStatus.CANCELED -> neumorphicColors().onSurfaceVariant
    }
}

/**
 * Returns status icon based on execution status
 */
@Composable
fun getStatusIcon(status: ExecutionStatus): ImageVector {
    return when (status) {
        ExecutionStatus.SUCCESS -> Icons.Filled.CheckCircle
        ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> Icons.Filled.Error
        ExecutionStatus.RUNNING -> Icons.Filled.PlayCircle
        ExecutionStatus.WAITING, ExecutionStatus.QUEUED -> Icons.Filled.Schedule
        ExecutionStatus.CANCELED -> Icons.Filled.PauseCircle
    }
}

/**
 * Returns user-friendly status label in English
 */
fun getStatusLabel(status: ExecutionStatus): String {
    return when (status) {
        ExecutionStatus.SUCCESS -> "Success"
        ExecutionStatus.ERROR -> "Error"
        ExecutionStatus.CRASHED -> "Crashed"
        ExecutionStatus.RUNNING -> "Running"
        ExecutionStatus.WAITING -> "Waiting"
        ExecutionStatus.QUEUED -> "Queued"
        ExecutionStatus.CANCELED -> "Canceled"
    }
}

/**
 * Neumorphic styled execution status chip
 */
@Composable
fun ExecutionStatusChip(status: ExecutionStatus) {
    val colors = neumorphicColors()
    val statusColor = getStatusColor(status)
    val statusIcon = getStatusIcon(status)
    val statusLabel = getStatusLabel(status)
    
    // Simple chip without shadow - cleaner look, works in dark mode
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.10f),
                        statusColor.copy(alpha = 0.16f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = statusColor
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

/**
 * String version of ExecutionStatusChip for backward compatibility
 */
@Composable
fun ExecutionStatusChip(status: String) {
    val executionStatus = try {
        ExecutionStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        ExecutionStatus.ERROR // Fallback to ERROR for unknown statuses
    }
    ExecutionStatusChip(executionStatus)
}

// ============================================
// NEUMORPHIC DIVIDER
// ============================================

/**
 * Subtle neumorphic divider
 */
@Composable
fun NeumorphicDivider(
    modifier: Modifier = Modifier,
    thickness: Float = 1f
) {
    val colors = neumorphicColors()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.darkShadow.copy(alpha = 0.1f),
                        colors.lightShadow.copy(alpha = 0.5f)
                    )
                )
            )
    )
}

// ============================================
// NEUMORPHIC BADGE
// ============================================

/**
 * Small badge for notifications or counts
 */
@Composable
fun NeumorphicBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = N8nPrimary
) {
    // Simple badge without shadow - cleaner look
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ============================================
// ANIMATED PULSE DOT
// ============================================

/**
 * Animated pulsing dot for status indicators
 */
@Composable
fun PulseDot(
    modifier: Modifier = Modifier,
    color: Color = StatusSuccess,
    size: Int = 10
) {
    val pulseAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "pulseAlpha"
    )
    
    // Simple dot without shadow - cleaner look
    Box(
        modifier = modifier
            .size(size.dp)
            .graphicsLayer {
                alpha = pulseAlpha
            }
            .clip(CircleShape)
            .background(color)
    )
}
