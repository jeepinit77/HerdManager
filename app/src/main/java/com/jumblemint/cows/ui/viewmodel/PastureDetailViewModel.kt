package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PastureDetailViewModel(
    application: CattleApplication,
    private val repository: CattleRepository,
    private val pastureId: String
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(PastureDetailUiState())
    val uiState: StateFlow<PastureDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadPastureDetails()
    }
    
    private fun loadPastureDetails() {
        viewModelScope.launch {
            combine(
                repository.getPastureById(pastureId),
                repository.getAllCows()
            ) { pasture, allCows ->
                val activeCowsInPasture = allCows.filter { cow ->
                    cow.pastureId == pastureId && cow.status == Status.ACTIVE
                }
                
                val classificationBreakdown = activeCowsInPasture
                    .groupBy { it.classification }
                    .mapValues { it.value.size }
                
                _uiState.value = _uiState.value.copy(
                    pasture = pasture,
                    activeCows = activeCowsInPasture,
                    classificationBreakdown = classificationBreakdown,
                    isLoading = false
                )
            }.collect { }
        }
    }
    
    fun toggleWatch(cow: Cow) {
        viewModelScope.launch {
            repository.updateCowWatchStatus(cow.id, !cow.isWatched)
        }
    }
    
    suspend fun deleteCow(cow: Cow) {
        // Soft delete: mark as deleted instead of hard delete
        val deletedCow = cow.copy(
            isDeleted = true,
            lastSyncAt = 0L // Mark for sync
        )
        repository.updateCow(deletedCow)
        
        // If user is signed in, immediately sync the deletion to cloud
        viewModelScope.launch {
            val application = getApplication<CattleApplication>()
            application.authService.currentUser.first()?.let { user ->
                if (!user.isLocalUser) {
                    try {
                        application.syncService.syncItemImmediately(user.uid, deletedCow)
                    } catch (e: Exception) {
                        println("Failed to sync deletion immediately: ${e.message}")
                    }
                }
            }
        }
    }
    
    suspend fun undoDeleteCow(cow: Cow) {
        // Undo soft delete: mark as not deleted
        val restoredCow = cow.copy(
            isDeleted = false,
            lastSyncAt = 0L // Mark for sync
        )
        repository.updateCow(restoredCow)
        
        // If user is signed in, immediately sync the restoration to cloud
        viewModelScope.launch {
            val application = getApplication<CattleApplication>()
            application.authService.currentUser.first()?.let { user ->
                if (!user.isLocalUser) {
                    try {
                        application.syncService.syncItemImmediately(user.uid, restoredCow)
                    } catch (e: Exception) {
                        println("Failed to sync restoration immediately: ${e.message}")
                    }
                }
            }
        }
    }
}

data class PastureDetailUiState(
    val pasture: Pasture? = null,
    val activeCows: List<Cow> = emptyList(),
    val classificationBreakdown: Map<Classification, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)