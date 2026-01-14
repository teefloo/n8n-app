package com.n8n.mobilemanager.ui.screens.credentials

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.Credential
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.n8n.mobilemanager.data.local.PreferencesManager

private const val TAG = "CredentialsViewModel"

/**
 * Cache des credentials avec timestamp d'expiration
 */
data class CachedCredentials(
    val credentials: List<Credential>,
    val timestamp: Long
) {
    companion object {
        // Cache valide pendant 2 minutes
        private const val CACHE_DURATION_MS = 2 * 60 * 1000L
    }
    
    fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
}

data class CredentialsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false, // Chargement en arrière-plan
    val error: String? = null,
    val credentials: List<Credential> = emptyList(),
    val filteredCredentials: List<Credential> = emptyList(),
    val searchQuery: String = "",
    val lastRefreshTime: Long = 0,
    val showLoginDialog: Boolean = false,
    val isLoggingIn: Boolean = false
)

@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val repository: N8nRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        // Cache statique partagé entre toutes les instances du ViewModel
        private var cachedData: CachedCredentials? = null
    }

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()

    init {
        // Restaurer depuis le cache si disponible
        val cached = cachedData
        if (cached != null && cached.isValid()) {
            Log.d(TAG, "init: Restoring ${cached.credentials.size} credentials from cache")
            _uiState.value = CredentialsUiState(
                isLoading = false,
                isRefreshing = true,
                credentials = cached.credentials,
                filteredCredentials = cached.credentials,
                lastRefreshTime = cached.timestamp
            )
            // Rafraîchir en arrière-plan
            refreshInBackground()
        } else {
            loadCredentials()
        }
    }

    /**
     * Rafraîchissement manuel (pull-to-refresh)
     */
    fun refresh() {
        loadCredentials(forceRefresh = true)
    }

    fun loadCredentials(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Vérifier le cache si ce n'est pas un refresh forcé
            if (!forceRefresh) {
                val cached = cachedData
                if (cached != null && cached.isValid()) {
                    Log.d(TAG, "loadCredentials: Using cached data (${cached.credentials.size} credentials)")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            credentials = cached.credentials
                        )
                    }
                    applyFilters()
                    return@launch
                }
            }
            
            Log.d(TAG, "loadCredentials: Starting to load credentials...")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val startTime = System.currentTimeMillis()
            
            repository.getCredentials().fold(
                onSuccess = { credentials ->
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "loadCredentials: Success! Received ${credentials.size} credentials in ${elapsed}ms")
                    
                    // Mettre en cache
                    cachedData = CachedCredentials(
                        credentials = credentials,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _uiState.update { 
                        it.copy(
                            credentials = credentials,
                            isLoading = false,
                            isRefreshing = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    // Try auto-login if available
                    viewModelScope.launch {
                        val email = preferencesManager.credentialsEmail.firstOrNull()
                        val password = preferencesManager.credentialsPassword.firstOrNull()

                        if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                            Log.d(TAG, "loadCredentials: Failed, attempting auto-login with saved credentials...")
                            loadCredentialsWithPassword(email, password)
                        } else {
                            Log.e(TAG, "loadCredentials: Failed to load credentials", error)
                            _uiState.update { 
                                it.copy(
                                    error = error.message,
                                    isLoading = false,
                                    isRefreshing = false
                                )
                            }
                        }
                    }
                }
            )
        }
    }
    
    fun loadCredentialsWithPassword(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, error = null) }
            
            repository.getCredentialsWithLogin(email, password).fold(
                onSuccess = { credentials ->
                    // Sauvegarder les identifiants pour la prochaine fois
                    preferencesManager.setCredentialsAuth(email, password)
                    
                    // Mettre en cache
                    cachedData = CachedCredentials(
                        credentials = credentials,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _uiState.update { 
                        it.copy(
                            credentials = credentials,
                            isLoggingIn = false,
                            showLoginDialog = false,
                            isLoading = false,
                            lastRefreshTime = System.currentTimeMillis()
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoggingIn = false,
                            isLoading = false,
                            error = "Échec authentification: ${error.message}"
                        )
                    }
                }
            )
        }
    }
    
    fun showLoginDialog() {
        _uiState.update { it.copy(showLoginDialog = true) }
    }
    
    fun hideLoginDialog() {
        _uiState.update { it.copy(showLoginDialog = false) }
    }
    
    /**
     * Rafraîchit les données en arrière-plan sans bloquer l'UI
     */
    private fun refreshInBackground() {
        viewModelScope.launch {
            Log.d(TAG, "refreshInBackground: Starting background refresh...")
            
            repository.getCredentials().fold(
                onSuccess = { credentials ->
                    Log.d(TAG, "refreshInBackground: Updated with ${credentials.size} credentials")
                    
                    // Mettre en cache
                    cachedData = CachedCredentials(
                        credentials = credentials,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _uiState.update { 
                        it.copy(
                            credentials = credentials,
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
    
    private fun applyFilters() {
        val state = _uiState.value
        val filtered = if (state.searchQuery.isEmpty()) {
            state.credentials
        } else {
            state.credentials.filter { credential ->
                credential.name.contains(state.searchQuery, ignoreCase = true) ||
                credential.type.contains(state.searchQuery, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(filteredCredentials = filtered) }
    }

    fun loadCredential(id: String) {
        viewModelScope.launch {
            repository.getCredential(id).fold(
                onSuccess = { credential ->
                    _uiState.update { current ->
                        val updatedList = current.credentials.map { 
                            if (it.id == id) credential else it 
                        }
                        current.copy(credentials = updatedList)
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to refresh credential details", error)
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
