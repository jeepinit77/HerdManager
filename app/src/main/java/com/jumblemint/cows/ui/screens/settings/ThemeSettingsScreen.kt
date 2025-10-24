package com.jumblemint.cows.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
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
    val scope = rememberCoroutineScope()

    val systemDark = isSystemInDarkTheme()
    var currentMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var previewDark by remember { mutableStateOf(systemDark) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var resetCount by remember { mutableStateOf(0) }

    LaunchedEffect(themeSettings.mode) {
        currentMode = themeSettings.mode
        previewDark = when (themeSettings.mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemDark
        }
    }

    if (showResetConfirm) {
        com.jumblemint.cows.ui.components.AppAlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { 
                Icon(
                    Icons.Filled.WarningAmber, 
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                ) 
            },
            title = { Text("Reset Theme?") },
            text = { Text("This will reset the theme to the default settings. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            themeManager.resetToDefaults()
                            currentMode = ThemeMode.SYSTEM
                            previewDark = systemDark
                            resetCount++
                            showResetConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Reset") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { showResetConfirm = false }) { 
                    Text("Cancel") 
                }
            }
        )
    }

    Column(modifier.fillMaxSize()) {
        // Mode switcher
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("App Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = listOf(
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                        ThemeMode.SYSTEM to "System"
                    )
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
            themeSettings = themeSettings,
            isDarkTheme = previewDark,
            scope = scope,
            resetCount = resetCount,
            onShowResetConfirm = { showResetConfirm = true },
            modifier = Modifier.weight(1f)
        )
    }
}

// TODO: Offer a mini preview carousel so users can see palette changes on actual list tiles.
// TODO: Surface accessibility hints when chosen colors fall below recommended contrast ratios.
// TODO: Add optional haptic feedback and snackbars for critical theme actions to reinforce success.

@Composable
private fun TintRow(
    currentSeedColor: SeedColor,
    row: List<SeedColor>,
    onPick: (SeedColor) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { seedColor ->
            PresetChip(
                isSelected = currentSeedColor == seedColor,
                name = seedColor.displayName,
                color = seedColor.color,
                onClick = { onPick(seedColor) },
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
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(20.dp)
                )
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun GenderPaletteDots(
    palette: GenderColorPalette,
    size: Dp = 14.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(palette.male, palette.female, palette.neutral).forEach { color ->
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun GenderPaletteOption(
    palette: GenderColorPalette,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier.widthIn(min = 180.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GenderPaletteDots(palette = palette, size = 28.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GenderLegend(label = "M", color = palette.male)
                GenderLegend(label = "F", color = palette.female)
                GenderLegend(label = "U", color = palette.neutral)
            }
            Text(
                text = if (isSelected) "Currently applied" else "Tap to apply",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun GenderLegend(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                    }
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}



@Composable
private fun ThemePicker(
    themeManager: ThemeManager,
    themeSettings: ThemeSettings,
    isDarkTheme: Boolean,
    scope: CoroutineScope,
    resetCount: Int,
    onShowResetConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var navBarTone by remember { mutableFloatStateOf(50f) }
    var surfaceTone by remember { mutableFloatStateOf(90f) }
    var style by remember(resetCount) { mutableStateOf(themeSettings.style) }
    
    LaunchedEffect(themeSettings.style) {
        style = themeSettings.style
    }

    LaunchedEffect(themeSettings, isDarkTheme) {
        navBarTone = themeSettings.getNavBarTone(isDarkTheme)
        surfaceTone = themeSettings.getSurfaceTone(isDarkTheme)
    }

    val previewSettings = themeSettings.copy(style = style)

    // Generate current theme colors for preview
    val currentColorScheme = generateThemeFromSeed(
        themeSettings = previewSettings,
        surfaceTone = surfaceTone,
        navBarTone = navBarTone,
        isDark = isDarkTheme,
        style = style
    )

    // Generate preview colors for each style
    val coloredCardsScheme = generateThemeFromSeed(
        themeSettings = previewSettings.copy(style = ThemeStyle.COLORED_CARDS),
        surfaceTone = surfaceTone,
        navBarTone = navBarTone,
        isDark = isDarkTheme,
        style = ThemeStyle.COLORED_CARDS
    )
    val coloredBgScheme = generateThemeFromSeed(
        themeSettings = previewSettings.copy(style = ThemeStyle.COLORED_BACKGROUND),
        surfaceTone = surfaceTone,
        navBarTone = navBarTone,
        isDark = isDarkTheme,
        style = ThemeStyle.COLORED_BACKGROUND
    )
    val grayCardsScheme = generateThemeFromSeed(
        themeSettings = previewSettings.copy(style = ThemeStyle.GRAY_CARDS),
        surfaceTone = surfaceTone,
        navBarTone = navBarTone,
        isDark = isDarkTheme,
        style = ThemeStyle.GRAY_CARDS
    )
    val grayBgScheme = generateThemeFromSeed(
        themeSettings = previewSettings.copy(style = ThemeStyle.GRAY_BACKGROUND),
        surfaceTone = surfaceTone,
        navBarTone = navBarTone,
        isDark = isDarkTheme,
        style = ThemeStyle.GRAY_BACKGROUND
    )

    val orderedSeedColors = remember { SeedColor.entries.toList() }
    val primaryRows = remember(orderedSeedColors) { orderedSeedColors.take(10).chunked(5) }
    val extraRows = remember(orderedSeedColors) { orderedSeedColors.drop(10).chunked(5) }
    var showFullPalette by rememberSaveable(themeSettings.seedColor.name) { mutableStateOf(false) }
    var genderSectionExpanded by rememberSaveable { mutableStateOf(false) }
    val genderPaletteOptions = remember(themeSettings.seedColor) { themeSettings.seedColor.genderPaletteOptions() }
    val paletteChoices = remember(themeSettings.genderPalette, genderPaletteOptions) {
        (listOf(themeSettings.genderPalette) + genderPaletteOptions).distinctBy { it.signature() }
    }

    Box(modifier.fillMaxSize().background(currentColorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Choose Color Scheme
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Choose Color Scheme",
                            style = MaterialTheme.typography.titleMedium
                        )

                        primaryRows.forEach { row ->
                            TintRow(
                                row = row,
                                currentSeedColor = themeSettings.seedColor,
                                onPick = { scope.launch { themeManager.setSeedColor(it) } }
                            )
                        }

                        if (extraRows.isNotEmpty()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showFullPalette = !showFullPalette }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (showFullPalette) "Hide full palette" else "Show full palette",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    if (showFullPalette) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (showFullPalette) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showFullPalette,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    extraRows.forEach { row ->
                                        TintRow(
                                            row = row,
                                            currentSeedColor = themeSettings.seedColor,
                                            onPick = { scope.launch { themeManager.setSeedColor(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            "Navigation Bar Tone (${if (isDarkTheme) "Dark" else "Light"} Mode)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = navBarTone,
                            onValueChange = { navBarTone = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    if (isDarkTheme) {
                                        themeManager.setNavBarToneDark(navBarTone)
                                    } else {
                                        themeManager.setNavBarToneLight(navBarTone)
                                    }
                                }
                            },
                            valueRange = 10f..90f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.onSurface,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Theme Style
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Theme Style",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhoneStyleButton(
                                title = "Colored Cards",
                                bgColor = coloredCardsScheme.background,
                                cardColor = coloredCardsScheme.surfaceVariant,
                                navColor = MaterialTheme.colorScheme.primary,
                                isSelected = style == ThemeStyle.COLORED_CARDS,
                                selectedColor = MaterialTheme.colorScheme.primary,
                                onClick = { 
                                    style = ThemeStyle.COLORED_CARDS
                                    scope.launch { themeManager.setThemeStyle(ThemeStyle.COLORED_CARDS) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PhoneStyleButton(
                                title = "Colored Background",
                                bgColor = coloredBgScheme.background,
                                cardColor = coloredBgScheme.surfaceVariant,
                                navColor = MaterialTheme.colorScheme.primary,
                                isSelected = style == ThemeStyle.COLORED_BACKGROUND,
                                selectedColor = MaterialTheme.colorScheme.primary,
                                onClick = { 
                                    style = ThemeStyle.COLORED_BACKGROUND
                                    scope.launch { themeManager.setThemeStyle(ThemeStyle.COLORED_BACKGROUND) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PhoneStyleButton(
                                title = "Gray Cards",
                                bgColor = grayCardsScheme.background,
                                cardColor = grayCardsScheme.surfaceVariant,
                                navColor = MaterialTheme.colorScheme.primary,
                                isSelected = style == ThemeStyle.GRAY_CARDS,
                                selectedColor = MaterialTheme.colorScheme.primary,
                                onClick = { 
                                    style = ThemeStyle.GRAY_CARDS
                                    scope.launch { themeManager.setThemeStyle(ThemeStyle.GRAY_CARDS) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PhoneStyleButton(
                                title = "Gray Background",
                                bgColor = grayBgScheme.background,
                                cardColor = grayBgScheme.surfaceVariant,
                                navColor = MaterialTheme.colorScheme.primary,
                                isSelected = style == ThemeStyle.GRAY_BACKGROUND,
                                selectedColor = MaterialTheme.colorScheme.primary,
                                onClick = { 
                                    style = ThemeStyle.GRAY_BACKGROUND
                                    scope.launch { themeManager.setThemeStyle(ThemeStyle.GRAY_BACKGROUND) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val toneLabel = if (style == ThemeStyle.COLORED_CARDS || style == ThemeStyle.GRAY_CARDS) {
                            "Card Tone"
                        } else {
                            "Background Tone"
                        }
                        Text(
                            "$toneLabel (${if (isDarkTheme) "Dark" else "Light"} Mode)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = surfaceTone,
                            onValueChange = { surfaceTone = it },
                            onValueChangeFinished = {
                                scope.launch {
                                    if (isDarkTheme) {
                                        themeManager.setSurfaceToneDark(surfaceTone)
                                    } else {
                                        themeManager.setSurfaceToneLight(surfaceTone)
                                    }
                                }
                            },
                            valueRange = 10f..98f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.onSurface,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Gender Colors
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { genderSectionExpanded = !genderSectionExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Edit Gender Colors",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Controls the highlights for male, female, and unknown animals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            GenderPaletteDots(palette = themeSettings.genderPalette)
                            Icon(
                                if (genderSectionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (genderSectionExpanded) "Collapse gender colors" else "Expand gender colors",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = genderSectionExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "These accents appear on herd lists, filter chips, and detail screens.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = themeSettings.genderColorsLocked,
                                        onCheckedChange = { checked ->
                                            scope.launch { themeManager.setGenderColorsLock(checked) }
                                        }
                                    )
                                    Text(
                                        "Don't change gender colors when base theme color changes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    paletteChoices.forEach { palette ->
                                        GenderPaletteOption(
                                            palette = palette,
                                            isSelected = themeSettings.genderPalette.signature() == palette.signature(),
                                            onSelect = {
                                                scope.launch { themeManager.setGenderPalette(palette) }
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = if (themeSettings.genderColorsLocked) {
                                        "Locked palettes stay put even when you experiment with new base colors."
                                    } else {
                                        "Unlocking lets each theme suggest fresh gender colors automatically."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Reset Button
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onShowResetConfirm,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset to Defaults")
                        }
                    }
                }
            }
        }
    }
}

// TODO: Offer a mini preview carousel so users can see palette changes on actual list tiles.
// TODO: Surface accessibility hints when chosen colors fall below recommended contrast ratios.
// TODO: Add optional haptic feedback and snackbars for critical theme actions to reinforce success.
