package com.n8n.mobilemanager.ui.screens.workflows

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowsScreen(
    viewModel: WorkflowsViewModel = hiltViewModel(),
    onNavigateToWorkflow: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val neumorphColors = neumorphicColors()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            NeumorphicTopBar(
                title = "Workflows",
                onBackClick = onNavigateBack,
                filterActive = uiState.filterActive != null,
                onFilterClick = { showFilterSheet = true }
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
            
            // Search bar with neumorphic style
            NeumorphicSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Filter chips
            if (uiState.filterActive != null) {
                NeumorphicFilterChip(
                    text = if (uiState.filterActive == true) "Active only" else "Inactive only",
                    onRemove = { viewModel.setActiveFilter(null) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Workflow count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.filteredWorkflows.size} workflow(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content
            PullToRefreshBox(
                isRefreshing = uiState.isLoading || uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.filteredWorkflows.isEmpty() && !uiState.isLoading) {
                    EmptyState(
                        icon = Icons.Outlined.AccountTree,
                        title = if (uiState.searchQuery.isNotEmpty()) "No results" else "No workflow",
                        message = if (uiState.searchQuery.isNotEmpty()) 
                            "Try with other search terms" 
                        else 
                            "Create your first workflow in n8n",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.filteredWorkflows,
                            key = { it.id }
                        ) { workflow ->
                            WorkflowCard(
                                name = workflow.name,
                                isActive = workflow.active,
                                lastExecutionStatus = null,
                                nodesCount = workflow.nodes.size,
                                isToggling = uiState.isTogglingWorkflow == workflow.id,
                                onToggleActive = { 
                                    viewModel.toggleWorkflowActive(workflow.id, workflow.active) 
                                },
                                onClick = { onNavigateToWorkflow(workflow.id) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
        
        // Error snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
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
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    
    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            NeumorphicFilterBottomSheetContent(
                currentFilter = uiState.filterActive,
                onFilterSelected = { filter ->
                    viewModel.setActiveFilter(filter)
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun NeumorphicTopBar(
    title: String,
    onBackClick: () -> Unit,
    filterActive: Boolean,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
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
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Box {
            NeumorphicIconButton(
                icon = Icons.Outlined.FilterList,
                onClick = onFilterClick,
                size = 44.dp,
                iconSize = 22.dp,
                tint = if (filterActive) N8nPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (filterActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(10.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(N8nPrimary)
                )
            }
        }
    }
}

@Composable
private fun NeumorphicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NeumorphicTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Search workflows…",
        leadingIcon = Icons.Default.Search,
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        modifier = modifier
    )
}

@Composable
private fun NeumorphicFilterChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neumorphColors = neumorphicColors()
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        N8nPrimary.copy(alpha = 0.1f),
                        N8nPrimary.copy(alpha = 0.15f)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = N8nPrimary,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    tint = N8nPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun NeumorphicFilterBottomSheetContent(
    currentFilter: Boolean?,
    onFilterSelected: (Boolean?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filter by status",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NeumorphicFilterOption(
            title = "All workflows",
            isSelected = currentFilter == null,
            onClick = { onFilterSelected(null) }
        )
        
        NeumorphicFilterOption(
            title = "Active workflows",
            isSelected = currentFilter == true,
            onClick = { onFilterSelected(true) },
            icon = Icons.Filled.PlayCircle,
            iconTint = StatusSuccess
        )
        
        NeumorphicFilterOption(
            title = "Inactive workflows",
            isSelected = currentFilter == false,
            onClick = { onFilterSelected(false) },
            icon = Icons.Outlined.PauseCircle,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NeumorphicFilterOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) N8nPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
