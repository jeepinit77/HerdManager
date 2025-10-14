package com.jumblemint.cows.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

@Composable
fun CowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val db = CattleDatabase.getDatabase(context)
    val repository = remember {
        CattleRepository(
            db.cowDao(), db.pastureDao(), db.activityDao(), db.settingsDao(),
            db.noteDao(), db.userDao(), db.herdDao(), db.herdMemberDao(),
            db.tagColorDao(), db.activityTypeConfigDao(), db.breedDao()
        )
    }
    val themeManager = remember { ThemeManager(repository) }
    val themeSettings by themeManager.getThemeSettingsFlow().collectAsState(initial = ThemeSettings())

    val isDark = when (themeSettings.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }

    val colorScheme = generateThemeFromSeed(
        seedColor = themeSettings.seedColor,
        surfaceTone = themeSettings.getSurfaceTone(isDark),
        navBarTone = themeSettings.getNavBarTone(isDark),
        isDark = isDark,
        style = themeSettings.style
    )

    val view = LocalView.current
    val useDarkIcons = colorScheme.surface.luminance() > 0.5f

    SideEffect {
        val window = (view.context as androidx.activity.ComponentActivity).window
        window.statusBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
    }

    CompositionLocalProvider(
        LocalThemeSettings provides themeSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}