package com.n8n.mobilemanager.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.n8n.mobilemanager.R
import com.n8n.mobilemanager.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "NotificationHelper"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ERRORS = "n8n_errors"
        const val CHANNEL_SUCCESS = "n8n_success"
        const val CHANNEL_TEST = "n8n_test"
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels...")
            val errorChannel = NotificationChannel(
                CHANNEL_ERRORS,
                "Execution Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for workflow errors"
            }

            val successChannel = NotificationChannel(
                CHANNEL_SUCCESS,
                "Execution Success",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for workflow successes"
            }

            val testChannel = NotificationChannel(
                CHANNEL_TEST,
                "Notification Tests",
                NotificationManager.IMPORTANCE_HIGH // Augmenté pour le test
            ).apply {
                description = "Channel used to test notifications"
            }

            notificationManager.createNotificationChannels(listOf(errorChannel, successChannel, testChannel))
            Log.d(TAG, "Notification channels created successfully")
        }
    }

    fun showNotification(
        title: String,
        body: String,
        channelId: String = CHANNEL_ERRORS,
        workflowId: String? = null,
        executionId: String? = null
    ) {
        Log.d(TAG, "Attempting to show notification: $title - $body (Channel: $channelId)")
        
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                executionId?.let { putExtra("executionId", it) }
                workflowId?.let { putExtra("workflowId", it) }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(when(channelId) {
                    CHANNEL_ERRORS, CHANNEL_TEST -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                })
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "Notification sent to system successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    fun showTestNotification() {
        Log.d(TAG, "Creating test notification...")
        createNotificationChannels() // Re-check channels
        showNotification(
            title = "🔔 n8n Notification Test",
            body = "Congratulations! Notifications are working correctly on your device.",
            channelId = CHANNEL_TEST
        )
    }
}
