package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository

class CowInfoViewModelFactory(
    private val repository: CattleRepository,
    private val cowId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CowInfoViewModel::class.java)) {
            return CowInfoViewModel(repository, cowId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}