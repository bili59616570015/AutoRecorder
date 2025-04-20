package com.example.autorecorder.screen.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.autorecorder.INVALID_ID
import com.example.autorecorder.R
import com.example.autorecorder.common.Utils
import com.example.autorecorder.entity.MainTid
import com.example.autorecorder.entity.SubTid
import com.example.autorecorder.entity.Template
import com.example.autorecorder.screen.components.DropdownSelector
import com.example.autorecorder.screen.components.RequiredText
import com.example.autorecorder.screen.components.SwitchButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    isNew: Boolean,
    template: Template,
    onDoneButtonClick: (Template) -> Unit,
    onBackButtonClick: () -> Unit,
    onAddUpButtonClick: () -> Unit,
    onDeleteClick: (Template) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var item: Template by rememberSaveable {
        mutableStateOf(template)
    }
    var showDeleteDialog: Boolean by remember { mutableStateOf(false) }
    var mainTid: MainTid? by remember { mutableStateOf(item.mainTid) }
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
                title = { Text(stringResource(R.string.edit) + stringResource(R.string.template)) },
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.title,
                    onValueChange = { item = item.copy(title = it) },
                    label = { RequiredText(string = stringResource(R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (item.title.isNotBlank() && isNew) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "clear text",
                                modifier = Modifier
                                    .clickable {
                                        item = item.copy(title = "")
                                    }
                            )
                        }
                    },
                    enabled = isNew
                )

                DropdownSelector(
                    label = stringResource(R.string.up),
                    isRequired = true,
                    options = Utils.getCookieJsonNames(),
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = if (item.mid == INVALID_ID) null else item.mid.toString(),
                    onSelect = {
                        item = item.copy(mid = it?.toLongOrNull() ?: INVALID_ID)
                    },
                    onAddClick = onAddUpButtonClick
                )

                OutlinedTextField(
                    value = item.desc,
                    onValueChange = { item = item.copy(desc = it) },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (item.desc.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "clear text",
                                modifier = Modifier
                                    .clickable {
                                        item = item.copy(desc = "")
                                    }
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = item.tag,
                    onValueChange = {
                        item = item.copy(tag = it)
                    },
                    label = { RequiredText(string = stringResource(R.string.tag)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.tag_placeholder), color = Color.LightGray) },
                )
                Text(
                    text = stringResource(R.string.tag_info),
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DropdownSelector(
                        label = stringResource(R.string.main_tid),
                        options = MainTid.entries.map { it.title },
                        modifier = Modifier.weight(1f),
                        selectedOption = item.mainTid?.title,
                        onSelect = { title ->
                            if (title == null) return@DropdownSelector
                            mainTid = MainTid.entries.find { it.title == title }
                            if (item.subTid?.parent != mainTid) {
                                val subTid = SubTid.entries.firstOrNull { it.parent == mainTid } ?: return@DropdownSelector
                                item = item.copy(tid = subTid.tid)
                            }
                        },
                        onAddClick = null
                    )
                    if (mainTid != null) {
                        DropdownSelector(
                            label = stringResource(R.string.sub_tid),
                            options = SubTid.entries.filter { it.parent == mainTid }.map { it.title },
                            modifier = Modifier.weight(1f),
                            selectedOption = item.subTid?.title,
                            onSelect = { title ->
                                if (title == null) return@DropdownSelector
                                val subTid = SubTid.entries.find { it.parent == mainTid && it.title == title } ?: return@DropdownSelector
                                item = item.copy(tid = subTid.tid)
                            },
                            onAddClick = null
                        )
                    }
                }

                Column {
                    SwitchButton(
                        label = item.copyrightString,
                        checked = item.copyright == 1,
                        onCheckChange = { item = item.copy(copyright = if (it) 1 else 2) }
                    )
                    if (item.copyright == 2) {
                        OutlinedTextField(
                            value = item.source,
                            onValueChange = { item = item.copy(source = it) },
                            label = { Text(stringResource(R.string.source_url)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (item.source.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "clear text",
                                        modifier = Modifier
                                            .clickable {
                                                item = item.copy(source = "")
                                            }
                                    )
                                }
                            }
                        )
                        Text(
                            text = stringResource(R.string.copyright_info),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                if (item.copyright == 1) {
                    SwitchButton(
                        label = item.watermarkString,
                        checked = item.watermark == 1,
                        onCheckChange = { item = item.copy(watermark = if (it) 1 else 0) }
                    )
                }
                SwitchButton(
                    label = item.isOnlySelfString,
                    checked = item.isOnlySelf == 0,
                    onCheckChange = { item = item.copy(isOnlySelf = if (it) 0 else 1) }
                )
                SwitchButton(
                    label = item.noReprintString,
                    checked = item.noReprint == 0,
                    onCheckChange = { item = item.copy(noReprint = if (it) 0 else 1) }
                )
//                SwitchButton(
//                    label = item.recreateString,
//                    checked = item.recreate == 1,
//                    onCheckChange = { item = item.copy(recreate = if (it) 1 else -1) }
//                )
                Spacer(modifier = Modifier.height(200.dp))
            }
            Button(
                onClick = {
                    if (item.mid != INVALID_ID && item.tag.isNotBlank()) {
                        onDoneButtonClick(item)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_message, item.displayTitle())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(item)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
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