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

    private fun loadPastures() {
        viewModelScope.launch {
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
                        isLoading = false
                    )
                }
            }.collect { }
        }
    }

    fun deletePasture(pastureToDelete: Pasture) { // Removed Result<Unit> as it's async now
        viewModelScope.launch {
            pastureStateBeforeDelete = pastureToDelete // Save original state for potential undo

            val pastureToMarkAsDeleted = pastureToDelete.copy(
                isDeleted = true,
                lastSyncAt = 0L // Mark for sync
            )

            try {
                cattleRepository.updatePasture(pastureToMarkAsDeleted) // Use update for soft delete

                // Sync the deletion to Firestore
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { user ->
                    if (!user.isLocalUser) {
                        application.syncService.syncItemImmediately(user.uid, pastureToMarkAsDeleted)
                            .onFailure {
                                println("PasturesViewModel: Error immediately syncing pasture deletion: ${it.message}")
                                // Optionally update UI with sync error for this specific item
                            }
                    }
                }
            } catch (e: Exception) {
                // Handle local update error
                 _uiState.update { it.copy(error = "Error deleting pasture locally: ${e.message}") }
                // Restore original state if local update failed, so undo can work with original
                pastureStateBeforeDelete = null 
            }
        }
    }

    fun undoDeletePasture() {
        pastureStateBeforeDelete?.let { pastureToRestore ->
            viewModelScope.launch {
                // For undo, we want to restore its non-deleted state and mark for sync
                val pastureToUnDelete = pastureToRestore.copy(
                    isDeleted = false,
                    lastSyncAt = 0L // Mark for sync as it's a change of state
                )
                try {
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
                     _uiState.update { it.copy(error = "Error undoing pasture deletion: ${e.message}") }
                }
            }
        }
    }

    fun insertNewPasture(pastureFromUi: Pasture) {
        viewModelScope.launch {
            val application = getApplication<CattleApplication>()
            val currentUser = application.authService.currentUser.first()
            
            if (currentUser != null) {
                val currentUserId = currentUser.uid
                val newUniqueId = UUID.randomUUID().toString()

                val pastureToSave = pastureFromUi.copy(
                    id = newUniqueId,
                    firestoreId = newUniqueId,
                    isDeleted = false,
                    createdBy = currentUserId,
                    updatedBy = currentUserId,
                    lastSyncAt = 0L
                )

                try {
                    cattleRepository.insertPasture(pastureToSave)

                    if (!currentUser.isLocalUser) {
                        application.syncService.syncItemImmediately(currentUserId, pastureToSave)
                            .onFailure {
                                println("PasturesViewModel: Error immediately syncing new pasture: ${it.message}")
                            }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error inserting new pasture: ${e.message}") }
                }
            } else {
                _uiState.update { it.copy(error = "No authenticated user. Cannot save pasture.") } 
            }
        }
    }
}
