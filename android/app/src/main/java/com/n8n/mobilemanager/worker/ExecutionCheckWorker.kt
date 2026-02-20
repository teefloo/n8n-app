package com.n8n.mobilemanager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.data.repository.N8nRepository
import com.n8n.mobilemanager.ui.MainActivity
import com.n8n.mobilemanager.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import com.n8n.mobilemanager.utils.DateUtils

@HiltWorker
class ExecutionCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: N8nRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if notifications are enabled
            val notifyErrors = preferencesManager.notifyErrors.first()
            if (!notifyErrors) {
                return Result.success()
            }

            // Get last check time
            val lastCheckTime = preferencesManager.lastErrorCheckTimestamp.first()
            val currentTime = System.currentTimeMillis()

            // Fetch recent failed executions (limit to 10 to check recent ones)
            val result = repository.getExecutions(
                status = ExecutionStatus.ERROR,
                limit = 10,
                fetchAll = false
            )

            result.fold(
                onSuccess = { executions ->
                    // Find new errors since last check
                    val newErrors = executions.filter { execution ->
                        val startedAt = DateUtils.parseInstant(execution.startedAt)?.toEpochMilli()
                        startedAt != null && startedAt > lastCheckTime
                    }

                    // Show notification if there are new errors
                    if (newErrors.isNotEmpty()) {
                        val latestError = newErrors.first() // Most recent
                        val count = newErrors.size
                        
                        val title = if (count == 1) "Execution Error" else "$count execution errors"
                        val body = if (count == 1) {
                            "Workflow ${latestError.workflowName ?: "Unknown"} failed"
                        } else {
                            "Multiple workflows have failed recently"
                        }
                        
                        notificationHelper.showNotification(
                            title = title,
                            body = body,
                            workflowId = latestError.workflowId,
                            executionId = latestError.id
                        )
                    }

                    // Update last check time
                    preferencesManager.setLastErrorCheckTimestamp(currentTime)
                    Result.success()
                },
                onFailure = {
                    // Even if failed, we probably don't want to retry immediately to save battery
                    Result.failure()
                }
            )
        } catch (e: Exception) {
            Result.failure()
        }
    }

}
