package com.jumblemint.cows.ui.viewmodel

import android.app.Application // Added Application import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository

class ReportsViewModelFactory(
    private val application: Application, // Added Application parameter
    private val repository: CattleRepository,
    private val authService: com.jumblemint.cows.auth.AuthService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            // Pass application to the ReportsViewModel constructor
            return ReportsViewModel(application, repository, authService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}