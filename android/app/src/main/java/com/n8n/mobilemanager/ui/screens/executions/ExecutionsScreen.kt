package com.n8n.mobilemanager.ui.screens.executions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.n8n.mobilemanager.ui.components.getStatusLabel
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.utils.DateUtils

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ExecutionsScreen(
    viewModel: ExecutionsViewModel = hiltViewModel(),
    onNavigateToExecution: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStatusFilterSheet by remember { mutableStateOf(false) }
    var showWorkflowFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { N8nTopAppBar(title = "Executions", onBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatus != null,
                    onClick = { showStatusFilterSheet = true },
                    label = { Text(uiState.selectedStatus?.let(::getStatusLabel) ?: "Status") },
                    leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.selectedWorkflowId != null,
                    onClick = { showWorkflowFilterSheet = true },
                    label = {
                        Text(
                            uiState.workflows.firstOrNull { it.id == uiState.selectedWorkflowId }?.name
                                ?: "Workflow",
                            maxLines = 1
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.filteredExecutions.size} executions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.isRefreshing) {
                    Text("Updating…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            uiState.error?.let { message ->
                N8nErrorBanner(
                    message = message,
                    actionLabel = "Retry",
                    onAction = viewModel::refresh,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.filteredExecutions.isEmpty() -> {
                        LoadingState(message = "Loading executions…")
                    }
                    uiState.filteredExecutions.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.History,
                            title = "No executions",
                            message = if (uiState.selectedStatus != null || uiState.selectedWorkflowId != null) {
                                "No runs match the selected filters."
                            } else {
                                "Workflow executions will appear here."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.filteredExecutions, key = { it.id }) { execution ->
                                ExecutionListItem(
                                    execution = execution,
                                    onClick = { onNavigateToExecution(execution.id) },
                                    onRetry = { viewModel.retryExecution(execution.id) },
                                    onStop = { viewModel.stopExecution(execution.id) },
                                    isActionInProgress = uiState.actionExecutionId == execution.id
                                )
                            }
                            if (uiState.nextCursor != null) {
                                item(key = "load-more") {
                                    androidx.compose.runtime.LaunchedEffect(Unit) {
                                        viewModel.loadNextPage()
                                    }
                                    if (uiState.isLoadingMore) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStatusFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStatusFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            StatusFilterSheet(
                currentStatus = uiState.selectedStatus,
                onSelected = {
                    viewModel.setStatusFilter(it)
                    showStatusFilterSheet = false
                }
            )
        }
    }

    if (showWorkflowFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWorkflowFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            WorkflowFilterSheet(
                workflows = uiState.workflows,
                currentWorkflowId = uiState.selectedWorkflowId,
                onSelected = {
                    viewModel.setWorkflowFilter(it)
                    showWorkflowFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun ExecutionListItem(
    execution: Execution,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit,
    isActionInProgress: Boolean
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        onClick = onClick,
        onClickLabel = "Open execution"
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExecutionStatusChip(execution.status)
                Text(
                    text = "#${execution.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = execution.workflowName ?: "Workflow ${execution.workflowId}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DateUtils.formatFullDate(execution.startedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    execution.stoppedAt?.let { stoppedAt ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(DateUtils.calculateDuration(execution.startedAt, stoppedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (execution.status == ExecutionStatus.RUNNING) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Stop,
                            onClick = onStop,
                            iconSize = 20.dp,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = "Stop execution",
                            enabled = !isActionInProgress
                        )
                    }
                    if (execution.status == ExecutionStatus.ERROR || execution.status == ExecutionStatus.CRASHED) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Refresh,
                            onClick = onRetry,
                            iconSize = 20.dp,
                            tint = N8nPrimary,
                            contentDescription = "Retry execution",
                            enabled = !isActionInProgress
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterSheet(
    currentStatus: ExecutionStatus?,
    onSelected: (ExecutionStatus?) -> Unit
) {
    val statuses = listOf(null) + listOf(
        ExecutionStatus.ERROR,
        ExecutionStatus.SUCCESS,
        ExecutionStatus.RUNNING,
        ExecutionStatus.WAITING,
        ExecutionStatus.CANCELED
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("Filter by status", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp))
        statuses.forEach { status ->
            FilterOption(
                title = status?.let(::getStatusLabel) ?: "All statuses",
                selected = currentStatus == status,
                onClick = { onSelected(status) }
            )
        }
    }
}

@Composable
private fun WorkflowFilterSheet(
    workflows: List<com.n8n.mobilemanager.data.model.Workflow>,
    currentWorkflowId: String?,
    onSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text("Filter by workflow", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
            item {
                FilterOption(
                    title = "All workflows",
                    selected = currentWorkflowId == null,
                    onClick = { onSelected(null) }
                )
            }
            items(workflows, key = { it.id }) { workflow ->
                FilterOption(
                    title = workflow.name,
                    selected = currentWorkflowId == workflow.id,
                    onClick = { onSelected(workflow.id) }
                )
            }
        }
    }
}

@Composable
private fun FilterOption(title: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, maxLines = 2) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton }
    )
}
