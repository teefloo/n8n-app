package com.n8n.mobilemanager.ui.screens.workflows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Workflow
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkflowsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workflows: List<Workflow> = emptyList(),
    val filteredWorkflows: List<Workflow> = emptyList(),
    val searchQuery: String = "",
    val filterActive: Boolean? = null, // null = all, true = active only, false = inactive only
    val isTogglingWorkflow: String? = null // workflow ID being toggled
)

@HiltViewModel
class WorkflowsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkflowsUiState())
    val uiState: StateFlow<WorkflowsUiState> = _uiState.asStateFlow()

    init {
        loadWorkflows()
    }

    fun loadWorkflows() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getWorkflows().fold(
                onSuccess = { workflows ->
                    _uiState.update { 
                        it.copy(
                            workflows = workflows,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setActiveFilter(active: Boolean?) {
        _uiState.update { it.copy(filterActive = active) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.workflows.filter { workflow ->
            val matchesSearch = state.searchQuery.isEmpty() || 
                workflow.name.contains(state.searchQuery, ignoreCase = true)
            
            val matchesActive = when (state.filterActive) {
                null -> true
                true -> workflow.active
                false -> !workflow.active
            }
            
            matchesSearch && matchesActive
        }
        
        _uiState.update { it.copy(filteredWorkflows = filtered) }
    }

    fun toggleWorkflowActive(workflowId: String, currentlyActive: Boolean) {
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val updatedWorkflows = state.workflows.map { 
                    if (it.id == workflowId) it.copy(active = !currentlyActive) else it 
                }
                state.copy(
                    workflows = updatedWorkflows,
                    isTogglingWorkflow = workflowId
                )
            }
            applyFilters()
            
            val result = if (currentlyActive) {
                repository.deactivateWorkflow(workflowId)
            } else {
                repository.activateWorkflow(workflowId)
            }
            
            result.fold(
                onSuccess = { newActiveState ->
                    _uiState.update { state ->
                        // Verify state matches what API returned
                        val finalWorkflows = state.workflows.map { 
                            if (it.id == workflowId) it.copy(active = newActiveState) else it 
                        }
                        state.copy(
                            workflows = finalWorkflows,
                            isTogglingWorkflow = null
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    // Revert on failure
                    _uiState.update { state ->
                        val revertedWorkflows = state.workflows.map { 
                            if (it.id == workflowId) it.copy(active = currentlyActive) else it 
                        }
                        state.copy(
                            workflows = revertedWorkflows,
                            error = "Échec: ${error.message}",
                            isTogglingWorkflow = null
                        )
                    }
                    applyFilters()
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
