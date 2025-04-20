package com.example.autorecorder.common.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.example.autorecorder.adb.AdbRepository
import com.example.autorecorder.common.notification.NotificationHelper.Companion.TEXT_REPLY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdbPairNotificationReceiver : BroadcastReceiver() {
    private val adbRepository = AdbRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        remoteInput?.let {
            val list = it.getCharSequence(TEXT_REPLY).toString().split(",")
            val code = list.firstOrNull()
            val port = list.lastOrNull()

            coroutineScope.launch {
                try {
                    if (code != null && port != null) {
                        adbRepository.pair(port.toInt(), code)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
        }
    }
}