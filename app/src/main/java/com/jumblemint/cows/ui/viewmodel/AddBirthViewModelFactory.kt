package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.auth.AuthService // Ensure this import is present
import com.jumblemint.cows.sync.SyncService // Ensure this import is present

class AddBirthViewModelFactory(
    private val repository: CattleRepository,
    private val authService: AuthService, // Added
    private val syncService: SyncService  // Added
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBirthViewModel::class.java)) {
            return AddBirthViewModel(
                repository,
                authService, // Pass the service from the factory
                syncService  // Pass the service from the factory
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}