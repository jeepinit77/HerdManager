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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

fun blend(fg: Color, bg: Color): Color {
    val alpha = fg.alpha
    if (alpha == 1f) return fg

    val invAlpha = 1f - alpha

    val r = (fg.red * alpha) + (bg.red * invAlpha)
    val g = (fg.green * alpha) + (bg.green * invAlpha)
    val b = (fg.blue * alpha) + (bg.blue * invAlpha)

    return Color(red = r, green = g, blue = b)
}

private fun createDarkColorScheme(customColors: CustomColors) = darkColorScheme(
    primary = customColors.primaryDark,
    onPrimary = determineOnColor(customColors.primaryDark),
    secondary = customColors.secondaryDark,
    onSecondary = determineOnColor(customColors.secondaryDark),
    tertiary = customColors.tertiaryDark,
    onTertiary = determineOnColor(customColors.tertiaryDark),
    background = blend(customColors.backgroundDark, Color.Black),
    onBackground = determineOnColor(blend(customColors.backgroundDark, Color.Black)),
    surface = blend(customColors.surfaceDark, Color.Black),
    onSurface = determineOnColor(blend(customColors.surfaceDark, Color.Black)),
    surfaceVariant = blend(customColors.cardBackgroundDark, blend(customColors.backgroundDark, Color.Black)),
    onSurfaceVariant = determineOnColor(blend(customColors.cardBackgroundDark, blend(customColors.backgroundDark, Color.Black)))
)

private fun createLightColorScheme(customColors: CustomColors) = lightColorScheme(
    primary = customColors.primaryLight,
    onPrimary = determineOnColor(customColors.primaryLight),
    secondary = customColors.secondaryLight,
    onSecondary = determineOnColor(customColors.secondaryLight),
    tertiary = customColors.tertiaryLight,
    onTertiary = determineOnColor(customColors.tertiaryLight),
    background = blend(customColors.backgroundLight, Color.White),
    onBackground = determineOnColor(blend(customColors.backgroundLight, Color.White)),
    surface = blend(customColors.surfaceLight, Color.White),
    onSurface = determineOnColor(blend(customColors.surfaceLight, Color.White)),
    surfaceVariant = blend(customColors.cardBackgroundLight, blend(customColors.backgroundLight, Color.White)),
    onSurfaceVariant = determineOnColor(blend(customColors.cardBackgroundLight, blend(customColors.backgroundLight, Color.White)))
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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
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
        database.activityTypeConfigDao(),
        database.breedDao()
    )
    val themeManager = ThemeManager(repository)
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())
    
    val actualDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (actualDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        actualDarkTheme -> createDarkColorScheme(customColors)
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

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SharpShapes
        ) {
            content()
        }
    }
}
