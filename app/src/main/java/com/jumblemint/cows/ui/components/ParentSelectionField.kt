package com.jumblemint.cows.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.matchParentSize
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
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.util.primaryIdentifier
import com.jumblemint.cows.util.secondaryIdentifier
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

fun formatParentDisplay(cow: Cow, mode: AnimalIdentifierMode = AnimalIdentifierMode.BOTH): String {
    val primary = mode.primaryIdentifier(cow.name, cow.tagNumber, fallback = "")
    val secondary = mode.secondaryIdentifier(cow.name, cow.tagNumber)
    return when {
        primary.isNotBlank() && secondary != null -> "$primary ($secondary)"
        primary.isNotBlank() -> primary
        secondary != null -> secondary
        else -> ""
    }
}
