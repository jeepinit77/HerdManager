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
    
    // Authentication settings
    const val GOOGLE_AUTH_ENABLED = "google_auth_enabled"
    const val USE_LOCAL_ACCOUNT = "use_local_account"
    const val LOCAL_USER_NAME = "local_user_name"
    const val PREMIUM_FEATURES_ENABLED = "premium_features_enabled"
}