package com.n8n.mobilemanager.data.local

import androidx.room.*
import com.n8n.mobilemanager.data.model.N8nInstance
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour la gestion des instances n8n en base locale
 */
@Dao
interface InstanceDao {
    
    @Query("SELECT * FROM instances ORDER BY createdAt DESC")
    fun getAllInstances(): Flow<List<N8nInstance>>
    
    @Query("SELECT * FROM instances WHERE id = :id")
    suspend fun getInstanceById(id: Long): N8nInstance?
    
    @Query("SELECT * FROM instances WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveInstance(): N8nInstance?
    
    @Query("SELECT * FROM instances WHERE isActive = 1 LIMIT 1")
    fun getActiveInstanceFlow(): Flow<N8nInstance?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: N8nInstance): Long
    
    @Update
    suspend fun updateInstance(instance: N8nInstance)
    
    @Delete
    suspend fun deleteInstance(instance: N8nInstance)

    @Query("DELETE FROM instances WHERE id = :id")
    suspend fun deleteInstanceById(id: Long)
    
    @Query("UPDATE instances SET isActive = 0")
    suspend fun deactivateAllInstances()
    
    @Query("UPDATE instances SET isActive = 1 WHERE id = :id")
    suspend fun setActiveInstance(id: Long)
    
    @Query("UPDATE instances SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
    
    @Transaction
    suspend fun setAsActive(id: Long) {
        deactivateAllInstances()
        setActiveInstance(id)
    }
}
