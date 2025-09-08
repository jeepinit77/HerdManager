package com.jumblemint.cows.ui.viewmodel

import android.app.Application // <<< IMPORT Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.data.repository.CattleRepository

class SettingsViewModelFactory(
    private val application: Application, // <<< ADD Application parameter
    private val repository: CattleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            // <<< PASS application to SettingsViewModel constructor
            return SettingsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}