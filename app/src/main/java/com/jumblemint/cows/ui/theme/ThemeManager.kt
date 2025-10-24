package com.jumblemint.cows.ui.theme


import android.graphics.Color as AndroidColor
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.text.toBooleanStrictOrNull

// ---- HCT Color Utils ----
data class HctColor(val hue: Float, val chroma: Float, val tone: Float) {
    fun toColor(): Color {
        // Simplified HCT to RGB - adjust tone while preserving hue/chroma
        val normalizedTone = (tone / 100f).coerceIn(0f, 1f)
        val normalizedChroma = (chroma / 100f).coerceIn(0f, 1f)
        val normalizedHue = hue % 360f
        
        // Convert to HSV then RGB for better color accuracy
        val saturation = normalizedChroma
        val value = normalizedTone
        
        val c = value * saturation
        val x = c * (1 - kotlin.math.abs((normalizedHue / 60f) % 2 - 1))
        val m = value - c
        
        val (r, g, b) = when {
            normalizedHue < 60 -> Triple(c, x, 0f)
            normalizedHue < 120 -> Triple(x, c, 0f)
            normalizedHue < 180 -> Triple(0f, c, x)
            normalizedHue < 240 -> Triple(0f, x, c)
            normalizedHue < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        
        return Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f)
        )
    }
}

fun Color.toHct(): HctColor {
    val r = red
    val g = green
    val b = blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60 * (((g - b) / delta) % 6)
        max == g -> 60 * (((b - r) / delta) + 2)
        else -> 60 * (((r - g) / delta) + 4)
    }.let { if (it < 0) it + 360 else it }
    
    val value = max
    val saturation = if (max == 0f) 0f else delta / max
    
    return HctColor(
        hue = hue,
        chroma = saturation * 100f,
        tone = value * 100f
    )
}

// ---- Contrast helpers ----
fun getContrastingTextColor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() > 0.3f) Color(0xFF1C1B1F) else Color(0xFFFEFBFF)
fun Color.contrastingTextColor(): Color = getContrastingTextColor(this)

data class GenderColorPalette(
    val male: Color,
    val female: Color,
    val neutral: Color
) {
    fun signature(): String = listOf(male.toArgb(), female.toArgb(), neutral.toArgb()).joinToString(separator = "_")
}

private enum class SeedFamily { COOL, BOTANICAL, WARM, EARTH, NEUTRAL }

private fun SeedColor.family(): SeedFamily = when (this) {
    SeedColor.DENIM_BLUE,
    SeedColor.SLATE_BLUE,
    SeedColor.SKY_NAVY,
    SeedColor.VERDANT_TEAL,
    SeedColor.COPPER_TEAL,
    SeedColor.MIDNIGHT_CLAY -> SeedFamily.COOL

    SeedColor.SAGE_GREEN,
    SeedColor.MOSS_OLIVE,
    SeedColor.TERRACOTTA_SAGE,
    SeedColor.FROSTED_FOREST -> SeedFamily.BOTANICAL

    SeedColor.BRICK_RED,
    SeedColor.CRANBERRY,
    SeedColor.CORAL_RUST,
    SeedColor.HONEY_GOLD,
    SeedColor.SUN_MIST,
    SeedColor.OCHRE_GLOW -> SeedFamily.WARM

    SeedColor.ESPRESSO_BROWN,
    SeedColor.CLAY_TAUPE,
    SeedColor.CEDARWOOD,
    SeedColor.SAND_SEA -> SeedFamily.EARTH

    SeedColor.SLATE,
    SeedColor.ASH_MIST,
    SeedColor.CHARCOAL_GOLD,
    SeedColor.LAVENDER_GRAPHITE,
    SeedColor.PEARL_INK -> SeedFamily.NEUTRAL
}

private data class AccentPalette(val secondary: Color, val tertiary: Color)

private fun SeedColor.accentPalette(): AccentPalette {
    val seedHct = color.toHct()
    val family = family()
    val secondaryHue = when (family) {
        SeedFamily.COOL -> (seedHct.hue + 160f) % 360f
        SeedFamily.BOTANICAL -> (seedHct.hue + 135f) % 360f
        SeedFamily.WARM -> (seedHct.hue + 200f) % 360f
        SeedFamily.EARTH -> (seedHct.hue + 180f) % 360f
        SeedFamily.NEUTRAL -> (seedHct.hue + 40f) % 360f
    }
    val tertiaryHue = when (family) {
        SeedFamily.COOL -> (seedHct.hue + 20f) % 360f
        SeedFamily.BOTANICAL -> (seedHct.hue + 300f) % 360f
        SeedFamily.WARM -> (seedHct.hue + 330f) % 360f
        SeedFamily.EARTH -> (seedHct.hue + 45f) % 360f
        SeedFamily.NEUTRAL -> (seedHct.hue + 210f) % 360f
    }

    val secondary = HctColor(
        hue = secondaryHue,
        chroma = max(36f, seedHct.chroma * 0.75f),
        tone = 62f
    ).toColor()

    val tertiary = HctColor(
        hue = tertiaryHue,
        chroma = max(28f, seedHct.chroma * 0.55f),
        tone = 58f
    ).toColor()

    return AccentPalette(secondary, tertiary)
}

fun defaultGenderPalette(seedColor: SeedColor): GenderColorPalette {
    val hct = seedColor.color.toHct()
    return when (seedColor.family()) {
        SeedFamily.COOL -> GenderColorPalette(
            male = HctColor((hct.hue + 10f) % 360f, 52f, 48f).toColor(),
            female = HctColor((hct.hue + 320f) % 360f, 50f, 62f).toColor(),
            neutral = HctColor((hct.hue + 120f) % 360f, 26f, 68f).toColor()
        )

        SeedFamily.BOTANICAL -> GenderColorPalette(
            male = HctColor((hct.hue + 180f) % 360f, 40f, 50f).toColor(),
            female = HctColor((hct.hue + 310f) % 360f, 46f, 64f).toColor(),
            neutral = HctColor((hct.hue + 45f) % 360f, 24f, 70f).toColor()
        )

        SeedFamily.WARM -> GenderColorPalette(
            male = HctColor(210f, 42f, 48f).toColor(),
            female = HctColor((hct.hue + 10f) % 360f, 50f, 60f).toColor(),
            neutral = HctColor((hct.hue + 120f) % 360f, 30f, 72f).toColor()
        )

        SeedFamily.EARTH -> GenderColorPalette(
            male = HctColor(205f, 38f, 46f).toColor(),
            female = HctColor(340f, 42f, 60f).toColor(),
            neutral = HctColor((hct.hue + 90f) % 360f, 22f, 74f).toColor()
        )

        SeedFamily.NEUTRAL -> GenderColorPalette(
            male = HctColor(220f, 36f, 48f).toColor(),
            female = HctColor(325f, 46f, 62f).toColor(),
            neutral = HctColor((hct.hue + 100f) % 360f, 24f, 78f).toColor()
        )
    }
}

fun SeedColor.genderPaletteOptions(): List<GenderColorPalette> {
    val base = defaultGenderPalette(this)
    val vibrant = GenderColorPalette(
        male = base.male.toHct().copy(chroma = max(50f, base.male.toHct().chroma + 12f), tone = 52f).toColor(),
        female = base.female.toHct().copy(chroma = max(56f, base.female.toHct().chroma + 10f), tone = 64f).toColor(),
        neutral = base.neutral.toHct().copy(chroma = max(30f, base.neutral.toHct().chroma + 8f), tone = 72f).toColor()
    )

    val contrast = GenderColorPalette(
        male = base.male.toHct().copy(tone = 44f, chroma = max(45f, base.male.toHct().chroma)).toColor(),
        female = base.female.toHct().copy(hue = (base.female.toHct().hue + 6f) % 360f, tone = 68f).toColor(),
        neutral = base.neutral.toHct().copy(hue = (base.neutral.toHct().hue + 18f) % 360f, tone = 80f, chroma = 26f).toColor()
    )

    return listOf(base, vibrant, contrast).distinctBy { it.signature() }
}

private fun Color.adaptTone(isDark: Boolean, lightTone: Float, darkTone: Float): Color {
    val hct = toHct()
    val tone = if (isDark) darkTone else lightTone
    return hct.copy(tone = tone).toColor()
}

private fun Color.toHexString(): String = String.format("#%08X", toArgb())

private fun String.toColorOrNull(): Color? = runCatching {
    Color(AndroidColor.parseColor(this))
}.getOrNull()

// ---- Seed Colors ----
enum class SeedColor(val displayName: String, val color: Color) {
    // Cool & aquatic hues
    DENIM_BLUE("Denim", Color(0xFF3A6DA8)),
    SLATE_BLUE("Harbor", Color(0xFF4C6780)),
    SKY_NAVY("Navy", Color(0xFF2E4E73)),
    COPPER_TEAL("Lagoon", Color(0xFF2E7C78)),
    VERDANT_TEAL("Teal", Color(0xFF3C8C7A)),

    // Botanical greens
    SAGE_GREEN("Sage", Color(0xFF7A8F75)),
    MOSS_OLIVE("Olive", Color(0xFF6E7B4F)),
    FROSTED_FOREST("Frost", Color(0xFFE2F0E7)),

    // Warm sunrise tones
    HONEY_GOLD("Honey", Color(0xFFC9A33E)),
    SUN_MIST("Sun", Color(0xFFE5C861)),
    OCHRE_GLOW("Ochre", Color(0xFFDA9344)),
    CORAL_RUST("Coral", Color(0xFFD05B45)),
    BRICK_RED("Brick", Color(0xFFB84A4A)),
    CRANBERRY("Berry", Color(0xFFA73C56)),
    TERRACOTTA_SAGE("Terra", Color(0xFFB86B5C)),

    // Earthen neutrals
    CEDARWOOD("Cedar", Color(0xFF9C5A3A)),
    ESPRESSO_BROWN("Espresso", Color(0xFF4B342A)),
    CLAY_TAUPE("Clay", Color(0xFF8A7563)),
    SAND_SEA("Dune", Color(0xFFC1A57B)),

    // Moody & soft palettes
    SLATE("Slate", Color(0xFF586474)),
    CHARCOAL_GOLD("Charcoal", Color(0xFF3C3C3C)),
    MIDNIGHT_CLAY("Midnight", Color(0xFF374458)),
    LAVENDER_GRAPHITE("Lavender", Color(0xFF8D7FA9)),
    ASH_MIST("Ash", Color(0xFFCFCBC4)),
    PEARL_INK("Pearl", Color(0xFFF7F5F3))
}

// ---- Theme Settings ----
data class ThemeSettings(
    val seedColor: SeedColor = SeedColor.DENIM_BLUE,
    val navBarToneLight: Float = 80f,
    val navBarToneDark: Float = 20f,
    val surfaceToneLight: Float = 95f,
    val surfaceToneDark: Float = 15f,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val style: ThemeStyle = ThemeStyle.COLORED_BACKGROUND,
    val genderPalette: GenderColorPalette = defaultGenderPalette(SeedColor.DENIM_BLUE),
    val genderColorsLocked: Boolean = false
) {
    fun getNavBarTone(isDark: Boolean) = if (isDark) navBarToneDark else navBarToneLight
    fun getSurfaceTone(isDark: Boolean) = if (isDark) surfaceToneDark else surfaceToneLight
}

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class ThemeStyle { COLORED_CARDS, COLORED_BACKGROUND, GRAY_CARDS, GRAY_BACKGROUND }

// ---- Theme Generation ----
fun generateThemeFromSeed(
    themeSettings: ThemeSettings,
    surfaceTone: Float,
    navBarTone: Float,
    isDark: Boolean,
    style: ThemeStyle = themeSettings.style
): androidx.compose.material3.ColorScheme {
    val seedColor = themeSettings.seedColor
    val seed = seedColor.color
    val seedHct = seed.toHct()
    val accentPalette = seedColor.accentPalette()
    val secondary = accentPalette.secondary.adaptTone(isDark, lightTone = 66f, darkTone = 58f)
    val tertiary = accentPalette.tertiary.adaptTone(isDark, lightTone = 62f, darkTone = 55f)
    val secondaryContainer = accentPalette.secondary.adaptTone(isDark, lightTone = 86f, darkTone = 32f)
    val tertiaryContainer = accentPalette.tertiary.adaptTone(isDark, lightTone = 88f, darkTone = 34f)

    // Adjust tones based on theme mode
    val adjustedSurfaceTone = if (isDark) (100f - surfaceTone) * 0.2f else surfaceTone
    val adjustedNavTone = if (isDark) (100f - navBarTone) * 0.3f else navBarTone

    // Generate colors based on style
    val (surfaceVariant, background) = when (style) {
        ThemeStyle.COLORED_CARDS -> {
            val bg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
            val cards = seedHct.copy(
                tone = adjustedSurfaceTone,
                chroma = seedHct.chroma * 0.3f
            ).toColor()
            cards to bg
        }
        ThemeStyle.COLORED_BACKGROUND -> {
            val bg = seedHct.copy(
                tone = adjustedSurfaceTone,
                chroma = seedHct.chroma * 0.3f
            ).toColor()
            val cards = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
            cards to bg
        }
        ThemeStyle.GRAY_CARDS -> {
            val bg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
            val cards = HctColor(0f, 0f, adjustedSurfaceTone).toColor()
            cards to bg
        }
        ThemeStyle.GRAY_BACKGROUND -> {
            val bg = HctColor(0f, 0f, adjustedSurfaceTone).toColor()
            val cards = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
            cards to bg
        }
    }
    val navBarColor = seedHct.copy(tone = adjustedNavTone).toColor()
    
    // Create base scheme
    val baseScheme = if (isDark) {
        androidx.compose.material3.darkColorScheme(
            primary = seed,
            surface = navBarColor,
            surfaceVariant = surfaceVariant,
            background = seedHct.copy(tone = 6f, chroma = seedHct.chroma * 0.1f).toColor()
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = seed,
            surface = navBarColor,
            surfaceVariant = surfaceVariant,
            background = seedHct.copy(tone = 98f, chroma = seedHct.chroma * 0.1f).toColor()
        )
    }

    return baseScheme.copy(
        surfaceVariant = surfaceVariant,
        background = background,
        surface = navBarColor,
        surfaceContainer = navBarColor,
        surfaceContainerLow = surfaceVariant,
        surfaceContainerHigh = surfaceVariant,
        onPrimary = seed.contrastingTextColor(),
        secondary = secondary,
        onSecondary = secondary.contrastingTextColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastingTextColor(),
        tertiary = tertiary,
        onTertiary = tertiary.contrastingTextColor(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.contrastingTextColor(),
        onSurface = navBarColor.contrastingTextColor(),
        onSurfaceVariant = surfaceVariant.contrastingTextColor(),
        onBackground = background.contrastingTextColor(),
        outline = navBarColor.contrastingTextColor().copy(alpha = 0.6f),
        outlineVariant = navBarColor.contrastingTextColor().copy(alpha = 0.38f)
    )
}



// ---- Manager ----
class ThemeManager(private val repository: CattleRepository) {

    suspend fun getThemeMode(): ThemeMode =
        repository.getSettingByKey(SettingsKeys.THEME_MODE)?.value
            ?.let { runCatching { ThemeMode.valueOf(it.uppercase()) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    suspend fun setThemeMode(mode: ThemeMode) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_MODE, mode.name))
    }

    fun getThemeModeFlow(): Flow<ThemeMode> =
        repository.getAllSettings().map { settings ->
            settings.find { it.key == SettingsKeys.THEME_MODE }?.value
                ?.let { runCatching { ThemeMode.valueOf(it.uppercase()) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    // Seed Color
    suspend fun setSeedColor(seedColor: SeedColor) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.SEED_COLOR, seedColor.name))
        if (!isGenderColorsLocked()) {
            setGenderPalette(defaultGenderPalette(seedColor))
        }
    }

    fun getSeedColorFlow(): Flow<SeedColor> =
        repository.getAllSettings().map { settings ->
            settings.find { it.key == SettingsKeys.SEED_COLOR }?.value
                ?.let { runCatching { SeedColor.valueOf(it) }.getOrNull() }
                ?: SeedColor.DENIM_BLUE
        }

    // Light Mode Tones
    suspend fun setNavBarToneLight(tone: Float) {
        repository.insertOrUpdateSetting(Settings("NAV_BAR_TONE_LIGHT", tone.toString()))
    }
    
    suspend fun setSurfaceToneLight(tone: Float) {
        repository.insertOrUpdateSetting(Settings("SURFACE_TONE_LIGHT", tone.toString()))
    }
    
    // Dark Mode Tones
    suspend fun setNavBarToneDark(tone: Float) {
        repository.insertOrUpdateSetting(Settings("NAV_BAR_TONE_DARK", tone.toString()))
    }
    
    suspend fun setSurfaceToneDark(tone: Float) {
        repository.insertOrUpdateSetting(Settings("SURFACE_TONE_DARK", tone.toString()))
    }

    // Theme Style
    suspend fun setThemeStyle(style: ThemeStyle) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_STYLE, style.name))
    }

    // Theme Settings
    fun getThemeSettingsFlow(): Flow<ThemeSettings> =
        repository.getAllSettings().map { settings ->
            val map = settings.associate { it.key to it.value }

            val seedColor = map[SettingsKeys.SEED_COLOR]
                ?.let { runCatching { SeedColor.valueOf(it) }.getOrNull() }
                ?: SeedColor.DENIM_BLUE
                
            val mode = map[SettingsKeys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it.uppercase()) }.getOrNull() }
                ?: ThemeMode.SYSTEM
                
            val style = map[SettingsKeys.THEME_STYLE]
                ?.let { runCatching { ThemeStyle.valueOf(it) }.getOrNull() }
                ?: ThemeStyle.COLORED_BACKGROUND
                
            val fallbackPalette = defaultGenderPalette(seedColor)
            val genderPalette = GenderColorPalette(
                male = map[SettingsKeys.THEME_GENDER_MALE]?.toColorOrNull() ?: fallbackPalette.male,
                female = map[SettingsKeys.THEME_GENDER_FEMALE]?.toColorOrNull() ?: fallbackPalette.female,
                neutral = map[SettingsKeys.THEME_GENDER_NEUTRAL]?.toColorOrNull() ?: fallbackPalette.neutral
            )

            ThemeSettings(
                seedColor = seedColor,
                navBarToneLight = map["NAV_BAR_TONE_LIGHT"]?.toFloatOrNull() ?: 80f,
                navBarToneDark = map["NAV_BAR_TONE_DARK"]?.toFloatOrNull() ?: 20f,
                surfaceToneLight = map["SURFACE_TONE_LIGHT"]?.toFloatOrNull() ?: 95f,
                surfaceToneDark = map["SURFACE_TONE_DARK"]?.toFloatOrNull() ?: 15f,
                mode = mode,
                style = style,
                genderPalette = genderPalette,
                genderColorsLocked = map[SettingsKeys.THEME_GENDER_LOCKED]?.toBooleanStrictOrNull() ?: false
            )
        }

    suspend fun setGenderPalette(palette: GenderColorPalette) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_GENDER_MALE, palette.male.toHexString()))
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_GENDER_FEMALE, palette.female.toHexString()))
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_GENDER_NEUTRAL, palette.neutral.toHexString()))
    }

    suspend fun setGenderColorsLock(locked: Boolean) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_GENDER_LOCKED, locked.toString()))
        if (!locked) {
            val seed = getCurrentSeedColor()
            setGenderPalette(defaultGenderPalette(seed))
        }
    }

    private suspend fun isGenderColorsLocked(): Boolean =
        repository.getSettingByKey(SettingsKeys.THEME_GENDER_LOCKED)?.value?.toBooleanStrictOrNull() ?: false

    private suspend fun getCurrentSeedColor(): SeedColor {
        val stored = repository.getSettingByKey(SettingsKeys.SEED_COLOR)?.value
        return stored?.let { runCatching { SeedColor.valueOf(it) }.getOrNull() } ?: SeedColor.DENIM_BLUE
    }

    suspend fun resetToDefaults() {
        setSeedColor(SeedColor.DENIM_BLUE)
        setThemeMode(ThemeMode.SYSTEM)
        setNavBarToneLight(80f)
        setNavBarToneDark(20f)
        setSurfaceToneLight(95f)
        setSurfaceToneDark(15f)
        setThemeStyle(ThemeStyle.COLORED_BACKGROUND)
        setGenderColorsLock(false)
        setGenderPalette(defaultGenderPalette(SeedColor.DENIM_BLUE))
    }

    // Legacy methods for compatibility (return default values)
    fun getBottomNavBarAlpha(): Flow<Float> = kotlinx.coroutines.flow.flowOf(1.0f)
    fun getTopAppBarAlpha(): Flow<Float> = kotlinx.coroutines.flow.flowOf(1.0f)
}
