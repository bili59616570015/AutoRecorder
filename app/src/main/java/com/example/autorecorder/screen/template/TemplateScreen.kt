package com.example.autorecorder.screen.template

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.autorecorder.R
import com.example.autorecorder.entity.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(
    uiState: TemplateUiState,
    onBackClick: () -> Unit,
    onItemClick: (Template, Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.template)) },
                modifier = Modifier.shadow(4.dp),
                actions = {
                    IconButton(
                        onClick = {
                            onItemClick(Template(), true)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
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
        Box(
            modifier = Modifier
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
            ) {
                items(uiState.items) { item ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    onItemClick(item, false)
                                }
                            }
                            .height(48.dp)
                    ) {
                        Text(
                            text = item.displayTitle(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}