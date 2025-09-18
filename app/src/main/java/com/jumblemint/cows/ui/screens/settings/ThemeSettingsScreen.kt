package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.ColorPickerDialog
import com.github.skydoves.colorpicker.compose.*
import com.jumblemint.cows.ui.theme.CustomColors
import com.jumblemint.cows.ui.theme.PresetTheme
import com.jumblemint.cows.ui.theme.ThemeManager
import com.jumblemint.cows.ui.theme.getColors
import com.jumblemint.cows.ui.viewmodel.SettingsViewModel
import com.jumblemint.cows.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
        CattleRepository(
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
    }
    
    val themeManager = remember { ThemeManager(repository) }
    val customColors by themeManager.getCustomColors().collectAsState(initial = CustomColors())
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf("Presets", "Light Mode", "Dark Mode")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var currentPreset by remember { mutableStateOf(PresetTheme.DEFAULT) }
    
    LaunchedEffect(Unit) {
        currentPreset = themeManager.getCurrentPreset()
    }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTitle by remember { mutableStateOf("") }
    var colorPickerCurrent by remember { mutableStateOf(Color.White) }
    var colorPickerCallback by remember { mutableStateOf<(Color) -> Unit>({}) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (page) {
                0 -> {
                    item {
                        Text(
                            "Theme Presets",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        Text(
                            "Choose from pre-designed themes or customize your own",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    items(PresetTheme.values().filter { it != PresetTheme.CUSTOM }) { preset ->
                        PresetThemeCard(
                            preset = preset,
                            isSelected = currentPreset == preset,
                            onClick = {
                                coroutineScope.launch {
                                    themeManager.applyPresetTheme(preset)
                                    currentPreset = preset
                                }
                            }
                        )
                    }
                    item {
                        PresetThemeCard(
                            preset = PresetTheme.CUSTOM,
                            isSelected = currentPreset == PresetTheme.CUSTOM,
                            onClick = {
                                coroutineScope.launch {
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            }
                        )
                    }
                }
                1 -> {
                    item {
                        Text(
                            "Light Mode Colors",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item { 
                        ColorSection(
                            "Primary", 
                            customColors.primaryLight, 
                            "Buttons, links, highlights",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_PRIMARY_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Secondary", 
                            customColors.secondaryLight, 
                            "Secondary elements, icons",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_SECONDARY_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Tertiary", 
                            customColors.tertiaryLight, 
                            "Accent elements, special highlights",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_TERTIARY_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            "Gender Colors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    item { 
                        ColorSection(
                            "Male", 
                            customColors.maleColorLight, 
                            "Used for male cattle indicators",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_MALE_COLOR_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Female", 
                            customColors.femaleColorLight, 
                            "Used for female cattle indicators",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "TBD", 
                            customColors.tbdColorLight, 
                            "Used for undetermined gender",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_TBD_COLOR_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            "Background Colors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    item { 
                        ColorSection(
                            "Background", 
                            customColors.backgroundLight, 
                            "Main screen background color",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_BACKGROUND_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Surface", 
                            customColors.surfaceLight, 
                            "Card and component background color",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_SURFACE_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Card Background", 
                            customColors.cardBackgroundLight, 
                            "Background color for cards",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { ExampleSection("Light Mode", customColors.primaryLight, customColors.secondaryLight, customColors.tertiaryLight, customColors.maleColorLight, customColors.femaleColorLight, customColors.tbdColorLight, true) }
                }
                2 -> {
                    item {
                        Text(
                            "Dark Mode Colors",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item { 
                        ColorSection(
                            "Primary", 
                            customColors.primaryDark, 
                            "Buttons, links, highlights",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_PRIMARY_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Secondary", 
                            customColors.secondaryDark, 
                            "Secondary elements, icons",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_SECONDARY_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Tertiary", 
                            customColors.tertiaryDark, 
                            "Accent elements, special highlights",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_TERTIARY_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            "Gender Colors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    item { 
                        ColorSection(
                            "Male", 
                            customColors.maleColorDark, 
                            "Used for male cattle indicators",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_MALE_COLOR_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Female", 
                            customColors.femaleColorDark, 
                            "Used for female cattle indicators",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_FEMALE_COLOR_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "TBD", 
                            customColors.tbdColorDark, 
                            "Used for undetermined gender",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_TBD_COLOR_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            "Background Colors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    item { 
                        ColorSection(
                            "Background", 
                            customColors.backgroundDark, 
                            "Main screen background color",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_BACKGROUND_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Surface", 
                            customColors.surfaceDark, 
                            "Card and component background color",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_SURFACE_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { 
                        ColorSection(
                            "Card Background", 
                            customColors.cardBackgroundDark, 
                            "Background color for cards",
                            onColorChange = { color ->
                                coroutineScope.launch {
                                    themeManager.updateColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, color)
                                    currentPreset = PresetTheme.CUSTOM
                                }
                            },
                            onOpenColorPicker = { title, current, callback ->
                                colorPickerTitle = title
                                colorPickerCurrent = current
                                colorPickerCallback = callback
                                showColorPicker = true
                            }
                        ) 
                    }
                    item { ExampleSection("Dark Mode", customColors.primaryDark, customColors.secondaryDark, customColors.tertiaryDark, customColors.maleColorDark, customColors.femaleColorDark, customColors.tbdColorDark, false) }
                }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                themeManager.resetToDefaults()
                                currentPreset = PresetTheme.DEFAULT
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to Default")
                    }
                }
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
private fun ThemePreviewItem(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    text: String,
    textColor: Color,
    isOutlined: Boolean = false,
    borderColor: Color = Color.Transparent // Used if isOutlined is true
) {
    Surface(
        modifier = modifier.height(IntrinsicSize.Min),
        shape = RoundedCornerShape(6.dp), // Slightly more rounded
        color = backgroundColor,
        border = if (isOutlined) BorderStroke(1.dp, borderColor) else null,
        shadowElevation = 1.dp // Add a little depth
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp) // Adjusted padding
        ) {
            Text(text, fontSize = 9.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis) // Slightly larger font
        }
    }
}

@Composable
private fun PresetThemeCard(
    preset: PresetTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = preset.getColors()
    // Helper to determine text color for "Other" card based on background luminance
    val otherTextColorLight = if (colors.cardBackgroundLight.luminance() > 0.5f) Color(0xFF191C1E) else Color.White
    val otherTextColorDark = if (colors.cardBackgroundDark.luminance() > 0.5f) Color(0xFF191C1E) else Color.White

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    preset.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold // Bolder title
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle, // Changed to filled check circle
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp), // Slightly increased height
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Light mode preview (left half)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.backgroundLight)
                            .padding(6.dp), // Increased padding
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                             Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(6.dp) // Increased spacing
                            ) {
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.primaryLight,
                                    text = "Primary",
                                    textColor = if (colors.primaryLight.luminance() > 0.5f) Color.Black else Color.White
                                )
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.surfaceLight, // Use surface for outlined bg
                                    text = "Secondary",
                                    textColor = colors.secondaryLight,
                                    isOutlined = true,
                                    borderColor = colors.secondaryLight
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp) // spacing for smaller items
                            ) {
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.maleColorLight,
                                    text = "Male",
                                    textColor = if (colors.maleColorLight.luminance() > 0.5f) Color.Black else Color.White
                                )
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.femaleColorLight,
                                    text = "Female",
                                    textColor = if (colors.femaleColorLight.luminance() > 0.5f) Color.Black else Color.White
                                )
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.cardBackgroundLight,
                                    text = "Card",
                                    textColor = otherTextColorLight
                                )
                            }
                        }
                    }

                    // Dark mode preview (right half)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.backgroundDark)
                            .padding(6.dp), // Increased padding
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(6.dp) // Increased spacing
                            ) {
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.primaryDark,
                                    text = "Primary",
                                    textColor = if (colors.primaryDark.luminance() > 0.5f) Color.Black else Color.White
                                )
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.surfaceDark, // Use surface for outlined bg
                                    text = "Secondary",
                                    textColor = colors.secondaryDark,
                                    isOutlined = true,
                                    borderColor = colors.secondaryDark
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.maleColorDark,
                                    text = "Male",
                                    textColor = if (colors.maleColorDark.luminance() > 0.5f) Color.Black else Color.White
                                )
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.femaleColorDark,
                                    text = "Female",
                                    textColor = if (colors.femaleColorDark.luminance() > 0.5f) Color.Black else Color.White
                                )
                                ThemePreviewItem(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    backgroundColor = colors.cardBackgroundDark,
                                    text = "Card",
                                    textColor = otherTextColorDark
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
private fun RowScope.ColorPreview(color: Color, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ColorSection(
    name: String,
    currentColor: Color,
    description: String,
    onColorChange: (Color) -> Unit = {},
    onOpenColorPicker: (String, Color, (Color) -> Unit) -> Unit = { _, _, _ -> }
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onOpenColorPicker("Select $name Color", currentColor, onColorChange) }
                )
            }
        }
    }
}

@Composable
private fun ExampleSection(
    mode: String,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    maleColor: Color,
    femaleColor: Color,
    tbdColor: Color,
    isLightMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightMode) Color(0xFFFAFAFA) else Color(0xFF1C1C1E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$mode Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (isLightMode) Color.Black else Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text("Primary Button")
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, secondary)
                ) {
                    Text("Secondary", color = secondary)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Gender Color Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (isLightMode) Color.Black else Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CowCardPreview("Male Bull", "B001", maleColor, isLightMode)
                CowCardPreview("Female Cow", "C001", femaleColor, isLightMode)
                CowCardPreview("TBD Calf", "K001", tbdColor, isLightMode)
            }
        }
    }
}

@Composable
private fun CowCardPreview(
    name: String,
    tagNumber: String,
    genderColor: Color,
    isLightMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = genderColor
        ),
        border = BorderStroke(1.dp, if (isLightMode) Color.Gray.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp, 40.dp),
                color = genderColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        tagNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (genderColor.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isLightMode) Color.Black else Color.White
                )
                Text(
                    "Sample cow card",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLightMode) Color.Gray else Color.LightGray
                )
            }
        }
    }
}
