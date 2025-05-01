package com.example.autorecorder.screen.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autorecorder.R
import com.example.autorecorder.entity.Plan
import com.example.autorecorder.entity.PlanStatus
import com.example.autorecorder.screen.components.LinkText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    list: List<Plan>,
    runningList: List<String>,
    onBackButtonClick: () -> Unit,
    onCancelClick: (String) -> Unit,
    onUploadClick: (Plan) -> Unit,
    onDeleteClick: (Plan) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.upload)) },
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

            LazyColumn {
                items(list) { item ->
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = item.title)
                            val progress = item.progress.toInt()
                            val infoList = listOf(
                                item.status.name,
                                if (progress >= 100) "" else "$progress%",
                                if (runningList.contains(item.id)) stringResource(R.string.uploading) else "",
                            )
                            Text(
                                text = infoList.filter { it.isNotBlank() }.joinToString(", "),
                                color = item.status.color,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = item.fileNames.joinToString("\n"),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                style = androidx.compose.ui.text.TextStyle.Default.copy(
                                    lineHeight = 15.sp,
                                ),
                            )
                            if (item.errorMessage.isNotBlank()) {
                                Text(
                                    text = item.errorMessage,
                                    fontSize = 11.sp,
                                    color = Color.Red,
                                )
                            }
                            if (item.bvid.isNotEmpty()) {
                                LinkText(
                                    url = "https://www.bilibili.com/video/${item.bvid}",
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onDeleteClick(item)
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                        if (item.status < PlanStatus.POSTED) {
                            if (runningList.contains(item.id)) {
                                IconButton(
                                    onClick = {
                                        onCancelClick(item.id)
                                    }
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        onUploadClick(item)
                                    }
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Resume")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}