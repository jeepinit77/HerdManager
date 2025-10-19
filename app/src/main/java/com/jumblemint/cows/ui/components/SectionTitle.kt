package com.jumblemint.cows.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import com.jumblemint.cows.ui.theme.contrastingTextColor
import com.jumblemint.cows.ui.theme.getCardBackgroundColor

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = getCardBackgroundColor().contrastingTextColor()
    )
}


