package com.n8n.mobilemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n8n.mobilemanager.ui.theme.*

/**
 * Neumorphic styled Workflow Card
 * Shows workflow info with active/inactive toggle
 */
@Composable
fun WorkflowCard(
    name: String,
    isActive: Boolean,
    lastExecutionStatus: String?,
    nodesCount: Int,
    isToggling: Boolean,
    onToggleActive: () -> Unit,
    onClick: () -> Unit
) {
    val colors = neumorphicColors()
    
    // Animate the accent color based on active state
    val accentColor by animateColorAsState(
        targetValue = if (isActive) StatusSuccess else colors.onSurfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 300),
        label = "accentColor"
    )
    
    NeumorphicCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = NeumorphicDimensions.CardCornerRadius.dp,
        shadowOffset = NeumorphicDimensions.CardShadowOffset.dp,
        shadowBlur = NeumorphicDimensions.CardShadowBlur.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Subtle gradient accent at the top when active
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isActive) 0.08f else 0.02f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 150f
                    )
                )
                .padding(18.dp)
        ) {
            // Header row with icon and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workflow icon with neumorphic container - NO extra shadow, just styled box
                Box(
                    modifier = Modifier
                        .size(NeumorphicDimensions.IconContainerSize.dp)
                        .clip(RoundedCornerShape(NeumorphicDimensions.IconContainerRadius.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.12f),
                                    accentColor.copy(alpha = 0.20f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountTree,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Toggle switch with loading indicator
                Box(contentAlignment = Alignment.Center) {
                    if (isToggling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    } else {
                        NeumorphicToggle(
                            checked = isActive,
                            onCheckedChange = { onToggleActive() }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Workflow name
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nodes count chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$nodesCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "nœuds",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Status badge - NO extra shadow on the dot
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status dot - simple, no shadow
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = if (isActive) "Actif" else "Inactif",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                }
            }
            
            // Last execution status (if available)
            lastExecutionStatus?.let { status ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Dernière exécution:",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    ExecutionStatusChip(status = status)
                }
            }
        }
    }
}
