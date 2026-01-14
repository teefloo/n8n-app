package com.n8n.mobilemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.n8n.mobilemanager.R
import com.n8n.mobilemanager.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Service Firebase pour les notifications push
 */
@AndroidEntryPoint
class N8nFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Get notification data
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "n8n Manager"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val workflowId = remoteMessage.data["workflowId"]
        val executionId = remoteMessage.data["executionId"]
        val status = remoteMessage.data["status"]
        
        // Show notification
        showNotification(title, body, workflowId, executionId, status)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send token to server for push notification registration
    }

    private fun showNotification(
        title: String,
        body: String,
        workflowId: String?,
        executionId: String?,
        status: String?
    ) {
        val channelId = when (status?.lowercase()) {
            "error", "crashed" -> CHANNEL_ERROR
            else -> CHANNEL_DEFAULT
        }
        
        // Create notification channel
        createNotificationChannel(channelId)
        
        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            executionId?.let { putExtra("executionId", it) }
            workflowId?.let { putExtra("workflowId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_ERROR) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = when (channelId) {
                CHANNEL_ERROR -> NotificationChannel(
                    CHANNEL_ERROR,
                    "Execution Errors",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for workflow errors"
                }
                else -> NotificationChannel(
                    CHANNEL_DEFAULT,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications from n8n Manager"
                }
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_DEFAULT = "n8n_default"
        private const val CHANNEL_ERROR = "n8n_errors"
    }
}
