package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID // Added for UUID generation

data class PasturesUiState(
    val pastures: List<PastureWithDetails> = emptyList(),
    val unassignedCowCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PasturesViewModel(
    application: CattleApplication,
    private val cattleRepository: CattleRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PasturesUiState(isLoading = true))
    val uiState: StateFlow<PasturesUiState> = _uiState.asStateFlow()

    // Store the pasture as it was *before* marking for deletion, for undo.
    private var pastureStateBeforeDelete: Pasture? = null

    init {
        loadPastures()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadPastures() {
        viewModelScope.launch {
            try {
                combine(
                    cattleRepository.getPasturesWithCowCount(),
                    cattleRepository.getCowsByStatus(com.jumblemint.cows.data.model.Status.ACTIVE),
                    cattleRepository.getUnassignedCowCount()
                ) { pasturesWithCounts: List<PastureWithCowCount>, activeCows: List<com.jumblemint.cows.data.model.Cow>, unassignedCount: Int ->
                    val pasturesWithDetails = pasturesWithCounts.map { pastureWithCount ->
                        val cowsInPasture = activeCows.filter { it.pastureId == pastureWithCount.pasture.id }
                        val classificationBreakdown = cowsInPasture.groupBy { it.classification }
                            .mapValues { it.value.size }
                        
                        PastureWithDetails(
                            pastureWithCount = pastureWithCount,
                            classificationBreakdown = classificationBreakdown
                        )
                    }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            pastures = pasturesWithDetails,
                            unassignedCowCount = unassignedCount,
                            isLoading = false,
                            error = null // Clear any previous errors
                        )
                    }
                }.collect { }
            } catch (e: Exception) {
                println("PasturesViewModel: Error loading pastures: ${e.message}")
                e.printStackTrace()
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Error loading pastures: ${e.message}"
                    )
                }
            }
        }
    }

    fun deletePasture(pastureToDelete: Pasture) {
        viewModelScope.launch {
            try {
                pastureStateBeforeDelete = pastureToDelete // Save original state for potential undo

                val pastureToMarkAsDeleted = pastureToDelete.copy(
                    isDeleted = true,
                    lastSyncAt = 0L // Mark for sync
                )

                cattleRepository.updatePasture(pastureToMarkAsDeleted) // Use update for soft delete

                // Sync the deletion to Firestore
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { user ->
                    if (!user.isLocalUser) {
                        application.syncService.syncItemImmediately(user.uid, pastureToMarkAsDeleted)
                            .onFailure {
                                println("PasturesViewModel: Error immediately syncing pasture deletion: ${it.message}")
                            }
                    }
                }
            } catch (e: Exception) {
                println("PasturesViewModel: Error deleting pasture: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(error = "Error deleting pasture: ${e.message}") }
                pastureStateBeforeDelete = null 
            }
        }
    }

    fun undoDeletePasture() {
        pastureStateBeforeDelete?.let { pastureToRestore ->
            viewModelScope.launch {
                try {
                    // For undo, we want to restore its non-deleted state and mark for sync
                    val pastureToUnDelete = pastureToRestore.copy(
                        isDeleted = false,
                        lastSyncAt = 0L // Mark for sync as it's a change of state
                    )
                    
                    cattleRepository.updatePasture(pastureToUnDelete) // Update to un-delete
                    pastureStateBeforeDelete = null // Clear the state after successful undo

                    // Sync the un-deletion to Firestore
                    val application = getApplication<CattleApplication>()
                    application.authService.currentUser.first()?.let { user ->
                        if (!user.isLocalUser) {
                            application.syncService.syncItemImmediately(user.uid, pastureToUnDelete)
                                .onFailure {
                                    println("PasturesViewModel: Error immediately syncing pasture un-deletion: ${it.message}")
                                }
                        }
                    }
                } catch (e: Exception) {
                    println("PasturesViewModel: Error undoing pasture deletion: ${e.message}")
                    e.printStackTrace()
                    _uiState.update { it.copy(error = "Error undoing pasture deletion: ${e.message}") }
                }
            }
        }
    }

    fun insertNewPasture(pastureFromUi: Pasture) {
        viewModelScope.launch {
            try {
                val application = getApplication<CattleApplication>()
                val currentUser = application.authService.currentUser.first()
                
                val isNewPasture = pastureFromUi.id.isBlank()
                val pastureToSave = if (isNewPasture) {
                    // New pasture
                    val newUniqueId = UUID.randomUUID().toString()
                    pastureFromUi.copy(
                        id = newUniqueId,
                        firestoreId = newUniqueId,
                        isDeleted = false,
                        createdBy = currentUser?.uid,
                        updatedBy = currentUser?.uid,
                        lastSyncAt = 0L
                    )
                } else {
                    // Existing pasture (edit)
                    pastureFromUi.copy(
                        updatedBy = currentUser?.uid,
                        lastSyncAt = 0L
                    )
                }

                if (isNewPasture) {
                    cattleRepository.insertPasture(pastureToSave)
                } else {
                    cattleRepository.updatePasture(pastureToSave)
                }

                // Only sync if user is authenticated and not local
                currentUser?.let { user ->
                    if (!user.isLocalUser) {
                        application.syncService.syncItemImmediately(user.uid, pastureToSave)
                            .onFailure {
                                println("PasturesViewModel: Error immediately syncing pasture: ${it.message}")
                            }
                    }
                }
            } catch (e: Exception) {
                println("PasturesViewModel: Error saving pasture: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(error = "Error saving pasture: ${e.message}") }
            }
        }
    }
}
