package com.n8n.mobilemanager.ui.screens.executions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import com.n8n.mobilemanager.utils.DateUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionsScreen(
    viewModel: ExecutionsViewModel = hiltViewModel(),
    onNavigateToExecution: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val neumorphColors = neumorphicColors()
    
    var showStatusFilterSheet by remember { mutableStateOf(false) }
    var showWorkflowFilterSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            NeumorphicExecutionTopBar(
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filter chips row with neumorphic style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeumorphicFilterButton(
                    label = uiState.selectedStatus?.let { getStatusLabel(it) } ?: "Status",
                    isActive = uiState.selectedStatus != null,
                    onClick = { showStatusFilterSheet = true },
                    onClear = if (uiState.selectedStatus != null) {
                        { viewModel.setStatusFilter(null) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                
                NeumorphicFilterButton(
                    label = uiState.workflows
                        .find { it.id == uiState.selectedWorkflowId }?.name?.take(12)?.let { 
                            if (it.length >= 12) "$it…" else it 
                        } ?: "Workflow",
                    isActive = uiState.selectedWorkflowId != null,
                    onClick = { showWorkflowFilterSheet = true },
                    onClear = if (uiState.selectedWorkflowId != null) {
                        { viewModel.setWorkflowFilter(null) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Execution count
            Text(
                text = "${uiState.filteredExecutions.size} execution(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content
            PullToRefreshBox(
                isRefreshing = uiState.isLoading || uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.filteredExecutions.isEmpty() && !uiState.isLoading) {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        title = "No execution",
                        message = "Your workflow executions will appear here",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.filteredExecutions,
                            key = { it.id }
                        ) { execution ->
                            NeumorphicExecutionListItem(
                                execution = execution,
                                onClick = { onNavigateToExecution(execution.id) },
                                onRetry = { viewModel.retryExecution(execution.id) },
                                onStop = { viewModel.stopExecution(execution.id) }
                            )
                        }

                        // Infinite scroll trigger
                        item {
                            LaunchedEffect(true) {
                                viewModel.loadNextPage()
                            }
                        }
                        
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = N8nPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }
    
    // Status filter bottom sheet
    if (showStatusFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStatusFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            NeumorphicStatusFilterContent(
                currentStatus = uiState.selectedStatus,
                onStatusSelected = { status ->
                    viewModel.setStatusFilter(status)
                    showStatusFilterSheet = false
                }
            )
        }
    }
    
    // Workflow filter bottom sheet
    if (showWorkflowFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWorkflowFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            NeumorphicWorkflowFilterContent(
                workflows = uiState.workflows,
                currentWorkflowId = uiState.selectedWorkflowId,
                onWorkflowSelected = { workflowId ->
                    viewModel.setWorkflowFilter(workflowId)
                    showWorkflowFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun NeumorphicExecutionTopBar(
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
            text = "Executions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NeumorphicFilterButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = modifier.height(48.dp),
        cornerRadius = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isActive) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    N8nPrimary.copy(alpha = 0.08f),
                                    N8nPrimary.copy(alpha = 0.12f)
                                )
                            )
                        )
                    } else Modifier
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(N8nPrimary)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) N8nPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = N8nPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NeumorphicExecutionListItem(
    execution: com.n8n.mobilemanager.data.model.Execution,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExecutionStatusChip(status = execution.status)
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#${execution.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = execution.workflowName ?: "Workflow ${execution.workflowId}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Start time
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = DateUtils.formatFullDate(execution.startedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Duration
                    execution.stoppedAt?.let { stoppedAt ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = DateUtils.calculateDuration(execution.startedAt, stoppedAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (execution.status == ExecutionStatus.RUNNING) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Stop,
                            onClick = onStop,
                            size = 36.dp,
                            iconSize = 18.dp,
                            tint = StatusError
                        )
                    }
                    
                    if (execution.status == ExecutionStatus.ERROR) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Refresh,
                            onClick = onRetry,
                            size = 36.dp,
                            iconSize = 18.dp,
                            tint = N8nPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeumorphicStatusFilterContent(
    currentStatus: ExecutionStatus?,
    onStatusSelected: (ExecutionStatus?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Filter by status",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NeumorphicStatusFilterOption(
            status = null,
            label = "All statuses",
            isSelected = currentStatus == null,
            onClick = { onStatusSelected(null) }
        )
        
        val filterableStatuses = listOf(
            ExecutionStatus.ERROR,
            ExecutionStatus.SUCCESS,
            ExecutionStatus.RUNNING,
            ExecutionStatus.WAITING,
            ExecutionStatus.CANCELED
        )
        
        filterableStatuses.forEach { status ->
            val label = getStatusLabel(status)
            
            NeumorphicStatusFilterOption(
                status = status,
                label = label,
                isSelected = currentStatus == status,
                onClick = { onStatusSelected(status) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NeumorphicStatusFilterOption(
    status: ExecutionStatus?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        isPressed = isSelected,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    N8nPrimary.copy(alpha = 0.08f),
                                    N8nPrimary.copy(alpha = 0.12f)
                                )
                            )
                        )
                    } else Modifier
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status != null) {
                    ExecutionStatusChip(status = status)
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) N8nPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(N8nPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeumorphicWorkflowFilterContent(
    workflows: List<com.n8n.mobilemanager.data.model.Workflow>,
    currentWorkflowId: String?,
    onWorkflowSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Filter by workflow",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            isPressed = currentWorkflowId == null,
            onClick = { onWorkflowSelected(null) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (currentWorkflowId == null) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        N8nPrimary.copy(alpha = 0.08f),
                                        N8nPrimary.copy(alpha = 0.12f)
                                    )
                                )
                            )
                        } else Modifier
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All workflows",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (currentWorkflowId == null) N8nPrimary else MaterialTheme.colorScheme.onSurface
                )
                
                if (currentWorkflowId == null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(N8nPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        workflows.forEach { workflow ->
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                isPressed = currentWorkflowId == workflow.id,
                onClick = { onWorkflowSelected(workflow.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (currentWorkflowId == workflow.id) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            N8nPrimary.copy(alpha = 0.08f),
                                            N8nPrimary.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                            } else Modifier
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (workflow.active) StatusSuccess.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (workflow.active) Icons.Filled.PlayCircle else Icons.Outlined.PauseCircle,
                                contentDescription = null,
                                tint = if (workflow.active) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = workflow.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentWorkflowId == workflow.id) N8nPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (currentWorkflowId == workflow.id) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(N8nPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
