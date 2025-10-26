package com.jumblemint.cows.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.model.User
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import com.jumblemint.cows.ui.components.DataMergeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthService,
    private val repository: CattleRepository,
    private val syncService: SyncService
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
    
    fun showDataMergeDialog(hasLocalData: Boolean, hasServerData: Boolean) {
        _uiState.value = _uiState.value.copy(
            showDataMergeDialog = true,
            hasLocalData = hasLocalData,
            hasServerData = hasServerData
        )
    }
    
    fun hideDataMergeDialog() {
        _uiState.value = _uiState.value.copy(showDataMergeDialog = false)
    }
    
    suspend fun checkForExistingData(userId: String? = null): Pair<Boolean, Boolean> {
        // Check if there's local data
        val hasLocalData = try {
            val cows = repository.getAllCowsSync()
            val pastures = repository.getAllPasturesSync()
            val activities = repository.getAllActivitiesSync()
            val notes = repository.getAllNotesSync()
            cows.isNotEmpty() || pastures.isNotEmpty() || activities.isNotEmpty() || notes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
        
        // Check if there's server data
        val hasServerData = if (userId != null) {
            try {
                syncService.checkServerDataExists(userId)
            } catch (e: Exception) {
                true // Assume data exists to be safe
            }
        } else {
            true // Assume there might be server data if we don't have userId yet
        }
        
        return Pair(hasLocalData, hasServerData)
    }
    
    fun signInWithDataMergeOption(account: GoogleSignInAccount, mergeOption: DataMergeOption) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authService.signInWithGoogle(account)
            result.fold(
                onSuccess = { user ->
                    runCatching {
                        handleDataMerge(user.uid, mergeOption)
                    }.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null,
                            showDataMergeDialog = false
                        )
                    }.onFailure { mergeError ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Data merge failed: ${mergeError.message}",
                            showDataMergeDialog = true
                        )
                    }
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
    
    private suspend fun handleDataMerge(userId: String, mergeOption: DataMergeOption) {
        try {
            runCatching { syncService.stopRealtimeSync(userId) }
                .onFailure { println("AuthViewModel: Unable to stop real-time sync before merge: ${it.message}") }

            when (mergeOption) {
                DataMergeOption.MERGE_WITH_SERVER -> {
                    // Trigger a normal sync that merges data
                    syncService.syncUserData(userId).getOrThrow()
                }
                DataMergeOption.REPLACE_SERVER_WITH_DEVICE -> {
                    // Clear server data and upload all local data
                    syncService.clearServerData(userId)
                    syncService.forceUploadAllData(userId)
                }
                DataMergeOption.REPLACE_DEVICE_WITH_SERVER -> {
                    // Clear local data and download all server data
                    // Clear everything locally so device is fully replaced by cloud data
                    repository.deleteAllActivities()
                    repository.deleteAllNotes()
                    repository.deleteAllCows()
                    repository.deleteAllPastures()
                    repository.deleteAllTagColors(hardDelete = true)
                    repository.deleteAllActivityTypeConfigs()
                    repository.deleteAllBreeds()
                    repository.deleteAllSettings()
                    // Now download from server
                    syncService.syncUserData(userId).getOrThrow()
                }
            }
        } catch (e: Exception) {
            println("AuthViewModel: Data merge failed for option $mergeOption: ${e.message}")
            throw e
        } finally {
            runCatching { syncService.startRealtimeSync(userId) }
                .onFailure { println("AuthViewModel: Unable to restart real-time sync after merge: ${it.message}") }
        }
    }
}

data class AuthUiState(
    val currentUser: User? = null,
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDataMergeDialog: Boolean = false,
    val hasLocalData: Boolean = false,
    val hasServerData: Boolean = false
)