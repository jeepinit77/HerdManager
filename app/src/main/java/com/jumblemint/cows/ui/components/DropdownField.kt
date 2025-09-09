package com.jumblemint.cows.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    valueBackgroundColor: ((String) -> Color?)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val bgColor = remember(value, valueBackgroundColor) {
        valueBackgroundColor?.invoke(value)
    }
    val contrast = bgColor?.let { if (it.luminance() < 0.5f) Color.White else Color.Black }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        // Text colors
        focusedTextColor = if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
        disabledTextColor = (if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.38f),

        // Container colors
        focusedContainerColor = bgColor ?: Color.Transparent,
        unfocusedContainerColor = bgColor ?: Color.Transparent,
        disabledContainerColor = if (bgColor != null) bgColor.copy(alpha = 0.12f) else Color.Transparent,

        // Cursor color
        cursorColor = if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
        errorCursorColor = MaterialTheme.colorScheme.error,

        // Label colors
        focusedLabelColor = if (bgColor != null && value.isNotEmpty()) contrast?.copy(alpha = 0.9f) ?: MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = if (bgColor != null && value.isNotEmpty()) contrast?.copy(alpha = 0.7f) ?: MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = (if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.38f),

        // Border colors
        focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else (if (bgColor != null && value.isNotEmpty()) contrast?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary),
        unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else (if (bgColor != null && value.isNotEmpty()) contrast?.copy(alpha = 0.4f) ?: MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline),
        disabledBorderColor = (if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline).copy(alpha = 0.38f),
        errorBorderColor = MaterialTheme.colorScheme.error,

        // Trailing icon colors
        focusedTrailingIconColor = if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
        unfocusedTrailingIconColor = if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
        disabledTrailingIconColor = (if (bgColor != null && value.isNotEmpty()) contrast ?: MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.38f),
        errorTrailingIconColor = MaterialTheme.colorScheme.error
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { /* Handled by DropdownMenu item clicks */ },
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            isError = isError,
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable(enabled = enabled) { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
            colors = textFieldColors
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (value.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    }
                )
            }

            options.forEach { option ->
                if (option.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}