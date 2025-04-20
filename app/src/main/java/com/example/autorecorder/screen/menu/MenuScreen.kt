package com.example.autorecorder.screen.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autorecorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBackClick: () -> Unit,
    onStreamerClick: () -> Unit,
    onTemplateClick: () -> Unit,
    onUpClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRecorderClick: () -> Unit,
    onMenuMoreClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.menu))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onMenuMoreClick() },
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(OtherList.entries) { item ->
                    Row(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    when (item) {
                                        OtherList.Streamer -> { onStreamerClick() }
                                        OtherList.Template -> { onTemplateClick() }
                                        OtherList.Up -> { onUpClick() }
                                        OtherList.Upload -> { onUploadClick() }
                                        OtherList.Recorder -> { onRecorderClick() }
                                    }
                                }
                            }
                            .fillMaxWidth()
                            .padding(24.dp)
                        ,
                    ) {
                        Text(stringResource(item.res))
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Button")
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

enum class OtherList(val res: Int) {
    Streamer(R.string.streamer),
    Template(R.string.template),
    Up(R.string.up),
    Upload(R.string.upload),
    Recorder(R.string.record),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuMoreScreen(
    onBackClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onTermsClick: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.menu))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(OtherMoreList.entries) { item ->
                    Row(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    when (item) {
                                        OtherMoreList.License -> {
                                            onLicenseClick()
                                        }
                                        OtherMoreList.Terms -> {
                                            onTermsClick()
                                        }
                                        OtherMoreList.Report -> {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                                context.getString(R.string.report_url)
                                            ))
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            }
                            .fillMaxWidth()
                            .padding(24.dp)
                        ,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(item.res))
                            if (item == OtherMoreList.Report) {
                                Text(
                                    text = stringResource(R.string.report_description),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Button")
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
            Text(
                text = context.packageManager.getPackageInfo(context.packageName, 0).versionName,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

enum class OtherMoreList(val res: Int) {
    License(R.string.license),
    Terms(R.string.terms),
    Report(R.string.report)
}