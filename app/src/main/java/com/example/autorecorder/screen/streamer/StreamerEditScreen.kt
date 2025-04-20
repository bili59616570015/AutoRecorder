package com.example.autorecorder.screen.streamer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autorecorder.R
import com.example.autorecorder.entity.Streamer
import com.example.autorecorder.entity.Template
import com.example.autorecorder.screen.components.DropdownSelector
import com.example.autorecorder.screen.components.RequiredText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerEditScreen(
    isNew: Boolean,
    streamer: Streamer,
    templates: List<Template>,
    onDoneButtonClick: (Streamer) -> Unit,
    onBackButtonClick: () -> Unit,
    onAddTemplateClick: () -> Unit,
    onDeleteButtonClick: (Streamer) -> Unit
) {
    var showDeleteDialog: Boolean by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var item: Streamer by rememberSaveable {
        mutableStateOf(streamer)
    }
    Scaffold(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
            .fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit) + stringResource(R.string.streamer)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(
                        onClick = onBackButtonClick
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(
                            onClick = {
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = item.name,
                onValueChange = { item = item.copy(name = it) },
                label = { RequiredText(string = stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (item.name.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "clear text",
                            modifier = Modifier
                                .clickable {
                                    item = item.copy(name = "")
                                }
                        )
                    }
                }
            )
            OutlinedTextField(
                value = item.url,
                onValueChange = { item = item.copy(url = it) },
                label = { RequiredText(string = "URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.streamer_url_placeholder), color = Color.LightGray) },
                trailingIcon = {
                    if (item.url.isNotBlank() && isNew) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "clear text",
                            modifier = Modifier
                                .clickable {
                                    item = item.copy(url = "")
                                }
                        )
                    }
                },
                enabled = isNew
            )

            Column {
                DropdownSelector(
                    label = stringResource(R.string.template),
                    options = templates.map { it.title },
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = item.templateString,
                    onSelect = { templateString ->
                        item = item.copy(templateString = templateString)
                    },
                    onAddClick = onAddTemplateClick
                )
                Text(
                    text = stringResource(R.string.template_info),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = {
                    if (item.name.isBlank() || item.name.contains("_")) {
                        return@Button
                    }
                    if (!item.url.isValidDouyinUrl()) {
                        return@Button
                    }
                    onDoneButtonClick(item)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_message, item.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteButtonClick(item)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun String.isValidDouyinUrl(): Boolean {
    val regex = "^https://live\\.douyin\\.com/\\d+$"
    return matches(regex.toRegex())
}