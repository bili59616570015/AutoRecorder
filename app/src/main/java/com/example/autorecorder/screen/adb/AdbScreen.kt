package com.example.autorecorder.screen.adb

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.autorecorder.R
import com.example.autorecorder.common.SharedPreferencesHelper
import com.example.autorecorder.screen.components.SwitchButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbScreen(
    onBackButtonClick: () -> Unit,
    onTestCommandClick: (String) -> Unit,
    onPairClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    var startCommand by remember { mutableStateOf(SharedPreferencesHelper.startCommand) }
    var endCommand by remember { mutableStateOf(SharedPreferencesHelper.endCommand) }
    var adbPort: Int? by remember { mutableStateOf(SharedPreferencesHelper.adbPort) }
    var advanced: Boolean by remember { mutableStateOf(SharedPreferencesHelper.advancedAdb) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.adb)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
                .padding(innerPadding),
        ) {

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row {
                    OutlinedButton(
                        onClick = {
                            onPairClick()
                        }
                    ) {
                        Text(stringResource(R.string.pair))
                    }
                    IconButton(
                        onClick = {
                            onHelpClick()
                        }
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = "Help")
                    }
                }
                Text(
                    stringResource(R.string.adb_description_2),
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = adbPort?.toString() ?: "",
                    onValueChange = { item ->
                        adbPort = item.toIntOrNull()
                        SharedPreferencesHelper.adbPort = adbPort ?: 5555
                    },
                    label = { Text(stringResource(R.string.port)) },
                    modifier = Modifier.fillMaxWidth()
                )
                SwitchButton(
                    label = stringResource(R.string.adb_advanced),
                    checked = advanced,
                    onCheckChange = {
                        advanced = it
                        SharedPreferencesHelper.advancedAdb = it
                    }
                )
                if (advanced) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.start_command),
                        )
                        Text(
                            stringResource(R.string.start_command_info),
                            color = Color.Gray,
                        )
                    }
                    OutlinedTextField(
                        value = startCommand,
                        onValueChange = {
                            startCommand = it
                            SharedPreferencesHelper.startCommand = it
                        },
                        label = { Text(stringResource(R.string.start_command)) },
                        placeholder = { Text(stringResource(R.string.command_placeholder), color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.padding(top = 8.dp),
                            onClick = {
                                onTestCommandClick(startCommand)
                            },
                        ) {
                            Text(stringResource(R.string.test_command))
                        }
                        OutlinedButton(
                            modifier = Modifier.padding(top = 8.dp),
                            onClick = {
                                val command = context.getString(R.string.unlock_screen_command)
                                if (!startCommand.contains(command)) {
                                    startCommand = (listOf(command) + startCommand.split("\n")).filter { it.isNotEmpty() }.joinToString("\n")
                                    SharedPreferencesHelper.startCommand = startCommand
                                }
                            },
                        ) {
                            Text(stringResource(R.string.unlock_screen_command_label))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.end_command),
                        )
                        Text(
                            stringResource(R.string.end_command_info),
                            color = Color.Gray,
                        )
                    }
                    OutlinedTextField(
                        value = endCommand,
                        onValueChange = {
                            endCommand = it
                            SharedPreferencesHelper.endCommand = it
                        },
                        label = { Text(stringResource(R.string.end_command)) },
                        placeholder = { Text(stringResource(R.string.command_placeholder), color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            onTestCommandClick(endCommand)
                        },
                    ) {
                        Text(stringResource(R.string.test_command))
                    }
                }
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbHelpScreen(
    onBackButtonClick: () -> Unit
) {
    val images = listOf(
        R.drawable.adb_1,
        R.drawable.adb_2,
        R.drawable.adb_3,
        R.drawable.adb_4,
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.adb)) },
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
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val pageCount = images.size
                val pagerState = rememberPagerState(
                    pageCount = { pageCount },
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                        ,
                        contentAlignment = Alignment.Center
                    ) {
                        images.getOrNull(it)?.let { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .background(color, CircleShape)
                                .size(10.dp)
                        )
                    }
                }
            }
        }
    }
}