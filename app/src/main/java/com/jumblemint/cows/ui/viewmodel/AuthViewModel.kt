package com.jumblemint.cows.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.model.User
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthService,
    private val repository: CattleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Listen to auth state changes from AuthService
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isSignedIn = user != null,
                    isLoading = false
                )
                
                // Save user to local database
                user?.let { repository.insertUser(it) }
            }
        }
        
        // Also listen to sign-in state
        viewModelScope.launch {
            authService.isSignedIn.collect { isSignedIn ->
                _uiState.value = _uiState.value.copy(
                    isSignedIn = isSignedIn
                )
            }
        }
    }
    
    fun getGoogleSignInIntent(context: Context): Intent {
        return authService.googleSignInClient.signInIntent
    }
    
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authService.signInWithGoogle(account)
            result.fold(
                onSuccess = { user ->
                    // Don't update state here - let the AuthService flow handle it
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Sign-in failed"
                    )
                }
            )
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
            // Don't update state here - let the AuthService flow handle it
            _uiState.value = _uiState.value.copy(error = null)
        }
    }
    
    fun signInAsDemoGoogleUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authService.signInAsDemoGoogleUser(repository)
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Demo sign-in failed: ${e.message}"
                )
            }
        }
    }
    
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AuthUiState(
    val currentUser: User? = null,
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)