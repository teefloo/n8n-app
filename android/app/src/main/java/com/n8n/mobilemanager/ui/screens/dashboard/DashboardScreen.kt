package com.n8n.mobilemanager.ui.screens.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.n8n.mobilemanager.data.model.ExecutionMode
import com.n8n.mobilemanager.data.model.InstanceStats
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.ExecutionStatusChip
import com.n8n.mobilemanager.ui.components.LoadingState
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nSectionHeader
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicCard
import com.n8n.mobilemanager.ui.components.PulseDot
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.ui.theme.StatusSuccess
import com.n8n.mobilemanager.utils.DateUtils

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DashboardScreen(
    onNavigateToWorkflows: () -> Unit,
    onNavigateToExecutions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            N8nTopAppBar(
                title = uiState.instance?.name ?: "Dashboard",
                subtitle = if (uiState.isOnline) "Connected" else "Offline",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.stats == InstanceStats() && uiState.recentExecutions.isEmpty() -> {
                    LoadingState(message = "Loading instance activity…")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (!uiState.isOnline && uiState.instance != null) {
                            item {
                                N8nErrorBanner(
                                    message = "This instance is offline. Showing the last available data.",
                                    actionLabel = "Retry",
                                    onAction = viewModel::refresh
                                )
                            }
                        }

                        uiState.error?.let { message ->
                            item {
                                N8nErrorBanner(
                                    message = message,
                                    actionLabel = "Retry",
                                    onAction = viewModel::refresh
                                )
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                N8nSectionHeader(title = "Overview")
                                PeriodSelector(
                                    selectedPeriod = uiState.selectedPeriod,
                                    onPeriodSelected = viewModel::setPeriod
                                )
                            }
                        }

                        item { StatsGrid(stats = uiState.stats) }

                        item {
                            RecentExecutionsSection(
                                executions = uiState.recentExecutions,
                                onSeeAllClick = onNavigateToExecutions,
                                onExecutionClick = onNavigateToExecution
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTopBar(
    title: String,
    isOnline: Boolean,
    onSettingsClick: () -> Unit,
    isVisible: Boolean
) {
    N8nTopAppBar(
        title = title,
        subtitle = if (isOnline) "Connected" else "Offline",
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
fun StatusIndicatorPulse(isOnline: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PulseDot(
            color = if (isOnline) StatusSuccess else StatusError,
            size = 8
        )
        Text(
            text = if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(StatsPeriod.entries) { period ->
            FilterChip(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun StatsGrid(stats: InstanceStats) {
    val cards = listOf(
        StatCardData("Total workflows", stats.totalWorkflows.toString(), Icons.Outlined.AccountTree, N8nPrimary),
        StatCardData("Active", stats.activeWorkflows.toString(), Icons.Outlined.PlayArrow, StatusSuccess),
        StatCardData("Successful runs", stats.successfulExecutions.toString(), Icons.Outlined.CheckCircle, StatusSuccess),
        StatCardData("Failed runs", stats.failedExecutions.toString(), Icons.Outlined.Error, MaterialTheme.colorScheme.error)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cards.forEach { card ->
                    StatCard(card = card, modifier = Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { card ->
                            StatCard(card = card, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class StatCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun StatCard(card: StatCardData, modifier: Modifier = Modifier) {
    NeumorphicCard(modifier = modifier, cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(card.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(card.icon, contentDescription = null, tint = card.accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = card.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        N8nSectionHeader(
            title = "Recent executions",
            actionLabel = "See all",
            onAction = onSeeAllClick
        )

        if (executions.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "No executions yet",
                message = "When your workflows run, their execution history will appear here."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    val modeIcon = when (execution.mode) {
        ExecutionMode.WEBHOOK, ExecutionMode.TRIGGER -> Icons.Outlined.Bolt
        ExecutionMode.MANUAL -> Icons.Outlined.PlayArrow
        else -> Icons.Outlined.Settings
    }

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = "Open execution",
        cornerRadius = 14.dp
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = execution.workflowName ?: "Workflow ${execution.workflowId.take(5)}…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = DateUtils.formatTime(execution.startedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val duration = DateUtils.calculateDuration(execution.startedAt, execution.stoppedAt)
                    if (duration.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ExecutionStatusChip(execution.status)
        }
    }
}
