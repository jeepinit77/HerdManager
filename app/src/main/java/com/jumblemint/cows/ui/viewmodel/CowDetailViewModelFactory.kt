package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.preferences.tipsDataStore // Corrected import
import com.jumblemint.cows.data.repository.CattleRepository

class CowDetailViewModelFactory(
    private val application: CattleApplication,
    private val repository: CattleRepository,
    private val cowId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CowDetailViewModel::class.java)) {
            // Pass the tipsDataStore from the application context
            return CowDetailViewModel(application, repository, cowId, application.tipsDataStore) as T // Changed to tipsDataStore
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}