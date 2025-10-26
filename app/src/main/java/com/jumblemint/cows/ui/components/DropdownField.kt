package com.jumblemint.cows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.ui.theme.contrastingTextColor

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
    valueBackgroundColor: ((String) -> Color?)? = null,
    optionBackgroundColor: ((String) -> Color?)? = null,
    showNoneOption: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    val accentColor = remember(value, valueBackgroundColor) {
        if (value.isNotEmpty()) { // Only get accent color if there's a value
            valueBackgroundColor?.invoke(value)
        } else {
            null
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { /* Visual only, onValueChange handled by DropdownMenu */ },
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            isError = isError,
            colors = if (enabled && !isError && accentColor != null && value.isNotEmpty()) {
                val contrastingColor = if (accentColor.luminance() < 0.5f) Color.White else Color.Black
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = accentColor,
                    unfocusedContainerColor = accentColor,
                    focusedTextColor = contrastingColor,
                    unfocusedTextColor = contrastingColor,
                    focusedTrailingIconColor = contrastingColor,
                    unfocusedTrailingIconColor = contrastingColor,
                    cursorColor = contrastingColor
                    // Default label and border colors will be used, which is appropriate
                    // as the label is outside the colored container when a value is present.
                )
            } else {
                defaultOutlinedTextFieldColors()
            },
            trailingIcon = {
                // Swatch Box and Spacer removed
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable(enabled = enabled) { expanded = !expanded }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Clickable overlay to expand the dropdown
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = { expanded = true })
            )
        }

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (value.isNotEmpty() && showNoneOption) {
                DropdownMenuItem(
                    text = { Text("Clear Selection") },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    }
                )
                if (options.any { it.isNotEmpty() }) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }

            val nonEmptyOptions = options.filter { it.isNotEmpty() }
            nonEmptyOptions.forEachIndexed { index, option ->
                val optionBgColor = optionBackgroundColor?.invoke(option)
                val optionContrast = optionBgColor?.let { if (it.luminance() < 0.5f) Color.White else Color.Black }

                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = optionContrast ?: LocalContentColor.current
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    colors = if (optionBgColor != null) {
                        MenuDefaults.itemColors(
                            textColor = optionContrast ?: LocalContentColor.current
                        )
                    } else {
                        MenuDefaults.itemColors()
                    },
                    modifier = if (optionBgColor != null) {
                        Modifier.background(optionBgColor)
                    } else {
                        Modifier
                    }
                )

                if (index < nonEmptyOptions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}