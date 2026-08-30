package com.n8n.mobilemanager.ui.screens.executions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.ExecutionStatusChip
import com.n8n.mobilemanager.ui.components.LoadingState
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicCard
import com.n8n.mobilemanager.ui.components.NeumorphicIconButton
import com.n8n.mobilemanager.ui.components.getStatusColor
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.utils.DateUtils

@Composable
fun ExecutionDetailScreen(
    executionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    viewModel: ExecutionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { N8nTopAppBar(title = "Execution detail", onBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.execution == null -> LoadingState(message = "Loading execution…")
                uiState.execution == null -> {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Unable to load execution",
                        message = uiState.error ?: "The execution is not available.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        action = {
                            androidx.compose.material3.TextButton(onClick = viewModel::loadExecution) {
                                Text("Try again")
                            }
                        }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.error?.let { message ->
                            N8nErrorBanner(message = message, actionLabel = "Retry", onAction = viewModel::loadExecution)
                        }
                        ExecutionContent(
                            execution = uiState.execution!!,
                            actionInProgress = uiState.isLoading,
                            onRetry = viewModel::retryExecution,
                            onStop = viewModel::stopExecution
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionContent(
    execution: Execution,
    actionInProgress: Boolean,
    onRetry: () -> Unit,
    onStop: () -> Unit
) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExecutionStatusChip(execution.status)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (execution.status == ExecutionStatus.RUNNING) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Stop,
                            onClick = onStop,
                            iconSize = 20.dp,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = "Stop execution",
                            enabled = !actionInProgress
                        )
                    } else if (execution.status == ExecutionStatus.ERROR || execution.status == ExecutionStatus.CRASHED) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Refresh,
                            onClick = onRetry,
                            iconSize = 20.dp,
                            tint = N8nPrimary,
                            contentDescription = "Retry execution",
                            enabled = !actionInProgress
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Workflow", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    execution.workflowName ?: "Workflow ${execution.workflowId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 420.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetadataItem("ID", "#${execution.id}", monospace = true)
                        MetadataItem("Mode", execution.mode.name)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetadataItem("ID", "#${execution.id}", monospace = true)
                        MetadataItem("Mode", execution.mode.name)
                    }
                }
            }
        }
    }

    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 420.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TimelineItem(Icons.Outlined.PlayArrow, "Start", DateUtils.formatFullDate(execution.startedAt))
                        execution.stoppedAt?.let { TimelineItem(Icons.Filled.Stop, "End", DateUtils.formatFullDate(it)) }
                        execution.stoppedAt?.let { TimelineItem(Icons.Outlined.Timer, "Duration", DateUtils.calculateDuration(execution.startedAt, it)) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimelineItem(Icons.Outlined.PlayArrow, "Start", DateUtils.formatFullDate(execution.startedAt))
                        execution.stoppedAt?.let { TimelineItem(Icons.Filled.Stop, "End", DateUtils.formatFullDate(it)) }
                        execution.stoppedAt?.let { TimelineItem(Icons.Outlined.Timer, "Duration", DateUtils.calculateDuration(execution.startedAt, it)) }
                    }
                }
            }
        }
    }

    execution.data?.resultData?.error?.let { error ->
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Error details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
                Text(error.message, style = MaterialTheme.typography.bodyMedium)
                error.node?.let { Text("Node: $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                error.stack?.takeIf { it.isNotBlank() }?.let { stack ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(stack, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(label: String, value: String, monospace: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default)
    }
}

@Composable
private fun TimelineItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
