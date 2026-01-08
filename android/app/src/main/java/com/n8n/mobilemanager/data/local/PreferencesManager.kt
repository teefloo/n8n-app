package com.n8n.mobilemanager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "n8n_preferences")

/**
 * Gestionnaire de préférences utilisateur avec DataStore
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // ==================== Keys ====================
    
    private object PreferencesKeys {
        // Theme
        val THEME_MODE = stringPreferencesKey("theme_mode")
        
        // Security
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout")
        
        // Notifications
        val NOTIFY_ERRORS = booleanPreferencesKey("notify_errors")
        val NOTIFY_SUCCESS = booleanPreferencesKey("notify_success")
        
        // Sync
        val AUTO_REFRESH_INTERVAL = intPreferencesKey("auto_refresh_interval")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        
        // Active instance
        val ACTIVE_INSTANCE_ID = longPreferencesKey("active_instance_id")
        
        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // ==================== Theme ====================
    
    enum class ThemeMode { LIGHT, DARK, SYSTEM }
    
    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(themeName)
        }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    // ==================== Security ====================
    
    val biometricEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.BIOMETRIC_ENABLED] ?: false }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BIOMETRIC_ENABLED] = enabled }
    }
    
    val pinEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.PIN_ENABLED] ?: false }
    
    suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.PIN_ENABLED] = enabled }
    }
    
    val pinHash: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.PIN_HASH] }
    
    suspend fun setPinHash(hash: String?) {
        dataStore.edit { 
            if (hash != null) {
                it[PreferencesKeys.PIN_HASH] = hash 
            } else {
                it.remove(PreferencesKeys.PIN_HASH)
            }
        }
    }
    
    val autoLockTimeout: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.AUTO_LOCK_TIMEOUT] ?: 5 }
    
    suspend fun setAutoLockTimeout(minutes: Int) {
        dataStore.edit { it[PreferencesKeys.AUTO_LOCK_TIMEOUT] = minutes }
    }

    // ==================== Notifications ====================
    
    val notifyErrors: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.NOTIFY_ERRORS] ?: true }
    
    suspend fun setNotifyErrors(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFY_ERRORS] = enabled }
    }
    
    val notifySuccess: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.NOTIFY_SUCCESS] ?: false }
    
    suspend fun setNotifySuccess(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFY_SUCCESS] = enabled }
    }

    // ==================== Sync ====================
    
    val autoRefreshInterval: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.AUTO_REFRESH_INTERVAL] ?: 30 }
    
    suspend fun setAutoRefreshInterval(seconds: Int) {
        dataStore.edit { it[PreferencesKeys.AUTO_REFRESH_INTERVAL] = seconds }
    }
    
    val lastSyncTimestamp: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L }
    
    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp }
    }

    // ==================== Instance ====================
    
    val activeInstanceId: Flow<Long?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.ACTIVE_INSTANCE_ID] }
    
    suspend fun setActiveInstanceId(id: Long?) {
        dataStore.edit {
            if (id != null) {
                it[PreferencesKeys.ACTIVE_INSTANCE_ID] = id
            } else {
                it.remove(PreferencesKeys.ACTIVE_INSTANCE_ID)
            }
        }
    }

    // ==================== Onboarding ====================
    
    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.ONBOARDING_COMPLETED] ?: false }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }

    // ==================== Clear All ====================
    
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
