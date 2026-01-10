package com.n8n.mobilemanager.ui.screens.executions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.data.model.Workflow
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExecutionsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val executions: List<Execution> = emptyList(), // Accumulates all loaded executions
    val filteredExecutions: List<Execution> = emptyList(), // Same as executions in this context (filters are applied API side)
    val workflows: List<Workflow> = emptyList(),
    val selectedWorkflowId: String? = null,
    val selectedStatus: ExecutionStatus? = null,
    val nextCursor: String? = null
)

@HiltViewModel
class ExecutionsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExecutionsUiState())
    val uiState: StateFlow<ExecutionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Load workflows for filter dropdown
            repository.getWorkflows().fold(
                onSuccess = { workflows ->
                    _uiState.update { it.copy(workflows = workflows) }
                },
                onFailure = { /* Ignore, optional */ }
            )
            
            // Load initial executions
            loadExecutions(cursor = null)
        }
    }

    private suspend fun loadExecutions(cursor: String?) {
        val state = _uiState.value
        
        // Use pagination
        repository.getExecutionsPage(
            workflowId = state.selectedWorkflowId,
            status = state.selectedStatus,
            limit = 20, // Load 20 items per page
            cursor = cursor
        ).fold(
            onSuccess = { page ->
                _uiState.update { currentState ->
                    val newExecutions = if (cursor == null) {
                        page.executions // Reset list if loading first page
                    } else {
                        currentState.executions + page.executions // Append if loading more
                    }
                    
                    currentState.copy(
                        executions = newExecutions,
                        filteredExecutions = newExecutions,
                        nextCursor = page.nextCursor,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { 
                    it.copy(
                        error = error.message,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            }
        )
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.nextCursor == null) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            loadExecutions(state.nextCursor)
        }
    }

    fun setWorkflowFilter(workflowId: String?) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    selectedWorkflowId = workflowId, 
                    isLoading = true,
                    nextCursor = null // Reset cursor
                ) 
            }
            loadExecutions(null)
        }
    }

    fun setStatusFilter(status: ExecutionStatus?) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    selectedStatus = status, 
                    isLoading = true,
                    nextCursor = null // Reset cursor
                ) 
            }
            loadExecutions(null)
        }
    }

    fun retryExecution(executionId: String) {
        viewModelScope.launch {
            repository.retryExecution(executionId).fold(
                onSuccess = {
                    // Reload data to reflect status change
                    // Ideally we should just update the single item locally
                    loadExecutions(null) 
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Échec: ${error.message}") }
                }
            )
        }
    }

    fun stopExecution(executionId: String) {
        viewModelScope.launch {
            repository.stopExecution(executionId).fold(
                onSuccess = {
                    loadExecutions(null)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Échec: ${error.message}") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
