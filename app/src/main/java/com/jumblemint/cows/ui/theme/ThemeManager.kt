package com.jumblemint.cows.ui.theme


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

// ---- Seed Colors ----
enum class SeedColor(val displayName: String, val color: Color) {
    // Blues
    DENIM_BLUE("Denim Blue", Color(0xFF3A6DA8)),
    SLATE_BLUE("Slate Blue", Color(0xFF4C6780)),
    SKY_NAVY("Sky Navy", Color(0xFF2E4E73)),
    
    // Greens
    SAGE_GREEN("Sage Green", Color(0xFF7A8F75)),
    VERDANT_TEAL("Verdant Teal", Color(0xFF3C8C7A)),
    MOSS_OLIVE("Moss Olive", Color(0xFF6E7B4F)),
    
    // Reds
    BRICK_RED("Brick Red", Color(0xFFB84A4A)),
    CRANBERRY("Cranberry", Color(0xFFA73C56)),
    CORAL_RUST("Coral Rust", Color(0xFFD05B45)),
    
    // Yellows
    HONEY_GOLD("Honey Gold", Color(0xFFC9A33E)),
    SUN_MIST("Sun Mist", Color(0xFFE5C861)),
    OCHRE_GLOW("Ochre Glow", Color(0xFFDA9344)),
    
    // Browns
    ESPRESSO_BROWN("Espresso Brown", Color(0xFF4B342A)),
    CLAY_TAUPE("Clay Taupe", Color(0xFF8A7563)),
    CEDARWOOD("Cedarwood", Color(0xFF9C5A3A)),
    
    // Curated Schemes
    SLATE("Slate", Color(0xFF586474)),
    ASH_MIST("Ash Mist", Color(0xFFCFCBC4)),
    COPPER_TEAL("Copper Teal", Color(0xFF2E7C78)),
    SAND_SEA("Sand & Sea", Color(0xFFC1A57B)),
    CHARCOAL_GOLD("Charcoal Gold", Color(0xFF3C3C3C)),
    LAVENDER_GRAPHITE("Lavender Graphite", Color(0xFF8D7FA9)),
    TERRACOTTA_SAGE("Terracotta Sage", Color(0xFFB86B5C)),
    FROSTED_FOREST("Frosted Forest", Color(0xFFE2F0E7)),
    MIDNIGHT_CLAY("Midnight Clay", Color(0xFF374458)),
    PEARL_INK("Pearl & Ink", Color(0xFFF7F5F3))
}

// ---- Theme Settings ----
data class ThemeSettings(
    val seedColor: SeedColor = SeedColor.DENIM_BLUE,
    val navBarToneLight: Float = 80f,
    val navBarToneDark: Float = 20f,
    val surfaceToneLight: Float = 95f,
    val surfaceToneDark: Float = 15f,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val style: ThemeStyle = ThemeStyle.COLORED_BACKGROUND
) {
    fun getNavBarTone(isDark: Boolean) = if (isDark) navBarToneDark else navBarToneLight
    fun getSurfaceTone(isDark: Boolean) = if (isDark) surfaceToneDark else surfaceToneLight
}

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class ThemeStyle { COLORED_CARDS, COLORED_BACKGROUND, GRAY_CARDS, GRAY_BACKGROUND }

// ---- Theme Generation ----
fun generateThemeFromSeed(
    seedColor: SeedColor,
    surfaceTone: Float,
    navBarTone: Float,
    isDark: Boolean,
    style: ThemeStyle = ThemeStyle.COLORED_BACKGROUND
): androidx.compose.material3.ColorScheme {
    val seed = seedColor.color
    val seedHct = seed.toHct()
    
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
                
            ThemeSettings(
                seedColor = seedColor,
                navBarToneLight = map["NAV_BAR_TONE_LIGHT"]?.toFloatOrNull() ?: 80f,
                navBarToneDark = map["NAV_BAR_TONE_DARK"]?.toFloatOrNull() ?: 20f,
                surfaceToneLight = map["SURFACE_TONE_LIGHT"]?.toFloatOrNull() ?: 95f,
                surfaceToneDark = map["SURFACE_TONE_DARK"]?.toFloatOrNull() ?: 15f,
                mode = mode,
                style = style
            )
        }

    suspend fun resetToDefaults() {
        setSeedColor(SeedColor.DENIM_BLUE)
        setThemeMode(ThemeMode.SYSTEM)
        setNavBarToneLight(80f)
        setNavBarToneDark(20f)
        setSurfaceToneLight(95f)
        setSurfaceToneDark(15f)
        setThemeStyle(ThemeStyle.COLORED_BACKGROUND)
    }

    // Legacy methods for compatibility (return default values)
    fun getBottomNavBarAlpha(): Flow<Float> = kotlinx.coroutines.flow.flowOf(1.0f)
    fun getTopAppBarAlpha(): Flow<Float> = kotlinx.coroutines.flow.flowOf(1.0f)
}
