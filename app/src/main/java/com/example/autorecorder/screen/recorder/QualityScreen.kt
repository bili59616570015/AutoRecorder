package com.example.autorecorder.screen.recorder

import android.content.Intent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autorecorder.R
import com.example.autorecorder.common.Quality
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.common.UpCdn
import com.example.autorecorder.screen.components.DropdownSelector
import com.example.autorecorder.screen.components.LiveStatusEffect
import com.example.autorecorder.screen.components.RecordStatusEffect
import com.example.autorecorder.screen.components.SwitchButton
import com.example.autorecorder.services.LiveService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityScreen(
    onBackButtonClick: () -> Unit,
) {
    var quality by remember { mutableStateOf(SharedPreferencesHelper.quality) }
    var bitrate by remember { mutableStateOf(SharedPreferencesHelper.bitrate) }
    var frameRate by remember { mutableStateOf(SharedPreferencesHelper.frameRate) }
    var needBackupVideo: Boolean by remember { mutableStateOf(SharedPreferencesHelper.needBackupVideo) }
//    var splitFile: Boolean by remember { mutableStateOf(SharedPreferencesHelper.splitFile) } // 录到3.8GB以上崩溃了
    var showAlert: Boolean by remember { mutableStateOf(false) }
    var recordServiceConnected: Boolean by remember { mutableStateOf(false) }
    var liveServiceConnected: Boolean by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LiveStatusEffect(
        onConnected = { service ->
            liveServiceConnected = true
        },
        onDisconnected = {
            liveServiceConnected = false
        },
    )
    RecordStatusEffect(
        onConnected = { service ->
            recordServiceConnected = true
        },
        onDisconnected = {
            recordServiceConnected = false
        },
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.record_settings)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding),
        ) {

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                DropdownSelector(
                    label = stringResource(R.string.quality),
                    options = Quality.entries.map { "${it.width}" },
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = quality.width.toString(),
                    onSelect = { width ->
                        quality = Quality.from(width?.toInt() ?: 1080)
                    },
                )

                DropdownSelector(
                    label = stringResource(R.string.bitrate),
                    options = SharedPreferencesHelper.bitrateList.map { "${it}Mbps" },
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = bitrate.toString(),
                    onSelect = { item ->
                        bitrate = item?.substringBefore("Mbps")?.toInt() ?: 4
                    },
                )

                DropdownSelector(
                    label = stringResource(R.string.frame_rate),
                    options = SharedPreferencesHelper.frameRateList.map { "${it}FPS" },
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = frameRate.toString(),
                    onSelect = { item ->
                        frameRate = item?.substringBefore("FPS")?.toInt() ?: 30
                    },
                )

                Column {
                    SwitchButton(
                        label = "${stringResource(R.string.backup)}: ${if (needBackupVideo) stringResource(R.string.on) else stringResource(R.string.off)}",
                        checked = needBackupVideo,
                        onCheckChange = { needBackupVideo = it },
                    )
                    Text(
                        text = stringResource(R.string.backup_info),
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }

//                Column {
//                    SwitchButton(
//                        label = "${stringResource(R.string.split_file)}: ${if (splitFile) stringResource(R.string.on) else stringResource(R.string.off)}",
//                        checked = splitFile,
//                        onCheckChange = { splitFile = it },
//                    )
//                    Text(
//                        text = stringResource(if (splitFile) R.string.split_file_info else R.string.split_file_off_info),
//                        fontSize = 12.sp,
//                        color = Color.Gray,
//                    )
//                }

                Text(
                    text = stringResource(R.string.volume_info),
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                Button(
                    onClick = {
                        if (recordServiceConnected || liveServiceConnected) {
                            showAlert = true
                        } else {
                            SharedPreferencesHelper.quality = quality
                            SharedPreferencesHelper.bitrate = bitrate
                            SharedPreferencesHelper.frameRate = frameRate
                            SharedPreferencesHelper.needBackupVideo = needBackupVideo
//                            SharedPreferencesHelper.splitFile = splitFile
                            showAlert = false
                            onBackButtonClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save))
                }
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text(stringResource(R.string.save)) },
            text = { Text(stringResource(R.string.record_message)) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        SharedPreferencesHelper.quality = quality
                        SharedPreferencesHelper.bitrate = bitrate
                        SharedPreferencesHelper.frameRate = frameRate
                        SharedPreferencesHelper.needBackupVideo = needBackupVideo
//                        SharedPreferencesHelper.splitFile = splitFile
                        showAlert = false
                        if (liveServiceConnected) {
                            LiveService.exit(context)
                        } else if (recordServiceConnected) {
                            LiveService.exit(context)
                        }
                        onBackButtonClick()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAlert = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}