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
    val error: String? = null,
    val executions: List<Execution> = emptyList(),
    val filteredExecutions: List<Execution> = emptyList(),
    val workflows: List<Workflow> = emptyList(),
    val selectedWorkflowId: String? = null,
    val selectedStatus: ExecutionStatus? = null
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
            
            // Load executions
            loadExecutions()
        }
    }

    private suspend fun loadExecutions() {
        val state = _uiState.value
        
        repository.getExecutions(
            workflowId = state.selectedWorkflowId,
            status = state.selectedStatus,
            limit = 100
        ).fold(
            onSuccess = { executions ->
                _uiState.update { 
                    it.copy(
                        executions = executions,
                        filteredExecutions = executions,
                        isLoading = false
                    )
                }
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

    fun setWorkflowFilter(workflowId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedWorkflowId = workflowId, isLoading = true) }
            loadExecutions()
        }
    }

    fun setStatusFilter(status: ExecutionStatus?) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedStatus = status, isLoading = true) }
            loadExecutions()
        }
    }

    fun retryExecution(executionId: String) {
        viewModelScope.launch {
            repository.retryExecution(executionId).fold(
                onSuccess = {
                    loadExecutions()
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
                    loadExecutions()
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
