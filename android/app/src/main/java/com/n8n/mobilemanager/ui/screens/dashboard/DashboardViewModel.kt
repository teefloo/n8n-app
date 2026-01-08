package com.n8n.mobilemanager.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.*
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val instance: N8nInstance? = null,
    val isOnline: Boolean = false,
    val stats: InstanceStats = InstanceStats(),
    val recentExecutions: List<Execution> = emptyList(),
    val lastRefreshTime: Long = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeActiveInstance()
    }

    private fun observeActiveInstance() {
        viewModelScope.launch {
            repository.getActiveInstanceFlow()
                .distinctUntilChanged { old, new -> 
                    // On ne déclenche un rafraîchissement que si l'ID, l'URL ou la Clé API changent
                    old?.id == new?.id && old?.baseUrl == new?.baseUrl && old?.apiKey == new?.apiKey
                }
                .collect { instance ->
                    if (instance == null) {
                        val all = repository.getAllInstances().first()
                        if (all.isNotEmpty()) {
                            repository.setActiveInstance(all.first().id)
                            return@collect
                        }
                    }
                    
                    _uiState.update { it.copy(instance = instance) }
                    if (instance != null) {
                        refresh()
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                isOnline = false,
                                stats = InstanceStats(),
                                recentExecutions = emptyList()
                            ) 
                        }
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Test connection
            val instance = _uiState.value.instance
            if (instance != null) {
                val connectionResult = repository.testConnection(instance)
                val isOnline = connectionResult.isSuccess

                _uiState.update { it.copy(isOnline = isOnline) }

                if (isOnline) {
                    // Fetch stats
                    repository.getInstanceStats().fold(
                        onSuccess = { stats ->
                            _uiState.update { it.copy(stats = stats) }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(error = error.message) }
                        }
                    )

                    // Fetch recent executions
                    repository.getExecutions(limit = 10).fold(
                        onSuccess = { executions ->
                            _uiState.update { 
                                it.copy(
                                    recentExecutions = executions,
                                    lastRefreshTime = System.currentTimeMillis()
                                ) 
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(error = error.message) }
                        }
                    )
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
