package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository

class AddBirthViewModelFactory(
    private val repository: CattleRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBirthViewModel::class.java)) {
            return AddBirthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}