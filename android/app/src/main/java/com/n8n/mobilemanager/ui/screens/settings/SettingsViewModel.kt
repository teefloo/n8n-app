package com.n8n.mobilemanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.remote.normalizeN8nBaseUrl
import com.n8n.mobilemanager.data.repository.N8nRepository
import com.n8n.mobilemanager.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val instances: List<N8nInstance> = emptyList(),
    val hasLoadedInstances: Boolean = false,
    val activeInstanceId: Long? = null,
    val themeMode: PreferencesManager.ThemeMode = PreferencesManager.ThemeMode.SYSTEM,
    val biometricEnabled: Boolean = false,
    val notifyErrors: Boolean = true,
    val notifySuccess: Boolean = false,
    
    // Instance form
    val isAddingInstance: Boolean = false,
    val editingInstance: N8nInstance? = null,
    val instanceName: String = "",
    val instanceUrl: String = "",
    val instanceApiKey: String = "",
    val isTestingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null,
    val isSavingInstance: Boolean = false,
    val error: String? = null
)

sealed class ConnectionTestResult {
    object Success : ConnectionTestResult()
    data class Error(val message: String) : ConnectionTestResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: N8nRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var connectionJob: Job? = null
    private var saveJob: Job? = null
    private var instanceActionJob: Job? = null

    init {
        observeInstances()
        observePreferences()
    }

    private fun observeInstances() {
        viewModelScope.launch {
            repository.getAllInstances()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            hasLoadedInstances = true,
                            error = error.message ?: "Unable to load saved instances"
                        )
                    }
                }
                .collect { instances ->
                val activeInstance = instances.find { it.isActive }
                _uiState.update { 
                    it.copy(
                        instances = instances,
                        hasLoadedInstances = true,
                        activeInstanceId = activeInstance?.id
                    )
                }
                }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.themeMode.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }
        
        viewModelScope.launch {
            preferencesManager.biometricEnabled.collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            preferencesManager.notifyErrors.collect { enabled ->
                _uiState.update { it.copy(notifyErrors = enabled) }
            }
        }
        
        viewModelScope.launch {
            preferencesManager.notifySuccess.collect { enabled ->
                _uiState.update { it.copy(notifySuccess = enabled) }
            }
        }
    }

    // ==================== Theme ====================
    
    fun setThemeMode(mode: PreferencesManager.ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    // ==================== Security ====================
    
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricEnabled(enabled)
        }
    }

    // ==================== Notifications ====================
    
    fun setNotifyErrors(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotifyErrors(enabled)
        }
    }
    
    fun setNotifySuccess(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotifySuccess(enabled)
        }
    }

    fun testNotification() {
        notificationHelper.showTestNotification()
    }

    // ==================== Instances ====================
    
    fun startAddingInstance() {
        _uiState.update { 
            it.copy(
                isAddingInstance = true,
                editingInstance = null,
                instanceName = "",
                instanceUrl = "",
                instanceApiKey = "",
                connectionTestResult = null,
                error = null
            )
        }
    }
    
    fun startEditingInstance(instance: N8nInstance) {
        _uiState.update { 
            it.copy(
                isAddingInstance = true,
                editingInstance = instance,
                instanceName = instance.name,
                instanceUrl = instance.baseUrl,
                instanceApiKey = instance.apiKey,
                connectionTestResult = null,
                error = null
            )
        }
    }
    
    fun cancelInstanceForm() {
        _uiState.update { 
            it.copy(
                isAddingInstance = false,
                editingInstance = null,
                instanceName = "",
                instanceUrl = "",
                instanceApiKey = "",
                connectionTestResult = null,
                error = null
            )
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
    
    fun testConnection() {
        val state = _uiState.value
        if (connectionJob?.isActive == true || saveJob?.isActive == true) return

        val normalizedUrl = validateUrl(state.instanceUrl) ?: return
        if (state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API key is required") }
            return
        }
        
        connectionJob = viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            
            val testInstance = N8nInstance(
                id = state.editingInstance?.id ?: 0,
                name = state.instanceName.trim().ifBlank { "Test" },
                baseUrl = normalizedUrl,
                apiKey = state.instanceApiKey
            )
            
            repository.testConnection(testInstance).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isTestingConnection = false,
                            connectionTestResult = ConnectionTestResult.Success
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isTestingConnection = false,
                            connectionTestResult = ConnectionTestResult.Error(error.message ?: "Unknown error")
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
            _uiState.update { it.copy(isSavingInstance = true, error = null) }
            
            val result = if (state.editingInstance != null) {
                repository.updateInstance(
                    state.editingInstance.copy(
                        name = state.instanceName.trim(),
                        baseUrl = normalizedUrl,
                        apiKey = state.instanceApiKey
                    )
                )
            } else {
                repository.addInstance(
                    name = state.instanceName.trim(),
                    baseUrl = normalizedUrl,
                    apiKey = state.instanceApiKey
                ).map { }
            }
            
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isSavingInstance = false,
                            isAddingInstance = false,
                            editingInstance = null,
                            instanceName = "",
                            instanceUrl = "",
                            instanceApiKey = "",
                            connectionTestResult = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isSavingInstance = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }
    
    fun setActiveInstance(instanceId: Long) {
        if (instanceActionJob?.isActive == true || saveJob?.isActive == true) return
        instanceActionJob = viewModelScope.launch {
            repository.setActiveInstance(instanceId).fold(
                onSuccess = { _uiState.update { it.copy(error = null) } },
                onFailure = { error -> _uiState.update { it.copy(error = error.message ?: "Unable to switch instance") } }
            )
        }
    }
    
    fun deleteInstance(instance: N8nInstance) {
        if (instanceActionJob?.isActive == true || saveJob?.isActive == true) return
        instanceActionJob = viewModelScope.launch {
            repository.deleteInstance(instance).fold(
                onSuccess = { _uiState.update { it.copy(error = null) } },
                onFailure = { error -> _uiState.update { it.copy(error = error.message ?: "Unable to delete instance") } }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
