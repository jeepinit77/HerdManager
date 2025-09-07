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
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = if (bgColor != null && value.isNotEmpty()) {
                TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = bgColor,
                    focusedTextColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = contrast?.copy(alpha = 0.9f) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = contrast?.copy(alpha = 0.7f) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = contrast ?: MaterialTheme.colorScheme.primary,
                    focusedBorderColor = contrast?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = contrast?.copy(alpha = 0.4f) ?: MaterialTheme.colorScheme.outline,
                    focusedTrailingIconColor = contrast ?: MaterialTheme.colorScheme.onSurface,
                    unfocusedTrailingIconColor = contrast ?: MaterialTheme.colorScheme.onSurface
                )
            } else {
                TextFieldDefaults.outlinedTextFieldColors()
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add empty option
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onValueChange("")
                    expanded = false
                }
            )

            options.forEach { option ->
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