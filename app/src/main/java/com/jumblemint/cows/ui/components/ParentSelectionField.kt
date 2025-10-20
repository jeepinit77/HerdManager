package com.jumblemint.cows.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors

@Composable
fun ParentSelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    placeholder: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            readOnly = true,
            isError = isError,
            trailingIcon = { DropdownIcon() },
            colors = defaultOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        )
    }
}

@Composable
private fun DropdownIcon() {
    Icon(
        imageVector = Icons.Filled.ArrowDropDown,
        contentDescription = null
    )
}

fun formatParentDisplay(cow: Cow): String {
    val name = cow.name?.takeIf { it.isNotBlank() }
    val tag = cow.tagNumber?.takeIf { it.isNotBlank() }
    return when {
        name != null && tag != null -> "$name ($tag)"
        name != null -> name
        tag != null -> tag
        else -> ""
    }
}
