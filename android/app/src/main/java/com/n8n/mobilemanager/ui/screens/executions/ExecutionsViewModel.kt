package com.n8n.mobilemanager.ui.screens.executions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.data.model.Workflow
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExecutionsViewModel"

/**
 * Cache des exécutions avec timestamp d'expiration
 */
data class CachedExecutions(
    val executions: List<Execution>,
    val workflows: List<Workflow>,
    val nextCursor: String?,
    val timestamp: Long,
    val workflowFilter: String?,
    val statusFilter: ExecutionStatus?
) {
    companion object {
        // Cache valide pendant 1 minute
        private const val CACHE_DURATION_MS = 60 * 1000L
    }
    
    fun isValid(workflowId: String?, status: ExecutionStatus?): Boolean {
        return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS &&
               workflowFilter == workflowId &&
               statusFilter == status
    }
}

data class ExecutionsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false, // Chargement en arrière-plan
    val error: String? = null,
    val executions: List<Execution> = emptyList(), // Accumulates all loaded executions
    val filteredExecutions: List<Execution> = emptyList(), // Same as executions in this context (filters are applied API side)
    val workflows: List<Workflow> = emptyList(),
    val selectedWorkflowId: String? = null,
    val selectedStatus: ExecutionStatus? = null,
    val nextCursor: String? = null,
    val lastRefreshTime: Long = 0
)

@HiltViewModel
class ExecutionsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    companion object {
        // Cache statique partagé entre toutes les instances du ViewModel
        private var cachedData: CachedExecutions? = null
        private var cachedWorkflows: List<Workflow>? = null
        private var workflowsCacheTime: Long = 0
        private const val WORKFLOWS_CACHE_DURATION_MS = 2 * 60 * 1000L // 2 minutes
    }

    private val _uiState = MutableStateFlow(ExecutionsUiState())
    val uiState: StateFlow<ExecutionsUiState> = _uiState.asStateFlow()

    init {
        // Restaurer depuis le cache si disponible
        val cached = cachedData
        if (cached != null && cached.isValid(null, null)) {
            Log.d(TAG, "init: Restoring ${cached.executions.size} executions from cache")
            _uiState.value = ExecutionsUiState(
                isLoading = false,
                isRefreshing = true,
                executions = cached.executions,
                filteredExecutions = cached.executions,
                workflows = cached.workflows,
                nextCursor = cached.nextCursor,
                lastRefreshTime = cached.timestamp
            )
            // Rafraîchir en arrière-plan
            refreshInBackground()
        } else {
            loadData()
        }
    }

    /**
     * Rafraîchissement manuel (pull-to-refresh)
     */
    fun refresh() {
        loadData(forceRefresh = true)
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Vérifier le cache si ce n'est pas un refresh forcé
            if (!forceRefresh) {
                val cached = cachedData
                if (cached != null && cached.isValid(state.selectedWorkflowId, state.selectedStatus)) {
                    Log.d(TAG, "loadData: Using cached data (${cached.executions.size} executions)")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            executions = cached.executions,
                            filteredExecutions = cached.executions,
                            workflows = cached.workflows,
                            nextCursor = cached.nextCursor
                        )
                    }
                    return@launch
                }
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            Log.d(TAG, "loadData: Starting parallel load...")
            val startTime = System.currentTimeMillis()
            
            // Charger workflows et exécutions en parallèle
            val workflowsDeferred = async {
                // Utiliser le cache des workflows si valide
                val cached = cachedWorkflows
                if (cached != null && System.currentTimeMillis() - workflowsCacheTime < WORKFLOWS_CACHE_DURATION_MS) {
                    Log.d(TAG, "loadData: Using cached workflows")
                    Result.success(cached)
                } else {
                    repository.getWorkflows()
                }
            }
            
            val executionsDeferred = async {
                repository.getExecutionsPage(
                    workflowId = state.selectedWorkflowId,
                    status = state.selectedStatus,
                    limit = 30, // Charger plus d'items initialement
                    cursor = null
                )
            }
            
            // Attendre les résultats
            val workflowsResult = workflowsDeferred.await()
            val executionsResult = executionsDeferred.await()
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "loadData: Parallel load completed in ${elapsed}ms")
            
            // Traiter les workflows
            val workflows = workflowsResult.fold(
                onSuccess = { workflows ->
                    cachedWorkflows = workflows
                    workflowsCacheTime = System.currentTimeMillis()
                    workflows
                },
                onFailure = { cachedWorkflows ?: emptyList() }
            )
            
            // Traiter les exécutions
            executionsResult.fold(
                onSuccess = { page ->
                    Log.d(TAG, "loadData: Loaded ${page.executions.size} executions")
                    
                    // Mettre en cache
                    cachedData = CachedExecutions(
                        executions = page.executions,
                        workflows = workflows,
                        nextCursor = page.nextCursor,
                        timestamp = System.currentTimeMillis(),
                        workflowFilter = state.selectedWorkflowId,
                        statusFilter = state.selectedStatus
                    )
                    
                    _uiState.update { 
                        it.copy(
                            executions = page.executions,
                            filteredExecutions = page.executions,
                            workflows = workflows,
                            nextCursor = page.nextCursor,
                            isLoading = false,
                            isRefreshing = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "loadData: Failed to load executions", error)
                    _uiState.update { 
                        it.copy(
                            workflows = workflows,
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
            
            val state = _uiState.value
            
            val workflowsDeferred = async {
                repository.getWorkflows()
            }
            
            val executionsDeferred = async {
                repository.getExecutionsPage(
                    workflowId = state.selectedWorkflowId,
                    status = state.selectedStatus,
                    limit = 30,
                    cursor = null
                )
            }
            
            val workflowsResult = workflowsDeferred.await()
            val executionsResult = executionsDeferred.await()
            
            val workflows = workflowsResult.fold(
                onSuccess = { workflows ->
                    cachedWorkflows = workflows
                    workflowsCacheTime = System.currentTimeMillis()
                    workflows
                },
                onFailure = { state.workflows }
            )
            
            executionsResult.fold(
                onSuccess = { page ->
                    Log.d(TAG, "refreshInBackground: Updated with ${page.executions.size} executions")
                    
                    cachedData = CachedExecutions(
                        executions = page.executions,
                        workflows = workflows,
                        nextCursor = page.nextCursor,
                        timestamp = System.currentTimeMillis(),
                        workflowFilter = state.selectedWorkflowId,
                        statusFilter = state.selectedStatus
                    )
                    
                    _uiState.update { 
                        it.copy(
                            executions = page.executions,
                            filteredExecutions = page.executions,
                            workflows = workflows,
                            nextCursor = page.nextCursor,
                            isRefreshing = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.nextCursor == null) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            repository.getExecutionsPage(
                workflowId = state.selectedWorkflowId,
                status = state.selectedStatus,
                limit = 20,
                cursor = state.nextCursor
            ).fold(
                onSuccess = { page ->
                    val newExecutions = state.executions + page.executions
                    
                    // Mettre à jour le cache
                    cachedData = CachedExecutions(
                        executions = newExecutions,
                        workflows = state.workflows,
                        nextCursor = page.nextCursor,
                        timestamp = System.currentTimeMillis(),
                        workflowFilter = state.selectedWorkflowId,
                        statusFilter = state.selectedStatus
                    )
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            executions = newExecutions,
                            filteredExecutions = newExecutions,
                            nextCursor = page.nextCursor,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message,
                            isLoadingMore = false
                        )
                    }
                }
            )
        }
    }

    fun setWorkflowFilter(workflowId: String?) {
        val currentFilter = _uiState.value.selectedWorkflowId
        if (currentFilter == workflowId) return
        
        // Invalider le cache car le filtre change
        cachedData = null
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    selectedWorkflowId = workflowId, 
                    isLoading = true,
                    nextCursor = null
                ) 
            }
            loadData(forceRefresh = true)
        }
    }

    fun setStatusFilter(status: ExecutionStatus?) {
        val currentFilter = _uiState.value.selectedStatus
        if (currentFilter == status) return
        
        // Invalider le cache car le filtre change
        cachedData = null
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    selectedStatus = status, 
                    isLoading = true,
                    nextCursor = null
                ) 
            }
            loadData(forceRefresh = true)
        }
    }

    fun retryExecution(executionId: String) {
        viewModelScope.launch {
            repository.retryExecution(executionId).fold(
                onSuccess = {
                    // Invalider le cache et recharger
                    cachedData = null
                    loadData(forceRefresh = true)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Failed: ${error.message}") }
                }
            )
        }
    }

    fun stopExecution(executionId: String) {
        viewModelScope.launch {
            repository.stopExecution(executionId).fold(
                onSuccess = {
                    cachedData = null
                    loadData(forceRefresh = true)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Failed: ${error.message}") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
