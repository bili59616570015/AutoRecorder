package com.example.autorecorder.screen.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.common.UpCdn
import com.example.autorecorder.screen.components.DropdownSelector
import com.example.autorecorder.screen.components.LiveStatusEffect
import com.example.autorecorder.screen.components.RecordStatusEffect
import com.example.autorecorder.screen.components.SwitchButton
import com.example.autorecorder.services.LiveService
import com.example.autorecorder.services.LiveService.Companion.ACTION_EXIT
import com.example.autorecorder.services.LiveService.Companion.ACTION_START_FETCH
import com.example.autorecorder.services.RecordService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState?,
    onMenuClick: () -> Unit,
    onConnectClick: () -> Unit,
    onAddStreamerClick: () -> Unit,
    onAdbClick: () -> Unit,
    onQualityClick: () -> Unit,
    onPingClick: (onResult: (String) -> Unit) -> Unit,
) {
    val context: Context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            RecordService.setup(context, result.resultCode, result.data)
        }
    }

    var recordConnected by remember { mutableStateOf(false) }
    var liveConnected by remember { mutableStateOf(false) }
    var fetchingLive by remember { mutableStateOf(false) }
    var streamer by remember { mutableStateOf(SharedPreferencesHelper.streamer) }
    var upCdn by remember { mutableStateOf(SharedPreferencesHelper.upCdn) }

    RecordStatusEffect(
        onConnected = { _ ->
            recordConnected = true
        },
        onDisconnected = {
            recordConnected = false
        },
    )

    LiveStatusEffect(
        onConnected = { service ->
            fetchingLive = service.isFetching
            liveConnected = true
        },
        onDisconnected = {
            fetchingLive = false
            liveConnected = false
        },
        onReceived = { isLiveFetching ->
            fetchingLive = isLiveFetching
        },
    )

    Scaffold(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
            .fillMaxSize()
        ,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onMenuClick()
                        }
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                    }
                }
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
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),

            ) {
                uiState ?: return@Column
                Column {
                    Row {
                        SwitchButton(
                            label = "ADB: ${if (uiState.adbConnected) stringResource(R.string.connected) else stringResource(R.string.disconnected)}",
                            checked = uiState.adbConnected,
                            onCheckChange = { onConnectClick() }
                        )

                        IconButton(
                            onClick = {
                                onAdbClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Adb"
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.adb_description),
                        color = Color.Gray
                    )
                }

                Column {
                    Row {
                        SwitchButton(
                            label = "${stringResource(R.string.record)}: ${if (recordConnected) stringResource(R.string.connected) else stringResource(R.string.disconnected)}",
                            checked = recordConnected,
                            onCheckChange = {
                                if (it) {
                                    val projectionManager =
                                        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                                    launcher.launch(projectionManager.createScreenCaptureIntent())
                                } else {
                                    RecordService.exit(context)
                                }
                            }
                        )
                        IconButton(
                            onClick = {
                                onQualityClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Quality"
                            )
                        }
                    }
//                    Text(
//                        text = stringResource(R.string.record_description),
//                        color = Color.Gray
//                    )
                }

                Column {
                    DropdownSelector(
                        label = stringResource(R.string.streamer),
                        options = uiState.streamerList.map { it.name },
                        modifier = Modifier.fillMaxWidth(),
                        selectedOption = streamer,
                        onSelect = { name ->
                            SharedPreferencesHelper.streamer = name ?: ""
                            streamer = name ?: ""
                        },
                        onAddClick = onAddStreamerClick
                    )
                }

//                Row(
//                    verticalAlignment = Alignment.Bottom,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                ) {
//                    Column(modifier = Modifier.weight(1f)) {
//                        DropdownSelector(
//                            label = stringResource(R.string.cdn),
//                            options = UpCdn.entries.map { it.title },
//                            modifier = Modifier.fillMaxWidth(),
//                            selectedOption = UpCdn.entries.firstOrNull { it.name.lowercase() == upCdn }?.title,
//                            onSelect = { title ->
//                                SharedPreferencesHelper.upCdn = UpCdn.entries.firstOrNull { it.title == title }?.name?.lowercase() ?: UpCdn.BDA2.name.lowercase()
//                                upCdn = SharedPreferencesHelper.upCdn
//                            },
//                        )
//                    }
//                    OutlinedButton(
//                        modifier = Modifier.width(110.dp).height(54.dp),
//                        onClick = {
//                            onPingClick {
//                                upCdn = it
//                            }
//                        },
//                        enabled = !uiState.isLoading,
//                    ) {
//                        Text(if (uiState.isLoading) stringResource(R.string.loading) else stringResource(R.string.auto))
//                    }
//                }

                Column {
                    Button(
                        onClick = {
                            if (!fetchingLive) {
                                if (!uiState.adbConnected) {
                                    Toast.makeText(
                                        context,
                                        context.getText(R.string.require_adb),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                if (!recordConnected) {
                                    Toast.makeText(
                                        context,
                                        context.getText(R.string.require_record),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                if (streamer.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getText(R.string.require_streamer),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                            }
                            if (fetchingLive) {
                                LiveService.exit(context)
                            } else {
                                LiveService.start(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (fetchingLive) stringResource(R.string.stop_fetch) else stringResource(
                                R.string.start_fetch
                            )
                        )
                    }
                    Text(
                        text = stringResource(R.string.start_info),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}