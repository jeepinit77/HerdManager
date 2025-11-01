package com.jumblemint.cows.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun defaultOutlinedTextFieldColors(): TextFieldColors {
    val baseBackground = LocalBackgroundColor.current.takeIf { it != Color.Transparent }
        ?: MaterialTheme.colorScheme.background
    val contrast = baseBackground.contrastingTextColor()
    return OutlinedTextFieldDefaults.colors(
        unfocusedLabelColor = contrast.copy(alpha = 0.6f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = contrast.copy(alpha = 0.6f),
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = contrast.copy(alpha = 0.3f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = contrast,
        unfocusedTextColor = contrast,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedTrailingIconColor = contrast.copy(alpha = 0.6f),
        focusedPlaceholderColor = contrast.copy(alpha = 0.6f),
        unfocusedPlaceholderColor = contrast.copy(alpha = 0.6f),
        focusedContainerColor = baseBackground,
        unfocusedContainerColor = baseBackground,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorCursorColor = MaterialTheme.colorScheme.error
    )
}

@Composable
fun defaultDisabledOutlinedTextFieldColors(): TextFieldColors {
    val baseBackground = LocalBackgroundColor.current.takeIf { it != Color.Transparent }
        ?: MaterialTheme.colorScheme.background
    val contrast = baseBackground.contrastingTextColor()
    return OutlinedTextFieldDefaults.colors(
        disabledTextColor = contrast,
        disabledBorderColor = contrast.copy(alpha = 0.3f),
        disabledLabelColor = contrast.copy(alpha = 0.6f),
        disabledTrailingIconColor = contrast.copy(alpha = 0.6f),
        disabledContainerColor = baseBackground,
        disabledPlaceholderColor = contrast.copy(alpha = 0.6f)
    )
}


