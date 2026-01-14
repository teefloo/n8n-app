package com.n8n.mobilemanager.ui.screens.workflows

import androidx.lifecycle.SavedStateHandle
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

data class WorkflowDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workflow: Workflow? = null,
    val recentExecutions: List<Execution> = emptyList(),
    val isTogglingActive: Boolean = false,
    val successCount: Int = 0,
    val errorCount: Int = 0
)

@HiltViewModel
class WorkflowDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: N8nRepository
) : ViewModel() {

    private val workflowId: String = savedStateHandle.get<String>("workflowId") ?: ""
    
    private val _uiState = MutableStateFlow(WorkflowDetailUiState())
    val uiState: StateFlow<WorkflowDetailUiState> = _uiState.asStateFlow()

    init {
        if (workflowId.isNotEmpty()) {
            loadWorkflowDetails()
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Invalid workflow ID") }
        }
    }

    fun loadWorkflowDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Charger le workflow
            val workflowResult = repository.getWorkflow(workflowId)
            
            workflowResult.fold(
                onSuccess = { workflow ->
                    _uiState.update { it.copy(workflow = workflow) }
                    
                    // Charger les exécutions récentes pour ce workflow
                    loadRecentExecutions()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error loading workflow"
                        )
                    }
                }
            )
        }
    }

    private suspend fun loadRecentExecutions() {
        val executionsResult = repository.getExecutions(
            workflowId = workflowId,
            limit = 50,
            fetchAll = false
        )
        
        executionsResult.fold(
            onSuccess = { executions ->
                val successCount = executions.count { it.status == ExecutionStatus.SUCCESS }
                val errorCount = executions.count { 
                    it.status == ExecutionStatus.ERROR || it.status == ExecutionStatus.CRASHED 
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        recentExecutions = executions.take(10),
                        successCount = successCount,
                        errorCount = errorCount
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }
        )
    }

    fun toggleWorkflowActive() {
        val workflow = _uiState.value.workflow ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingActive = true) }
            
            val result = if (workflow.active) {
                repository.deactivateWorkflow(workflowId)
            } else {
                repository.activateWorkflow(workflowId)
            }
            
            result.fold(
                onSuccess = { newActiveState ->
                    _uiState.update { state ->
                        state.copy(
                            workflow = state.workflow?.copy(active = newActiveState),
                            isTogglingActive = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = "Failed: ${error.message}",
                            isTogglingActive = false
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
