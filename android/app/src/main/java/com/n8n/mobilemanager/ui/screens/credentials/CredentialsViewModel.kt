package com.n8n.mobilemanager.ui.screens.credentials

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.Credential
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CredentialsViewModel"

data class CachedCredentials(
    val credentials: List<Credential>,
    val timestamp: Long
) {
    fun isValid(): Boolean = System.currentTimeMillis() - timestamp < 2 * 60 * 1000L
}

data class CredentialsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val credentials: List<Credential> = emptyList(),
    val filteredCredentials: List<Credential> = emptyList(),
    val currentCredential: Credential? = null,
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

    private var cachedData: CachedCredentials? = null
    private var loadJob: Job? = null
    private var detailJob: Job? = null
    private var loginJob: Job? = null

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    fun refresh() {
        loadCredentials(forceRefresh = true)
    }

    fun loadCredentials(forceRefresh: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!forceRefresh) return
            loadJob?.cancel()
        }

        loadJob = viewModelScope.launch {
            if (!forceRefresh) {
                cachedData?.takeIf { it.isValid() }?.let { cached ->
                    _uiState.update { state ->
                        val next = state.copy(
                            credentials = cached.credentials,
                            isLoading = false,
                            isRefreshing = false,
                            lastRefreshTime = cached.timestamp
                        )
                        next.copy(filteredCredentials = filterCredentials(next))
                    }
                    return@launch
                }
            }

            val hasData = _uiState.value.credentials.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasData,
                    isRefreshing = hasData,
                    error = null
                )
            }

            repository.getCredentials().fold(
                onSuccess = { credentials ->
                    val timestamp = System.currentTimeMillis()
                    cachedData = CachedCredentials(credentials, timestamp)
                    _uiState.update { state ->
                        val next = state.copy(
                            credentials = credentials,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            lastRefreshTime = timestamp
                        )
                        next.copy(filteredCredentials = filterCredentials(next))
                    }
                },
                onFailure = { error ->
                    val email = runCatching { preferencesManager.credentialsEmail.first() }.getOrNull()
                    val password = runCatching { preferencesManager.credentialsPassword.first() }.getOrNull()
                    if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                        loadCredentialsWithPasswordInternal(email, password)
                    } else {
                        Log.e(TAG, "loadCredentials: Failed to load credentials", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = error.toUserMessage("Unable to load credentials")
                            )
                        }
                    }
                }
            )
        }
    }

    fun loadCredentialsWithPassword(email: String, password: String) {
        if (loginJob?.isActive == true) return
        loginJob = viewModelScope.launch {
            loadCredentialsWithPasswordInternal(email, password)
        }
    }

    private suspend fun loadCredentialsWithPasswordInternal(email: String, password: String) {
        _uiState.update { it.copy(isLoggingIn = true, error = null) }
        repository.getCredentialsWithLogin(email, password).fold(
            onSuccess = { credentials ->
                preferencesManager.setCredentialsAuth(email, password)
                val timestamp = System.currentTimeMillis()
                cachedData = CachedCredentials(credentials, timestamp)
                _uiState.update { state ->
                    val next = state.copy(
                        credentials = credentials,
                        currentCredential = null,
                        isLoggingIn = false,
                        showLoginDialog = false,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        lastRefreshTime = timestamp
                    )
                    next.copy(filteredCredentials = filterCredentials(next))
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        isLoading = false,
                        isRefreshing = false,
                        error = error.toUserMessage("Authentication failed")
                    )
                }
            }
        )
    }

    fun showLoginDialog() {
        _uiState.update { it.copy(showLoginDialog = true, error = null) }
    }

    fun hideLoginDialog() {
        if (_uiState.value.isLoggingIn) return
        _uiState.update { it.copy(showLoginDialog = false, error = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val next = state.copy(searchQuery = query)
            next.copy(filteredCredentials = filterCredentials(next))
        }
    }

    fun loadCredential(id: String) {
        if (id.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid credential ID") }
            return
        }
        loadJob?.cancel()
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    currentCredential = state.credentials.firstOrNull { it.id == id },
                    isLoading = state.credentials.none { it.id == id },
                    error = null
                )
            }
            repository.getCredential(id).fold(
                onSuccess = { credential ->
                    _uiState.update { state ->
                        val mergedCredentials = state.credentials
                            .filterNot { it.id == credential.id } + credential
                        val next = state.copy(
                            credentials = mergedCredentials,
                            currentCredential = credential,
                            isLoading = false,
                            error = null
                        )
                        next.copy(filteredCredentials = filterCredentials(next))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "loadCredential: Failed", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.toUserMessage("Unable to load credential")
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterCredentials(state: CredentialsUiState): List<Credential> {
        if (state.searchQuery.isBlank()) return state.credentials
        return state.credentials.filter { credential ->
            credential.name.contains(state.searchQuery, ignoreCase = true) ||
                credential.type.contains(state.searchQuery, ignoreCase = true)
        }
    }

    private fun Throwable.toUserMessage(fallback: String): String = message
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}
