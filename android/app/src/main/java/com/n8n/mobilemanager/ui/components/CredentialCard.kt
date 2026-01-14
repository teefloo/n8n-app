package com.n8n.mobilemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n8n.mobilemanager.ui.theme.*

/**
 * Neumorphic styled Credential Card
 * Displays credential information with a secure feel
 */
@Composable
fun CredentialCard(
    name: String,
    type: String,
    lastUpdated: String,
    onClick: () -> Unit
) {
    val colors = neumorphicColors()
    
    NeumorphicCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = NeumorphicDimensions.CardCornerRadius.dp,
        shadowOffset = NeumorphicDimensions.CardShadowOffset.dp,
        shadowBlur = NeumorphicDimensions.CardShadowBlur.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Key icon with styled container - NO extra shadow (already in NeumorphicCard)
            Box(
                modifier = Modifier
                    .size(NeumorphicDimensions.IconContainerSize.dp)
                    .clip(RoundedCornerShape(NeumorphicDimensions.SmallCornerRadius.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.08f),
                                colors.primary.copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Credential name
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                
                // Last updated
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = colors.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = lastUpdated,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Chevron indicator
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Voir détails",
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = colors.background, // Transparent or subtle background
                        shape = RoundedCornerShape(8.dp)
                    ),
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

    }
}
