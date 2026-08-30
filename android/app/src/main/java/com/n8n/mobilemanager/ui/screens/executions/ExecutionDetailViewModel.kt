package com.n8n.mobilemanager.ui.screens.executions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExecutionDetailUiState(
    val isLoading: Boolean = true,
    val execution: Execution? = null,
    val error: String? = null
)

@HiltViewModel
class ExecutionDetailViewModel @Inject constructor(
    private val repository: N8nRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val executionId: String = savedStateHandle.get<String>("executionId") ?: ""
    
    private val _uiState = MutableStateFlow(ExecutionDetailUiState())
    val uiState: StateFlow<ExecutionDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null
    
    init {
        if (executionId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid execution ID") }
        } else {
            loadExecution()
        }
    }

    fun loadExecution(forceRefresh: Boolean = false) {
        if (actionJob?.isActive == true) return
        if (loadJob?.isActive == true) {
            if (!forceRefresh) return
            loadJob?.cancel()
        }

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getExecution(executionId).fold(
                onSuccess = { execution ->
                    _uiState.update { it.copy(execution = execution, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message ?: "Unable to load execution", isLoading = false) }
                }
            )
        }
    }
    
    fun retryExecution() {
        val currentExecution = _uiState.value.execution ?: return
        if (actionJob?.isActive == true || loadJob?.isActive == true) return

        actionJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.retryExecution(currentExecution.id).fold(
                onSuccess = { execution ->
                    _uiState.update { it.copy(execution = execution, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Unable to retry execution: ${error.message ?: "Try again"}", isLoading = false) }
                }
            )
        }
    }

    fun stopExecution() {
        val currentExecution = _uiState.value.execution ?: return
        if (actionJob?.isActive == true || loadJob?.isActive == true) return

        actionJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.stopExecution(currentExecution.id).fold(
                onSuccess = { execution ->
                    _uiState.update { it.copy(execution = execution, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Unable to stop execution: ${error.message ?: "Try again"}", isLoading = false) }
                }
            )
        }
    }
}
