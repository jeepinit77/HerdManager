package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.ColorPickerDialog
import com.jumblemint.cows.ui.theme.CustomColors
import com.jumblemint.cows.ui.theme.PresetTheme
import com.jumblemint.cows.ui.theme.ThemeManager
import com.jumblemint.cows.ui.theme.ThemeMode
import com.jumblemint.cows.ui.theme.getColors
import com.jumblemint.cows.ui.theme.SmartText
import com.jumblemint.cows.ui.theme.AutoText
import com.jumblemint.cows.ui.theme.BackgroundColorProvider
import com.jumblemint.cows.ui.theme.contrastingTextColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
        CattleRepository(
            database.cowDao(), database.pastureDao(), database.activityDao(),
            database.settingsDao(), database.noteDao(), database.userDao(),
            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
            database.activityTypeConfigDao(),
            database.breedDao()
        )
    }
    
    val themeManager = remember { ThemeManager(repository) }
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    var currentPreset by remember { mutableStateOf(PresetTheme.DEFAULT) }
    var currentThemeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var previewDarkMode by remember { mutableStateOf(isDarkTheme) }

    LaunchedEffect(Unit) {
        currentPreset = themeManager.getCurrentPreset()
        currentThemeMode = themeManager.getThemeMode()
        previewDarkMode = when (currentThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isDarkTheme
        }
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTitle by remember { mutableStateOf("") }
    var colorPickerCurrent by remember { mutableStateOf(Color.White) }
    var colorPickerCallback by remember { mutableStateOf<(Color) -> Unit>({}) }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "App Theme",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modes = listOf("Light", "Dark", "System")
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            onClick = {
                                coroutineScope.launch {
                                    val themeMode = when (mode) {
                                        "Light" -> ThemeMode.LIGHT
                                        "Dark" -> ThemeMode.DARK
                                        else -> ThemeMode.SYSTEM
                                    }
                                    themeManager.setThemeMode(themeMode)
                                    currentThemeMode = themeMode
                                    previewDarkMode = when (themeMode) {
                                        ThemeMode.LIGHT -> false
                                        ThemeMode.DARK -> true
                                        ThemeMode.SYSTEM -> isDarkTheme
                                    }
                                }
                            },
                            selected = currentThemeMode == when (mode) {
                                "Light" -> ThemeMode.LIGHT
                                "Dark" -> ThemeMode.DARK
                                else -> ThemeMode.SYSTEM
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(mode)
                        }
                    }
                }
            }
        }

        ThemePickerTab(
            themeManager = themeManager,
            customColors = customColors,
            isDarkTheme = previewDarkMode,
            onThemeChange = { color, style, intensity ->
                coroutineScope.launch {
                    applyColorAndStyle(themeManager, color, style, intensity, previewDarkMode)
                    currentPreset = PresetTheme.CUSTOM
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
    
    if (showColorPicker) {
        ColorPickerDialog(
            title = colorPickerTitle,
            currentColor = colorPickerCurrent,
            onColorSelected = { color ->
                colorPickerCallback(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

enum class ThemeStyle { COLORED_CARDS, COLORED_BACKGROUND, GRAY_CARDS, GRAY_BACKGROUND }

data class ColorScheme(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val male: Color,
    val female: Color,
    val tbd: Color
)

private fun getColorScheme(baseColor: Color): ColorScheme {
    return when (baseColor) {
        Color(0xFF1565C0) -> ColorScheme(primary = baseColor, secondary = Color(0xFF64B5F6), tertiary = Color(0xFF0277BD), male = Color(0xFF0D47A1), female = Color(0xFFAD1457), tbd = Color(0xFF424242))
        Color(0xFF2E7D32) -> ColorScheme(primary = baseColor, secondary = Color(0xFF81C784), tertiary = Color(0xFF00695C), male = Color(0xFF1565C0), female = Color(0xFFC2185B), tbd = Color(0xFF5D4037))
        Color(0xFF7B1FA2) -> ColorScheme(primary = baseColor, secondary = Color(0xFFBA68C8), tertiary = Color(0xFF5E35B1), male = Color(0xFF3F51B5), female = Color(0xFFE91E63), tbd = Color(0xFF616161))
        Color(0xFFD84315) -> ColorScheme(primary = baseColor, secondary = Color(0xFFFF8A65), tertiary = Color(0xFFBF360C), male = Color(0xFF1565C0), female = Color(0xFFE91E63), tbd = Color(0xFF5D4037))
        Color(0xFFE91E63) -> ColorScheme(primary = baseColor, secondary = Color(0xFFF48FB1), tertiary = Color(0xFFAD1457), male = Color(0xFF1565C0), female = baseColor, tbd = Color(0xFF616161))
        Color(0xFFD32F2F) -> ColorScheme(primary = baseColor, secondary = Color(0xFFEF5350), tertiary = Color(0xFFB71C1C), male = Color(0xFF1565C0), female = Color(0xFFE91E63), tbd = Color(0xFF424242))
        Color(0xFFFBC02D) -> ColorScheme(primary = baseColor, secondary = Color(0xFFFFD54F), tertiary = Color(0xFFF57F17), male = Color(0xFF1565C0), female = Color(0xFFE91E63), tbd = Color(0xFF795548))
        Color(0xFF00838F) -> ColorScheme(primary = baseColor, secondary = Color(0xFF4DD0E1), tertiary = Color(0xFF006064), male = Color(0xFF1976D2), female = Color(0xFFAD1457), tbd = Color(0xFF455A64))
        Color(0xFF6D4C41) -> ColorScheme(primary = baseColor, secondary = Color(0xFFA1887F), tertiary = Color(0xFF3E2723), male = Color(0xFF1565C0), female = Color(0xFFE91E63), tbd = Color(0xFF5D4037))
        Color(0xFF525252) -> ColorScheme(primary = baseColor, secondary = Color(0xFF9E9E9E), tertiary = Color(0xFF212121), male = Color(0xFF424242), female = Color(0xFF757575), tbd = Color(0xFF616161))
        Color(0xFF4A5568) -> ColorScheme(primary = baseColor, secondary = Color(0xFFA0AEC0), tertiary = Color(0xFF2D3748), male = Color(0xFF2C5282), female = Color(0xFF742A2A), tbd = Color(0xFF718096))
        Color(0xFF00FF41) -> ColorScheme(primary = baseColor, secondary = Color(0xFF00E5FF), tertiary = Color(0xFFFF1744), male = Color(0xFF00BCD4), female = Color(0xFFFF4081), tbd = Color(0xFF9E9E9E))
        else -> ColorScheme(primary = baseColor, secondary = baseColor.copy(alpha = 0.4f), tertiary = baseColor.copy(alpha = 0.6f), male = Color(0xFF1565C0), female = Color(0xFFE91E63), tbd = Color(0xFF616161))
    }
}

private suspend fun applyColorAndStyle(themeManager: ThemeManager, baseColor: Color, style: ThemeStyle, intensity: Float, isDarkTheme: Boolean) {
    val colorScheme = getColorScheme(baseColor)
    val colorIntensity = 0.05f + (intensity * 0.25f)

    themeManager.updateColor(SettingsKeys.THEME_PRIMARY_LIGHT, colorScheme.primary)
    themeManager.updateColor(SettingsKeys.THEME_PRIMARY_DARK, colorScheme.primary)
    themeManager.updateColor(SettingsKeys.THEME_SECONDARY_LIGHT, colorScheme.secondary)
    themeManager.updateColor(SettingsKeys.THEME_SECONDARY_DARK, colorScheme.secondary)
    themeManager.updateColor(SettingsKeys.THEME_TERTIARY_LIGHT, colorScheme.tertiary)
    themeManager.updateColor(SettingsKeys.THEME_TERTIARY_DARK, colorScheme.tertiary)
    themeManager.updateColor(SettingsKeys.THEME_MALE_COLOR_LIGHT, colorScheme.male)
    themeManager.updateColor(SettingsKeys.THEME_MALE_COLOR_DARK, colorScheme.male)
    themeManager.updateColor(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, colorScheme.female)
    themeManager.updateColor(SettingsKeys.THEME_FEMALE_COLOR_DARK, colorScheme.female)
    themeManager.updateColor(SettingsKeys.THEME_TBD_COLOR_LIGHT, colorScheme.tbd)
    themeManager.updateColor(SettingsKeys.THEME_TBD_COLOR_DARK, colorScheme.tbd)

    val grayIntensity = colorIntensity * 0.5f
    val (bgLight, cardLight, surfaceLight) = when (style) {
        ThemeStyle.COLORED_CARDS -> Triple(Color(0xFFFFFFFF), colorScheme.primary.copy(alpha = colorIntensity), colorScheme.secondary)
        ThemeStyle.COLORED_BACKGROUND -> Triple(colorScheme.primary.copy(alpha = colorIntensity), Color(0xFFFFFFFF), colorScheme.secondary)
        ThemeStyle.GRAY_CARDS -> Triple(Color(0xFF000000).copy(alpha = grayIntensity), Color(0xFFFFFFFF), colorScheme.secondary)
        ThemeStyle.GRAY_BACKGROUND -> Triple(Color(0xFFFFFFFF), Color(0xFF000000).copy(alpha = grayIntensity), colorScheme.secondary)
    }
    val (bgDark, cardDark, surfaceDark) = when (style) {
        ThemeStyle.COLORED_CARDS -> Triple(Color(0xFF1A1A1A), colorScheme.primary.copy(alpha = colorIntensity * 1.5f), colorScheme.secondary)
        ThemeStyle.COLORED_BACKGROUND -> Triple(colorScheme.primary.copy(alpha = colorIntensity * 2f), Color(0xFF1A1A1A), colorScheme.secondary)
        ThemeStyle.GRAY_CARDS -> Triple(Color(0xFF000000).copy(alpha = grayIntensity * 2f), Color(0xFF1A1A1A), colorScheme.secondary)
        ThemeStyle.GRAY_BACKGROUND -> Triple(Color(0xFF1A1A1A), Color(0xFF000000).copy(alpha = grayIntensity * 2f), colorScheme.secondary)
    }

    themeManager.updateColor(SettingsKeys.THEME_BACKGROUND_LIGHT, bgLight)
    themeManager.updateColor(SettingsKeys.THEME_BACKGROUND_DARK, bgDark)
    themeManager.updateColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, cardLight)
    themeManager.updateColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, cardDark)
    themeManager.updateColor(SettingsKeys.THEME_SURFACE_LIGHT, surfaceLight)
    themeManager.updateColor(SettingsKeys.THEME_SURFACE_DARK, surfaceDark)

    themeManager.updateIntensity(intensity)
}

private fun detectCurrentStyle(colors: CustomColors, isDarkTheme: Boolean): ThemeStyle {
    val bgColor = if (isDarkTheme) colors.backgroundDark else colors.backgroundLight
    val cardColor = if (isDarkTheme) colors.cardBackgroundDark else colors.cardBackgroundLight
    
    return when {
        cardColor.alpha < 1f -> ThemeStyle.COLORED_CARDS
        bgColor.alpha < 1f -> ThemeStyle.COLORED_BACKGROUND
        (cardColor == (if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF))) -> ThemeStyle.GRAY_CARDS
        else -> ThemeStyle.GRAY_BACKGROUND
    }
}

private fun detectIntensity(colors: CustomColors, isDarkTheme: Boolean): Float {
    val bgColor = if (isDarkTheme) colors.backgroundDark else colors.backgroundLight
    val cardColor = if (isDarkTheme) colors.cardBackgroundDark else colors.cardBackgroundLight
    val alpha = maxOf(bgColor.alpha, cardColor.alpha)
    return ((alpha - 0.05f) / 0.25f).coerceIn(0.2f, 1.0f)
}

@Composable
private fun ThemePickerTab(
    themeManager: ThemeManager,
    customColors: CustomColors,
    isDarkTheme: Boolean,
    onThemeChange: (Color, ThemeStyle, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIntensity by themeManager.getCurrentIntensity().collectAsState(initial = 0.2f)

    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedStyle by remember { mutableStateOf(ThemeStyle.COLORED_CARDS) }
    var intensity by remember { mutableStateOf(0.2f) }

    LaunchedEffect(customColors, isDarkTheme) {
        selectedColor = if (isDarkTheme) customColors.primaryDark else customColors.primaryLight
        selectedStyle = detectCurrentStyle(customColors, isDarkTheme)
    }

    LaunchedEffect(currentIntensity) {
        intensity = currentIntensity
    }

    val defaultColors = PresetTheme.DEFAULT.getColors()
    val presetColors = listOf(
        defaultColors.primaryLight to "Default",
        Color(0xFF1565C0) to "Blue",
        Color(0xFF2E7D32) to "Green",
        Color(0xFF7B1FA2) to "Purple",
        Color(0xFFD84315) to "Orange",
        Color(0xFFE91E63) to "Pink",
        Color(0xFFD32F2F) to "Red",
        Color(0xFFFBC02D) to "Yellow",
        Color(0xFF00838F) to "Teal",
        Color(0xFF6D4C41) to "Brown",
        Color(0xFF525252) to "Gray",
        Color(0xFF4A5568) to "Slate"
    )

    val bgColor = if (isDarkTheme) customColors.backgroundDark else customColors.backgroundLight
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        BackgroundColorProvider(backgroundColor = bgColor) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = null
                    ) {
                        BackgroundColorProvider(backgroundColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Choose Color",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    presetColors.chunked(6).forEach { rowColors ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowColors.forEach { (color, name) ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            selectedColor = color
                                                            onThemeChange(color, selectedStyle, intensity)
                                                        }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                            .then(
                                                                if (selectedColor == color) Modifier.padding(2.dp)
                                                                else Modifier
                                                            )
                                                    ) {
                                                        if (selectedColor == color) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = color.contrastingTextColor(),
                                                                modifier = Modifier.align(Alignment.Center)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(top = 4.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            repeat(6 - rowColors.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = null
                    ) {
                        BackgroundColorProvider(backgroundColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Theme Style",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PhoneStyleButton(
                                        title = "Colored Cards",
                                        bgColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF),
                                        cardColor = selectedColor.copy(alpha = 0.15f),
                                        navColor = selectedColor,
                                        isSelected = selectedStyle == ThemeStyle.COLORED_CARDS,
                                        selectedColor = selectedColor,
                                        onClick = {
                                            selectedStyle = ThemeStyle.COLORED_CARDS
                                            onThemeChange(selectedColor, ThemeStyle.COLORED_CARDS, intensity)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    PhoneStyleButton(
                                        title = "Colored Background",
                                        bgColor = selectedColor.copy(alpha = 0.15f),
                                        cardColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF),
                                        navColor = selectedColor,
                                        isSelected = selectedStyle == ThemeStyle.COLORED_BACKGROUND,
                                        selectedColor = selectedColor,
                                        onClick = {
                                            selectedStyle = ThemeStyle.COLORED_BACKGROUND
                                            onThemeChange(selectedColor, ThemeStyle.COLORED_BACKGROUND, intensity)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PhoneStyleButton(
                                        title = "Gray Cards",
                                        bgColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF),
                                        cardColor = if (isDarkTheme) Color(0xFF000000).copy(alpha = (0.05f + (intensity * 0.25f)) * 2f) else Color(0xFF000000).copy(alpha = (0.05f + (intensity * 0.25f)) * 0.5f),
                                        navColor = selectedColor,
                                        isSelected = selectedStyle == ThemeStyle.GRAY_CARDS,
                                        selectedColor = selectedColor,
                                        onClick = {
                                            selectedStyle = ThemeStyle.GRAY_CARDS
                                            onThemeChange(selectedColor, ThemeStyle.GRAY_CARDS, intensity)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    PhoneStyleButton(
                                        title = "Gray Background",
                                        bgColor = if (isDarkTheme) Color(0xFF000000).copy(alpha = (0.05f + (intensity * 0.25f)) * 2f) else Color(0xFF000000).copy(alpha = (0.05f + (intensity * 0.25f)) * 0.5f),
                                        cardColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF),
                                        navColor = selectedColor,
                                        isSelected = selectedStyle == ThemeStyle.GRAY_BACKGROUND,
                                        selectedColor = selectedColor,
                                        onClick = {
                                            selectedStyle = ThemeStyle.GRAY_BACKGROUND
                                            onThemeChange(selectedColor, ThemeStyle.GRAY_BACKGROUND, intensity)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = null
                    ) {
                        BackgroundColorProvider(backgroundColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Theme Intensity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = intensity,
                                    onValueChange = { intensity = it },
                                    onValueChangeFinished = { onThemeChange(selectedColor, selectedStyle, intensity) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = selectedColor,
                                        activeTrackColor = selectedColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneStyleButton(
    title: String,
    bgColor: Color,
    cardColor: Color,
    navColor: Color,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) selectedColor else Color.Black.copy(alpha = 0.1f))
                .padding(if (isSelected) 3.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(navColor)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(cardColor)
                        )
                    }
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(12.dp)
                )
            }
        }
        
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) selectedColor else LocalContentColor.current,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
