package com.example.autorecorder.screen.components

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.autorecorder.services.LiveService
import com.example.autorecorder.services.LiveService.Companion.EXTRA_IS_LIVE_FETCHING
import com.example.autorecorder.services.LiveService.Companion.LIVE_ACTION_STATUS

@Composable
fun LiveStatusEffect(
    onConnected: (LiveService) -> Unit,
    onDisconnected: () -> Unit,
    onReceived: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val serviceConnectionRef = remember { mutableStateOf<ServiceConnection?>(null) }

    fun bind() {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val item = (service as LiveService.LocalBinder).getService()
                onConnected(item)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                onDisconnected()
                bind()
            }
        }
        serviceConnectionRef.value?.let {
            context.unbindService(it)
        }
        val intent = Intent(context, LiveService::class.java)
        context.bindService(intent, connection, 0)
        serviceConnectionRef.value = connection
    }

    DisposableEffect(Unit) {
        bind()
        onDispose {
            serviceConnectionRef.value?.let {
                context.unbindService(it)
            }
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LIVE_ACTION_STATUS) {
                    val fetching = intent.getBooleanExtra(EXTRA_IS_LIVE_FETCHING, false)
                    onReceived(fetching)
                }
            }
        }
        val filter = IntentFilter(LIVE_ACTION_STATUS)
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}