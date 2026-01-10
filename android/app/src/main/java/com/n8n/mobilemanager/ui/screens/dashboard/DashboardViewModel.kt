package com.n8n.mobilemanager.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.*
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatsPeriod(val label: String, val durationMs: Long?) {
    LAST_24H("24h", 24 * 60 * 60 * 1000L),
    LAST_7D("7j", 7 * 24 * 60 * 60 * 1000L),
    LAST_30D("30j", 30 * 24 * 60 * 60 * 1000L),
    ALL_TIME("Tout", null)
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val instance: N8nInstance? = null,
    val isOnline: Boolean = false,
    val stats: InstanceStats = InstanceStats(),
    val recentExecutions: List<Execution> = emptyList(),
    val lastRefreshTime: Long = 0,
    val selectedPeriod: StatsPeriod = StatsPeriod.LAST_24H
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    companion object {
        // Cache statique partagé entre toutes les instances du ViewModel
        private var cachedState: DashboardUiState? = null
        private var cachedInstanceId: Long? = null
        private var cachedPeriod: StatsPeriod? = null
    }

    private val _uiState = MutableStateFlow(
        // Restaurer depuis le cache si disponible
        cachedState ?: DashboardUiState(isLoading = true)
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeActiveInstance()
    }

    private fun observeActiveInstance() {
        viewModelScope.launch {
            repository.getActiveInstanceFlow()
                .distinctUntilChanged { old, new -> 
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
                        // Vérifier si on doit recharger les données
                        val instanceChanged = cachedInstanceId != null && cachedInstanceId != instance.id
                        val hasNoData = cachedState == null || cachedState?.lastRefreshTime == 0L
                        
                        if (hasNoData || instanceChanged) {
                            cachedInstanceId = instance.id
                            loadData()
                        } else {
                            // Utiliser les données en cache, juste mettre à jour l'instance
                            _uiState.update { 
                                cachedState?.copy(instance = instance, isLoading = false) ?: it 
                            }
                        }
                    } else {
                        clearCache()
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

    fun setPeriod(period: StatsPeriod) {
        if (cachedPeriod != period) {
            cachedPeriod = period
            _uiState.update { it.copy(selectedPeriod = period) }
            loadData()
        }
    }

    /**
     * Appelé lors du pull-to-refresh par l'utilisateur
     */
    fun refresh() {
        loadData()
    }
    
    private fun clearCache() {
        cachedState = null
        cachedInstanceId = null
        cachedPeriod = null
    }
    
    /**
     * Charge les données depuis l'API
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val instance = _uiState.value.instance
            if (instance != null) {
                val connectionResult = repository.testConnection(instance)
                val isOnline = connectionResult.isSuccess

                _uiState.update { it.copy(isOnline = isOnline) }

                if (isOnline) {
                    // Fetch stats
                    val startDate = _uiState.value.selectedPeriod.durationMs?.let { duration ->
                        System.currentTimeMillis() - duration
                    }
                    
                    repository.getInstanceStats(startDate).fold(
                        onSuccess = { stats ->
                            _uiState.update { it.copy(stats = stats) }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(error = error.message) }
                        }
                    )

                    // Fetch recent executions
                    repository.getExecutions(limit = 10, fetchAll = false).fold(
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
            
            // Sauvegarder dans le cache après chargement réussi
            cachedState = _uiState.value
            cachedPeriod = _uiState.value.selectedPeriod
        }
    }
}


