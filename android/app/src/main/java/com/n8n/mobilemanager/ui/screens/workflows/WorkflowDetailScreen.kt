package com.n8n.mobilemanager.ui.screens.workflows

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.data.model.Node
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowDetailScreen(
    viewModel: WorkflowDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToExecution: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val neumorphColors = neumorphicColors()
    
    Scaffold(
        topBar = {
            WorkflowDetailTopBar(
                workflowName = uiState.workflow?.name ?: "Workflow",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadWorkflowDetails() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.error != null && uiState.workflow == null) {
                // Error state
                EmptyState(
                    icon = Icons.Outlined.Error,
                    title = "Loading error",
                    message = uiState.error ?: "An error occurred",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (uiState.workflow != null) {
                val workflow = uiState.workflow!!
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header card with workflow info
                    item {
                        WorkflowHeaderCard(
                            name = workflow.name,
                            isActive = workflow.active,
                            isToggling = uiState.isTogglingActive,
                            onToggleActive = { viewModel.toggleWorkflowActive() },
                            createdAt = workflow.createdAt,
                            updatedAt = workflow.updatedAt
                        )
                    }
                    
                    // Statistics
                    item {
                        WorkflowStatsRow(
                            nodesCount = workflow.nodes.size,
                            successCount = uiState.successCount,
                            errorCount = uiState.errorCount
                        )
                    }
                    
                    // Tags section
                    if (workflow.tags.isNotEmpty()) {
                        item {
                            SectionTitle(title = "Tags")
                        }
                        
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(workflow.tags) { tag ->
                                    TagChip(name = tag.name)
                                }
                            }
                        }
                    }
                    
                    // Nodes section
                    if (workflow.nodes.isNotEmpty()) {
                        item {
                            SectionTitle(title = "Nodes (${workflow.nodes.size})")
                        }
                        
                        items(workflow.nodes.take(10)) { node ->
                            NodeCard(node = node)
                        }
                        
                        if (workflow.nodes.size > 10) {
                            item {
                                Text(
                                    text = "+ ${workflow.nodes.size - 10} more nodes...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    
                    // Recent executions section
                    if (uiState.recentExecutions.isNotEmpty()) {
                        item {
                            SectionTitle(title = "Recent executions")
                        }
                        
                        items(uiState.recentExecutions) { execution ->
                            ExecutionMiniCard(
                                execution = execution,
                                onClick = { onNavigateToExecution(execution.id) }
                            )
                        }
                    }
                    
                    // Settings section - always show with default values
                    item {
                        SectionTitle(title = "Settings")
                    }
                    
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
                    
                    // Bottom spacer
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
        
        // Error snackbar
        uiState.error?.let { error ->
            if (uiState.workflow != null) {
                LaunchedEffect(error) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearError()
                }
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ErrorSnackbar(message = error)
                }
            }
        }
    }
}

@Composable
private fun WorkflowDetailTopBar(
    workflowName: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeumorphicIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackClick,
            size = 44.dp,
            iconSize = 22.dp,
            tint = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = workflowName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            (if (isActive) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                                .copy(alpha = 0.05f),
                            neumorphColors.surface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isActive) StatusSuccess.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountTree,
                            contentDescription = null,
                            tint = if (isActive) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(
                                text = if (isActive) "Active" else "Inactive",
                                color = if (isActive) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Toggle switch
                Box {
                    if (isToggling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = N8nPrimary
                        )
                    } else {
                        NeumorphicToggle(
                            checked = isActive,
                            onCheckedChange = { onToggleActive() }
                        )
                    }
                }
            }
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DateInfo(
                    label = "Created",
                    dateString = createdAt
                )
                DateInfo(
                    label = "Modified",
                    dateString = updatedAt
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun DateInfo(
    label: String,
    dateString: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = formatDate(dateString),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WorkflowStatsRow(
    nodesCount: Int,
    successCount: Int,
    errorCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Hub,
            value = nodesCount.toString(),
            label = "Nodes",
            color = N8nPrimary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.CheckCircle,
            value = successCount.toString(),
            label = "Success",
            color = StatusSuccess
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.ErrorOutline,
            value = errorCount.toString(),
            label = "Errors",
            color = StatusError
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    NeumorphicCard(
        modifier = modifier,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TagChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(N8nPrimary.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = N8nPrimary
        )
    }
}

@Composable
private fun NodeCard(node: Node) {
    val nodeTypeIcon = getNodeTypeIcon(node.type)
    val isDisabled = node.disabled
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isDisabled) Modifier.background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ) else Modifier
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        else N8nPrimary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = nodeTypeIcon,
                    contentDescription = null,
                    tint = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant 
                           else N8nPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDisabled) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getNodeTypeDisplayName(node.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isDisabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutionMiniCard(
    execution: Execution,
    onClick: () -> Unit
) {
    val statusColor = when (execution.status) {
        ExecutionStatus.SUCCESS -> StatusSuccess
        ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> StatusError
        ExecutionStatus.RUNNING -> StatusRunning
        ExecutionStatus.WAITING -> StatusWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val statusIcon = when (execution.status) {
        ExecutionStatus.SUCCESS -> Icons.Filled.CheckCircle
        ExecutionStatus.ERROR, ExecutionStatus.CRASHED -> Icons.Filled.Error
        ExecutionStatus.RUNNING -> Icons.Filled.PlayCircle
        ExecutionStatus.WAITING -> Icons.Outlined.Schedule
        else -> Icons.Outlined.Circle
    }
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Execution #${execution.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatDate(execution.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
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
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ID du workflow
            SettingsRow(
                icon = Icons.Outlined.Tag,
                label = "ID",
                value = workflowId
            )
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            
            // Fuseau horaire
            SettingsRow(
                icon = Icons.Outlined.Schedule,
                label = "Timezone",
                value = timezone ?: "Default (server)"
            )
            
            // Timeout
            SettingsRow(
                icon = Icons.Outlined.Timer,
                label = "Execution timeout",
                value = executionTimeout?.let { "${it}s" } ?: "Unlimited"
            )
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            
            // Sauvegarde des exécutions manuelles
            SettingsRow(
                icon = Icons.Outlined.Save,
                label = "Save manual executions",
                value = when (saveManualExecutions) {
                    true -> "Yes"
                    false -> "No"
                    null -> "Default"
                }
            )
            
            // Sauvegarde de la progression
            SettingsRow(
                icon = Icons.Outlined.Pending,
                label = "Save progress",
                value = when (saveExecutionProgress) {
                    true -> "Yes"
                    false -> "No"
                    null -> "Default"
                }
            )
            
            // Sauvegarde en cas d'erreur
            SettingsRow(
                icon = Icons.Outlined.ErrorOutline,
                label = "Save on error",
                value = when (saveDataErrorExecution) {
                    "all" -> "All data"
                    "none" -> "No data"
                    else -> saveDataErrorExecution ?: "Default"
                }
            )
            
            // Sauvegarde en cas de succès
            SettingsRow(
                icon = Icons.Outlined.CheckCircleOutline,
                label = "Save on success",
                value = when (saveDataSuccessExecution) {
                    "all" -> "All data"
                    "none" -> "No data"
                    else -> saveDataSuccessExecution ?: "Default"
                }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorSnackbar(message: String) {
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            StatusError.copy(alpha = 0.1f),
                            StatusError.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Helper functions
private fun formatDate(dateString: String): String {
    return try {
        val inputFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        )
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRANCE)
        
        for (format in inputFormats) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(dateString)
                if (date != null) {
                    outputFormat.timeZone = TimeZone.getDefault()
                    return outputFormat.format(date)
                }
            } catch (_: Exception) { }
        }
        dateString
    } catch (_: Exception) {
        dateString
    }
}

private fun getNodeTypeIcon(type: String): ImageVector {
    return when {
        type.contains("webhook", ignoreCase = true) -> Icons.Outlined.Webhook
        type.contains("http", ignoreCase = true) -> Icons.Outlined.Http
        type.contains("schedule", ignoreCase = true) || 
            type.contains("cron", ignoreCase = true) -> Icons.Outlined.Schedule
        type.contains("trigger", ignoreCase = true) -> Icons.Outlined.PlayArrow
        type.contains("if", ignoreCase = true) || 
            type.contains("switch", ignoreCase = true) -> Icons.AutoMirrored.Outlined.CallSplit
        type.contains("code", ignoreCase = true) || 
            type.contains("function", ignoreCase = true) -> Icons.Outlined.Code
        type.contains("set", ignoreCase = true) -> Icons.Outlined.EditNote
        type.contains("merge", ignoreCase = true) -> Icons.AutoMirrored.Outlined.MergeType
        type.contains("split", ignoreCase = true) -> Icons.AutoMirrored.Outlined.CallSplit
        type.contains("loop", ignoreCase = true) -> Icons.Outlined.Loop
        type.contains("wait", ignoreCase = true) -> Icons.Outlined.HourglassEmpty
        type.contains("email", ignoreCase = true) || 
            type.contains("gmail", ignoreCase = true) -> Icons.Outlined.Email
        type.contains("slack", ignoreCase = true) -> Icons.AutoMirrored.Outlined.Chat
        type.contains("database", ignoreCase = true) || 
            type.contains("postgres", ignoreCase = true) ||
            type.contains("mysql", ignoreCase = true) -> Icons.Outlined.Storage
        else -> Icons.Outlined.Extension
    }
}

private fun getNodeTypeDisplayName(type: String): String {
    // Extract the main node name from the full type
    val name = type.substringAfterLast(".")
        .replace(Regex("V[0-9]+$"), "")
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
    
    return name.ifEmpty { type }
}
