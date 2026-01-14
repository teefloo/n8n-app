package com.n8n.mobilemanager.ui.screens.workflows

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Workflow
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkflowsViewModel"

/**
 * Cache des workflows avec timestamp d'expiration
 */
data class CachedWorkflows(
    val workflows: List<Workflow>,
    val timestamp: Long
) {
    companion object {
        // Cache valide pendant 2 minutes
        private const val CACHE_DURATION_MS = 2 * 60 * 1000L
    }
    
    fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
}

data class WorkflowsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false, // Chargement en arrière-plan
    val error: String? = null,
    val workflows: List<Workflow> = emptyList(),
    val filteredWorkflows: List<Workflow> = emptyList(),
    val searchQuery: String = "",
    val filterActive: Boolean? = null, // null = all, true = active only, false = inactive only
    val isTogglingWorkflow: String? = null, // workflow ID being toggled
    val lastRefreshTime: Long = 0
)

@HiltViewModel
class WorkflowsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    companion object {
        // Cache statique partagé entre toutes les instances du ViewModel
        private var cachedData: CachedWorkflows? = null
    }

    private val _uiState = MutableStateFlow(WorkflowsUiState())
    val uiState: StateFlow<WorkflowsUiState> = _uiState.asStateFlow()

    init {
        // Restaurer depuis le cache si disponible
        val cached = cachedData
        if (cached != null && cached.isValid()) {
            Log.d(TAG, "init: Restoring ${cached.workflows.size} workflows from cache")
            _uiState.value = WorkflowsUiState(
                isLoading = false,
                isRefreshing = true,
                workflows = cached.workflows,
                lastRefreshTime = cached.timestamp
            )
            applyFilters()
            // Rafraîchir en arrière-plan
            refreshInBackground()
        } else {
            loadWorkflows()
        }
    }

    /**
     * Rafraîchissement manuel (pull-to-refresh)
     */
    fun refresh() {
        loadWorkflows(forceRefresh = true)
    }

    fun loadWorkflows(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Vérifier le cache si ce n'est pas un refresh forcé
            if (!forceRefresh) {
                val cached = cachedData
                if (cached != null && cached.isValid()) {
                    Log.d(TAG, "loadWorkflows: Using cached data (${cached.workflows.size} workflows)")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            workflows = cached.workflows
                        )
                    }
                    applyFilters()
                    return@launch
                }
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            Log.d(TAG, "loadWorkflows: Fetching from API...")
            val startTime = System.currentTimeMillis()
            
            repository.getWorkflows().fold(
                onSuccess = { workflows ->
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "loadWorkflows: Loaded ${workflows.size} workflows in ${elapsed}ms")
                    
                    // Mettre en cache
                    cachedData = CachedWorkflows(
                        workflows = workflows,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _uiState.update { 
                        it.copy(
                            workflows = workflows,
                            isLoading = false,
                            isRefreshing = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    Log.e(TAG, "loadWorkflows: Failed", error)
                    _uiState.update { 
                        it.copy(
                            error = error.message,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Rafraîchit les données en arrière-plan sans bloquer l'UI
     */
    private fun refreshInBackground() {
        viewModelScope.launch {
            Log.d(TAG, "refreshInBackground: Starting background refresh...")
            
            repository.getWorkflows().fold(
                onSuccess = { workflows ->
                    Log.d(TAG, "refreshInBackground: Updated with ${workflows.size} workflows")
                    
                    // Mettre en cache
                    cachedData = CachedWorkflows(
                        workflows = workflows,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _uiState.update { 
                        it.copy(
                            workflows = workflows,
                            isRefreshing = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                    applyFilters()
                },
                onFailure = {
                    _uiState.update { it.copy(isRefreshing = false) }
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
            
            // Mettre à jour le cache aussi
            cachedData?.let { cached ->
                cachedData = cached.copy(
                    workflows = cached.workflows.map { 
                        if (it.id == workflowId) it.copy(active = !currentlyActive) else it 
                    }
                )
            }
            
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
                    
                    // Mettre à jour le cache
                    cachedData?.let { cached ->
                        cachedData = cached.copy(
                            workflows = cached.workflows.map { 
                                if (it.id == workflowId) it.copy(active = newActiveState) else it 
                            }
                        )
                    }
                },
                onFailure = { error ->
                    // Revert on failure
                    _uiState.update { state ->
                        val revertedWorkflows = state.workflows.map { 
                            if (it.id == workflowId) it.copy(active = currentlyActive) else it 
                        }
                        state.copy(
                            workflows = revertedWorkflows,
                            error = "Failed: ${error.message}",
                            isTogglingWorkflow = null
                        )
                    }
                    applyFilters()
                    
                    // Rétablir le cache
                    cachedData?.let { cached ->
                        cachedData = cached.copy(
                            workflows = cached.workflows.map { 
                                if (it.id == workflowId) it.copy(active = currentlyActive) else it 
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
}
