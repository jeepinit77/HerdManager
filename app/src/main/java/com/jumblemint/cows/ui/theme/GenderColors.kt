package com.jumblemint.cows.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jumblemint.cows.data.model.Gender
import kotlin.math.max

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
    val palette = themeSettings.genderPalette
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val baseColor = when (gender) {
        Gender.MALE -> palette.male
        Gender.FEMALE -> palette.female
        Gender.TBD -> palette.neutral
    }

    val hct = baseColor.toHct()
    val (targetTone, minChroma) = when (gender) {
        Gender.MALE -> if (isDarkTheme) 65f to 42f else 48f to 36f
        Gender.FEMALE -> if (isDarkTheme) 66f to 44f else 52f to 38f
        Gender.TBD -> if (isDarkTheme) 68f to 22f else 58f to 20f
    }

    val adjustedTone = ((targetTone + hct.tone) / 2f).coerceIn(35f, 82f)
    val adjustedChroma = max(minChroma, hct.chroma)

    return hct.copy(tone = adjustedTone, chroma = adjustedChroma).toColor()
}
