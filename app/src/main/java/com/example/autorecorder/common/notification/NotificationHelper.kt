package com.example.autorecorder.common.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "message_channel"
        const val NOTIFICATION_ID = 999
        const val TEXT_REPLY = "text_reply"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Message notifications"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showNotification() {
        val action = NotificationCompat.Action
            .Builder(
                android.R.drawable.ic_menu_send,
                "Code,Port",
                PendingIntent.getBroadcast(
                    context,
                    11,
                    Intent(context, AdbPairNotificationReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
            )
            .addRemoteInput(
                RemoteInput.Builder(TEXT_REPLY)
                    .setLabel("Code,Port")
                    .build()
            )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setAllowGeneratedReplies(true)
            .build()

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .addAction(action)
            .setAutoCancel(true)
            .build()

        // Show notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}