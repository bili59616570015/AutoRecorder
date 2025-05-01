package com.example.autorecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.autorecorder.R
import com.example.autorecorder.adb.AdbRepository
import com.example.autorecorder.api.bili.BilibiliUseCase
import com.example.autorecorder.database.StreamerRepository
import com.example.autorecorder.entity.Plan
import com.example.autorecorder.screen.upload.UploadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UploadService : Service() {
    private lateinit var notificationManager: NotificationManager
    private var notificationId: Int = 1000
    private var isRunning = false
    private lateinit var repository: StreamerRepository
    private lateinit var adbRepository: AdbRepository
    private val useCase = BilibiliUseCase()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::UploadWakeLock"
        )
        wakeLock?.acquire(60*60*1000L /*60 minutes*/) // Keep CPU on
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        repository = StreamerRepository()
        adbRepository = AdbRepository()
        createNotificationChannel()
        CoroutineScope(Dispatchers.IO + Job()).launch {
            UploadViewModel.runningList.collectLatest { list ->
                list.forEach {
                    updateNotification(it.first.progress.toInt(), it.second)
                }
            }
        }
        CoroutineScope(Dispatchers.IO + Job()).launch {
            UploadViewModel.runningList.collectLatest { list ->
                if (list.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "CANCEL_UPLOAD" -> {
                val id = intent.getStringExtra("planId") ?: return START_STICKY
                UploadViewModel.cancelUpload(id)
                cancelNotification(id)
                return START_NOT_STICKY
            }
            "START_UPLOAD" -> {
                val plan = intent.getSerializableExtra("plan") as? Plan ?: return START_STICKY
                startForeground(notificationId, createNotification(0))
                val job = Job()
                UploadViewModel.upsertUpload(plan.id, job, notificationId++)
                val serviceScope = CoroutineScope(Dispatchers.IO + job)
                serviceScope.launch {
                    startUpload(plan)
                }
                return START_NOT_STICKY
            }
            ACTION_EXIT -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                return START_STICKY
            }
        }
    }

    private suspend fun startUpload(plan: Plan) {
        try {
            useCase.upload(plan)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cancelNotification(plan.id)
            UploadViewModel.removeUpload(plan.id)
        }
    }

    private fun updateNotification(progress: Int, notificationId: Int) {
        val notification = createNotification(progress)
        notificationManager.notify(notificationId, notification)
    }

    private fun cancelNotification(planId: String) {
        val notificationId = UploadViewModel.runningFlow.value[planId]?.second ?: return
        notificationManager.cancel(notificationId)
    }

    private fun createNotification(progress: Int): Notification {
        val exitIntent = Intent(this, UploadService::class.java).apply { action = ACTION_EXIT }
        val exitPendingIntent = PendingIntent.getService(this, 7, exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.upload_service))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)  // Keeps notification active
            .setProgress(100, progress, false)
            .addAction(android.R.drawable.ic_delete, getString(R.string.exit), exitPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Upload Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (wakeLock?.isHeld == true) wakeLock?.release() // Release to save battery
    }

    private val CHANNEL_ID = "upload_service_channel"

    companion object {
        const val ACTION_EXIT = "ACTION_EXIT"
    }

}