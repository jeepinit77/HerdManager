package com.jumblemint.cows.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository

@Composable
fun getCardColors(): CardColors {
    val cardBackgroundColor = getCardBackgroundColor()
    return CardDefaults.cardColors(containerColor = cardBackgroundColor)
}

@Composable
fun getCardBackgroundColor(): Color {
    val customColors = LocalCustomColors.current
    val isDarkTheme = isSystemInDarkTheme()

    return if (isDarkTheme) customColors.cardBackgroundDark else customColors.cardBackgroundLight
}

@Composable
fun getGenderColor(gender: Gender): Color {
    val customColors = LocalCustomColors.current
    val isDarkTheme = isSystemInDarkTheme()

    return when (gender) {
        Gender.MALE -> if (isDarkTheme) customColors.maleColorDark else customColors.maleColorLight
        Gender.FEMALE -> if (isDarkTheme) customColors.femaleColorDark else customColors.femaleColorLight
        Gender.TBD -> if (isDarkTheme) customColors.tbdColorDark else customColors.tbdColorLight
    }
}