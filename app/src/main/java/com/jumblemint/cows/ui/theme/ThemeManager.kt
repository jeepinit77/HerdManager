package com.jumblemint.cows.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ---- Contrast helpers ----
fun determineOnColor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() > 0.5f) Color(0xFF000000) else Color(0xFFFFFFFF)

fun getContrastingTextColor(backgroundColor: Color): Color = determineOnColor(backgroundColor)
fun Color.contrastingTextColor(): Color = getContrastingTextColor(this)

// ---- Theme model ----
data class CustomColors(
    // Light
    val primaryLight: Color = Color(0xFF0EA5E9),
    val secondaryLight: Color = Color(0xFF64748B),
    val tertiaryLight: Color = Color(0xFF22D3EE),

    val backgroundLight: Color = Color(0xFFF5F5F5),
    val surfaceLight: Color = Color(0xFFF3F2F7),
    val cardBackgroundLight: Color = Color(0xFFE8E8E8),

    val maleColorLight: Color = Color(0xFF1E40AF),
    val femaleColorLight: Color = Color(0xFFBE185D),
    val tbdColorLight: Color = Color(0xFF4B5563),

    // Dark
    val primaryDark: Color = Color(0xFF7DD3FC),
    val secondaryDark: Color = Color(0xFF94A3B8),
    val tertiaryDark: Color = Color(0xFF67E8F9),

    val backgroundDark: Color = Color(0xFF1C1B1F),
    val surfaceDark: Color = Color(0xFF211F26),
    val cardBackgroundDark: Color = Color(0xFF2C2B2F),

    val maleColorDark: Color = Color(0xFF60A5FA),
    val femaleColorDark: Color = Color(0xFFF472B6),
    val tbdColorDark: Color = Color(0xFF9CA3AF)
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Presets: two rows of *tint* themes (single-hue families) + one row of curated sets.
 */
enum class PresetTheme(val displayName: String) {
    // Tint rows
    TINT_BLUE("Blue"),
    TINT_GREEN("Green"),
    TINT_PURPLE("Purple"),
    TINT_ORANGE("Orange"),
    TINT_PINK("Pink"),
    TINT_RED("Red"),
    TINT_YELLOW("Yellow"),
    TINT_TEAL("Teal"),
    TINT_BROWN("Brown"),
    TINT_GRAY("Gray"),

    // Curated row
    SLATE_GRAY("Industrial Slate"),
    MONOCHROME("Monochrome"),
    NORD_FROST("Nord Frost"),
    MAUVE_MIST("Mauve Mist"),
    OLIVE_NOIR("Olive Noir"),

    CUSTOM("Custom")
}

/** Explicit colors for every preset (no auto-calculation). */
fun PresetTheme.getColors(): CustomColors = when (this) {
    // --------- Tint themes (light/dark tuned to stay in-hue) ----------
    PresetTheme.TINT_BLUE -> CustomColors(
        primaryLight = Color(0xFF1565C0), secondaryLight = Color(0xFF1976D2), tertiaryLight = Color(0xFF0EA5E9),
        backgroundLight = Color(0xFFE8F1FE), surfaceLight = Color(0xFFDBEAFE), cardBackgroundLight = Color(0xFFF3F8FF),
        maleColorLight = Color(0xFF0D47A1), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF1F2937),

        primaryDark = Color(0xFF64B5F6), secondaryDark = Color(0xFF42A5F5), tertiaryDark = Color(0xFF7DD3FC),
        backgroundDark = Color(0xFF0A1220), surfaceDark = Color(0xFF142133), cardBackgroundDark = Color(0xFF1B2A3F),
        maleColorDark = Color(0xFF90CAF9), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFF93A3B5)
    )

    PresetTheme.TINT_GREEN -> CustomColors(
        primaryLight = Color(0xFF2E7D32), secondaryLight = Color(0xFF388E3C), tertiaryLight = Color(0xFF43A047),
        backgroundLight = Color(0xFFE9F6EC), surfaceLight = Color(0xFFD7F0DD), cardBackgroundLight = Color(0xFFF2FBF4),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFC2185B), tbdColorLight = Color(0xFF374151),

        primaryDark = Color(0xFF81C784), secondaryDark = Color(0xFF66BB6A), tertiaryDark = Color(0xFFA5D6A7),
        backgroundDark = Color(0xFF0B160C), surfaceDark = Color(0xFF152418), cardBackgroundDark = Color(0xFF1E2E22),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFF88A08F)
    )

    PresetTheme.TINT_PURPLE -> CustomColors(
        primaryLight = Color(0xFF7B1FA2), secondaryLight = Color(0xFF8E24AA), tertiaryLight = Color(0xFF9C27B0),
        backgroundLight = Color(0xFFF4E9F9), surfaceLight = Color(0xFFEAD7F6), cardBackgroundLight = Color(0xFFFBF7FF),
        maleColorLight = Color(0xFF3F51B5), femaleColorLight = Color(0xFFE91E63), tbdColorLight = Color(0xFF4B5563),

        primaryDark = Color(0xFFCE93D8), secondaryDark = Color(0xFFBA68C8), tertiaryDark = Color(0xFFB39DDB),
        backgroundDark = Color(0xFF170D1B), surfaceDark = Color(0xFF24172B), cardBackgroundDark = Color(0xFF2E1F37),
        maleColorDark = Color(0xFF7986CB), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFFA3A3B3)
    )

    PresetTheme.TINT_ORANGE -> CustomColors(
        primaryLight = Color(0xFFD84315), secondaryLight = Color(0xFFE64A19), tertiaryLight = Color(0xFFFF7043),
        backgroundLight = Color(0xFFFCEEE9), surfaceLight = Color(0xFFF9DACF), cardBackgroundLight = Color(0xFFFFF4EF),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFD81B60), tbdColorLight = Color(0xFF6B4F4F),

        primaryDark = Color(0xFFFF8A65), secondaryDark = Color(0xFFFF7043), tertiaryDark = Color(0xFFFFAB91),
        backgroundDark = Color(0xFF1A0E0A), surfaceDark = Color(0xFF2A1A14), cardBackgroundDark = Color(0xFF35231D),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFFB1907E)
    )

    PresetTheme.TINT_PINK -> CustomColors(
        primaryLight = Color(0xFFE91E63), secondaryLight = Color(0xFFEC407A), tertiaryLight = Color(0xFFF06292),
        backgroundLight = Color(0xFFFDF0F5), surfaceLight = Color(0xFFFADCE7), cardBackgroundLight = Color(0xFFFFF6FA),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFC2185B), tbdColorLight = Color(0xFF6B6B6B),

        primaryDark = Color(0xFFF48FB1), secondaryDark = Color(0xFFE91E63), tertiaryDark = Color(0xFFFF80AB),
        backgroundDark = Color(0xFF190D13), surfaceDark = Color(0xFF2A1822), cardBackgroundDark = Color(0xFF341F2A),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF8BBD0), tbdColorDark = Color(0xFFA6A0A3)
    )

    PresetTheme.TINT_RED -> CustomColors(
        primaryLight = Color(0xFFD32F2F), secondaryLight = Color(0xFFE53935), tertiaryLight = Color(0xFFEF5350),
        backgroundLight = Color(0xFFFBEAEA), surfaceLight = Color(0xFFF4D1D1), cardBackgroundLight = Color(0xFFFFF4F4),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFC2185B), tbdColorLight = Color(0xFF5C5757),

        primaryDark = Color(0xFFE57373), secondaryDark = Color(0xFFEF5350), tertiaryDark = Color(0xFFEF9A9A),
        backgroundDark = Color(0xFF200C0C), surfaceDark = Color(0xFF321616), cardBackgroundDark = Color(0xFF3D1D1D),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFFB79A9A)
    )

    PresetTheme.TINT_YELLOW -> CustomColors(
        primaryLight = Color(0xFFFBC02D), secondaryLight = Color(0xFFFDD835), tertiaryLight = Color(0xFFFFEE58),
        backgroundLight = Color(0xFFFFF9E5), surfaceLight = Color(0xFFFFF3C4), cardBackgroundLight = Color(0xFFFFFDF0),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFC2185B), tbdColorLight = Color(0xFF6B5E2E),

        primaryDark = Color(0xFFFFEE58), secondaryDark = Color(0xFFFDD835), tertiaryDark = Color(0xFFFFF176),
        backgroundDark = Color(0xFF231D08), surfaceDark = Color(0xFF352E11), cardBackgroundDark = Color(0xFF3D3518),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFFCABF7A)
    )

    PresetTheme.TINT_TEAL -> CustomColors(
        primaryLight = Color(0xFF00838F), secondaryLight = Color(0xFF0097A7), tertiaryLight = Color(0xFF00ACC1),
        backgroundLight = Color(0xFFE7F7F7), surfaceLight = Color(0xFFCEF0F0), cardBackgroundLight = Color(0xFFF0FBFB),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF3C5356),

        primaryDark = Color(0xFF4DD0E1), secondaryDark = Color(0xFF80DEEA), tertiaryDark = Color(0xFFB2EBF2),
        backgroundDark = Color(0xFF0B1F1F), surfaceDark = Color(0xFF163333), cardBackgroundDark = Color(0xFF1F3C3C),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF48FB1), tbdColorDark = Color(0xFF7DA6A6)
    )

    PresetTheme.TINT_BROWN -> CustomColors(
        primaryLight = Color(0xFF6D4C41), secondaryLight = Color(0xFF8D6E63), tertiaryLight = Color(0xFFA1887F),
        backgroundLight = Color(0xFFFAF6F3), surfaceLight = Color(0xFFF0E6E1), cardBackgroundLight = Color(0xFFFFFBF8),
        maleColorLight = Color(0xFF3E2723), femaleColorLight = Color(0xFF6D4C41), tbdColorLight = Color(0xFF4E342E),

        primaryDark = Color(0xFFBCAAA4), secondaryDark = Color(0xFFA1887F), tertiaryDark = Color(0xFF8D6E63),
        backgroundDark = Color(0xFF1F1A18), surfaceDark = Color(0xFF2C2522), cardBackgroundDark = Color(0xFF352D29),
        maleColorDark = Color(0xFF8D6E63), femaleColorDark = Color(0xFFFF7043), tbdColorDark = Color(0xFF9E7F73)
    )

    PresetTheme.TINT_GRAY -> CustomColors(
        primaryLight = Color(0xFF525252), secondaryLight = Color(0xFF6B7280), tertiaryLight = Color(0xFF9CA3AF),
        backgroundLight = Color(0xFFF7F7F7), surfaceLight = Color(0xFFEDEDED), cardBackgroundLight = Color(0xFFFFFFFF),
        maleColorLight = Color(0xFF4B5563), femaleColorLight = Color(0xFF6B7280), tbdColorLight = Color(0xFF737373),

        primaryDark = Color(0xFFA3A3A3), secondaryDark = Color(0xFF9CA3AF), tertiaryDark = Color(0xFFCBD5E1),
        backgroundDark = Color(0xFF121212), surfaceDark = Color(0xFF1E1E1E), cardBackgroundDark = Color(0xFF232323),
        maleColorDark = Color(0xFF9CA3AF), femaleColorDark = Color(0xFFB0B6BF), tbdColorDark = Color(0xFFC7C7C7)
    )

    // --------- Curated themes ----------
    PresetTheme.SLATE_GRAY -> CustomColors(
        primaryLight = Color(0xFF4A5568), secondaryLight = Color(0xFF718096), tertiaryLight = Color(0xFFA0AEC0),
        backgroundLight = Color(0xFFF7FAFC), surfaceLight = Color(0xFFEDF2F7), cardBackgroundLight = Color(0xFFFFFFFF),
        maleColorLight = Color(0xFF2C5282), femaleColorLight = Color(0xFF742A2A), tbdColorLight = Color(0xFF4A5568),

        primaryDark = Color(0xFFCBD5E0), secondaryDark = Color(0xFFA0AEC0), tertiaryDark = Color(0xFF718096),
        backgroundDark = Color(0xFF1A202C), surfaceDark = Color(0xFF2D3748), cardBackgroundDark = Color(0xFF252E3D),
        maleColorDark = Color(0xFF63B3ED), femaleColorDark = Color(0xFFF56565), tbdColorDark = Color(0xFFA0AEC0)
    )

    PresetTheme.MONOCHROME -> CustomColors(
        primaryLight = Color(0xFF525252), secondaryLight = Color(0xFF737373), tertiaryLight = Color(0xFF8A8A8A),
        backgroundLight = Color(0xFFFFFFFF), surfaceLight = Color(0xFFF3F3F3), cardBackgroundLight = Color(0xFFFAFAFA),
        maleColorLight = Color(0xFF4A4A4A), femaleColorLight = Color(0xFF6E6E6E), tbdColorLight = Color(0xFF7D7D7D),

        primaryDark = Color(0xFFA3A3A3), secondaryDark = Color(0xFFBDBDBD), tertiaryDark = Color(0xFFD4D4D4),
        backgroundDark = Color(0xFF121212), surfaceDark = Color(0xFF1E1E1E), cardBackgroundDark = Color(0xFF2A2A2A),
        maleColorDark = Color(0xFF8C8C8C), femaleColorDark = Color(0xFFAFAFAF), tbdColorDark = Color(0xFFC2C2C2)
    )

    PresetTheme.NORD_FROST -> CustomColors(
        primaryLight = Color(0xFF4C7899), secondaryLight = Color(0xFF6CA6B7), tertiaryLight = Color(0xFFA7D0D8),
        backgroundLight = Color(0xFFF6FAFD), surfaceLight = Color(0xFFE9F2F6), cardBackgroundLight = Color(0xFFFFFFFF),
        maleColorLight = Color(0xFF2E5A89), femaleColorLight = Color(0xFF8B6A7A), tbdColorLight = Color(0xFF4A5B6B),

        primaryDark = Color(0xFF9FC2D6), secondaryDark = Color(0xFF7FB5C3), tertiaryDark = Color(0xFFBDE3EA),
        backgroundDark = Color(0xFF10171D), surfaceDark = Color(0xFF1C252C), cardBackgroundDark = Color(0xFF22313A),
        maleColorDark = Color(0xFF9CC2F1), femaleColorDark = Color(0xFFCFA5B3), tbdColorDark = Color(0xFF92A3B0)
    )

    PresetTheme.MAUVE_MIST -> CustomColors(
        primaryLight = Color(0xFF9B7AAE), secondaryLight = Color(0xFF6C8AA6), tertiaryLight = Color(0xFFE8D9C6),
        backgroundLight = Color(0xFFFCFAF8), surfaceLight = Color(0xFFF4EEF1), cardBackgroundLight = Color(0xFFFFFFFF),
        maleColorLight = Color(0xFF566B8B), femaleColorLight = Color(0xFFB07A8C), tbdColorLight = Color(0xFF7C7D86),

        primaryDark = Color(0xFFC7A9D3), secondaryDark = Color(0xFFA0B6CC), tertiaryDark = Color(0xFFF1E4D2),
        backgroundDark = Color(0xFF171518), surfaceDark = Color(0xFF27242A), cardBackgroundDark = Color(0xFF2F2B32),
        maleColorDark = Color(0xFF9BB1D1), femaleColorDark = Color(0xFFD7A9B8), tbdColorDark = Color(0xFFB4B5BD)
    )

    PresetTheme.OLIVE_NOIR -> CustomColors(
        primaryLight = Color(0xFF6B7D48), secondaryLight = Color(0xFF2F343B), tertiaryLight = Color(0xFFD6C8A2),
        backgroundLight = Color(0xFFFAFAF6), surfaceLight = Color(0xFFF0EFE7), cardBackgroundLight = Color(0xFFFFFFFF),
        maleColorLight = Color(0xFF3E4B2D), femaleColorLight = Color(0xFF5F4B44), tbdColorLight = Color(0xFF6C6B63),

        primaryDark = Color(0xFFB2C08C), secondaryDark = Color(0xFF3A444D), tertiaryDark = Color(0xFFE7DBB6),
        backgroundDark = Color(0xFF171814), surfaceDark = Color(0xFF232520), cardBackgroundDark = Color(0xFF2C2E28),
        maleColorDark = Color(0xFFC4D29B), femaleColorDark = Color(0xFFBA9E93), tbdColorDark = Color(0xFFB8B7A9)
    )

    PresetTheme.CUSTOM -> CustomColors()
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

    // Intensity (fade amount used by the editor to set alpha on backgrounds/cards) - deprecated, use separate
    suspend fun updateIntensity(intensity: Float) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_INTENSITY, intensity.toString()))
    }

    fun getCurrentIntensity(): Flow<Float> =
        repository.getAllSettings().map { settings ->
            settings.find { it.key == SettingsKeys.THEME_INTENSITY }?.value?.toFloatOrNull() ?: 0.2f
        }

    suspend fun updateLightIntensity(intensity: Float) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_INTENSITY_LIGHT, intensity.toString()))
    }

    fun getLightIntensity(): Flow<Float> =
        repository.getAllSettings().map { settings ->
            settings.find { it.key == SettingsKeys.THEME_INTENSITY_LIGHT }?.value?.toFloatOrNull() ?: 0.2f
        }

    suspend fun updateDarkIntensity(intensity: Float) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_INTENSITY_DARK, intensity.toString()))
    }

    fun getDarkIntensity(): Flow<Float> =
        repository.getAllSettings().map { settings ->
            settings.find { it.key == SettingsKeys.THEME_INTENSITY_DARK }?.value?.toFloatOrNull() ?: 0.2f
        }

    // Colors
    fun getCustomColors(): Flow<CustomColors> =
        repository.getAllSettings().map { settings ->
            val map = settings.associate { it.key to it.value }
            val d = CustomColors()

            fun read(key: String, def: Color) = map[key]?.toIntOrNull()?.let { Color(it) } ?: def

            CustomColors(
                // light
                primaryLight = read(SettingsKeys.THEME_PRIMARY_LIGHT, d.primaryLight),
                secondaryLight = read(SettingsKeys.THEME_SECONDARY_LIGHT, d.secondaryLight),
                tertiaryLight = read(SettingsKeys.THEME_TERTIARY_LIGHT, d.tertiaryLight),
                backgroundLight = read(SettingsKeys.THEME_BACKGROUND_LIGHT, d.backgroundLight),
                surfaceLight = read(SettingsKeys.THEME_SURFACE_LIGHT, d.surfaceLight),
                cardBackgroundLight = read(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, d.cardBackgroundLight),
                maleColorLight = read(SettingsKeys.THEME_MALE_COLOR_LIGHT, d.maleColorLight),
                femaleColorLight = read(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, d.femaleColorLight),
                tbdColorLight = read(SettingsKeys.THEME_TBD_COLOR_LIGHT, d.tbdColorLight),

                // dark
                primaryDark = read(SettingsKeys.THEME_PRIMARY_DARK, d.primaryDark),
                secondaryDark = read(SettingsKeys.THEME_SECONDARY_DARK, d.secondaryDark),
                tertiaryDark = read(SettingsKeys.THEME_TERTIARY_DARK, d.tertiaryDark),
                backgroundDark = read(SettingsKeys.THEME_BACKGROUND_DARK, d.backgroundDark),
                surfaceDark = read(SettingsKeys.THEME_SURFACE_DARK, d.surfaceDark),
                cardBackgroundDark = read(SettingsKeys.THEME_CARD_BACKGROUND_DARK, d.cardBackgroundDark),
                maleColorDark = read(SettingsKeys.THEME_MALE_COLOR_DARK, d.maleColorDark),
                femaleColorDark = read(SettingsKeys.THEME_FEMALE_COLOR_DARK, d.femaleColorDark),
                tbdColorDark = read(SettingsKeys.THEME_TBD_COLOR_DARK, d.tbdColorDark)
            )
        }

    suspend fun updateColor(key: String, color: Color) {
        repository.insertOrUpdateSetting(Settings(key, color.toArgb().toString()))
    }

    suspend fun applyPresetTheme(theme: PresetTheme) {
        val c = theme.getColors()

        // Light
        updateColor(SettingsKeys.THEME_PRIMARY_LIGHT, c.primaryLight)
        updateColor(SettingsKeys.THEME_SECONDARY_LIGHT, c.secondaryLight)
        updateColor(SettingsKeys.THEME_TERTIARY_LIGHT, c.tertiaryLight)
        updateColor(SettingsKeys.THEME_BACKGROUND_LIGHT, c.backgroundLight)
        updateColor(SettingsKeys.THEME_SURFACE_LIGHT, c.surfaceLight)
        updateColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, c.cardBackgroundLight)
        updateColor(SettingsKeys.THEME_MALE_COLOR_LIGHT, c.maleColorLight)
        updateColor(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, c.femaleColorLight)
        updateColor(SettingsKeys.THEME_TBD_COLOR_LIGHT, c.tbdColorLight)

        // Dark
        updateColor(SettingsKeys.THEME_PRIMARY_DARK, c.primaryDark)
        updateColor(SettingsKeys.THEME_SECONDARY_DARK, c.secondaryDark)
        updateColor(SettingsKeys.THEME_TERTIARY_DARK, c.tertiaryDark)
        updateColor(SettingsKeys.THEME_BACKGROUND_DARK, c.backgroundDark)
        updateColor(SettingsKeys.THEME_SURFACE_DARK, c.surfaceDark)
        updateColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, c.cardBackgroundDark)
        updateColor(SettingsKeys.THEME_MALE_COLOR_DARK, c.maleColorDark)
        updateColor(SettingsKeys.THEME_FEMALE_COLOR_DARK, c.femaleColorDark)
        updateColor(SettingsKeys.THEME_TBD_COLOR_DARK, c.tbdColorDark)

        repository.insertOrUpdateSetting(Settings(SettingsKeys.CURRENT_THEME_NAME, theme.name))
    }

    suspend fun getCurrentPreset(): PresetTheme =
        repository.getSettingByKey(SettingsKeys.CURRENT_THEME_NAME)?.value
            ?.let { runCatching { PresetTheme.valueOf(it) }.getOrNull() }
            ?: PresetTheme.TINT_BLUE

    fun getCurrentPresetFlow(): Flow<PresetTheme> =
        repository.getAllSettings().map { settings ->
            settings.find { it.key == SettingsKeys.CURRENT_THEME_NAME }?.value
                ?.let { runCatching { PresetTheme.valueOf(it) }.getOrNull() }
                ?: PresetTheme.TINT_BLUE
        }

    suspend fun resetToDefaults() {
        applyPresetTheme(PresetTheme.TINT_BLUE)
        updateLightIntensity(0.2f)
        updateDarkIntensity(0.2f)
    }
}
