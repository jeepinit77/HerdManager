package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.repository.CattleRepository

class AuthViewModelFactory(
    private val authService: AuthService,
    private val repository: CattleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authService, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}