package com.jumblemint.cows.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
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
                            modifier = Modifier.heightIn(min = SegmentedButtonMinHeight),
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
// TODO: Allow saving custom color bundles as sharable presets for team members.
// TODO: Add optional haptic feedback and snackbars for critical theme actions to reinforce success.

@Composable
private const val COLOR_GRID_MAX_ROWS = 3
private val SegmentedButtonMinHeight = 44.dp

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
private fun BaseColorGrid(
    colors: List<SeedColor>,
    currentSeedColor: SeedColor,
    onPick: (SeedColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = remember(colors) { colors.chunked(5) }
    val scrollState = rememberScrollState()
    val gridHeight = remember { 84.dp * COLOR_GRID_MAX_ROWS }
    Box(modifier = modifier.heightIn(max = gridHeight)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp)
                .heightIn(max = gridHeight)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { row ->
                TintRow(
                    currentSeedColor = currentSeedColor,
                    row = row,
                    onPick = onPick
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(4.dp)
                .padding(vertical = 8.dp)
        ) {
            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            val trackWidth = size.width
            drawRoundRect(
                color = trackColor,
                size = Size(trackWidth, size.height),
                cornerRadius = CornerRadius(trackWidth / 2f)
            )

            if (scrollState.maxValue > 0) {
                val viewportHeightPx = size.height
                val contentHeightPx = scrollState.maxValue.toFloat() + viewportHeightPx
                val indicatorHeight = (viewportHeightPx * (viewportHeightPx / contentHeightPx))
                    .coerceAtLeast(trackWidth * 2f)
                val fraction = scrollState.value / scrollState.maxValue.toFloat()
                val indicatorTop = (viewportHeightPx - indicatorHeight) * fraction

                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(0f, indicatorTop),
                    size = Size(trackWidth, indicatorHeight),
                    cornerRadius = CornerRadius(trackWidth / 2f)
                )
            }
        }
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

private enum class GenderSwatch(val label: String, val dialogLabel: String) {
    Male(
        label = "Male",
        dialogLabel = "Male accent"
    ),
    Female(
        label = "Female",
        dialogLabel = "Female accent"
    ),
    Neutral(
        label = "TBD",
        dialogLabel = "Unknown accent"
    )
}

@Composable
private fun GenderSwatchChip(
    swatch: GenderSwatch,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier = modifier
            .widthIn(min = 72.dp)
            .alpha(alpha)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
        )
        Text(
            text = swatch.label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderColorPickerDialog(
    label: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }
    val controller = rememberColorPickerController()

    val wheelColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.outline

    com.jumblemint.cows.ui.components.AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust $label color") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Current choice",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.dp, borderColor, CircleShape)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HsvColorPicker(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            controller = controller,
                            initialColor = selectedColor,
                            onColorChanged = { envelope: ColorEnvelope ->
                                selectedColor = envelope.color
                            }
                        )
                        BrightnessSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            controller = controller,
                            initialColor = selectedColor,
                            borderRadius = 8.dp,
                            wheelRadius = 8.dp,
                            wheelColor = wheelColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedColor) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

    val seedColors = remember { SeedColor.entries.sortedBy { it.color.toHct().hue } }
    var genderSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var genderEditTarget by remember { mutableStateOf<GenderSwatch?>(null) }
    val currentPalette by rememberUpdatedState(newValue = themeSettings.genderPalette)
    val genderLocked by rememberUpdatedState(newValue = themeSettings.genderColorsLocked)

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

                        BaseColorGrid(
                            colors = seedColors,
                            currentSeedColor = themeSettings.seedColor,
                            onPick = { seed -> scope.launch { themeManager.setSeedColor(seed) } }
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

            // Advanced Options
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedSectionExpanded = !advancedSectionExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Advanced",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Fine-tune tones and specialty accents.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (advancedSectionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (advancedSectionExpanded) "Collapse advanced options" else "Expand advanced options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = advancedSectionExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Tone Adjustments",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val toneOptions = listOf(
                                        false to "Reset tone with base color",
                                        true to "Keep custom tone"
                                    )
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        toneOptions.forEachIndexed { index, (locked, label) ->
                                            SegmentedButton(
                                                modifier = Modifier.heightIn(min = SegmentedButtonMinHeight),
                                                shape = SegmentedButtonDefaults.itemShape(index, toneOptions.size),
                                                selected = themeSettings.toneLocked == locked,
                                                onClick = {
                                                    scope.launch {
                                                        themeManager.setToneLock(locked)
                                                    }
                                                },
                                                colors = SegmentedButtonDefaults.colors(
                                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Text(label)
                                            }
                                        }
                                    }

                                    val toneSupport = if (themeSettings.toneLocked) {
                                        "Tone sliders stay put even after changing the base color."
                                    } else {
                                        "We’ll reset these tones whenever you pick a new base color."
                                    }
                                    Text(
                                        toneSupport,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { scope.launch { themeManager.resetToneToDefaults() } }) {
                                            Text("Reset tone to defaults")
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.fillMaxWidth())

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { genderSectionExpanded = !genderSectionExpanded },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Edit Gender Colors",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
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

                                            val genderColorOptions = listOf(
                                                false to "Use theme colors",
                                                true to "Use custom colors"
                                            )
                                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                                genderColorOptions.forEachIndexed { index, (locked, label) ->
                                                    SegmentedButton(
                                                        modifier = Modifier.heightIn(min = SegmentedButtonMinHeight),
                                                        shape = SegmentedButtonDefaults.itemShape(index, genderColorOptions.size),
                                                        selected = themeSettings.genderColorsLocked == locked,
                                                        onClick = {
                                                            scope.launch {
                                                                genderEditTarget = null
                                                                themeManager.setGenderColorsLock(locked)
                                                            }
                                                        },
                                                        colors = SegmentedButtonDefaults.colors(
                                                            activeContainerColor = MaterialTheme.colorScheme.primary,
                                                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    ) {
                                                        Text(label)
                                                    }
                                                }
                                            }

                                            val supportText = if (themeSettings.genderColorsLocked) {
                                                "Custom colors stay put when you pick a new base color."
                                            } else {
                                                "Theme colors update automatically whenever you change the base color."
                                            }
                                            Text(
                                                text = supportText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Divider(modifier = Modifier.fillMaxWidth())

                                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Text(
                                                    "Tap a swatch to fine-tune that accent color.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    GenderSwatch.values().forEach { swatch ->
                                                        val swatchColor = when (swatch) {
                                                            GenderSwatch.Male -> themeSettings.genderPalette.male
                                                            GenderSwatch.Female -> themeSettings.genderPalette.female
                                                            GenderSwatch.Neutral -> themeSettings.genderPalette.neutral
                                                        }
                                                        GenderSwatchChip(
                                                            swatch = swatch,
                                                            color = swatchColor,
                                                            enabled = themeSettings.genderColorsLocked,
                                                            onClick = { genderEditTarget = swatch }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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

    genderEditTarget?.let { target ->
        val initialColor = when (target) {
            GenderSwatch.Male -> currentPalette.male
            GenderSwatch.Female -> currentPalette.female
            GenderSwatch.Neutral -> currentPalette.neutral
        }
        GenderColorPickerDialog(
            label = target.dialogLabel,
            initialColor = initialColor,
            onDismiss = { genderEditTarget = null },
            onConfirm = { chosenColor ->
                val updatedPalette = when (target) {
                    GenderSwatch.Male -> currentPalette.copy(male = chosenColor)
                    GenderSwatch.Female -> currentPalette.copy(female = chosenColor)
                    GenderSwatch.Neutral -> currentPalette.copy(neutral = chosenColor)
                }
                scope.launch {
                    themeManager.setGenderPalette(updatedPalette)
                    if (!genderLocked) {
                        themeManager.setGenderColorsLock(true)
                    }
                }
                genderEditTarget = null
            }
        )
    }
}

// TODO: Offer a mini preview carousel so users can see palette changes on actual list tiles.
// TODO: Allow saving custom color bundles as sharable presets for team members.
// TODO: Add optional haptic feedback and snackbars for critical theme actions to reinforce success.
