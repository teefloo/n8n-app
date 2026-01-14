package com.n8n.mobilemanager.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionMode
import com.n8n.mobilemanager.data.model.InstanceStats
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToWorkflows: () -> Unit,
    onNavigateToExecutions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            DashboardTopBar(
                title = uiState.instance?.name ?: "Dashboard",
                isOnline = uiState.isOnline,
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Period Selector
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = viewModel::setPeriod
                )

                // Stats Cards Grid
                StatsGrid(stats = uiState.stats)

                // Recent Executions
                RecentExecutionsSection(
                    executions = uiState.recentExecutions,
                    onSeeAllClick = onNavigateToExecutions,
                    onExecutionClick = onNavigateToExecution
                )
                
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun DashboardTopBar(
    title: String,
    isOnline: Boolean,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusIndicator(isOnline = isOnline)
            }
        }
        
        NeumorphicIconButton(
            icon = Icons.Outlined.Settings,
            onClick = onSettingsClick,
            size = 44.dp,
            iconSize = 22.dp,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusIndicator(isOnline: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (isOnline) StatusSuccess else StatusError)
    )
}

@Composable
fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        items(StatsPeriod.entries) { period ->
            val isSelected = period == selectedPeriod
            val colors = neumorphicColors()
            
            // Custom button for period selection
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .neumorphicRaised(
                        lightShadowColor = if (isSelected) colors.lightShadow.copy(alpha = 0.5f) else colors.lightShadow,
                        darkShadowColor = if (isSelected) colors.darkShadow.copy(alpha = 0.5f) else colors.darkShadow,
                        backgroundColor = if (isSelected) colors.primary else colors.background,
                        cornerRadius = 18.dp,
                        shadowOffset = 3.dp,
                        shadowBlur = 6.dp
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { 
                        android.util.Log.d("PeriodSelector", "Clicked on period: $period (current: $selectedPeriod)")
                        onPeriodSelected(period) 
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else colors.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatsGrid(stats: InstanceStats) {
    // Log pour debug
    android.util.Log.d("StatsGrid", "Rendering stats: workflows=${stats.totalWorkflows}, active=${stats.activeWorkflows}, success=${stats.successfulExecutions}, failed=${stats.failedExecutions}")
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Workflows",
                value = stats.totalWorkflows.toString(),
                icon = Icons.Outlined.AccountTree,
                color = N8nPrimary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Active",
                value = stats.activeWorkflows.toString(),
                icon = Icons.Filled.PlayCircle,
                color = StatusSuccess
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Success",
                value = stats.successfulExecutions.toString(),
                icon = Icons.Outlined.CheckCircle,
                color = StatusSuccess
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Failures",
                value = stats.failedExecutions.toString(),
                icon = Icons.Outlined.Error,
                color = StatusError
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    NeumorphicCard(
        modifier = modifier,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentExecutionsSection(
    executions: List<Execution>,
    onSeeAllClick: () -> Unit,
    onExecutionClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent executions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSeeAllClick) {
                Text("See all", color = N8nPrimary)
            }
        }
        
        if (executions.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "No execution",
                message = "Recent executions will appear here",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                executions.take(5).forEach { execution ->
                    ExecutionItem(
                        execution = execution,
                        onClick = { onExecutionClick(execution.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutionItem(
    execution: Execution,
    onClick: () -> Unit
) {
    val colors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 14.dp,
        shadowOffset = 4.dp,
        shadowBlur = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode Icon (Trigger/Manual)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(execution.mode) {
                        ExecutionMode.WEBHOOK, ExecutionMode.TRIGGER -> Icons.Outlined.Bolt
                        ExecutionMode.MANUAL -> Icons.Outlined.PlayArrow
                        else -> Icons.Outlined.Settings
                    },
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                // Workflow Name with fallback
                Text(
                    text = execution.workflowName ?: "Workflow ${execution.workflowId.take(5)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Details Row: ID • Time • Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ID
                    Text(
                        text = "#${execution.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    // Separator
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(colors.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    
                    // Time
                    Text(
                        text = formatTime(execution.startedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    // Duration (if available)
                    val duration = calculateDuration(execution.startedAt, execution.stoppedAt)
                    if (duration.isNotEmpty()) {
                        // Separator
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(colors.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = colors.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            ExecutionStatusChip(status = execution.status)
        }
    }
}

private fun calculateDuration(start: String, end: String?): String {
    if (end == null) return ""
    try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        // Try alternate format if first fails
        val format2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        
        val startTime = try { format.parse(start)?.time } catch (e: Exception) { format2.parse(start)?.time } ?: return ""
        val endTime = try { format.parse(end)?.time } catch (e: Exception) { format2.parse(end)?.time } ?: return ""
        
        val diff = endTime - startTime
        return when {
            diff < 1000 -> "${diff}ms"
            diff < 60000 -> "${diff/1000}s"
            else -> "${diff/60000}m ${diff%60000/1000}s"
        }
    } catch (e: Exception) {
        return ""
    }
}

private fun formatTime(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: ""
    } catch (e: Exception) {
        ""
    }
}