package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.ActivityTypeConfig
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ActivityViewModel(
    private val repository: CattleRepository
) : ViewModel() {

    // Load activity types to show in dropdown
    val availableActivityTypes: StateFlow<List<ActivityTypeConfig>> = repository.getAllActiveActivityTypes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}