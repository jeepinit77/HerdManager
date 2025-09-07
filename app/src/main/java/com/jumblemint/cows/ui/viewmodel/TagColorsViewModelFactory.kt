package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService

class TagColorsViewModelFactory(
    private val repository: CattleRepository,
    private val syncService: SyncService,
    private val getUserId: () -> String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagColorsViewModel::class.java)) {
            return TagColorsViewModel(repository, syncService, getUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}