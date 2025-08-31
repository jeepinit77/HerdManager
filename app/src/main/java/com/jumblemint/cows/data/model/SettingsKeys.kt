package com.jumblemint.cows.data.model

// This is now the single source of truth for all setting keys.
object SettingsKeys {
    const val TAG_COLORS = "tag_colors"
    const val ACTIVITY_TYPES = "activity_types"
    const val CLASSIFICATION_LABELS = "classification_labels"
    const val COAT_COLOR_PRESETS = "coat_color_presets"

    // Choose the correct key you intend to use. I'm assuming it's the one with "_ID".
    const val DEFAULT_CALF_PASTURE_ID = "default_calf_pasture_id"
}