package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService

class HerdViewModelFactory(
    private val repository: CattleRepository,
    private val authService: AuthService,
    private val syncService: SyncService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HerdViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HerdViewModel(repository, authService, syncService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}