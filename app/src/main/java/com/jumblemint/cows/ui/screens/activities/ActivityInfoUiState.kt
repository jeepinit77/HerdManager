package com.jumblemint.cows.ui.screens.activities

import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.AnimalIdentifierMode

public data class ActivityInfoUiState(
    val activity: Activity? = null,
    val associatedCows: List<Cow> = emptyList(),
    val identifierMode: AnimalIdentifierMode = AnimalIdentifierMode.BOTH,
    val isLoading: Boolean = true,
    val error: String? = null
)