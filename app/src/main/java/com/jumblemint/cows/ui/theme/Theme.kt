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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

private fun createDarkColorScheme(customColors: CustomColors) = darkColorScheme(
    primary = customColors.primaryDark,
    onPrimary = getContrastingTextColor(customColors.primaryDark),
    secondary = customColors.secondaryDark,
    onSecondary = getContrastingTextColor(customColors.secondaryDark),
    tertiary = customColors.tertiaryDark,
    onTertiary = getContrastingTextColor(customColors.tertiaryDark),
    background = customColors.backgroundDark,
    onBackground = getContrastingTextColor(customColors.backgroundDark),
    surface = customColors.surfaceDark,
    onSurface = getContrastingTextColor(customColors.surfaceDark),
    surfaceVariant = customColors.cardBackgroundDark,
    onSurfaceVariant = getContrastingTextColor(customColors.cardBackgroundDark)
)

private fun createLightColorScheme(customColors: CustomColors) = lightColorScheme(
    primary = customColors.primaryLight,
    onPrimary = getContrastingTextColor(customColors.primaryLight),
    secondary = customColors.secondaryLight,
    onSecondary = getContrastingTextColor(customColors.secondaryLight),
    tertiary = customColors.tertiaryLight,
    onTertiary = getContrastingTextColor(customColors.tertiaryLight),
    background = customColors.backgroundLight,
    onBackground = getContrastingTextColor(customColors.backgroundLight),
    surface = customColors.surfaceLight,
    onSurface = getContrastingTextColor(customColors.surfaceLight),
    surfaceVariant = customColors.cardBackgroundLight,
    onSurfaceVariant = getContrastingTextColor(customColors.cardBackgroundLight)
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

    // Handle status bar colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as ComponentActivity).window
            val isDark = colorScheme.surface.luminance() < 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SharpShapes
    ) {
        content()
    }
}


