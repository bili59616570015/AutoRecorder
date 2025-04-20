package com.example.autorecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.autorecorder.R
import com.example.autorecorder.common.Utils
import com.example.autorecorder.common.recorder.Recorder

class RecordService : Service() {
    var isRecording = false
        private set
    private var recorder: Recorder? = null
    private var resultCode: Int = 0
    private var resultData: Intent? = null

    inner class LocalBinder : Binder() {
        fun getService(): RecordService = this@RecordService
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        isRecording = false
        recorder = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val streamerName = intent.getStringExtra(STREAMER_NAME) ?: "recorder"
                startRecord(streamerName)
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopRecording()
                return START_NOT_STICKY
            }
            ACTION_SETUP -> {
                resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                return START_NOT_STICKY
            }
            ACTION_EXIT -> {
                if (isRecording) {
                    stopRecording()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Recorder",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.record_service))
            .setContentText(if (isRecording) getString(R.string.running) else getString(R.string.pending))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
//        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        if (isRecording) {
//            val stopIntent = Intent(this, RecordService::class.java).apply { action = ACTION_STOP }
//            val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, flags)
//            builder.addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
//        } else {
//            val startIntent = Intent(this, RecordService::class.java).apply { action = ACTION_START }
//            val startPendingIntent = PendingIntent.getService(this, 5, startIntent, flags)
//            builder.addAction(android.R.drawable.ic_media_play, "Start", startPendingIntent)
//        }
//        val exitIntent = Intent(this, RecordService::class.java).apply { action = ACTION_EXIT }
//        val exitPendingIntent = PendingIntent.getService(this, 6, exitIntent, flags)
//        builder.addAction(android.R.drawable.ic_delete, "Exit", exitPendingIntent)

        return builder
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendStatusBroadcast(isRecording: Boolean) {
        val intent = Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_IS_RECORDING, isRecording)
        }
        sendBroadcast(intent)
    }

    private fun startRecord(streamerName: String) {
        recorder = Recorder(this, resultData!!, resultCode, streamerName)
        recorder?.start()
        isRecording = true
        sendStatusBroadcast(isRecording)
        updateNotification()
    }

    private fun stopRecording() {
        recorder?.stop()
        recorder = null
        isRecording = false
        sendStatusBroadcast(isRecording)
        updateNotification()
        Utils.scanFolder()
    }

    companion object {
        const val ACTION_SETUP = "setup_recording"
        const val ACTION_STOP = "stop_recording"
        const val ACTION_START = "start_recording"
        const val ACTION_EXIT = "ACTION_EXIT"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val STREAMER_NAME = "STREAMER_NAME"
        const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "screen_recorder_channel"
        const val ACTION_STATUS = "recording_status"
        const val EXTRA_IS_RECORDING = "extra_is_recording"

        fun start(context: Context, streamerName: String) {
            val intent = Intent(context, RecordService::class.java).apply {
                action = ACTION_START
                putExtra(STREAMER_NAME, streamerName)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startForegroundService(intent)
        }

        fun exit(context: Context) {
            val intent = Intent(context, RecordService::class.java).apply {
                action = ACTION_EXIT
            }
            context.startForegroundService(intent)
        }

        fun setup(context: Context, resultCode: Int, resultData: Intent?) {
            val intent = Intent(context, RecordService::class.java).apply {
                action = ACTION_SETUP
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            context.startForegroundService(intent)
        }
    }
}