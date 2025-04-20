package com.example.autorecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.autorecorder.R
import com.example.autorecorder.adb.AdbRepository
import com.example.autorecorder.api.douyin.DouyinRepository
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.common.adbCommands
import com.example.autorecorder.database.StreamerRepository
import com.example.autorecorder.entity.LiveRoom
import com.example.autorecorder.entity.Plan
import com.example.autorecorder.screen.upload.UploadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

class LiveService : Service() {
    inner class LocalBinder : Binder() {
        fun getService(): LiveService = this@LiveService
    }
    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    var isFetching = false
    private var launched = false
    private var cnt = 0
    private lateinit var repository: StreamerRepository
    private val douyinRepository = DouyinRepository()
    private lateinit var adbRepository: AdbRepository

    override fun onCreate() {
        super.onCreate()
        repository = StreamerRepository()
        adbRepository = AdbRepository()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FETCH -> {
//                CoroutineScope(Dispatchers.IO + Job()).launch {
//                    val item = repository.getAllItems().firstOrNull() ?: return@launch
//                    val plan = Plan.new(item) ?: return@launch
//                    UploadViewModel.startUpload(this@LiveService, plan)
//                }
                startFetching()
//                RecordService.start(this, "test")
            }
            ACTION_STOP_FETCH -> stopFetching()
            ACTION_EXIT -> {
                RecordService.exit(this)
                stopFetching()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()  // Ensure cleanup
    }

    private fun startFetching() {
        if (isFetching) return  // Prevent duplicate starts
        SharedPreferencesHelper.latestRecorded = Date()
        isFetching = true
        sendStatusBroadcast(isFetching)
        serviceScope = CoroutineScope(Dispatchers.IO + Job())  // Recreate scope
        serviceScope.launch {
            while (isActive) {
                if (isFetching) {
                    fetchApiData()
                    delay(20_000)  // Fetch 20 seconds
                } else {
                    delay(1_000)  // Wait while paused
                }
            }
        }
        updateNotification(true)
    }

    private fun stopFetching() {
        isFetching = false
        sendStatusBroadcast(isFetching)
        launched = false
        cnt = 0
        serviceScope.cancel()  // Fully stop coroutines
        updateNotification(false)
    }

    private suspend fun fetchApiData() {
        kotlin.runCatching {
            val item = repository.getItem(SharedPreferencesHelper.streamer) ?: return@runCatching
            val room = douyinRepository.getRoom(item.webRid)
            updateNotification(true, "${item.name}: ${room.statusString}(${cnt++})")

            if (room.status == 2) {
                if (!launched) {
                    launched = true
                    runStartAdbCommand(room)
                    RecordService.start(this, item.name)
                }
            } else {
                if (launched) {
                    launched = false
                    RecordService.stop(this)
                    runEndAdbCommand()
                    val plan = Plan.new(item) ?: return@runCatching // if no template name will not create plan
                    UploadViewModel.startUpload(this@LiveService, plan)
                }
            }
        }.onFailure { error ->

        }
    }

    private fun updateNotification(isRunning: Boolean, message: String = "") {
        val notification = createNotification(isRunning, message)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(isRunning: Boolean, message: String = ""): Notification {
        val status = if (isRunning) getString(R.string.running) else getString(R.string.pending)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.live_service))
            .setContentText(status + if (message.isNotEmpty()) ": $message" else "")
            .setSmallIcon(android.R.drawable.ic_menu_search)

//        if (isRunning) {
//            val stopIntent = Intent(this, LiveService::class.java).apply { action = ACTION_STOP_FETCH }
//            val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, flags)
//            builder.addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
//        } else {
//            val startIntent = Intent(this, LiveService::class.java).apply { action = ACTION_START_FETCH }
//            val startPendingIntent = PendingIntent.getService(this, 1, startIntent, flags)
//            builder.addAction(android.R.drawable.ic_media_play, "Start", startPendingIntent)
//        }
        val exitIntent = Intent(this, LiveService::class.java).apply { action = ACTION_EXIT }
        val exitPendingIntent = PendingIntent.getService(this, 3, exitIntent, flags)
        builder.addAction(android.R.drawable.ic_delete, getString(R.string.exit), exitPendingIntent)
        return builder
            .setOngoing(true)  // Keeps notification active
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Live Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun sendStatusBroadcast(isFetching: Boolean) {
        val intent = Intent(LIVE_ACTION_STATUS).apply {
            putExtra(EXTRA_IS_LIVE_FETCHING, isFetching)
        }
        sendBroadcast(intent)
    }

    private suspend fun runStartAdbCommand(room: LiveRoom) {
        adbRepository.connect("127.0.0.1", SharedPreferencesHelper.adbPort)
        if (SharedPreferencesHelper.advancedAdb) {
            SharedPreferencesHelper.startCommand.adbCommands().forEach {
                adbRepository.execute(it)
            }
            delay(2000L)
        }
        val url = "snssdk1128://live?room_id=${room.roomId}&user_id=${room.userId}&u_code=0&from=webview&refer=web"
        adbRepository.execute("am start -a android.intent.action.VIEW -d '$url'")
        delay(1000L)
        adbRepository.disconnect()
    }

    private suspend fun runEndAdbCommand() {
        adbRepository.connect("127.0.0.1", SharedPreferencesHelper.adbPort)
        adbRepository.execute("sleep 1; am force-stop com.ss.android.ugc.aweme")
        delay(1000L)
        if (SharedPreferencesHelper.advancedAdb) {
            SharedPreferencesHelper.endCommand.adbCommands().forEach {
                adbRepository.execute(it)
            }
            delay(2000L)
        }
        adbRepository.disconnect()
    }

    companion object {
        private const val CHANNEL_ID = "live_service_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_FETCH = "START_FETCH"
        const val ACTION_STOP_FETCH = "STOP_FETCH"
        const val ACTION_EXIT = "ACTION_EXIT"
        const val EXTRA_IS_LIVE_FETCHING = "EXTRA_IS_LIVE_FETCHING"
        const val LIVE_ACTION_STATUS = "LIVE_ACTION_STATUS"

        fun start(context: Context) {
            val intent = Intent(context, LiveService::class.java).apply {
                action = ACTION_START_FETCH
            }
            context.startForegroundService(intent)
        }

        fun exit(context: Context) {
            val intent = Intent(context, LiveService::class.java).apply {
                action = ACTION_EXIT
            }
            context.startForegroundService(intent)
        }
    }
}