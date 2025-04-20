package com.example.autorecorder.screen.recorder

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autorecorder.R
import com.example.autorecorder.common.Utils
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBackButtonClick: () -> Unit,
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(Utils.getBackupFiles()) }
    var deleteItem: File? by remember { mutableStateOf(null) }

    LaunchedEffect(deleteItem) {
        files = Utils.getBackupFiles()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.backup)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                items(files) { file ->
                    Row {
                        Column(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        Utils.openMoviesFolder(context, file)
                                    }
                                }
                                .weight(1f),
                        ) {
                            Text(
                                text = file.name,
                            )
                            Text(
                                text = listOf(Utils.formatFileSize(file.length()), Utils.getVideoDurationFormatted(file)).filter { it.isNotBlank() }.joinToString(", "),
                                color = Color.Gray,
                                fontSize = 12.sp,
                            )
                        }
                        IconButton(
                            onClick = {
                                deleteItem = file
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                            )
                        }
                    }
                }
            }
        }
    }

    deleteItem?.let {
        AlertDialog(
            onDismissRequest = { deleteItem = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_message, it.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        Utils.deleteFile(it)
                        deleteItem = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteItem = null }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}