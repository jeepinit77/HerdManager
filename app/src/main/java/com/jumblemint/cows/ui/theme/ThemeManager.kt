package com.jumblemint.cows.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CustomColors(
    val primaryLight: Color = Color(0xFF0EA5E9),
    val secondaryLight: Color = Color(0xFF64748B),
    val tertiaryLight: Color = Color(0xFF22D3EE),
    val primaryDark: Color = Color(0xFF7DD3FC),
    val secondaryDark: Color = Color(0xFF94A3B8),
    val tertiaryDark: Color = Color(0xFF67E8F9),
    val maleColorLight: Color = Color(0xFF1E40AF),
    val femaleColorLight: Color = Color(0xFFBE185D),
    val tbdColorLight: Color = Color(0xFF4B5563),
    val maleColorDark: Color = Color(0xFF60A5FA),
    val femaleColorDark: Color = Color(0xFFF472B6),
    val tbdColorDark: Color = Color(0xFF9CA3AF),
    val backgroundLight: Color = Color(0xFFF5F5F5),
    val backgroundDark: Color = Color(0xFF1C1B1F),
    val surfaceLight: Color = Color(0xFFF3F2F7),
    val surfaceDark: Color = Color(0xFF211F26),
    val cardBackgroundLight: Color = Color(0xFFE8E8E8),
    val cardBackgroundDark: Color = Color(0xFF2C2B2F)
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

enum class PresetTheme(val displayName: String) {
    DEFAULT("Default"),
    BLUE("Blue"),
    GREEN("Green"),
    PURPLE("Purple"),
    WARM("Warm Autumn"),
    COOL("Cool Ocean"),
    DARK("Dark Mode+"),
    LIGHT("Light & Airy"),
    BRIGHT("Bright & Cheery"),
    RAINBOW("Rainbow"),
    BOLD_BLUE("Bold Blue"),
    BOLD_GREEN("Bold Green"),
    BOLD_ORANGE("Bold Orange"),
    BOLD_PINK("Bold Pink"),
    CUSTOM("Custom")
}

fun PresetTheme.getColors(): CustomColors = when (this) {
    PresetTheme.DEFAULT -> CustomColors()
    
    PresetTheme.BLUE -> CustomColors(
        primaryLight = Color(0xFF1565C0), secondaryLight = Color(0xFF1976D2), tertiaryLight = Color(0xFF0277BD),
        primaryDark = Color(0xFF42A5F5), secondaryDark = Color(0xFF64B5F6), tertiaryDark = Color(0xFF29B6F6),
        maleColorLight = Color(0xFF0D47A1), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF424242),
        maleColorDark = Color(0xFF2196F3), femaleColorDark = Color(0xFFE91E63), tbdColorDark = Color(0xFF757575),
        backgroundLight = Color(0xFFE3F2FD), backgroundDark = Color(0xFF0D1421),
        surfaceLight = Color(0xFFBBDEFB), surfaceDark = Color(0xFF1A252F),
        cardBackgroundLight = Color(0xFFF3F8FF), cardBackgroundDark = Color(0xFF1E2A35)
    )
    
    PresetTheme.GREEN -> CustomColors(
        primaryLight = Color(0xFF2E7D32), secondaryLight = Color(0xFF388E3C), tertiaryLight = Color(0xFF00695C),
        primaryDark = Color(0xFF66BB6A), secondaryDark = Color(0xFF81C784), tertiaryDark = Color(0xFF4DB6AC),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFC2185B), tbdColorLight = Color(0xFF5D4037),
        maleColorDark = Color(0xFF42A5F5), femaleColorDark = Color(0xFFE91E63), tbdColorDark = Color(0xFF8D6E63),
        backgroundLight = Color(0xFFE8F5E8), backgroundDark = Color(0xFF0D1B0D),
        surfaceLight = Color(0xFFC8E6C9), surfaceDark = Color(0xFF1B2E1B),
        cardBackgroundLight = Color(0xFFF1F8F1), cardBackgroundDark = Color(0xFF243028)
    )
    
    PresetTheme.PURPLE -> CustomColors(
        primaryLight = Color(0xFF7B1FA2), secondaryLight = Color(0xFF8E24AA), tertiaryLight = Color(0xFF5E35B1),
        primaryDark = Color(0xFFBA68C8), secondaryDark = Color(0xFFCE93D8), tertiaryDark = Color(0xFF9575CD),
        maleColorLight = Color(0xFF3F51B5), femaleColorLight = Color(0xFFE91E63), tbdColorLight = Color(0xFF616161),
        maleColorDark = Color(0xFF7986CB), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFF9E9E9E),
        backgroundLight = Color(0xFFF3E5F5), backgroundDark = Color(0xFF1A0D1F),
        surfaceLight = Color(0xFFE1BEE7), surfaceDark = Color(0xFF2D1B33),
        cardBackgroundLight = Color(0xFFFAF5FB), cardBackgroundDark = Color(0xFF2F2135)
    )
    
    PresetTheme.WARM -> CustomColors(
        primaryLight = Color(0xFFD84315), secondaryLight = Color(0xFFE64A19), tertiaryLight = Color(0xFFFF5722),
        primaryDark = Color(0xFFFF7043), secondaryDark = Color(0xFFFF8A65), tertiaryDark = Color(0xFFFFAB91),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFD81B60), tbdColorLight = Color(0xFF6D4C41),
        maleColorDark = Color(0xFF42A5F5), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFFA1887F),
        backgroundLight = Color(0xFFFBE9E7), backgroundDark = Color(0xFF1F0D0A),
        surfaceLight = Color(0xFFFFCCBC), surfaceDark = Color(0xFF331A14),
        cardBackgroundLight = Color(0xFFFFF5F3), cardBackgroundDark = Color(0xFF35211C)
    )
    
    PresetTheme.COOL -> CustomColors(
        primaryLight = Color(0xFF00838F), secondaryLight = Color(0xFF0097A7), tertiaryLight = Color(0xFF00ACC1),
        primaryDark = Color(0xFF4DD0E1), secondaryDark = Color(0xFF80DEEA), tertiaryDark = Color(0xFFB2EBF2),
        maleColorLight = Color(0xFF1976D2), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF455A64),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFE91E63), tbdColorDark = Color(0xFF78909C),
        backgroundLight = Color(0xFFE0F2F1), backgroundDark = Color(0xFF0A1F1F),
        surfaceLight = Color(0xFFB2DFDB), surfaceDark = Color(0xFF143333),
        cardBackgroundLight = Color(0xFFF0F9F9), cardBackgroundDark = Color(0xFF1B3535)
    )
    
    PresetTheme.DARK -> CustomColors(
        primaryLight = Color(0xFF424242), secondaryLight = Color(0xFF616161), tertiaryLight = Color(0xFF757575),
        primaryDark = Color(0xFFE0E0E0), secondaryDark = Color(0xFFBDBDBD), tertiaryDark = Color(0xFF9E9E9E),
        maleColorLight = Color(0xFF1976D2), femaleColorLight = Color(0xFFE91E63), tbdColorLight = Color(0xFF795548),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFFBCAAA4),
        backgroundLight = Color(0xFFF5F5F5), backgroundDark = Color(0xFF121212),
        surfaceLight = Color(0xFFEEEEEE), surfaceDark = Color(0xFF1E1E1E),
        cardBackgroundLight = Color(0xFFF8F8F8), cardBackgroundDark = Color(0xFF242424)
    )
    
    PresetTheme.LIGHT -> CustomColors(
        primaryLight = Color(0xFF0288D1), secondaryLight = Color(0xFF0277BD), tertiaryLight = Color(0xFF01579B),
        primaryDark = Color(0xFF4FC3F7), secondaryDark = Color(0xFF29B6F6), tertiaryDark = Color(0xFF03A9F4),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF37474F),
        maleColorDark = Color(0xFF42A5F5), femaleColorDark = Color(0xFFE91E63), tbdColorDark = Color(0xFF607D8B),
        backgroundLight = Color(0xFFFAFAFA), backgroundDark = Color(0xFF1C1B1F),
        surfaceLight = Color(0xFFECEFF1), surfaceDark = Color(0xFF211F26),
        cardBackgroundLight = Color(0xFFFCFCFC), cardBackgroundDark = Color(0xFF2C2B2F)
    )
    
    PresetTheme.BRIGHT -> CustomColors(
        primaryLight = Color(0xFFFF6F00), secondaryLight = Color(0xFFFF8F00), tertiaryLight = Color(0xFFFFA000),
        primaryDark = Color(0xFFFFB74D), secondaryDark = Color(0xFFFFCC02), tertiaryDark = Color(0xFFFDD835),
        maleColorLight = Color(0xFF3F51B5), femaleColorLight = Color(0xFFE91E63), tbdColorLight = Color(0xFF795548),
        maleColorDark = Color(0xFF7986CB), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFFBCAAA4),
        backgroundLight = Color(0xFFFFF8E1), backgroundDark = Color(0xFF1F1A0D),
        surfaceLight = Color(0xFFFFF3C4), surfaceDark = Color(0xFF332B1A),
        cardBackgroundLight = Color(0xFFFFFDF5), cardBackgroundDark = Color(0xFF352B1C)
    )
    
    PresetTheme.RAINBOW -> CustomColors(
        primaryLight = Color(0xFFE91E63), secondaryLight = Color(0xFF9C27B0), tertiaryLight = Color(0xFF673AB7),
        primaryDark = Color(0xFFF06292), secondaryDark = Color(0xFFBA68C8), tertiaryDark = Color(0xFF9575CD),
        maleColorLight = Color(0xFF2196F3), femaleColorLight = Color(0xFFE91E63), tbdColorLight = Color(0xFF4CAF50),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFF81C784),
        backgroundLight = Color(0xFFF8BBD9), backgroundDark = Color(0xFF1A0D14),
        surfaceLight = Color(0xFFE1BEE7), surfaceDark = Color(0xFF2D1B28),
        cardBackgroundLight = Color(0xFFFDF9FC), cardBackgroundDark = Color(0xFF2F212A)
    )
    
    PresetTheme.BOLD_BLUE -> CustomColors(
        primaryLight = Color(0xFF1976D2), secondaryLight = Color(0xFF1565C0), tertiaryLight = Color(0xFF0D47A1),
        primaryDark = Color(0xFF42A5F5), secondaryDark = Color(0xFF64B5F6), tertiaryDark = Color(0xFF90CAF9),
        maleColorLight = Color(0xFF0D47A1), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF424242),
        maleColorDark = Color(0xFF2196F3), femaleColorDark = Color(0xFFE91E63), tbdColorDark = Color(0xFF757575),
        backgroundLight = Color(0xFFE3F2FD), backgroundDark = Color(0xFF0D1421),
        surfaceLight = Color(0xFFBBDEFB), surfaceDark = Color(0xFF1A252F),
        cardBackgroundLight = Color(0xFFBBDEFB), cardBackgroundDark = Color(0xFF1E2A35)
    )
    
    PresetTheme.BOLD_GREEN -> CustomColors(
        primaryLight = Color(0xFF388E3C), secondaryLight = Color(0xFF2E7D32), tertiaryLight = Color(0xFF1B5E20),
        primaryDark = Color(0xFF66BB6A), secondaryDark = Color(0xFF81C784), tertiaryDark = Color(0xFFA5D6A7),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFC2185B), tbdColorLight = Color(0xFF5D4037),
        maleColorDark = Color(0xFF42A5F5), femaleColorDark = Color(0xFFE91E63), tbdColorDark = Color(0xFF8D6E63),
        backgroundLight = Color(0xFFE8F5E8), backgroundDark = Color(0xFF0D1B0D),
        surfaceLight = Color(0xFFC8E6C9), surfaceDark = Color(0xFF1B2E1B),
        cardBackgroundLight = Color(0xFFC8E6C9), cardBackgroundDark = Color(0xFF243028)
    )
    
    PresetTheme.BOLD_ORANGE -> CustomColors(
        primaryLight = Color(0xFFFF5722), secondaryLight = Color(0xFFE64A19), tertiaryLight = Color(0xFFD84315),
        primaryDark = Color(0xFFFF8A65), secondaryDark = Color(0xFFFFAB91), tertiaryDark = Color(0xFFFFCCBC),
        maleColorLight = Color(0xFF1565C0), femaleColorLight = Color(0xFFD81B60), tbdColorLight = Color(0xFF6D4C41),
        maleColorDark = Color(0xFF42A5F5), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFFA1887F),
        backgroundLight = Color(0xFFFBE9E7), backgroundDark = Color(0xFF1F0D0A),
        surfaceLight = Color(0xFFFFCCBC), surfaceDark = Color(0xFF331A14),
        cardBackgroundLight = Color(0xFFFFCCBC), cardBackgroundDark = Color(0xFF35211C)
    )
    
    PresetTheme.BOLD_PINK -> CustomColors(
        primaryLight = Color(0xFFE91E63), secondaryLight = Color(0xFFAD1457), tertiaryLight = Color(0xFF880E4F),
        primaryDark = Color(0xFFF06292), secondaryDark = Color(0xFFF48FB1), tertiaryDark = Color(0xFFF8BBD0),
        maleColorLight = Color(0xFF1976D2), femaleColorLight = Color(0xFFAD1457), tbdColorLight = Color(0xFF616161),
        maleColorDark = Color(0xFF64B5F6), femaleColorDark = Color(0xFFF06292), tbdColorDark = Color(0xFF9E9E9E),
        backgroundLight = Color(0xFFFCE4EC), backgroundDark = Color(0xFF1A0D14),
        surfaceLight = Color(0xFFF8BBD0), surfaceDark = Color(0xFF2D1B28),
        cardBackgroundLight = Color(0xFFF8BBD0), cardBackgroundDark = Color(0xFF2F212A)
    )
    
    PresetTheme.CUSTOM -> CustomColors()
}

class ThemeManager(private val repository: CattleRepository) {
    
    fun getCustomColors(): Flow<CustomColors> {
        return repository.getAllSettings().map { settings ->
            val settingsMap = settings.associate { it.key to it.value }
            
            CustomColors(
                primaryLight = settingsMap[SettingsKeys.THEME_PRIMARY_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFF0EA5E9),
                secondaryLight = settingsMap[SettingsKeys.THEME_SECONDARY_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFF64748B),
                tertiaryLight = settingsMap[SettingsKeys.THEME_TERTIARY_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFF22D3EE),
                primaryDark = settingsMap[SettingsKeys.THEME_PRIMARY_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF7DD3FC),
                secondaryDark = settingsMap[SettingsKeys.THEME_SECONDARY_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF94A3B8),
                tertiaryDark = settingsMap[SettingsKeys.THEME_TERTIARY_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF67E8F9),
                maleColorLight = settingsMap[SettingsKeys.THEME_MALE_COLOR_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFF1E40AF),
                femaleColorLight = settingsMap[SettingsKeys.THEME_FEMALE_COLOR_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFBE185D),
                tbdColorLight = settingsMap[SettingsKeys.THEME_TBD_COLOR_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFF4B5563),
                maleColorDark = settingsMap[SettingsKeys.THEME_MALE_COLOR_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF60A5FA),
                femaleColorDark = settingsMap[SettingsKeys.THEME_FEMALE_COLOR_DARK]?.let { Color(it.toInt()) } ?: Color(0xFFF472B6),
                tbdColorDark = settingsMap[SettingsKeys.THEME_TBD_COLOR_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF9CA3AF),
                backgroundLight = settingsMap[SettingsKeys.THEME_BACKGROUND_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFF5F5F5),
                backgroundDark = settingsMap[SettingsKeys.THEME_BACKGROUND_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF1C1B1F),
                surfaceLight = settingsMap[SettingsKeys.THEME_SURFACE_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFF3F2F7),
                surfaceDark = settingsMap[SettingsKeys.THEME_SURFACE_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF211F26),
                cardBackgroundLight = settingsMap[SettingsKeys.THEME_CARD_BACKGROUND_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFE8E8E8),
                cardBackgroundDark = settingsMap[SettingsKeys.THEME_CARD_BACKGROUND_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF2C2B2F)
            )
        }
    }
    
    suspend fun updateColor(key: String, color: Color) {
        repository.insertOrUpdateSetting(Settings(key, color.toArgb().toString()))
    }
    
    suspend fun applyPresetTheme(preset: PresetTheme) {
        val colors = preset.getColors()
        updateColor(SettingsKeys.THEME_PRIMARY_LIGHT, colors.primaryLight)
        updateColor(SettingsKeys.THEME_SECONDARY_LIGHT, colors.secondaryLight)
        updateColor(SettingsKeys.THEME_TERTIARY_LIGHT, colors.tertiaryLight)
        updateColor(SettingsKeys.THEME_PRIMARY_DARK, colors.primaryDark)
        updateColor(SettingsKeys.THEME_SECONDARY_DARK, colors.secondaryDark)
        updateColor(SettingsKeys.THEME_TERTIARY_DARK, colors.tertiaryDark)
        updateColor(SettingsKeys.THEME_MALE_COLOR_LIGHT, colors.maleColorLight)
        updateColor(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, colors.femaleColorLight)
        updateColor(SettingsKeys.THEME_TBD_COLOR_LIGHT, colors.tbdColorLight)
        updateColor(SettingsKeys.THEME_MALE_COLOR_DARK, colors.maleColorDark)
        updateColor(SettingsKeys.THEME_FEMALE_COLOR_DARK, colors.femaleColorDark)
        updateColor(SettingsKeys.THEME_TBD_COLOR_DARK, colors.tbdColorDark)
        updateColor(SettingsKeys.THEME_BACKGROUND_LIGHT, colors.backgroundLight)
        updateColor(SettingsKeys.THEME_BACKGROUND_DARK, colors.backgroundDark)
        updateColor(SettingsKeys.THEME_SURFACE_LIGHT, colors.surfaceLight)
        updateColor(SettingsKeys.THEME_SURFACE_DARK, colors.surfaceDark)
        updateColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, colors.cardBackgroundLight)
        updateColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, colors.cardBackgroundDark)
        repository.insertOrUpdateSetting(Settings(SettingsKeys.CURRENT_THEME_PRESET, preset.name))
    }
    
    suspend fun getCurrentPreset(): PresetTheme {
        val setting = repository.getSettingByKey(SettingsKeys.CURRENT_THEME_PRESET)
        return try {
            PresetTheme.valueOf(setting?.value ?: PresetTheme.DEFAULT.name)
        } catch (e: IllegalArgumentException) {
            PresetTheme.DEFAULT
        }
    }
    
    suspend fun resetToDefaults() = applyPresetTheme(PresetTheme.DEFAULT)
}
