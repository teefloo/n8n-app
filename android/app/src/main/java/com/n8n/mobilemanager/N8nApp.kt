package com.n8n.mobilemanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.n8n.mobilemanager.worker.ExecutionCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application principale n8n Mobile Manager
 * Point d'entrée de l'injection de dépendances Hilt
 */
@HiltAndroidApp
class N8nApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicWork()
    }

    private fun schedulePeriodicWork() {
        val workRequest = PeriodicWorkRequestBuilder<ExecutionCheckWorker>(
            15, TimeUnit.MINUTES // Minimum interval allowed by Android
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExecutionCheckWork",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            workRequest
        )
    }
}

