package com.n8n.mobilemanager.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class LoginUiState(
    val instanceName: String = "",
    val instanceUrl: String = "",
    val instanceApiKey: String = "",
    val showApiKey: Boolean = false,
    val isTesting: Boolean = false,
    val connectionTestResult: ConnectionResult? = null,
    val isSaving: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkActiveInstance()
    }

    private fun checkActiveInstance() {
        viewModelScope.launch {
            repository.getActiveInstanceFlow().collect { instance ->
                if (instance != null) {
                    _uiState.update { it.copy(isLoggedIn = true) }
                }
            }
        }
    }

    fun updateInstanceName(name: String) {
        _uiState.update { it.copy(instanceName = name, error = null) }
    }

    fun updateInstanceUrl(url: String) {
        _uiState.update { it.copy(instanceUrl = url, connectionTestResult = null, error = null) }
    }

    fun updateInstanceApiKey(apiKey: String) {
        _uiState.update { it.copy(instanceApiKey = apiKey, connectionTestResult = null, error = null) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(showApiKey = !it.showApiKey) }
    }

    fun testConnection() {
        val state = _uiState.value
        
        if (state.instanceUrl.isBlank()) {
            _uiState.update { it.copy(error = "URL required") }
            return
        }
        if (state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, connectionTestResult = null) }

            val testInstance = N8nInstance(
                id = 0,
                name = state.instanceName.ifBlank { "Test" },
                baseUrl = state.instanceUrl,
                apiKey = state.instanceApiKey
            )

            repository.testConnection(testInstance).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            connectionTestResult = ConnectionResult(isSuccess = true)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            connectionTestResult = ConnectionResult(
                                isSuccess = false,
                                errorMessage = error.message ?: "Connection error"
                            )
                        )
                    }
                }
            )
        }
    }

    fun saveInstance() {
        val state = _uiState.value

        if (state.instanceName.isBlank()) {
            _uiState.update { it.copy(error = "Instance name required") }
            return
        }
        if (state.instanceUrl.isBlank()) {
            _uiState.update { it.copy(error = "URL required") }
            return
        }
        if (state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            repository.addInstance(
                name = state.instanceName,
                baseUrl = state.instanceUrl,
                apiKey = state.instanceApiKey
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isLoggedIn = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Error while saving"
                        )
                    }
                }
            )
        }
    }
}
