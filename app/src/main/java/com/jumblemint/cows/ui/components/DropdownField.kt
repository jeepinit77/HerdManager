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
    isError: Boolean = false, // <<< ADDED isError PARAMETER
    enabled: Boolean = true, // <<< ADDED enabled PARAMETER for consistency
    // Optional: provide a color for the current value to tint the field background
    valueBackgroundColor: ((String) -> Color?)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    val bgColor = remember(value, valueBackgroundColor) {
        valueBackgroundColor?.invoke(value)
    }
    val contrast = bgColor?.let { if (it.luminance() < 0.5f) Color.White else Color.Black }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { /* Handled by DropdownMenu item clicks */ },
            label = { Text(label) },
            readOnly = true,
            enabled = enabled, // <<< APPLY enabled STATE
            isError = isError, // <<< APPLY isError STATE
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable(enabled = enabled) { expanded = !expanded } // Only clickable if enabled
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }, // Only clickable if enabled
            colors = if (bgColor != null && value.isNotEmpty()) {
                TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = bgColor,
                    focusedTextColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = (contrast ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.38f),
                    focusedLabelColor = contrast?.copy(alpha = 0.9f) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = contrast?.copy(alpha = 0.7f) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = (contrast ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.38f),
                    cursorColor = contrast ?: MaterialTheme.colorScheme.primary,
                    focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else contrast?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else contrast?.copy(alpha = 0.4f) ?: MaterialTheme.colorScheme.outline,
                    disabledBorderColor = (contrast ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.38f),
                    focusedTrailingIconColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    unfocusedTrailingIconColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    disabledTrailingIconColor = (contrast ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.38f)
                )
            } else {
                TextFieldDefaults.outlinedTextFieldColors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        )

        DropdownMenu(
            expanded = expanded && enabled, // Menu should not expand if field is disabled
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Option to clear selection if value is not empty
            if (value.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("None") }, // Or "Clear Selection"
                    onClick = {
                        onValueChange("") // Send empty string to clear
                        expanded = false
                    }
                )
            }

            options.forEach { option ->
                if (option.isNotEmpty()) { // Avoid adding an empty option if "None" is already handled
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