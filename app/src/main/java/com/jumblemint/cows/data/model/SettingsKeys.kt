package com.jumblemint.cows.data.model

// This is now the single source of truth for all setting keys.
object SettingsKeys {
    const val TAG_COLORS = "tag_colors"
    const val ACTIVITY_TYPES = "activity_types"
    const val CLASSIFICATION_LABELS = "classification_labels"
    const val COAT_COLOR_PRESETS = "coat_color_presets"

    // Choose the correct key you intend to use. I'm assuming it's the one with "_ID".
    const val DEFAULT_CALF_PASTURE_ID = "default_calf_pasture_id"
    
    // Sample data tracking
    const val SAMPLE_DATA_INSTALLED = "sample_data_installed"
    
    // Theme settings
    const val THEME_PRIMARY_LIGHT = "theme_primary_light"
    const val THEME_SECONDARY_LIGHT = "theme_secondary_light"
    const val THEME_TERTIARY_LIGHT = "theme_tertiary_light"
    const val THEME_PRIMARY_DARK = "theme_primary_dark"
    const val THEME_SECONDARY_DARK = "theme_secondary_dark"
    const val THEME_TERTIARY_DARK = "theme_tertiary_dark"
    const val THEME_MALE_COLOR_LIGHT = "theme_male_color_light"
    const val THEME_FEMALE_COLOR_LIGHT = "theme_female_color_light"
    const val THEME_TBD_COLOR_LIGHT = "theme_tbd_color_light"
    const val THEME_MALE_COLOR_DARK = "theme_male_color_dark"
    const val THEME_FEMALE_COLOR_DARK = "theme_female_color_dark"
    const val THEME_TBD_COLOR_DARK = "theme_tbd_color_dark"
    const val CURRENT_THEME_PRESET = "current_theme_preset"
    
    // Authentication settings
    const val GOOGLE_AUTH_ENABLED = "google_auth_enabled"
    const val USE_LOCAL_ACCOUNT = "use_local_account"
    const val LOCAL_USER_NAME = "local_user_name"
    const val PREMIUM_FEATURES_ENABLED = "premium_features_enabled"
}