package com.n8n.mobilemanager.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToWorkflows: () -> Unit = {},
    onNavigateToExecutions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToExecution: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with instance status
            item {
                DashboardHeader(
                    instanceName = uiState.instance?.name ?: "n8n Manager",
                    isOnline = uiState.isOnline,
                    lastRefreshTime = uiState.lastRefreshTime,
                    onSettingsClick = onNavigateToSettings
                )
            }
            
            // No instance configured
            if (uiState.instance == null) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Cloud,
                        title = "Aucune instance configurée",
                        message = "Ajoutez votre instance n8n pour commencer",
                        action = {
                            NeumorphicButton(
                                text = "Configurer",
                                onClick = onNavigateToSettings,
                                icon = Icons.Filled.Add,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    )
                }
            } else {
                // Period Selector
                item {
                    StatsPeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.setPeriod(it) }
                    )
                }

                // Stats Grid
                item {
                    StatsGrid(
                        activeWorkflows = uiState.stats.activeWorkflows,
                        totalWorkflows = uiState.stats.totalWorkflows,
                        successfulExecutions = uiState.stats.successfulExecutions,
                        failedExecutions = uiState.stats.failedExecutions,
                        averageExecutionTimeMs = uiState.stats.averageExecutionTime,
                        isTotalExecutionsEstimated = uiState.stats.isTotalExecutionsEstimated,
                        onWorkflowsClick = onNavigateToWorkflows,
                        onExecutionsClick = onNavigateToExecutions
                    )
                }
                
                // Quick Actions
                item {
                    QuickActionsSection(
                        onViewWorkflows = onNavigateToWorkflows,
                        onViewExecutions = onNavigateToExecutions
                    )
                }
                
                // Recent Executions
                item {
                    SectionHeader(
                        title = "Exécutions récentes",
                        action = "Voir tout",
                        onActionClick = onNavigateToExecutions
                    )
                }
                
                if (uiState.recentExecutions.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.History,
                            title = "Aucune exécution",
                            message = "Les exécutions de vos workflows apparaîtront ici"
                        )
                    }
                } else {
                    items(
                        items = uiState.recentExecutions.take(5),
                        key = { it.id }
                    ) { execution ->
                        ExecutionCard(
                            workflowName = execution.workflowName ?: "Workflow ${execution.workflowId}",
                            status = execution.status,
                            startedAt = formatDateTime(execution.startedAt),
                            duration = calculateDuration(execution.startedAt, execution.stoppedAt),
                            onClick = { onNavigateToExecution(execution.id) }
                        )
                    }
                }
                
                // Error message with neumorphic style
                uiState.error?.let { error ->
                    item {
                        NeumorphicErrorCard(error = error)
                    }
                }
            }
            
            // Bottom spacer for navigation bar
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun StatsPeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    colors = listOf(N8nPrimary, N8nPrimaryVariant)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        neumorphColors.surface,
                                        neumorphColors.surface
                                    )
                                )
                            }
                        )
                        .clickable { onPeriodSelected(period) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) 
                            androidx.compose.ui.graphics.Color.White 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    instanceName: String,
    isOnline: Boolean,
    lastRefreshTime: Long,
    onSettingsClick: () -> Unit
) {
    val neumorphColors = neumorphicColors()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bonjour 👋",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = instanceName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                StatusIndicator(isOnline = isOnline)
            }
        }
        
        NeumorphicIconButton(
            icon = Icons.Outlined.Settings,
            onClick = onSettingsClick,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsGrid(
    activeWorkflows: Int,
    totalWorkflows: Int,
    successfulExecutions: Int,
    failedExecutions: Int,
    averageExecutionTimeMs: Long = 0,
    isTotalExecutionsEstimated: Boolean = false,
    onWorkflowsClick: () -> Unit,
    onExecutionsClick: () -> Unit
) {
    val totalExecutions = successfulExecutions + failedExecutions
    val failureRate = if (totalExecutions > 0) {
        (failedExecutions * 100f / totalExecutions)
    } else 0f
    
    // Formater le temps d'exécution moyen
    val avgTimeFormatted = formatDuration(averageExecutionTimeMs)
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title
        Text(
            text = "📊 Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Row 1: Total Executions + Failed Executions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Total exécutions",
                value = if (isTotalExecutionsEstimated) "${formatNumber(totalExecutions)}+" else formatNumber(totalExecutions),
                icon = Icons.Filled.PlayCircle,
                iconTint = N8nPrimary,
                onClick = onExecutionsClick,
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Échecs",
                value = formatNumber(failedExecutions),
                icon = Icons.Filled.Error,
                iconTint = StatusError,
                valueColor = if (failedExecutions > 0) StatusError else null,
                onClick = onExecutionsClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 2: Failure Rate + Success Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Taux d'échec",
                value = String.format("%.1f%%", failureRate),
                icon = Icons.Filled.TrendingDown,
                iconTint = if (failureRate > 20) StatusError else StatusWarning,
                valueColor = if (failureRate > 20) StatusError else if (failureRate > 10) StatusWarning else StatusSuccess,
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Taux de succès",
                value = if (totalExecutions > 0) "${100 - failureRate.toInt()}%" else "—",
                icon = Icons.Filled.CheckCircle,
                iconTint = StatusSuccess,
                valueColor = StatusSuccess,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 3: Active Workflows + Total Workflows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Workflows actifs",
                value = activeWorkflows.toString(),
                subtitle = "sur $totalWorkflows",
                icon = Icons.Filled.AccountTree,
                iconTint = StatusSuccess,
                onClick = onWorkflowsClick,
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Workflows total",
                value = totalWorkflows.toString(),
                icon = Icons.Outlined.AccountTree,
                iconTint = N8nAccent,
                onClick = onWorkflowsClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 4: Average Execution Time (comme "Run time (avg.)" sur n8n)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Temps moyen",
                value = avgTimeFormatted,
                subtitle = "par exécution",
                icon = Icons.Filled.Timer,
                iconTint = N8nAccent,
                modifier = Modifier.weight(1f)
            )
            // Espace vide pour équilibrer
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    valueColor: androidx.compose.ui.graphics.Color? = null,
    onClick: (() -> Unit)? = null
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                
                // Neumorphic icon container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neumorphicShadow(
                            lightShadowColor = neumorphColors.lightShadow,
                            darkShadowColor = neumorphColors.darkShadow,
                            shadowOffset = 2.dp,
                            shadowRadius = 4.dp,
                            cornerRadius = 10.dp
                        )
                        .clip(RoundedCornerShape(10.dp))
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onViewWorkflows: () -> Unit,
    onViewExecutions: () -> Unit
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Actions rapides",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NeumorphicQuickActionButton(
                    icon = Icons.Outlined.AccountTree,
                    label = "Workflows",
                    onClick = onViewWorkflows,
                    modifier = Modifier.weight(1f)
                )
                NeumorphicQuickActionButton(
                    icon = Icons.Outlined.History,
                    label = "Exécutions",
                    onClick = onViewExecutions,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NeumorphicQuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicButton(
        text = label,
        onClick = onClick,
        icon = icon,
        isPrimary = false,
        modifier = modifier
    )
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        action?.let {
            TextButton(onClick = onActionClick) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = N8nPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = N8nPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun NeumorphicErrorCard(error: String) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            StatusError.copy(alpha = 0.08f),
                            StatusError.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StatusError.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Erreur",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusError,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ==================== Utility Functions ====================

private fun formatDateTime(dateString: String): String {
    if (dateString.isBlank()) return "—"
    
    return try {
        val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        
        // Essayer plusieurs formats de date
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to null, // Format avec timezone
            "yyyy-MM-dd'T'HH:mm:ss.SSS" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss" to TimeZone.getTimeZone("UTC")
        )
        
        for ((pattern, tz) in formats) {
            try {
                val inputFormat = SimpleDateFormat(pattern, Locale.getDefault())
                if (tz != null) inputFormat.timeZone = tz
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                // Essayer le format suivant
            }
        }
        
        dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateDuration(startedAt: String, stoppedAt: String?): String? {
    if (stoppedAt == null) return null
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val start = format.parse(startedAt)
        val stop = format.parse(stoppedAt)
        if (start != null && stop != null) {
            val durationMs = stop.time - start.time
            when {
                durationMs < 1000 -> "${durationMs}ms"
                durationMs < 60000 -> "${durationMs / 1000}s"
                else -> "${durationMs / 60000}m ${(durationMs % 60000) / 1000}s"
            }
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun calculateSuccessRate(success: Int, failed: Int): String {
    val total = success + failed
    return if (total > 0) {
        "${(success * 100 / total)}%"
    } else {
        "—"
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> String.format("%.1fM", number / 1000000f)
        number >= 1000 -> String.format("%.1fK", number / 1000f)
        else -> number.toString()
    }
}

private fun formatDuration(durationMs: Long): String {
    return when {
        durationMs <= 0 -> "—"
        durationMs < 1000 -> "${durationMs}ms"
        durationMs < 60000 -> String.format("%.1fs", durationMs / 1000f)
        durationMs < 3600000 -> {
            val minutes = durationMs / 60000
            val seconds = (durationMs % 60000) / 1000
            "${minutes}m ${seconds}s"
        }
        else -> {
            val hours = durationMs / 3600000
            val minutes = (durationMs % 3600000) / 60000
            "${hours}h ${minutes}m"
        }
    }
}
