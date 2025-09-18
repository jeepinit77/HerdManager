package com.jumblemint.cows.data.model

object SettingsKeys {
    // Theme Keys - Light
    const val THEME_PRIMARY_LIGHT = "theme_primary_light"
    const val THEME_ON_PRIMARY_LIGHT = "theme_on_primary_light"
    const val THEME_SECONDARY_LIGHT = "theme_secondary_light"
    const val THEME_ON_SECONDARY_LIGHT = "theme_on_secondary_light"
    const val THEME_TERTIARY_LIGHT = "theme_tertiary_light"
    const val THEME_ON_TERTIARY_LIGHT = "theme_on_tertiary_light"
    const val THEME_BACKGROUND_LIGHT = "theme_background_light"
    const val THEME_ON_BACKGROUND_LIGHT = "theme_on_background_light"
    const val THEME_SURFACE_LIGHT = "theme_surface_light"
    const val THEME_ON_SURFACE_LIGHT = "theme_on_surface_light"
    const val THEME_CARD_BACKGROUND_LIGHT = "theme_card_background_light"
    // No THEME_ON_CARD_BACKGROUND_LIGHT as it typically derives from onSurface or similar

    // Theme Keys - Dark
    const val THEME_PRIMARY_DARK = "theme_primary_dark"
    const val THEME_ON_PRIMARY_DARK = "theme_on_primary_dark"
    const val THEME_SECONDARY_DARK = "theme_secondary_dark"
    const val THEME_ON_SECONDARY_DARK = "theme_on_secondary_dark"
    const val THEME_TERTIARY_DARK = "theme_tertiary_dark"
    const val THEME_ON_TERTIARY_DARK = "theme_on_tertiary_dark"
    const val THEME_BACKGROUND_DARK = "theme_background_dark"
    const val THEME_ON_BACKGROUND_DARK = "theme_on_background_dark"
    const val THEME_SURFACE_DARK = "theme_surface_dark"
    const val THEME_ON_SURFACE_DARK = "theme_on_surface_dark"
    const val THEME_CARD_BACKGROUND_DARK = "theme_card_background_dark"
    // No THEME_ON_CARD_BACKGROUND_DARK

    // Gender/Type Specific Colors - Light
    const val THEME_MALE_COLOR_LIGHT = "theme_male_color_light"
    const val THEME_FEMALE_COLOR_LIGHT = "theme_female_color_light"
    const val THEME_TBD_COLOR_LIGHT = "theme_tbd_color_light" // For TBD/Unknown/Steer etc.
    // No "on" versions for these as they are typically icons or small indicators

    // Gender/Type Specific Colors - Dark
    const val THEME_MALE_COLOR_DARK = "theme_male_color_dark"
    const val THEME_FEMALE_COLOR_DARK = "theme_female_color_dark"
    const val THEME_TBD_COLOR_DARK = "theme_tbd_color_dark"

    // General App Settings
    const val CURRENT_THEME_NAME = "current_theme_name" // Stores the enum name of the current preset or "CUSTOM"
    const val SYNC_URL = "sync_url"
    const val SYNC_INTERVAL_HOURS = "sync_interval_hours"
    const val LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val INITIAL_SETUP_COMPLETE = "initial_setup_complete"
    const val TAG_COLORS = "tag_colors"
    const val ACTIVITY_TYPES = "activity_types"
    const val SAMPLE_DATA_INSTALLED = "sample_data_installed"

    // Add other settings keys as needed
}