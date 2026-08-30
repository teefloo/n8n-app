package com.n8n.mobilemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.theme.NeuroBackgroundDark
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.ui.theme.StatusRunning
import com.n8n.mobilemanager.ui.theme.StatusSuccess
import com.n8n.mobilemanager.ui.theme.StatusWarning

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(Modifier.height(20.dp))
                action()
            }
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading…"
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun getStatusColor(status: ExecutionStatus): Color {
    val isDark = MaterialTheme.colorScheme.background == NeuroBackgroundDark
    return when (status) {
        ExecutionStatus.SUCCESS -> if (isDark) Color(0xFF6DDB9A) else StatusSuccess
        ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> MaterialTheme.colorScheme.error
        ExecutionStatus.RUNNING -> if (isDark) Color(0xFFD0BCFF) else StatusRunning
        ExecutionStatus.WAITING, ExecutionStatus.QUEUED -> if (isDark) Color(0xFFFFC66D) else StatusWarning
        ExecutionStatus.CANCELED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

fun getStatusIcon(status: ExecutionStatus): ImageVector = when (status) {
    ExecutionStatus.SUCCESS -> Icons.Filled.CheckCircle
    ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> Icons.Filled.Error
    ExecutionStatus.RUNNING -> Icons.Filled.PlayCircle
    ExecutionStatus.WAITING, ExecutionStatus.QUEUED -> Icons.Filled.Schedule
    ExecutionStatus.CANCELED -> Icons.Filled.PauseCircle
}

fun getStatusLabel(status: ExecutionStatus): String = when (status) {
    ExecutionStatus.SUCCESS -> "Success"
    ExecutionStatus.ERROR -> "Error"
    ExecutionStatus.CRASHED -> "Crashed"
    ExecutionStatus.RUNNING -> "Running"
    ExecutionStatus.WAITING -> "Waiting"
    ExecutionStatus.QUEUED -> "Queued"
    ExecutionStatus.CANCELED -> "Canceled"
}

@Composable
fun ExecutionStatusChip(status: ExecutionStatus) {
    val statusColor = getStatusColor(status)
    Surface(
        color = statusColor.copy(alpha = 0.12f),
        contentColor = statusColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getStatusIcon(status),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = getStatusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExecutionStatusChip(status: String) {
    val executionStatus = runCatching { ExecutionStatus.valueOf(status.uppercase()) }
        .getOrDefault(ExecutionStatus.ERROR)
    ExecutionStatusChip(executionStatus)
}

@Composable
fun NeumorphicDivider(
    modifier: Modifier = Modifier,
    thickness: Float = 1f
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    )
}

@Composable
fun NeumorphicBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = N8nPrimary
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PulseDot(
    modifier: Modifier = Modifier,
    color: Color = StatusSuccess,
    size: Int = 10
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}
