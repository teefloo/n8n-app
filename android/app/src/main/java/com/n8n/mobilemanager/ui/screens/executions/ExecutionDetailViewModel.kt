package com.n8n.mobilemanager.ui.screens.executions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val executionId: String = checkNotNull(savedStateHandle["executionId"])
    
    private val _uiState = MutableStateFlow(ExecutionDetailUiState())
    val uiState: StateFlow<ExecutionDetailUiState> = _uiState.asStateFlow()

    init {
        loadExecution()
    }

    fun loadExecution() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getExecution(executionId).fold(
                onSuccess = { execution ->
                    _uiState.update { it.copy(execution = execution, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }
    
    fun retryExecution() {
        val currentExecution = _uiState.value.execution ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.retryExecution(currentExecution.id).fold(
                onSuccess = { execution ->
                    _uiState.update { it.copy(execution = execution, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Erreur lors du retry: ${error.message}", isLoading = false) }
                }
            )
        }
    }

    fun stopExecution() {
        val currentExecution = _uiState.value.execution ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.stopExecution(currentExecution.id).fold(
                onSuccess = { execution ->
                    _uiState.update { it.copy(execution = execution, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Erreur lors de l'arrêt: ${error.message}", isLoading = false) }
                }
            )
        }
    }
}
