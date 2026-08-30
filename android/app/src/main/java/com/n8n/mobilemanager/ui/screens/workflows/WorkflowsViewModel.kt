package com.n8n.mobilemanager.ui.screens.workflows

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Workflow
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkflowsViewModel"

data class CachedWorkflows(
    val workflows: List<Workflow>,
    val timestamp: Long
) {
    companion object {
        private const val CACHE_DURATION_MS = 2 * 60 * 1000L
    }

    fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
}

data class WorkflowsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val workflows: List<Workflow> = emptyList(),
    val filteredWorkflows: List<Workflow> = emptyList(),
    val searchQuery: String = "",
    val filterActive: Boolean? = null,
    val isTogglingWorkflow: String? = null,
    val lastRefreshTime: Long = 0
)

@HiltViewModel
class WorkflowsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private var cachedData: CachedWorkflows? = null
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(WorkflowsUiState())
    val uiState: StateFlow<WorkflowsUiState> = _uiState.asStateFlow()

    init {
        loadWorkflows()
    }

    fun refresh() {
        loadWorkflows(forceRefresh = true)
    }

    fun loadWorkflows(forceRefresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!forceRefresh) return
            loadJob?.cancel()
        }

        loadJob = viewModelScope.launch {
            if (!forceRefresh) {
                cachedData?.takeIf { it.isValid() }?.let { cached ->
                    _uiState.update { state ->
                        state.copy(
                            workflows = cached.workflows,
                            filteredWorkflows = filterWorkflows(state.copy(workflows = cached.workflows)),
                            isLoading = false,
                            isRefreshing = false,
                            lastRefreshTime = cached.timestamp
                        )
                    }
                    return@launch
                }
            }

            val hasData = _uiState.value.workflows.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasData,
                    isRefreshing = hasData,
                    error = null
                )
            }

            repository.getWorkflows().fold(
                onSuccess = { workflows ->
                    val timestamp = System.currentTimeMillis()
                    cachedData = CachedWorkflows(workflows, timestamp)
                    _uiState.update { state ->
                        val next = state.copy(
                            workflows = workflows,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            lastRefreshTime = timestamp
                        )
                        next.copy(filteredWorkflows = filterWorkflows(next))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "loadWorkflows: Failed", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.toUserMessage("Unable to load workflows")
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val next = state.copy(searchQuery = query)
            next.copy(filteredWorkflows = filterWorkflows(next))
        }
    }

    fun setActiveFilter(active: Boolean?) {
        _uiState.update { state ->
            val next = state.copy(filterActive = active)
            next.copy(filteredWorkflows = filterWorkflows(next))
        }
    }

    fun toggleWorkflowActive(workflowId: String, currentlyActive: Boolean) {
        if (_uiState.value.isTogglingWorkflow != null || loadJob?.isActive == true) return

        viewModelScope.launch {
            val current = _uiState.value.workflows.firstOrNull { it.id == workflowId }?.active
                ?: currentlyActive
            val optimistic = _uiState.value.copy(
                workflows = _uiState.value.workflows.map {
                    if (it.id == workflowId) it.copy(active = !current) else it
                },
                isTogglingWorkflow = workflowId,
                error = null
            )
            _uiState.value = optimistic.copy(filteredWorkflows = filterWorkflows(optimistic))

            val result = if (current) {
                repository.deactivateWorkflow(workflowId)
            } else {
                repository.activateWorkflow(workflowId)
            }

            result.fold(
                onSuccess = { active ->
                    val next = _uiState.value.copy(
                        workflows = _uiState.value.workflows.map {
                            if (it.id == workflowId) it.copy(active = active) else it
                        },
                        isTogglingWorkflow = null
                    )
                    _uiState.value = next.copy(filteredWorkflows = filterWorkflows(next))
                    cachedData?.let { cached ->
                        cachedData = cached.copy(
                            workflows = cached.workflows.map {
                                if (it.id == workflowId) it.copy(active = active) else it
                            }
                        )
                    }
                },
                onFailure = { error ->
                    val reverted = _uiState.value.copy(
                        workflows = _uiState.value.workflows.map {
                            if (it.id == workflowId) it.copy(active = current) else it
                        },
                        isTogglingWorkflow = null,
                        error = error.toUserMessage("Unable to update workflow")
                    )
                    _uiState.value = reverted.copy(filteredWorkflows = filterWorkflows(reverted))
                    cachedData?.let { cached ->
                        cachedData = cached.copy(
                            workflows = cached.workflows.map {
                                if (it.id == workflowId) it.copy(active = current) else it
                            }
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterWorkflows(state: WorkflowsUiState): List<Workflow> = state.workflows.filter { workflow ->
        val matchesSearch = state.searchQuery.isBlank() ||
            workflow.name.contains(state.searchQuery, ignoreCase = true)
        val matchesActive = when (state.filterActive) {
            null -> true
            true -> workflow.active
            false -> !workflow.active
        }
        matchesSearch && matchesActive
    }

    private fun Throwable.toUserMessage(fallback: String): String = message
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}
