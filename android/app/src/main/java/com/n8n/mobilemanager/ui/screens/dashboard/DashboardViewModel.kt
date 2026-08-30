package com.n8n.mobilemanager.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.*
import com.n8n.mobilemanager.data.repository.N8nRepository
import com.n8n.mobilemanager.utils.NotificationHelper
import com.n8n.mobilemanager.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DashboardViewModel"
private const val RECENT_EXECUTIONS_CACHE_DURATION_MS = 60 * 1000L

enum class StatsPeriod(val label: String, val durationMs: Long?, val maxPages: Int) {
    LAST_24H("24h", 24 * 60 * 60 * 1000L, 4),    // ~1000 exécutions max
    LAST_7D("7d", 7 * 24 * 60 * 60 * 1000L, 10),  // ~2500 exécutions max
    LAST_30D("30d", 30 * 24 * 60 * 60 * 1000L, 20), // ~5000 exécutions max
    ALL_TIME("All", null, 40)                     // ~10000 executions max
}

/**
 * Cache des stats par période avec timestamp d'expiration
 */
data class CachedPeriodStats(
    val stats: InstanceStats,
    val executions: List<Execution>,
    val timestamp: Long,
    val periodStartDate: Long?
) {
    companion object {
        // Cache valide pendant 5 minutes (augmenté pour de meilleures performances)
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L
    }
    
    fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false, // Pour le chargement en arrière-plan
    val isPreloading: Boolean = false, // Pour le pré-chargement des autres périodes
    val error: String? = null,
    val instance: N8nInstance? = null,
    val isOnline: Boolean = false,
    val stats: InstanceStats = InstanceStats(),
    val recentExecutions: List<Execution> = emptyList(),
    val lastRefreshTime: Long = 0,
    val selectedPeriod: StatsPeriod = StatsPeriod.LAST_24H,
    val preloadedPeriods: Set<StatsPeriod> = emptySet() // Périodes déjà pré-chargées
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: N8nRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private var autoRefreshJob: Job? = null
    private var preloadJob: Job? = null
    private var loadJob: Job? = null
    private var backgroundRefreshJob: Job? = null
    private var periodLoadJob: Job? = null
    private var lastObservedExecutionId: String? = null

    // Keep dashboard data scoped to this ViewModel. A static cache can leak one
    // instance's data into another account or a different screen lifecycle.
    private var cachedState: DashboardUiState? = null
    private var cachedInstanceId: Long? = null
    private val periodStatsCache = ConcurrentHashMap<StatsPeriod, CachedPeriodStats>()
    private var cachedRecentExecutions: List<Execution>? = null
    private var recentExecutionsCacheTime: Long = 0
    private var isPreloadingAllPeriods = false
    private var periodRequestId = 0L

    private val _uiState = MutableStateFlow(
        DashboardUiState(isLoading = true)
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Afficher immédiatement les données cachées si disponibles
        restoreFromCache()
        observeActiveInstance()
        startAutoRefresh()
    }
    
    /**
     * Restaure l'état depuis le cache statique pour un affichage instantané
     */
    private fun restoreFromCache() {
        cachedState?.let { cached ->
            Log.d(TAG, "restoreFromCache: Restoring cached state immediately")
            _uiState.value = cached.copy(isLoading = false, isRefreshing = false)
        }
        
        // Restaurer aussi les stats de la période sélectionnée
        val selectedPeriod = _uiState.value.selectedPeriod
        periodStatsCache[selectedPeriod]?.let { cachedStats ->
            if (cachedStats.isValid()) {
                Log.d(TAG, "restoreFromCache: Restoring cached stats for $selectedPeriod")
                _uiState.update { it.copy(stats = cachedStats.stats) }
            }
        }
        
        // Restaurer les exécutions récentes
        cachedRecentExecutions?.let { executions ->
            if (System.currentTimeMillis() - recentExecutionsCacheTime < RECENT_EXECUTIONS_CACHE_DURATION_MS) {
                Log.d(TAG, "restoreFromCache: Restoring ${executions.size} cached executions")
                _uiState.update { it.copy(recentExecutions = executions) }
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(120_000) // 2 minutes (augmenté car on a plus de cache)
                if (!_uiState.value.isLoading && !_uiState.value.isRefreshing) {
                    Log.d(TAG, "Auto-refreshing dashboard data...")
                    loadDataInBackground(refreshAllPeriods = true)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
        preloadJob?.cancel()
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
                        val hasCachedData = periodStatsCache.isNotEmpty()
                        
                        if (instanceChanged) {
                            // Instance changée, invalider tout le cache
                            clearAllCaches()
                            cachedInstanceId = instance.id
                            loadDataAndPreloadOthers(forceRefresh = true)
                        } else if (hasNoData && !hasCachedData) {
                            cachedInstanceId = instance.id
                            loadDataAndPreloadOthers(forceRefresh = false)
                        } else if (hasCachedData) {
                            // On a des données en cache, les afficher et rafraîchir silencieusement
                            Log.d(TAG, "observeActiveInstance: Using cached data, refreshing in background")
                            _uiState.update { 
                                cachedState?.copy(instance = instance, isLoading = false) ?: it 
                            }
                            // Test de connexion et rafraîchissement en arrière-plan
                            testConnectionAndRefreshInBackground(instance)
                        } else {
                            loadDataAndPreloadOthers(forceRefresh = false)
                        }
                    } else {
                        clearAllCaches()
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
    
    /**
     * Test la connexion et rafraîchit en arrière-plan sans bloquer l'UI
     */
    private fun testConnectionAndRefreshInBackground(instance: N8nInstance) {
        viewModelScope.launch {
            // Test de connexion en arrière-plan
            val connectionResult = repository.testConnection(instance)
            val isOnline = connectionResult.isSuccess
            _uiState.update { it.copy(isOnline = isOnline) }
            
            if (isOnline) {
                // Rafraîchir les données silencieusement
                loadDataInBackground(refreshAllPeriods = false)
            }
        }
    }
    
    /**
     * Charge les données et pré-charge toutes les autres périodes en arrière-plan
     */
    private fun loadDataAndPreloadOthers(forceRefresh: Boolean) {
        if (loadJob?.isActive == true) {
            if (!forceRefresh) return
            loadJob?.cancel()
        }

        loadJob = viewModelScope.launch {
            try {
                loadData(forceRefresh)
                preloadAllPeriods()
            } finally {
                loadJob = null
            }
        }
    }
    /**
     * Pré-charge les stats ET les exécutions de toutes les périodes en arrière-plan
     * pour un changement de période instantané
     */
    private fun preloadAllPeriods() {
        if (isPreloadingAllPeriods) {
            Log.d(TAG, "preloadAllPeriods: Already preloading, skipping")
            return
        }
        
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            isPreloadingAllPeriods = true
            _uiState.update { it.copy(isPreloading = true) }

            try {
                val instance = _uiState.value.instance ?: return@launch
                val instanceId = instance.id
                val currentPeriod = _uiState.value.selectedPeriod

                Log.d(TAG, "preloadAllPeriods: Starting preload for all periods except $currentPeriod")
                val startTime = System.currentTimeMillis()
                val otherPeriods = StatsPeriod.entries.filter { it != currentPeriod }

                supervisorScope {
                    otherPeriods.map { period ->
                        async(Dispatchers.IO) {
                            try {
                                val cached = periodStatsCache[period]
                                if (cached != null && cached.isValid()) return@async

                                val startDate = period.durationMs?.let { duration ->
                                    System.currentTimeMillis() - duration
                                }
                                val statsDeferred = async {
                                    repository.getInstanceStatsOptimized(startDate, period.maxPages)
                                }
                                val executionsDeferred = async {
                                    repository.getExecutions(
                                        limit = 10,
                                        fetchAll = false,
                                        includeWorkflowNames = true,
                                        startDate = startDate
                                    )
                                }

                                val statsResult = statsDeferred.await()
                                val executionsResult = executionsDeferred.await()
                                if (_uiState.value.instance?.id != instanceId) return@async
                                statsResult.fold(
                                    onSuccess = { stats ->
                                        val executions = executionsResult.getOrDefault(emptyList())
                                        periodStatsCache[period] = CachedPeriodStats(
                                            stats = stats,
                                            executions = executions,
                                            timestamp = System.currentTimeMillis(),
                                            periodStartDate = startDate
                                        )
                                        _uiState.update { it.copy(preloadedPeriods = it.preloadedPeriods + period) }
                                    },
                                    onFailure = { error ->
                                        Log.w(TAG, "preloadAllPeriods: $period stats failed", error)
                                    }
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "preloadAllPeriods: $period exception", e)
                            }
                        }
                    }.awaitAll()
                }

                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "preloadAllPeriods: Completed in ${elapsed}ms, cached periods: ${periodStatsCache.keys}")
            } finally {
                isPreloadingAllPeriods = false
                _uiState.update { it.copy(isPreloading = false) }
            }
        }
    }

    fun setPeriod(period: StatsPeriod) {
        val currentPeriod = _uiState.value.selectedPeriod
        if (currentPeriod == period) {
            Log.d(TAG, "setPeriod: Same period $period, ignoring")
            return
        }

        val requestId = ++periodRequestId
        periodLoadJob?.cancel()

        Log.d(TAG, "setPeriod: ===== CHANGING PERIOD from $currentPeriod to $period =====")
        Log.d(TAG, "setPeriod: Current cache keys: ${periodStatsCache.keys}")
        
        // Vérifier si les données sont déjà en cache
        val cachedData = periodStatsCache[period]
        Log.d(TAG, "setPeriod: Cache for $period = ${if (cachedData != null) "EXISTS (valid=${cachedData.isValid()}, timestamp=${cachedData.timestamp})" else "NULL"}")
        
        if (cachedData != null && cachedData.isValid()) {
            // Utiliser le cache - changement INSTANTANÉ et ATOMIQUE (période + stats en même temps)
            Log.d(TAG, "setPeriod: USING CACHED DATA for $period - " +
                    "success=${cachedData.stats.successfulExecutions}, failed=${cachedData.stats.failedExecutions}, " +
                    "total=${cachedData.stats.totalExecutions}, executions=${cachedData.executions.size}")
            _uiState.update { 
                it.copy(
                    selectedPeriod = period,
                    stats = cachedData.stats,
                    recentExecutions = cachedData.executions
                ) 
            }
            Log.d(TAG, "setPeriod: UI updated with cached data")
            
            // Rafraîchir silencieusement en arrière-plan pour garder les données à jour
            periodLoadJob = viewModelScope.launch {
                refreshPeriodSilently(period)
            }
        } else {
            // Pas de cache, charger les données
            Log.d(TAG, "setPeriod: NO VALID CACHE for $period, loading fresh data...")
            _uiState.update { it.copy(selectedPeriod = period, isRefreshing = true) }
            periodLoadJob = viewModelScope.launch {
                val instance = _uiState.value.instance
                if (instance != null) {
                    loadStatsAndExecutions(instance, period)
                }
                if (requestId == periodRequestId && _uiState.value.selectedPeriod == period) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }
    
    /**
     * Rafraîchit une période spécifique sans aucun indicateur de chargement
     */
    private suspend fun refreshPeriodSilently(period: StatsPeriod) {
        val instance = _uiState.value.instance ?: return
        
        try {
            val startDate = period.durationMs?.let { duration ->
                System.currentTimeMillis() - duration
            }
            
            val (statsResult, executionsResult) = coroutineScope {
                val statsDeferred = async(Dispatchers.IO) {
                    repository.getInstanceStatsOptimized(startDate, period.maxPages)
                }
                val executionsDeferred = async(Dispatchers.IO) {
                    repository.getExecutions(
                        limit = 10,
                        fetchAll = false,
                        includeWorkflowNames = true,
                        startDate = startDate
                    )
                }
                statsDeferred.await() to executionsDeferred.await()
            }
            
            statsResult.fold(
                onSuccess = { stats ->
                    val executions = executionsResult.getOrDefault(emptyList())
                    
                    // Mettre en cache avec les exécutions
                    periodStatsCache[period] = CachedPeriodStats(
                        stats = stats,
                        executions = executions,
                        timestamp = System.currentTimeMillis(),
                        periodStartDate = startDate
                    )
                    
                    // Mettre à jour l'UI seulement si c'est toujours la période sélectionnée
                    if (_uiState.value.selectedPeriod == period) {
                        _uiState.update { 
                            it.copy(
                                stats = stats,
                                recentExecutions = executions
                            ) 
                        }
                        cachedState = _uiState.value
                    }
                },
                onFailure = { /* Ignorer les erreurs silencieuses */ }
            )
        } catch (e: Exception) {
            Log.w(TAG, "refreshPeriodSilently: Exception for $period", e)
        }
    }

    /**
     * Appelé lors du pull-to-refresh par l'utilisateur
     */
    fun refresh() {
        // Invalider le cache pour forcer un rechargement complet
        periodStatsCache.clear()
        cachedRecentExecutions = null
        _uiState.update { it.copy(preloadedPeriods = emptySet()) }
        loadDataAndPreloadOthers(forceRefresh = true)
    }
    
    private fun clearAllCaches() {
        cachedState = null
        cachedInstanceId = null
        periodStatsCache.clear()
        cachedRecentExecutions = null
        recentExecutionsCacheTime = 0
        isPreloadingAllPeriods = false
        lastObservedExecutionId = null
        loadJob?.cancel()
        backgroundRefreshJob?.cancel()
        periodLoadJob?.cancel()
        preloadJob?.cancel()
        periodRequestId++
        _uiState.update { it.copy(preloadedPeriods = emptySet()) }
    }
    
    /**
     * Charge les données en arrière-plan sans bloquer l'UI
     */
    private fun loadDataInBackground(refreshAllPeriods: Boolean = false) {
        if (backgroundRefreshJob?.isActive == true) return

        backgroundRefreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                val instance = _uiState.value.instance
                if (instance != null) {
                    loadStatsAndExecutions(instance, _uiState.value.selectedPeriod)
                    if (refreshAllPeriods) {
                        preloadAllPeriods()
                    }
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
                cachedState = _uiState.value
                backgroundRefreshJob = null
            }
        }
    }
    
    /**
     * Charge les données depuis l'API avec possibilité de forcer le rafraîchissement
     */
    private suspend fun loadData(forceRefresh: Boolean) {
        val selectedPeriod = _uiState.value.selectedPeriod
        
        // Afficher immédiatement les données cachées si disponibles (avant même de montrer le loader)
        if (!forceRefresh) {
            var hasDisplayedCache = false
            
            periodStatsCache[selectedPeriod]?.let { cached ->
                if (cached.isValid()) {
                    _uiState.update { it.copy(stats = cached.stats) }
                    hasDisplayedCache = true
                }
            }
            cachedRecentExecutions?.let { executions ->
                if (System.currentTimeMillis() - recentExecutionsCacheTime < RECENT_EXECUTIONS_CACHE_DURATION_MS) {
                    _uiState.update { it.copy(recentExecutions = executions) }
                    hasDisplayedCache = true
                }
            }
            
            // Si on a affiché du cache, ne montrer qu'un indicateur de refresh léger
            if (hasDisplayedCache) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
        } else {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        val instance = _uiState.value.instance
        if (instance != null) {
            coroutineScope {
                val connectionDeferred = async(Dispatchers.IO) {
                    repository.testConnection(instance)
                }
                val dataDeferred = async {
                    loadStatsAndExecutions(instance, selectedPeriod)
                }
                val connectionResult = connectionDeferred.await()
                if (_uiState.value.instance?.id == instance.id) {
                    _uiState.update { it.copy(isOnline = connectionResult.isSuccess) }
                }
                dataDeferred.await()
            }
        }

        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        
        // Sauvegarder dans le cache après chargement réussi
        cachedState = _uiState.value
    }
    
    /**
     * Charge les stats et exécutions en parallèle pour de meilleures performances
     */
    private suspend fun loadStatsAndExecutions(instance: N8nInstance, period: StatsPeriod) {
        val selectedPeriod = period
        val startDate = selectedPeriod.durationMs?.let { duration ->
            System.currentTimeMillis() - duration
        }
        
        Log.d(TAG, "loadStatsAndExecutions: Starting parallel load for period $selectedPeriod")
        val startTime = System.currentTimeMillis()
        
        val (statsResult, executionsResult) = coroutineScope {
            val statsDeferred = async(Dispatchers.IO) {
                repository.getInstanceStatsOptimized(startDate, selectedPeriod.maxPages)
            }
            val executionsDeferred = async(Dispatchers.IO) {
                repository.getExecutions(
                    limit = 10,
                    fetchAll = false,
                    includeWorkflowNames = true,
                    startDate = startDate
                )
            }
            statsDeferred.await() to executionsDeferred.await()
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "loadStatsAndExecutions: Parallel load completed in ${elapsed}ms")

        if (_uiState.value.instance?.id != instance.id || _uiState.value.selectedPeriod != selectedPeriod) {
            return
        }
        
        // Récupérer les exécutions (même si les stats échouent)
        val loadedExecutions = executionsResult.getOrNull() ?: emptyList()
        
        // Traiter les exécutions d'abord
        executionsResult.fold(
            onSuccess = { executions ->
                Log.d(TAG, "loadStatsAndExecutions: ${executions.size} executions loaded")
                
                // Détection de nouvelles erreurs pour notifications locales
                if (lastObservedExecutionId != null && executions.isNotEmpty()) {
                    checkForNewErrors(executions)
                }
                
                if (executions.isNotEmpty()) {
                    lastObservedExecutionId = executions.first().id
                }

                _uiState.update { 
                    it.copy(
                        recentExecutions = executions,
                        lastRefreshTime = System.currentTimeMillis()
                    ) 
                }
                
                // Mettre en cache global
                cachedRecentExecutions = executions
                recentExecutionsCacheTime = System.currentTimeMillis()
            },
            onFailure = { error ->
                Log.e(TAG, "loadStatsAndExecutions: Executions error", error)
                _uiState.update { it.copy(error = error.message) }
            }
        )
        
        // Traiter les stats et mettre en cache avec les exécutions
        statsResult.fold(
            onSuccess = { stats ->
                Log.d(TAG, "loadStatsAndExecutions: Stats loaded successfully")
                _uiState.update { it.copy(stats = stats) }
                
                // Mettre en cache avec les exécutions pour cette période
                periodStatsCache[selectedPeriod] = CachedPeriodStats(
                    stats = stats,
                    executions = loadedExecutions,
                    timestamp = System.currentTimeMillis(),
                    periodStartDate = startDate
                )
            },
            onFailure = { error ->
                Log.e(TAG, "loadStatsAndExecutions: Stats error", error)
                _uiState.update { it.copy(error = error.message) }
            }
        )
    }

    private fun checkForNewErrors(executions: List<Execution>) {
        viewModelScope.launch {
            val notifyEnabled = preferencesManager.notifyErrors.first()
            if (!notifyEnabled) return@launch

            // On cherche s'il y a une erreur plus récente que celle qu'on a vu en dernier
            // Les exécutions sont triées par date décroissante
            for (exec in executions) {
                if (exec.id == lastObservedExecutionId) break
                
                if (exec.status == ExecutionStatus.ERROR || exec.status == ExecutionStatus.CRASHED) {
                    notificationHelper.showNotification(
                        title = "New error detected",
                        body = "Workflow ${exec.workflowName ?: "Unknown"} failed",
                        workflowId = exec.workflowId,
                        executionId = exec.id
                    )
                    // On ne notifie que pour la plus récente pour éviter le spam
                    break
                }
            }
        }
    }
}
