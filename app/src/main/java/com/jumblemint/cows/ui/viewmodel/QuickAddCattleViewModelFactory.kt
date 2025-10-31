package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.repository.CattleRepository

class QuickAddCattleViewModelFactory(
    private val application: CattleApplication,
    private val repository: CattleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickAddCattleViewModel::class.java)) {
            return QuickAddCattleViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
