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

private const val TAG = "CredentialsViewModel"

data class CredentialsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val credentials: List<Credential> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()

    init {
        // Charger automatiquement les credentials au démarrage
        loadCredentials()
    }

    fun loadCredentials() {
        viewModelScope.launch {
            Log.d(TAG, "loadCredentials: Starting to load credentials...")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getCredentials().fold(
                onSuccess = { credentials ->
                    Log.d(TAG, "loadCredentials: Success! Received ${credentials.size} credentials")
                    credentials.forEach { credential ->
                        Log.d(TAG, "  - Credential: id=${credential.id}, name=${credential.name}, type=${credential.type}")
                    }
                    _uiState.update { 
                        it.copy(
                            credentials = credentials,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "loadCredentials: Failed to load credentials", error)
                    _uiState.update { 
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
