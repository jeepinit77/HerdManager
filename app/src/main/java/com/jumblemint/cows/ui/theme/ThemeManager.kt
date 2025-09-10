package com.jumblemint.cows.ui.theme

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
    val backgroundLight: Color = Color(0xFFFEFBFF),
    val backgroundDark: Color = Color(0xFF1C1B1F),
    val surfaceLight: Color = Color(0xFFFEFBFF),
    val surfaceDark: Color = Color(0xFF1C1B1F),
    val cardBackgroundLight: Color = Color(0xFFFEFBFF),
    val cardBackgroundDark: Color = Color(0xFF1C1B1F)
)

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
    CUSTOM("Custom")
}

fun PresetTheme.getColors(): CustomColors = when (this) {
    PresetTheme.DEFAULT -> CustomColors()
    
    // Simple color changes - only primary/secondary/tertiary (use default backgrounds)
    PresetTheme.BLUE -> CustomColors(
        primaryLight = Color(0xFF1E40AF), secondaryLight = Color(0xFF3B82F6), tertiaryLight = Color(0xFF06B6D4),
        primaryDark = Color(0xFF60A5FA), secondaryDark = Color(0xFF93C5FD), tertiaryDark = Color(0xFF67E8F9)
    )
    PresetTheme.GREEN -> CustomColors(
        primaryLight = Color(0xFF059669), secondaryLight = Color(0xFF10B981), tertiaryLight = Color(0xFF65A30D),
        primaryDark = Color(0xFF34D399), secondaryDark = Color(0xFF6EE7B7), tertiaryDark = Color(0xFFA3E635)
    )
    PresetTheme.PURPLE -> CustomColors(
        primaryLight = Color(0xFF7C3AED), secondaryLight = Color(0xFF8B5CF6), tertiaryLight = Color(0xFFD946EF),
        primaryDark = Color(0xFFA78BFA), secondaryDark = Color(0xFFC4B5FD), tertiaryDark = Color(0xFFF0ABFC)
    )
    
    // Comprehensive themes - all colors customized
    PresetTheme.WARM -> CustomColors(
        primaryLight = Color(0xFFDC2626), secondaryLight = Color(0xFFEA580C), tertiaryLight = Color(0xFFF59E0B),
        primaryDark = Color(0xFFF87171), secondaryDark = Color(0xFFFB923C), tertiaryDark = Color(0xFFFDE047),
        maleColorLight = Color(0xFF991B1B), femaleColorLight = Color(0xFFBE123C), tbdColorLight = Color(0xFF78716C),
        maleColorDark = Color(0xFFFCA5A5), femaleColorDark = Color(0xFFFB7185), tbdColorDark = Color(0xFFA8A29E),
        backgroundLight = Color(0xFFFEF2F2), backgroundDark = Color(0xFF1F1B1A),
        surfaceLight = Color(0xFFFEF7FF), surfaceDark = Color(0xFF2D1B1F),
        cardBackgroundLight = Color(0xFFFED7D7), cardBackgroundDark = Color(0xFF3F2626)
    )
    PresetTheme.COOL -> CustomColors(
        primaryLight = Color(0xFF0369A1), secondaryLight = Color(0xFF0F766E), tertiaryLight = Color(0xFF1E40AF),
        primaryDark = Color(0xFF0EA5E9), secondaryDark = Color(0xFF2DD4BF), tertiaryDark = Color(0xFF60A5FA),
        maleColorLight = Color(0xFF0C4A6E), femaleColorLight = Color(0xFF164E63), tbdColorLight = Color(0xFF374151),
        maleColorDark = Color(0xFF7DD3FC), femaleColorDark = Color(0xFF67E8F9), tbdColorDark = Color(0xFF9CA3AF),
        backgroundLight = Color(0xFFF0F9FF), backgroundDark = Color(0xFF0C1821),
        surfaceLight = Color(0xFFECFEFF), surfaceDark = Color(0xFF1A252F),
        cardBackgroundLight = Color(0xFFE0F7FA), cardBackgroundDark = Color(0xFF1E3A42)
    )
    PresetTheme.DARK -> CustomColors(
        primaryLight = Color(0xFF6366F1), secondaryLight = Color(0xFF8B5CF6), tertiaryLight = Color(0xFFEC4899),
        primaryDark = Color(0xFFA5B4FC), secondaryDark = Color(0xFFC4B5FD), tertiaryDark = Color(0xFFF472B6),
        maleColorLight = Color(0xFF4F46E5), femaleColorLight = Color(0xFFDB2777), tbdColorLight = Color(0xFF6B7280),
        maleColorDark = Color(0xFF818CF8), femaleColorDark = Color(0xFFF9A8D4), tbdColorDark = Color(0xFF9CA3AF),
        backgroundLight = Color(0xFFFEFBFF), backgroundDark = Color(0xFF0A0A0A),
        surfaceLight = Color(0xFFFEFBFF), surfaceDark = Color(0xFF1A1A1A),
        cardBackgroundLight = Color(0xFFF5F5F5), cardBackgroundDark = Color(0xFF262626)
    )
    PresetTheme.LIGHT -> CustomColors(
        primaryLight = Color(0xFF0EA5E9), secondaryLight = Color(0xFF64748B), tertiaryLight = Color(0xFF22D3EE),
        primaryDark = Color(0xFF7DD3FC), secondaryDark = Color(0xFF94A3B8), tertiaryDark = Color(0xFF67E8F9),
        maleColorLight = Color(0xFF1E40AF), femaleColorLight = Color(0xFFBE185D), tbdColorLight = Color(0xFF4B5563),
        maleColorDark = Color(0xFF60A5FA), femaleColorDark = Color(0xFFF472B6), tbdColorDark = Color(0xFF9CA3AF),
        backgroundLight = Color(0xFFFFFFFF), backgroundDark = Color(0xFF1C1B1F),
        surfaceLight = Color(0xFFFAFAFA), surfaceDark = Color(0xFF1C1B1F),
        cardBackgroundLight = Color(0xFFF8F9FA), cardBackgroundDark = Color(0xFF1C1B1F)
    )
    PresetTheme.BRIGHT -> CustomColors(
        primaryLight = Color(0xFFFF6B35), secondaryLight = Color(0xFFFF9500), tertiaryLight = Color(0xFFFFD23F),
        primaryDark = Color(0xFFFF8A65), secondaryDark = Color(0xFFFFB74D), tertiaryDark = Color(0xFFFFF176),
        maleColorLight = Color(0xFFE91E63), femaleColorLight = Color(0xFF9C27B0), tbdColorLight = Color(0xFF607D8B),
        maleColorDark = Color(0xFFF48FB1), femaleColorDark = Color(0xFFCE93D8), tbdColorDark = Color(0xFF90A4AE),
        backgroundLight = Color(0xFFFFF8E1), backgroundDark = Color(0xFF1C1B1F),
        surfaceLight = Color(0xFFFFFDE7), surfaceDark = Color(0xFF1C1B1F),
        cardBackgroundLight = Color(0xFFFFF3C4), cardBackgroundDark = Color(0xFF2E2E2E)
    )
    PresetTheme.RAINBOW -> CustomColors(
        primaryLight = Color(0xFFE91E63), secondaryLight = Color(0xFF9C27B0), tertiaryLight = Color(0xFF2196F3),
        primaryDark = Color(0xFFF48FB1), secondaryDark = Color(0xFFCE93D8), tertiaryDark = Color(0xFF90CAF9),
        maleColorLight = Color(0xFF4CAF50), femaleColorLight = Color(0xFFFF5722), tbdColorLight = Color(0xFF795548),
        maleColorDark = Color(0xFFA5D6A7), femaleColorDark = Color(0xFFFFAB91), tbdColorDark = Color(0xFFBCAAA4),
        backgroundLight = Color(0xFFF3E5F5), backgroundDark = Color(0xFF1A1A2E),
        surfaceLight = Color(0xFFE8F5E8), surfaceDark = Color(0xFF16213E),
        cardBackgroundLight = Color(0xFFE1F5FE), cardBackgroundDark = Color(0xFF0F3460)
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
                backgroundLight = settingsMap[SettingsKeys.THEME_BACKGROUND_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFFEFBFF),
                backgroundDark = settingsMap[SettingsKeys.THEME_BACKGROUND_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF1C1B1F),
                surfaceLight = settingsMap[SettingsKeys.THEME_SURFACE_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFFEFBFF),
                surfaceDark = settingsMap[SettingsKeys.THEME_SURFACE_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF1C1B1F),
                cardBackgroundLight = settingsMap[SettingsKeys.THEME_CARD_BACKGROUND_LIGHT]?.let { Color(it.toInt()) } ?: Color(0xFFFEFBFF),
                cardBackgroundDark = settingsMap[SettingsKeys.THEME_CARD_BACKGROUND_DARK]?.let { Color(it.toInt()) } ?: Color(0xFF1C1B1F)
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