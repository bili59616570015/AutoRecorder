package com.example.autorecorder.screen.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.example.autorecorder.R

@Composable
fun DropdownSelector(
    label: String,
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedOption: String?,
    isRequired: Boolean = false,
    onSelect: (String?) -> Unit,
    onAddClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val defaultOption = "-"
    val addOption = stringResource(R.string.add)


    BoxWithConstraints(modifier = modifier) {
        OutlinedTextField(
            value = if (options.isEmpty()) {
                "$addOption $label"
            } else {
                selectedOption ?: defaultOption
            },
            onValueChange = {},
            label = { if (isRequired) RequiredText(string = label) else Text(label) },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = Color.Transparent,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures {
                        expanded = true
                    }
                }
            ,
        )

        DropdownMenu(
            modifier = Modifier.width(with(LocalDensity.current) { maxWidth }),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (listOf(defaultOption) + options + if (onAddClick == null) emptyList() else  listOf(addOption)).forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = { Text(option) },
                    onClick = {
                        when (option) {
                            addOption -> onAddClick?.invoke()
                            defaultOption -> onSelect(null)
                            else -> onSelect(option)
                        }
                        expanded = false
                    },
                )
            }
        }
    }
}