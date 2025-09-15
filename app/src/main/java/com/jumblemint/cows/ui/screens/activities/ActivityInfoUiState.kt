package com.jumblemint.cows.ui.screens.activities

import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.Cow

public data class ActivityInfoUiState(
    val activity: Activity? = null,
    val associatedCows: List<Cow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)