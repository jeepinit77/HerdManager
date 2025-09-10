package com.jumblemint.cows.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

private fun createDarkColorScheme(customColors: CustomColors) = darkColorScheme(
    primary = customColors.primaryDark,
    secondary = customColors.secondaryDark,
    tertiary = customColors.tertiaryDark
)

private fun createLightColorScheme(customColors: CustomColors) = lightColorScheme(
    primary = customColors.primaryLight,
    secondary = customColors.secondaryLight,
    tertiary = customColors.tertiaryLight
)

// Modern-sharp shapes across the app
private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun CowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Use brand palette consistently across devices
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao(),
        database.noteDao(),
        database.userDao(),
        database.herdDao(),
        database.herdMemberDao(),
        database.tagColorDao(),
        database.activityTypeConfigDao()
    )
    val themeManager = ThemeManager(repository)
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(customColors)
        else -> createLightColorScheme(customColors)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SharpShapes,
        content = content
    )
}
