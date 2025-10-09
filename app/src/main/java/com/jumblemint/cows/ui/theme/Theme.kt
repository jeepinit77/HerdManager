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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

/** Blend fg over bg, preserving hue and using alpha strictly as fade. */
fun blend(fg: Color, bg: Color): Color {
    val a = fg.alpha
    if (a == 1f) return fg
    val inv = 1f - a
    return Color(
        red = fg.red * a + bg.red * inv,
        green = fg.green * a + bg.green * inv,
        blue = fg.blue * a + bg.blue * inv,
        alpha = 1f
    )
}

private fun createDarkColorScheme(colors: CustomColors) = darkColorScheme(
    primary = colors.primaryDark,
    onPrimary = determineOnColor(colors.primaryDark),
    secondary = colors.secondaryDark,
    onSecondary = determineOnColor(colors.secondaryDark),
    tertiary = colors.tertiaryDark,
    onTertiary = determineOnColor(colors.tertiaryDark),

    background = blend(colors.backgroundDark, Color.Black),
    onBackground = determineOnColor(blend(colors.backgroundDark, Color.Black)),
    surface = blend(colors.surfaceDark, Color.Black),
    onSurface = determineOnColor(blend(colors.surfaceDark, Color.Black)),
    surfaceVariant = blend(colors.cardBackgroundDark, blend(colors.backgroundDark, Color.Black)),
    onSurfaceVariant = determineOnColor(
        blend(colors.cardBackgroundDark, blend(colors.backgroundDark, Color.Black))
    )
)

private fun createLightColorScheme(colors: CustomColors) = lightColorScheme(
    primary = colors.primaryLight,
    onPrimary = determineOnColor(colors.primaryLight),
    secondary = colors.secondaryLight,
    onSecondary = determineOnColor(colors.secondaryLight),
    tertiary = colors.tertiaryLight,
    onTertiary = determineOnColor(colors.tertiaryLight),

    background = blend(colors.backgroundLight, Color.White),
    onBackground = determineOnColor(blend(colors.backgroundLight, Color.White)),
    surface = blend(colors.surfaceLight, Color.White),
    onSurface = determineOnColor(blend(colors.surfaceLight, Color.White)),
    surfaceVariant = blend(colors.cardBackgroundLight, blend(colors.backgroundLight, Color.White)),
    onSurfaceVariant = determineOnColor(
        blend(colors.cardBackgroundLight, blend(colors.backgroundLight, Color.White))
    )
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
