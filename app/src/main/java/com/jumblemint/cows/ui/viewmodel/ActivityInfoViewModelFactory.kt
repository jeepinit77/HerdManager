package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository

class ActivityInfoViewModelFactory(
    private val repository: CattleRepository,
    private val activityId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityInfoViewModel(repository, activityId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}