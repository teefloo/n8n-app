package com.n8n.mobilemanager.ui.screens.executions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.data.model.Workflow
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExecutionsViewModel"

data class CachedExecutions(
    val executions: List<Execution>,
    val workflows: List<Workflow>,
    val nextCursor: String?,
    val timestamp: Long,
    val workflowFilter: String?,
    val statusFilter: ExecutionStatus?
) {
    fun isValid(workflowId: String?, status: ExecutionStatus?): Boolean =
        System.currentTimeMillis() - timestamp < 60 * 1000L &&
            workflowFilter == workflowId && statusFilter == status
}

data class ExecutionsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val executions: List<Execution> = emptyList(),
    val filteredExecutions: List<Execution> = emptyList(),
    val workflows: List<Workflow> = emptyList(),
    val selectedWorkflowId: String? = null,
    val selectedStatus: ExecutionStatus? = null,
    val nextCursor: String? = null,
    val lastRefreshTime: Long = 0,
    val actionExecutionId: String? = null
)

@HiltViewModel
class ExecutionsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private var cachedData: CachedExecutions? = null
    private var cachedWorkflows: List<Workflow>? = null
    private var workflowsCacheTime: Long = 0
    private var loadJob: Job? = null
    private var requestToken = 0L

    private val _uiState = MutableStateFlow(ExecutionsUiState())
    val uiState: StateFlow<ExecutionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        loadData(forceRefresh = true)
    }

    fun loadData(forceRefresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!forceRefresh) return
            loadJob?.cancel()
        }

        val token = ++requestToken
        loadJob = viewModelScope.launch {
            val requestedWorkflow = _uiState.value.selectedWorkflowId
            val requestedStatus = _uiState.value.selectedStatus

            if (!forceRefresh) {
                cachedData?.takeIf { it.isValid(requestedWorkflow, requestedStatus) }?.let { cached ->
                    _uiState.update {
                        it.copy(
                            executions = cached.executions,
                            filteredExecutions = cached.executions,
                            workflows = cached.workflows,
                            nextCursor = cached.nextCursor,
                            isLoading = false,
                            isRefreshing = false,
                            lastRefreshTime = cached.timestamp
                        )
                    }
                    return@launch
                }
            }

            val hasData = _uiState.value.executions.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasData,
                    isRefreshing = hasData,
                    isLoadingMore = false,
                    nextCursor = null,
                    error = null
                )
            }

            try {
                val (workflowsResult, executionsResult) = coroutineScope {
                    val workflowsDeferred = async {
                        val cached = cachedWorkflows
                        if (cached != null && System.currentTimeMillis() - workflowsCacheTime < 2 * 60 * 1000L) {
                            Result.success(cached)
                        } else {
                            repository.getWorkflows()
                        }
                    }
                    val executionsDeferred = async {
                        repository.getExecutionsPage(
                            workflowId = requestedWorkflow,
                            status = requestedStatus,
                            limit = 30
                        )
                    }
                    workflowsDeferred.await() to executionsDeferred.await()
                }

                if (token != requestToken) return@launch

                val workflows = workflowsResult.getOrElse {
                    _uiState.value.workflows
                }.also {
                    if (workflowsResult.isSuccess) {
                        cachedWorkflows = it
                        workflowsCacheTime = System.currentTimeMillis()
                    }
                }

                executionsResult.fold(
                    onSuccess = { page ->
                        val timestamp = System.currentTimeMillis()
                        cachedData = CachedExecutions(
                            executions = page.executions,
                            workflows = workflows,
                            nextCursor = page.nextCursor,
                            timestamp = timestamp,
                            workflowFilter = requestedWorkflow,
                            statusFilter = requestedStatus
                        )
                        _uiState.update {
                            it.copy(
                                executions = page.executions,
                                filteredExecutions = page.executions,
                                workflows = workflows,
                                nextCursor = page.nextCursor,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                lastRefreshTime = timestamp
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                workflows = workflows,
                                isLoading = false,
                                isRefreshing = false,
                                error = error.toUserMessage("Unable to load executions")
                            )
                        }
                    }
                )
            } catch (error: Exception) {
                Log.e(TAG, "loadData: Failed", error)
                if (token == requestToken) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.toUserMessage("Unable to load executions")
                        )
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore) return
        val token = requestToken

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getExecutionsPage(
                workflowId = state.selectedWorkflowId,
                status = state.selectedStatus,
                limit = 20,
                cursor = cursor
            ).fold(
                onSuccess = { page ->
                    _uiState.update { current ->
                        if (token != requestToken || current.nextCursor != cursor) {
                            return@update current.copy(isLoadingMore = false)
                        }
                        val ids = current.executions.asSequence().map { it.id }.toHashSet()
                        val newExecutions = current.executions + page.executions.filterNot { it.id in ids }
                        cachedData = cachedData?.copy(
                            executions = newExecutions,
                            nextCursor = page.nextCursor,
                            timestamp = System.currentTimeMillis()
                        )
                        current.copy(
                            executions = newExecutions,
                            filteredExecutions = newExecutions,
                            nextCursor = page.nextCursor,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    if (token == requestToken) {
                        _uiState.update {
                            it.copy(isLoadingMore = false, error = error.toUserMessage("Unable to load more executions"))
                        }
                    }
                }
            )
        }
    }

    fun setWorkflowFilter(workflowId: String?) {
        if (_uiState.value.selectedWorkflowId == workflowId) return
        _uiState.update { it.copy(selectedWorkflowId = workflowId, nextCursor = null) }
        cachedData = null
        loadData(forceRefresh = true)
    }

    fun setStatusFilter(status: ExecutionStatus?) {
        if (_uiState.value.selectedStatus == status) return
        _uiState.update { it.copy(selectedStatus = status, nextCursor = null) }
        cachedData = null
        loadData(forceRefresh = true)
    }

    fun retryExecution(executionId: String) {
        performAction(executionId) { repository.retryExecution(executionId) }
    }

    fun stopExecution(executionId: String) {
        performAction(executionId) { repository.stopExecution(executionId) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun performAction(executionId: String, action: suspend () -> Result<Execution>) {
        if (_uiState.value.actionExecutionId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(actionExecutionId = executionId, error = null) }
            action().fold(
                onSuccess = {
                    _uiState.update { it.copy(actionExecutionId = null) }
                    cachedData = null
                    loadData(forceRefresh = true)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            actionExecutionId = null,
                            error = error.toUserMessage("Unable to update execution")
                        )
                    }
                }
            )
        }
    }

    private fun Throwable.toUserMessage(fallback: String): String = message
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}
