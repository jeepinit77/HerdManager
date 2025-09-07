package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService

class ActivityTypesViewModelFactory(
    private val repository: CattleRepository,
    private val syncService: SyncService,
    private val getUserId: () -> String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityTypesViewModel::class.java)) {
            return ActivityTypesViewModel(repository, syncService, getUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}