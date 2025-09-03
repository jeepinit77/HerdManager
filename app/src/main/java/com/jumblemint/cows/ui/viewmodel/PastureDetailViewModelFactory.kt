package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository

class PastureDetailViewModelFactory(
    private val repository: CattleRepository,
    private val pastureId: String
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PastureDetailViewModel::class.java)) {
            return PastureDetailViewModel(repository, pastureId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}