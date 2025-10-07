package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
            database.activityTypeConfigDao()
        )
    }
    
    val themeManager = remember { ThemeManager(repository) }
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    val tabs = listOf("Presets", "Custom Theme")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
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
                            }
                        ) {
                            Text(mode)
                        }
                    }
                }
            }
        }

        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    icon = {
                        Icon(
                            if (index == 0) Icons.Outlined.Palette else Icons.Outlined.Edit,
                            contentDescription = null
                        )
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> PresetsTab(
                    currentPreset = currentPreset,
                    isDarkTheme = previewDarkMode,
                    onPresetSelected = { preset ->
                        coroutineScope.launch {
                            themeManager.applyPresetTheme(preset)
                            currentPreset = preset
                        }
                    }
                )
                1 -> CustomThemeTab(
                    customColors = customColors,
                    isDarkTheme = previewDarkMode,
                    onColorChange = { key, color ->
                        coroutineScope.launch {
                            themeManager.updateColor(key, color)
                            currentPreset = PresetTheme.CUSTOM
                        }
                    },
                    onOpenColorPicker = { title, current, callback ->
                        colorPickerTitle = title
                        colorPickerCurrent = current
                        colorPickerCallback = callback
                        showColorPicker = true
                    },
                    onResetToDefaults = {
                        coroutineScope.launch {
                            themeManager.resetToDefaults()
                            currentPreset = PresetTheme.DEFAULT
                        }
                    }
                )
            }
        }
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

@Composable
private fun PresetsTab(
    currentPreset: PresetTheme,
    isDarkTheme: Boolean,
    onPresetSelected: (PresetTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PresetTheme.values()) { preset ->
            PresetThemeCard(
                preset = preset,
                isSelected = currentPreset == preset,
                isDarkTheme = isDarkTheme,
                onClick = { onPresetSelected(preset) }
            )
        }
    }
}

@Composable
private fun PresetThemeCard(
    preset: PresetTheme,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = preset.getColors()
    val borderColor = if (isSelected) {
        if (isDarkTheme) colors.primaryDark else colors.primaryLight
    } else Color.Transparent
    
    val cardColor = if (isDarkTheme) colors.surfaceDark else colors.surfaceLight
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        BackgroundColorProvider(backgroundColor = cardColor) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoText(
                        preset.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = if (isDarkTheme) colors.primaryDark else colors.primaryLight
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    if (isDarkTheme) colors.primaryDark else colors.primaryLight,
                    if (isDarkTheme) colors.secondaryDark else colors.secondaryLight,
                    if (isDarkTheme) colors.tertiaryDark else colors.tertiaryLight
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
            }
        } // End BackgroundColorProvider
    }
}

@Composable
private fun CustomThemeTab(
    customColors: CustomColors,
    isDarkTheme: Boolean,
    onColorChange: (String, Color) -> Unit,
    onOpenColorPicker: (String, Color, (Color) -> Unit) -> Unit,
    onResetToDefaults: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InteractiveThemePreview(
                customColors = customColors,
                isDarkTheme = isDarkTheme,
                onColorChange = onColorChange,
                onOpenColorPicker = onOpenColorPicker
            )
        }
        
        item {
            Button(
                onClick = onResetToDefaults,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
private fun InteractiveThemePreview(
    customColors: CustomColors,
    isDarkTheme: Boolean,
    onColorChange: (String, Color) -> Unit,
    onOpenColorPicker: (String, Color, (Color) -> Unit) -> Unit
) {
    val bgColor = if (isDarkTheme) customColors.backgroundDark else customColors.backgroundLight
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val key = if (isDarkTheme) SettingsKeys.THEME_BACKGROUND_DARK else SettingsKeys.THEME_BACKGROUND_LIGHT
                onOpenColorPicker("Background Color\n• Main app background\n• Screen backgrounds\n• Base layer color", bgColor) { color -> onColorChange(key, color) }
            }
            .background(bgColor)
            .padding(16.dp)
    ) {
        BackgroundColorProvider(backgroundColor = bgColor) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoText(
                    "Tap anywhere to edit colors",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                )
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = "Tap to edit",
                    modifier = Modifier.size(16.dp)
                )
            }

            val cardColor = if (isDarkTheme) customColors.surfaceDark else customColors.surfaceLight
            Card(
                onClick = {
                    val key = if (isDarkTheme) SettingsKeys.THEME_SURFACE_DARK else SettingsKeys.THEME_SURFACE_LIGHT
                    onOpenColorPicker("Card Background\n• Card backgrounds\n• Dialog backgrounds\n• Surface containers", cardColor) { color -> onColorChange(key, color) }
                },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackgroundColorProvider(backgroundColor = cardColor) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AutoText(
                            "Herd Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Active" to "42", "Calves" to "8", "Bulls" to "3").forEach { (label, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    val key = if (isDarkTheme) SettingsKeys.THEME_PRIMARY_DARK else SettingsKeys.THEME_PRIMARY_LIGHT
                                    val current = if (isDarkTheme) customColors.primaryDark else customColors.primaryLight
                                    onOpenColorPicker("Primary Color\n• Primary buttons\n• Selected tabs\n• Progress indicators\n• Important numbers", current) { color -> onColorChange(key, color) }
                                }
                            ) {
                                Text(
                                    count,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) customColors.primaryDark else customColors.primaryLight
                                )
                                AutoText(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    }
                } // End BackgroundColorProvider
            }

            val buttonsCardColor = if (isDarkTheme) customColors.surfaceDark else customColors.surfaceLight
            Card(
                onClick = {
                    val key = if (isDarkTheme) SettingsKeys.THEME_SURFACE_DARK else SettingsKeys.THEME_SURFACE_LIGHT
                    onOpenColorPicker("Card Background\n• Card backgrounds\n• Dialog backgrounds\n• Surface containers", buttonsCardColor) { color -> onColorChange(key, color) }
                },
                colors = CardDefaults.cardColors(containerColor = buttonsCardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackgroundColorProvider(backgroundColor = buttonsCardColor) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoText(
                            "Buttons",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val key = if (isDarkTheme) SettingsKeys.THEME_ON_SURFACE_DARK else SettingsKeys.THEME_ON_SURFACE_LIGHT
                                val current = if (isDarkTheme) customColors.onSurfaceDark else customColors.onSurfaceLight
                                onOpenColorPicker("Text on Cards\n• Card titles\n• Body text on cards\n• Labels on surfaces", current) { color -> onColorChange(key, color) }
                            }
                        )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                val key = if (isDarkTheme) SettingsKeys.THEME_PRIMARY_DARK else SettingsKeys.THEME_PRIMARY_LIGHT
                                val current = if (isDarkTheme) customColors.primaryDark else customColors.primaryLight
                                onOpenColorPicker("Add Cow Button\n• Add/Save buttons\n• Selected tabs\n• Progress bars\n• Important numbers", current) { color -> onColorChange(key, color) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Cow")
                        }
                        OutlinedButton(
                            onClick = {
                                val key = if (isDarkTheme) SettingsKeys.THEME_SECONDARY_DARK else SettingsKeys.THEME_SECONDARY_LIGHT
                                val current = if (isDarkTheme) customColors.secondaryDark else customColors.secondaryLight
                                onOpenColorPicker("Cancel Button\n• Cancel/Back buttons\n• Icon tints\n• Accent elements\n• Toggle switches", current) { color -> onColorChange(key, color) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkTheme) customColors.secondaryDark else customColors.secondaryLight
                            ),
                            border = BorderStroke(1.dp, if (isDarkTheme) customColors.secondaryDark else customColors.secondaryLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val key = if (isDarkTheme) SettingsKeys.THEME_SECONDARY_DARK else SettingsKeys.THEME_SECONDARY_LIGHT
                                val current = if (isDarkTheme) customColors.secondaryDark else customColors.secondaryLight
                                onOpenColorPicker("Secondary Color\n• Secondary buttons\n• Icon tints\n• Accent elements\n• Toggle switches", current) { color -> onColorChange(key, color) }
                            }
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isDarkTheme) customColors.secondaryDark else customColors.secondaryLight
                        )
                        AutoText(
                            "Settings & navigation icons"
                        )
                    }
                    }
                } // End BackgroundColorProvider
            }

            val genderCardColor = if (isDarkTheme) customColors.surfaceDark else customColors.surfaceLight
            Card(
                colors = CardDefaults.cardColors(containerColor = genderCardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackgroundColorProvider(backgroundColor = genderCardColor) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AutoText(
                            "Gender Colors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            onClick = {
                                val current = if (isDarkTheme) customColors.maleColorDark else customColors.maleColorLight
                                onOpenColorPicker("Male Gender Color\n• Male cow cards\n• Bull indicators\n• Male-specific backgrounds", current) { color ->
                                    val key = if (isDarkTheme) SettingsKeys.THEME_MALE_COLOR_DARK else SettingsKeys.THEME_MALE_COLOR_LIGHT
                                    onColorChange(key, color)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) customColors.maleColorDark else customColors.maleColorLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Male,
                                    contentDescription = "Male",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Male",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Card(
                            onClick = {
                                val current = if (isDarkTheme) customColors.femaleColorDark else customColors.femaleColorLight
                                onOpenColorPicker("Female Gender Color\n• Female cow cards\n• Cow indicators\n• Female-specific backgrounds", current) { color ->
                                    val key = if (isDarkTheme) SettingsKeys.THEME_FEMALE_COLOR_DARK else SettingsKeys.THEME_FEMALE_COLOR_LIGHT
                                    onColorChange(key, color)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) customColors.femaleColorDark else customColors.femaleColorLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Female,
                                    contentDescription = "Female",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Female",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Card(
                            onClick = {
                                val current = if (isDarkTheme) customColors.tbdColorDark else customColors.tbdColorLight
                                onOpenColorPicker("Unknown Gender Color\n• TBD cow cards\n• Unknown status indicators\n• Neutral backgrounds", current) { color ->
                                    val key = if (isDarkTheme) SettingsKeys.THEME_TBD_COLOR_DARK else SettingsKeys.THEME_TBD_COLOR_LIGHT
                                    onColorChange(key, color)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) customColors.tbdColorDark else customColors.tbdColorLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Help,
                                    contentDescription = "TBD",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "TBD",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    

                    }
                } // End BackgroundColorProvider
            }
        }
        } // End BackgroundColorProvider
    }
}