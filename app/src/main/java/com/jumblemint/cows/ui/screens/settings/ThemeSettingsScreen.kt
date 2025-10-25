package com.jumblemint.cows.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

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
// TODO: Allow saving custom color bundles as sharable presets for team members.
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BaseColorPager(
    pagerState: PagerState,
    pages: List<PalettePage>,
    currentSeedColor: SeedColor,
    onPick: (SeedColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val canScrollBackward = pagerState.currentPage > 0 && pages.isNotEmpty()
    val canScrollForward = pages.isNotEmpty() && pagerState.currentPage < pages.lastIndex

    Column(modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                pageSpacing = 12.dp,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) { page ->
                val palettePage = pages.getOrNull(page)
                val rows = (palettePage?.colors ?: emptyList()).chunked(5)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rows.forEach { row ->
                        TintRow(
                            currentSeedColor = currentSeedColor,
                            row = row,
                            onPick = onPick
                        )
                    }
                    if (rows.size == 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (canScrollBackward) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Scroll left for more colors",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                )
            }
            if (canScrollForward) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Scroll right for more colors",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(4.dp)
                            .width(if (isSelected) 28.dp else 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            Text(
                "Swipe sideways to explore more colors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center
            )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(min = 72.dp)
            .clickable(onClick = onClick),
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

    val colorsPerPage = 10
    val colorPages = remember {
        SeedColor.entries
            .sortedBy { it.color.toHct().hue }
            .chunked(colorsPerPage)
            .map { chunk -> PalettePage(colors = chunk) }
    }
    var genderSectionExpanded by rememberSaveable { mutableStateOf(false) }
    val genderPaletteOptions = remember(themeSettings.seedColor) { themeSettings.seedColor.genderPaletteOptions() }
    val paletteChoices = remember(themeSettings.genderPalette, genderPaletteOptions) {
        (listOf(themeSettings.genderPalette) + genderPaletteOptions).distinctBy { it.signature() }
    }
    var genderEditTarget by remember { mutableStateOf<GenderSwatch?>(null) }
    val currentPalette by rememberUpdatedState(newValue = themeSettings.genderPalette)
    val genderLocked by rememberUpdatedState(newValue = themeSettings.genderColorsLocked)

    val pagerState = rememberPagerState(pageCount = { colorPages.size.coerceAtLeast(1) })

    LaunchedEffect(themeSettings.seedColor, colorPages) {
        val targetPage = colorPages.indexOfFirst { page -> themeSettings.seedColor in page.colors }
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val contrastWarnings = remember(currentColorScheme) {
        listOf(
            ContrastIssue(
                area = "Background",
                ratio = contrastRatio(currentColorScheme.background, currentColorScheme.onBackground)
            ),
            ContrastIssue(
                area = "Cards",
                ratio = contrastRatio(currentColorScheme.surfaceVariant, currentColorScheme.onSurfaceVariant)
            ),
            ContrastIssue(
                area = "Navigation bar",
                ratio = contrastRatio(currentColorScheme.surface, currentColorScheme.onSurface)
            )
        ).filter { it.ratio < 4.5 }
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

                        BaseColorPager(
                            pagerState = pagerState,
                            pages = colorPages,
                            currentSeedColor = themeSettings.seedColor,
                            onPick = { seed -> scope.launch { themeManager.setSeedColor(seed) } }
                        )

                        if (contrastWarnings.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.WarningAmber,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Low contrast alert",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    contrastWarnings.forEach { issue ->
                                        Text(
                                            text = "• ${issue.area} text contrast is ${issue.ratio.formatRatio()}:1 (recommended 4.5:1)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        "Try a deeper base color or adjust the card and nav tones for better readability.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
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
                                        if (themeSettings.genderColorsLocked) {
                                            "Keep the gender colors I set here when I pick a new base color"
                                        } else {
                                            "Update gender colors automatically when I choose a new base color"
                                        },
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
                                        "Locked palettes stay put when you explore new base colors."
                                    } else {
                                        "Turn lock on to keep the custom colors you dial in here."
                                    },
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
                                            GenderSwatchChip(
                                                swatch = swatch,
                                                color = when (swatch) {
                                                    GenderSwatch.Male -> themeSettings.genderPalette.male
                                                    GenderSwatch.Female -> themeSettings.genderPalette.female
                                                    GenderSwatch.Neutral -> themeSettings.genderPalette.neutral
                                                },
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

private data class ContrastIssue(val area: String, val ratio: Double)

private data class PalettePage(val colors: List<SeedColor>)

private fun contrastRatio(colorA: Color, colorB: Color): Double {
    val luminanceA = colorA.luminance()
    val luminanceB = colorB.luminance()
    val (lighter, darker) = if (luminanceA >= luminanceB) luminanceA to luminanceB else luminanceB to luminanceA
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Double.formatRatio(): String = String.format(Locale.US, "%.1f", this)

// TODO: Offer a mini preview carousel so users can see palette changes on actual list tiles.
// TODO: Allow saving custom color bundles as sharable presets for team members.
// TODO: Add optional haptic feedback and snackbars for critical theme actions to reinforce success.
