package com.n8n.mobilemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.n8n.mobilemanager.data.model.N8nInstance

/**
 * Base de données Room pour le stockage local
 */
@Database(
    entities = [N8nInstance::class],
    version = 1,
    exportSchema = false
)
abstract class N8nDatabase : RoomDatabase() {
    abstract fun instanceDao(): InstanceDao
    
    companion object {
        const val DATABASE_NAME = "n8n_manager_db"
    }
}
