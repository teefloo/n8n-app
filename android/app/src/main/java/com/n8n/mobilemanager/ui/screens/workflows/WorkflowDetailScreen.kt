package com.n8n.mobilemanager.ui.screens.workflows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Webhook
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.data.model.Node
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.ExecutionStatusChip
import com.n8n.mobilemanager.ui.components.LoadingState
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nSectionHeader
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicCard
import com.n8n.mobilemanager.ui.components.NeumorphicToggle
import com.n8n.mobilemanager.ui.components.getStatusColor
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.ui.theme.StatusSuccess
import com.n8n.mobilemanager.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowDetailScreen(
    viewModel: WorkflowDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToExecution: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            N8nTopAppBar(
                title = uiState.workflow?.name ?: "Workflow",
                onBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.workflow != null,
            onRefresh = viewModel::loadWorkflowDetails,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.workflow == null -> {
                    LoadingState(message = "Loading workflow…")
                }
                uiState.workflow == null -> {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Unable to load workflow",
                        message = uiState.error ?: "The workflow is not available.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        action = {
                            androidx.compose.material3.TextButton(onClick = viewModel::loadWorkflowDetails) {
                                Text("Try again")
                            }
                        }
                    )
                }
                else -> {
                    val workflow = uiState.workflow!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        uiState.error?.let { message ->
                            item {
                                N8nErrorBanner(message = message, actionLabel = "Retry", onAction = viewModel::loadWorkflowDetails)
                            }
                        }

                        item {
                            WorkflowHeaderCard(
                                name = workflow.name,
                                isActive = workflow.active,
                                isToggling = uiState.isTogglingActive,
                                onToggleActive = viewModel::toggleWorkflowActive,
                                createdAt = workflow.createdAt,
                                updatedAt = workflow.updatedAt
                            )
                        }

                        item {
                            WorkflowStatsRow(
                                nodesCount = workflow.nodes.size,
                                successCount = uiState.successCount,
                                errorCount = uiState.errorCount
                            )
                        }

                        if (workflow.tags.isNotEmpty()) {
                            item { N8nSectionHeader(title = "Tags") }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(workflow.tags, key = { it.id }) { tag -> TagChip(tag.name) }
                                }
                            }
                        }

                        if (workflow.nodes.isNotEmpty()) {
                            item { N8nSectionHeader(title = "Nodes (${workflow.nodes.size})") }
                            items(workflow.nodes.take(10), key = { it.id }) { node -> NodeCard(node) }
                            if (workflow.nodes.size > 10) {
                                item {
                                    Text(
                                        text = "+${workflow.nodes.size - 10} more nodes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.recentExecutions.isNotEmpty()) {
                            item { N8nSectionHeader(title = "Recent executions") }
                            items(uiState.recentExecutions, key = { it.id }) { execution ->
                                ExecutionMiniCard(execution) { onNavigateToExecution(execution.id) }
                            }
                        }

                        item { N8nSectionHeader(title = "Settings") }
                        item {
                            WorkflowSettingsCard(
                                workflowId = workflow.id,
                                timezone = workflow.settings?.timezone,
                                executionTimeout = workflow.settings?.executionTimeout,
                                saveManualExecutions = workflow.settings?.saveManualExecutions,
                                saveExecutionProgress = workflow.settings?.saveExecutionProgress,
                                saveDataErrorExecution = workflow.settings?.saveDataErrorExecution,
                                saveDataSuccessExecution = workflow.settings?.saveDataSuccessExecution
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowHeaderCard(
    name: String,
    isActive: Boolean,
    isToggling: Boolean,
    onToggleActive: () -> Unit,
    createdAt: String,
    updatedAt: String
) {
    val accent = if (isActive) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AccountTree, contentDescription = null, tint = accent)
                    }
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
                if (isToggling) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    NeumorphicToggle(checked = isActive, onCheckedChange = { onToggleActive() })
                }
            }

            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 420.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DateInfo("Created", createdAt)
                        DateInfo("Modified", updatedAt)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateInfo("Created", createdAt)
                        DateInfo("Modified", updatedAt)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateInfo(label: String, dateString: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(DateUtils.formatFullDate(dateString), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WorkflowStatsRow(nodesCount: Int, successCount: Int, errorCount: Int) {
    val stats = listOf(
        Triple(Icons.Outlined.Extension, nodesCount.toString(), "Nodes"),
        Triple(Icons.Filled.CheckCircle, successCount.toString(), "Success"),
        Triple(Icons.Filled.Error, errorCount.toString(), "Errors")
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEachIndexed { index, (icon, value, label) ->
            MiniStatCard(
                modifier = Modifier.weight(1f),
                icon = icon,
                value = value,
                label = label,
                color = when (index) {
                    0 -> N8nPrimary
                    1 -> StatusSuccess
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    NeumorphicCard(modifier = modifier, cornerRadius = 14.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TagChip(name: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NodeCard(node: Node) {
    val accent = if (node.disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(getNodeTypeIcon(node.type), contentDescription = null, tint = accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (node.disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    getNodeTypeDisplayName(node.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (node.disabled) {
                Text("Disabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExecutionMiniCard(execution: Execution, onClick: () -> Unit) {
    val statusColor = getStatusColor(execution.status)
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        onClick = onClick,
        onClickLabel = "Open execution"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (execution.status) {
                        ExecutionStatus.SUCCESS -> Icons.Filled.CheckCircle
                        ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> Icons.Filled.Error
                        ExecutionStatus.RUNNING -> Icons.Filled.PlayCircle
                        ExecutionStatus.WAITING, ExecutionStatus.QUEUED -> Icons.Outlined.Schedule
                        ExecutionStatus.CANCELED -> Icons.Outlined.Circle
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Execution #${execution.id}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    DateUtils.formatFullDate(execution.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExecutionStatusChip(execution.status)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkflowSettingsCard(
    workflowId: String,
    timezone: String?,
    executionTimeout: Int?,
    saveManualExecutions: Boolean?,
    saveExecutionProgress: Boolean?,
    saveDataErrorExecution: String?,
    saveDataSuccessExecution: String?
) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkflowSettingRow(Icons.Outlined.Tag, "ID", workflowId)
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WorkflowSettingRow(Icons.Outlined.Schedule, "Timezone", timezone ?: "Default (server)")
            WorkflowSettingRow(Icons.Outlined.Timer, "Execution timeout", executionTimeout?.let { "${it}s" } ?: "Unlimited")
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WorkflowSettingRow(Icons.Outlined.Save, "Save manual executions", saveManualExecutions.toSettingLabel())
            WorkflowSettingRow(Icons.Outlined.Pending, "Save progress", saveExecutionProgress.toSettingLabel())
            WorkflowSettingRow(Icons.Outlined.ErrorOutline, "Save on error", saveDataErrorExecution.toSettingLabel())
            WorkflowSettingRow(Icons.Filled.CheckCircle, "Save on success", saveDataSuccessExecution.toSettingLabel())
        }
    }
}

@Composable
private fun WorkflowSettingRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private fun Boolean?.toSettingLabel(): String = when (this) {
    true -> "Yes"
    false -> "No"
    null -> "Default"
}

private fun String?.toSettingLabel(): String = when (this) {
    "all" -> "All data"
    "none" -> "No data"
    null -> "Default"
    else -> this
}

private fun getNodeTypeIcon(type: String): ImageVector = when {
    type.contains("webhook", ignoreCase = true) -> Icons.Outlined.Webhook
    type.contains("http", ignoreCase = true) -> Icons.Outlined.Http
    type.contains("schedule", ignoreCase = true) || type.contains("cron", ignoreCase = true) -> Icons.Outlined.Schedule
    type.contains("trigger", ignoreCase = true) -> Icons.Outlined.PlayArrow
    type.contains("if", ignoreCase = true) || type.contains("switch", ignoreCase = true) -> Icons.AutoMirrored.Outlined.CallSplit
    type.contains("code", ignoreCase = true) || type.contains("function", ignoreCase = true) -> Icons.Outlined.Code
    type.contains("merge", ignoreCase = true) -> Icons.AutoMirrored.Outlined.MergeType
    type.contains("split", ignoreCase = true) -> Icons.AutoMirrored.Outlined.CallSplit
    type.contains("loop", ignoreCase = true) -> Icons.Outlined.Loop
    type.contains("wait", ignoreCase = true) -> Icons.Outlined.HourglassEmpty
    type.contains("email", ignoreCase = true) || type.contains("gmail", ignoreCase = true) -> Icons.Outlined.Email
    type.contains("slack", ignoreCase = true) -> Icons.AutoMirrored.Outlined.Chat
    type.contains("database", ignoreCase = true) || type.contains("postgres", ignoreCase = true) || type.contains("mysql", ignoreCase = true) -> Icons.Outlined.Storage
    else -> Icons.Outlined.Extension
}

private fun getNodeTypeDisplayName(type: String): String = type.substringAfterLast(".")
    .replace(Regex("V[0-9]+$"), "")
    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
    .ifEmpty { type }
