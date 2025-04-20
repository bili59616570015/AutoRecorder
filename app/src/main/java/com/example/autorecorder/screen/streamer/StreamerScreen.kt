package com.example.autorecorder.screen.streamer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autorecorder.R
import com.example.autorecorder.api.bili.DataState
import com.example.autorecorder.entity.Streamer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerScreen(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onBackClick: () -> Unit,
    onErrorDialogDismiss: () -> Unit,
    onItemClick: (Streamer, Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.streamer)) },
                modifier = Modifier.shadow(4.dp),
                actions = {
                    IconButton(
                        onClick = {
                            onItemClick(Streamer.new(), true)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.items) { item ->
                    Row(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    onItemClick(item, false)
                                }
                            }
                    ) {
                        Column(
                            modifier = modifier
                                .weight(1f),
                        ) {
                            Text(text = item.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = item.url,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.dataState is DataState.Error) {
        AlertDialog(
            onDismissRequest = onErrorDialogDismiss,
            title = { Text(stringResource(R.string.error)) },
            text = { Text((uiState.dataState as DataState.Error).message) },
            confirmButton = {
                TextButton(
                    onClick = onErrorDialogDismiss
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}