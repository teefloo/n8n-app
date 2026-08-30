package com.n8n.mobilemanager.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.repository.N8nRepository
import com.n8n.mobilemanager.data.remote.normalizeN8nBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
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

    private var connectionJob: Job? = null
    private var saveJob: Job? = null

    init {
        checkActiveInstance()
    }

    private fun checkActiveInstance() {
        viewModelScope.launch {
            repository.getActiveInstanceFlow()
                .distinctUntilChangedBy { it?.id }
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoggedIn = false, error = error.message ?: "Unable to read saved instance")
                    }
                }
                .collect { instance ->
                    _uiState.update { it.copy(isLoggedIn = instance != null) }
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
        if (connectionJob?.isActive == true || saveJob?.isActive == true) return

        val normalizedUrl = validateUrl(state.instanceUrl) ?: return
        if (state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API key is required") }
            return
        }

        connectionJob = viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, connectionTestResult = null) }

            val testInstance = N8nInstance(
                id = 0,
                name = state.instanceName.ifBlank { "Test" },
                baseUrl = normalizedUrl,
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
        if (saveJob?.isActive == true || connectionJob?.isActive == true) return
        if (state.instanceName.isBlank()) {
            _uiState.update { it.copy(error = "Instance name is required") }
            return
        }
        val normalizedUrl = validateUrl(state.instanceUrl) ?: return
        if (state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API key is required") }
            return
        }

        saveJob = viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            repository.addInstance(
                name = state.instanceName,
                baseUrl = normalizedUrl,
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

    private fun validateUrl(value: String): String? {
        if (value.isBlank()) {
            _uiState.update { it.copy(error = "URL required") }
            return null
        }
        return normalizeN8nBaseUrl(value).getOrElse { error ->
            _uiState.update { it.copy(error = error.message ?: "Enter a valid n8n URL") }
            null
        }
    }
}
