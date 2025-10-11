package com.jumblemint.cows.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

private fun createDarkColorScheme(colors: CustomColors) = darkColorScheme(
    primary = colors.primaryDark,
    secondary = colors.secondaryDark,
    tertiary = colors.tertiaryDark,
    background = colors.backgroundDark,
    surface = colors.surfaceDark,
    surfaceVariant = colors.cardBackgroundDark
)

private fun createLightColorScheme(colors: CustomColors) = lightColorScheme(
    primary = colors.primaryLight,
    secondary = colors.secondaryLight,
    tertiary = colors.tertiaryLight,
    background = colors.backgroundLight,
    surface = colors.surfaceLight,
    surfaceVariant = colors.cardBackgroundLight
)

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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val db = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        db.cowDao(), db.pastureDao(), db.activityDao(), db.settingsDao(),
        db.noteDao(), db.userDao(), db.herdDao(), db.herdMemberDao(),
        db.tagColorDao(), db.activityTypeConfigDao(), db.breedDao()
    )
    val themeManager = ThemeManager(repository)
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())

    val actualDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }

    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (actualDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (actualDark) createDarkColorScheme(customColors) else createLightColorScheme(customColors)
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as ComponentActivity).window
            val isSurfaceDark = colorScheme.surface.luminance() < 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isSurfaceDark
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SharpShapes,
            content = content
        )
    }
}
