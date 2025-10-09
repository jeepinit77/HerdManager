package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
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
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())
    val currentPreset by themeManager.getCurrentPresetFlow().collectAsState(initial = PresetTheme.TINT_BLUE)
    val scope = rememberCoroutineScope()

    val systemDark = isSystemInDarkTheme()
    var currentMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var previewDark by remember { mutableStateOf(systemDark) }

    LaunchedEffect(Unit) {
        currentMode = themeManager.getThemeMode()
        previewDark = when (currentMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemDark
        }
    }

    Column(modifier.fillMaxSize()) {
        // Mode switcher
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("App Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System")
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            selected = currentMode == mode,
                            onClick = {
                                scope.launch {
                                    themeManager.setThemeMode(mode)
                                    currentMode = mode
                                    previewDark = when (mode) {
                                        ThemeMode.LIGHT -> false
                                        ThemeMode.DARK -> true
                                        ThemeMode.SYSTEM -> systemDark
                                    }
                                }
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text(label) }
                    }
                }
            }
        }

        ThemePicker(
            themeManager = themeManager,
            customColors = customColors,
            currentPreset = currentPreset,
            isDarkTheme = previewDark,
            scope = scope,
            modifier = Modifier.weight(1f)
        )
    }
}

private enum class ThemeStyle { COLORED_CARDS, COLORED_BACKGROUND, GRAY_CARDS, GRAY_BACKGROUND }

/** Apply fade-only intensity to background/card based on chosen style. */
private suspend fun applyColorAndStyle(
    manager: ThemeManager,
    base: CustomColors,
    style: ThemeStyle,
    intensity: Float
) {
    val colorAlpha = intensity * 0.8f      // 0.0..0.8
    val grayAlphaLight = (0.05f + intensity * 0.25f) * 0.5f  // Same as before
    val grayAlphaDark = (0.05f + intensity * 0.25f) * 2f

    val (bgL, cardL, surfL) = when (style) {
        ThemeStyle.COLORED_CARDS -> Triple(base.backgroundLight, base.primaryLight.copy(alpha = colorAlpha), base.surfaceLight)
        ThemeStyle.COLORED_BACKGROUND -> Triple(base.primaryLight.copy(alpha = colorAlpha), base.cardBackgroundLight, base.surfaceLight)
        ThemeStyle.GRAY_CARDS -> Triple(Color.White, Color.Black.copy(alpha = grayAlphaLight), base.surfaceLight)
        ThemeStyle.GRAY_BACKGROUND -> Triple(Color.Black.copy(alpha = grayAlphaLight), Color.White, base.surfaceLight)
    }
    val (bgD, cardD, surfD) = when (style) {
        ThemeStyle.COLORED_CARDS -> Triple(base.backgroundDark, base.primaryDark.copy(alpha = colorAlpha * 1.5f), base.surfaceDark)
        ThemeStyle.COLORED_BACKGROUND -> Triple(base.primaryDark.copy(alpha = colorAlpha * 2f), base.cardBackgroundDark, base.surfaceDark)
        ThemeStyle.GRAY_CARDS -> Triple(Color(0xFF1A1A1A), Color.Black.copy(alpha = grayAlphaDark), base.surfaceDark)
        ThemeStyle.GRAY_BACKGROUND -> Triple(Color.Black.copy(alpha = grayAlphaDark), Color(0xFF1A1A1A), base.surfaceDark)
    }

    manager.updateColor(SettingsKeys.THEME_BACKGROUND_LIGHT, bgL)
    manager.updateColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, cardL)
    manager.updateColor(SettingsKeys.THEME_SURFACE_LIGHT, surfL)

    manager.updateColor(SettingsKeys.THEME_BACKGROUND_DARK, bgD)
    manager.updateColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, cardD)
    manager.updateColor(SettingsKeys.THEME_SURFACE_DARK, surfD)

    manager.updateIntensity(intensity)
}

@Composable
private fun ThemePicker(
    themeManager: ThemeManager,
    customColors: CustomColors,
    currentPreset: PresetTheme,
    isDarkTheme: Boolean,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val intensity by themeManager.getCurrentIntensity().collectAsState(initial = 0.2f)
    var currentIntensity by remember(intensity) { mutableStateOf(intensity) }
    var style by remember { mutableStateOf(ThemeStyle.COLORED_CARDS) }

    // --- Two rows of tint themes ---
    val tintRow1 = listOf(
        PresetTheme.TINT_BLUE, PresetTheme.TINT_GREEN, PresetTheme.TINT_PURPLE,
        PresetTheme.TINT_ORANGE, PresetTheme.TINT_PINK
    )
    val tintRow2 = listOf(
        PresetTheme.TINT_RED, PresetTheme.TINT_YELLOW, PresetTheme.TINT_TEAL,
        PresetTheme.TINT_BROWN, PresetTheme.TINT_GRAY
    )

    // --- One row of curated themes ---
    val curatedRow = listOf(
        PresetTheme.SLATE_GRAY, PresetTheme.MONOCHROME,
        PresetTheme.NORD_FROST, PresetTheme.MAUVE_MIST, PresetTheme.OLIVE_NOIR
    )

    val bg = if (isDarkTheme) customColors.backgroundDark else customColors.backgroundLight
    Box(modifier.fillMaxSize().background(bg)) {
        BackgroundColorProvider(backgroundColor = bg) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ----- Choose Color Scheme -----
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        BackgroundColorProvider(MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Choose Color Scheme", style = MaterialTheme.typography.titleMedium)

                                TintRow(row = tintRow1, currentPreset = currentPreset, isDarkTheme = isDarkTheme, onPick = {
                                    scope.launch { themeManager.applyPresetTheme(it); val newColors = it.getColors(); applyColorAndStyle(themeManager, newColors, style, currentIntensity) }
                                })

                                TintRow(row = tintRow2, currentPreset = currentPreset, isDarkTheme = isDarkTheme, onPick = {
                                    scope.launch { themeManager.applyPresetTheme(it); val newColors = it.getColors(); applyColorAndStyle(themeManager, newColors, style, currentIntensity) }
                                })

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    curatedRow.forEach { preset ->
                                        val swatch = if (isDarkTheme) preset.getColors().primaryDark else preset.getColors().primaryLight
                                        PresetChip(
                                            isSelected = currentPreset == preset,
                                            name = preset.displayName,
                                            color = swatch,
                                            onClick = { scope.launch { themeManager.applyPresetTheme(preset); val newColors = preset.getColors(); applyColorAndStyle(themeManager, newColors, style, currentIntensity) } },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ----- Style (where to apply the fade) -----
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        BackgroundColorProvider(MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Theme Style", style = MaterialTheme.typography.titleMedium)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PhoneStyleButton(
                                        title = "Colored Cards",
                                        bgColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color.White,
                                        cardColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        navColor = MaterialTheme.colorScheme.primary,
                                        isSelected = style == ThemeStyle.COLORED_CARDS,
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            style = ThemeStyle.COLORED_CARDS
                                            scope.launch { applyColorAndStyle(themeManager, customColors, style, currentIntensity) }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PhoneStyleButton(
                                        title = "Colored Background",
                                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        cardColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color.White,
                                        navColor = MaterialTheme.colorScheme.primary,
                                        isSelected = style == ThemeStyle.COLORED_BACKGROUND,
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            style = ThemeStyle.COLORED_BACKGROUND
                                            scope.launch { applyColorAndStyle(themeManager, customColors, style, currentIntensity) }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PhoneStyleButton(
                                        title = "Gray Cards",
                                        bgColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color.White,
                                        cardColor = (if (isDarkTheme) Color.Black else Color.Black).copy(
                                            alpha = if (isDarkTheme) (0.05f + currentIntensity * 0.25f) * 2f else (0.05f + currentIntensity * 0.25f) * 0.5f
                                        ),
                                        navColor = MaterialTheme.colorScheme.primary,
                                        isSelected = style == ThemeStyle.GRAY_CARDS,
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            style = ThemeStyle.GRAY_CARDS
                                            scope.launch { applyColorAndStyle(themeManager, customColors, style, currentIntensity) }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PhoneStyleButton(
                                        title = "Gray Background",
                                        bgColor = Color.Black.copy(
                                            alpha = if (isDarkTheme) (0.05f + currentIntensity * 0.25f) * 2f else (0.05f + currentIntensity * 0.25f) * 0.5f
                                        ),
                                        cardColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color.White,
                                        navColor = MaterialTheme.colorScheme.primary,
                                        isSelected = style == ThemeStyle.GRAY_BACKGROUND,
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            style = ThemeStyle.GRAY_BACKGROUND
                                            scope.launch { applyColorAndStyle(themeManager, customColors, style, currentIntensity) }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ----- Intensity slider (fade only) -----
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        BackgroundColorProvider(MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Theme Intensity", style = MaterialTheme.typography.titleMedium)
                                Slider(
                                    value = currentIntensity,
                                    onValueChange = { currentIntensity = it },
                                    onValueChangeFinished = {
                                        scope.launch { applyColorAndStyle(themeManager, customColors, style, currentIntensity) }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.onSurface,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
private fun TintRow(
    currentPreset: PresetTheme,
    row: List<PresetTheme>,
    isDarkTheme: Boolean,
    onPick: (PresetTheme) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { preset ->
            val swatch = if (isDarkTheme) preset.getColors().primaryDark else preset.getColors().primaryLight
            PresetChip(
                isSelected = currentPreset == preset,
                name = preset.displayName,
                color = swatch,
                onClick = { onPick(preset) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresetChip(
    isSelected: Boolean = false,
    name: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check, contentDescription = null, tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }
        }
        Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
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
        modifier = modifier.clickable { onClick() },
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
                    Icons.Filled.Check, contentDescription = null, tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp)
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) selectedColor else LocalContentColor.current,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
