package com.n8n.mobilemanager.ui.screens.workflows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.LoadingState
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nSectionHeader
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicTextField
import com.n8n.mobilemanager.ui.components.WorkflowCard
import com.n8n.mobilemanager.ui.theme.StatusSuccess

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun WorkflowsScreen(
    viewModel: WorkflowsViewModel = hiltViewModel(),
    onNavigateToWorkflow: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            N8nTopAppBar(
                title = "Workflows",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filter workflows",
                            tint = if (uiState.filterActive != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NeumorphicTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = "Search workflows",
                leadingIcon = Icons.Outlined.Search,
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (uiState.filterActive != null) {
                FilterChip(
                    selected = true,
                    onClick = { viewModel.setActiveFilter(null) },
                    label = {
                        Text(if (uiState.filterActive == true) "Active only" else "Inactive only")
                    },
                    trailingIcon = { Icon(Icons.Filled.Clear, contentDescription = "Remove filter") },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.filteredWorkflows.size} workflows",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.isRefreshing) {
                    Text(
                        text = "Updating…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            uiState.error?.let { message ->
                N8nErrorBanner(
                    message = message,
                    actionLabel = "Retry",
                    onAction = viewModel::refresh,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.filteredWorkflows.isEmpty() -> {
                        LoadingState(message = "Loading workflows…")
                    }
                    uiState.filteredWorkflows.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.AccountTree,
                            title = if (uiState.searchQuery.isNotBlank()) "No matching workflows" else "No workflows",
                            message = if (uiState.searchQuery.isNotBlank()) {
                                "Try a different name or clear the search."
                            } else {
                                "Create your first workflow in n8n."
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
                            items(uiState.filteredWorkflows, key = { it.id }) { workflow ->
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
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FilterBottomSheetContent(
                currentFilter = uiState.filterActive,
                onFilterSelected = {
                    viewModel.setActiveFilter(it)
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun FilterBottomSheetContent(
    currentFilter: Boolean?,
    onFilterSelected: (Boolean?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Filter workflows",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge
        )
        WorkflowFilterOption("All workflows", currentFilter == null) { onFilterSelected(null) }
        WorkflowFilterOption("Active workflows", currentFilter == true, StatusSuccess) { onFilterSelected(true) }
        WorkflowFilterOption("Inactive workflows", currentFilter == false) { onFilterSelected(false) }
    }
}

@Composable
private fun WorkflowFilterOption(
    title: String,
    selected: Boolean,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton },
        tonalElevation = if (selected) 1.dp else 0.dp
    )
}
