package com.jumblemint.cows.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository

@Composable
fun getCardColors(): CardColors {
    return CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun getCardBackgroundColor(): Color {
    return MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun getGenderColor(gender: Gender): Color {
    val themeSettings = LocalThemeSettings.current
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val seedHct = themeSettings.seedColor.color.toHct()
    
    return when (gender) {
        Gender.MALE -> seedHct.copy(hue = 220f, tone = if (isDarkTheme) 70f else 40f).toColor()
        Gender.FEMALE -> seedHct.copy(hue = 340f, tone = if (isDarkTheme) 70f else 40f).toColor()
        Gender.TBD -> seedHct.copy(chroma = 20f, tone = if (isDarkTheme) 60f else 50f).toColor()
    }
}