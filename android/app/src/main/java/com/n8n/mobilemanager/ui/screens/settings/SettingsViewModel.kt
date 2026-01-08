package com.n8n.mobilemanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val instances: List<N8nInstance> = emptyList(),
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
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeInstances()
        observePreferences()
    }

    private fun observeInstances() {
        viewModelScope.launch {
            repository.getAllInstances().collect { instances ->
                val activeInstance = instances.find { it.isActive }
                _uiState.update { 
                    it.copy(
                        instances = instances,
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

    // ==================== Instances ====================
    
    fun startAddingInstance() {
        _uiState.update { 
            it.copy(
                isAddingInstance = true,
                editingInstance = null,
                instanceName = "",
                instanceUrl = "",
                instanceApiKey = "",
                connectionTestResult = null
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
                connectionTestResult = null
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
        _uiState.update { it.copy(instanceName = name) }
    }
    
    fun updateInstanceUrl(url: String) {
        _uiState.update { it.copy(instanceUrl = url, connectionTestResult = null) }
    }
    
    fun updateInstanceApiKey(apiKey: String) {
        _uiState.update { it.copy(instanceApiKey = apiKey, connectionTestResult = null) }
    }
    
    fun testConnection() {
        val state = _uiState.value
        if (state.instanceUrl.isBlank() || state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "URL et clé API requises") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            
            val testInstance = N8nInstance(
                id = state.editingInstance?.id ?: 0,
                name = state.instanceName.ifBlank { "Test" },
                baseUrl = state.instanceUrl,
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
                            connectionTestResult = ConnectionTestResult.Error(error.message ?: "Erreur inconnue")
                        )
                    }
                }
            )
        }
    }
    
    fun saveInstance() {
        val state = _uiState.value
        
        if (state.instanceName.isBlank()) {
            _uiState.update { it.copy(error = "Nom requis") }
            return
        }
        if (state.instanceUrl.isBlank()) {
            _uiState.update { it.copy(error = "URL requise") }
            return
        }
        if (state.instanceApiKey.isBlank()) {
            _uiState.update { it.copy(error = "Clé API requise") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingInstance = true, error = null) }
            
            val result = if (state.editingInstance != null) {
                repository.updateInstance(
                    state.editingInstance.copy(
                        name = state.instanceName,
                        baseUrl = state.instanceUrl,
                        apiKey = state.instanceApiKey
                    )
                )
            } else {
                repository.addInstance(
                    name = state.instanceName,
                    baseUrl = state.instanceUrl,
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
        viewModelScope.launch {
            repository.setActiveInstance(instanceId)
        }
    }
    
    fun deleteInstance(instance: N8nInstance) {
        viewModelScope.launch {
            repository.deleteInstance(instance)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
