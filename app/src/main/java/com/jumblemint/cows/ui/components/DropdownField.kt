package com.jumblemint.cows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

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
        valueBackgroundColor?.invoke(value)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { /* Visual only, onValueChange handled by DropdownMenu */ },
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            isError = isError,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (accentColor != null && value.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = accentColor,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        modifier = Modifier.clickable(enabled = enabled) { expanded = !expanded }
                    )
                }
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
            expanded = expanded && enabled, // expanded state is now controlled by the overlay click or icon click
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
            }

            options.forEach { option ->
                if (option.isNotEmpty()) {
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
                            Modifier.drawBehind {
                                drawRect(optionBgColor)
                            }
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}