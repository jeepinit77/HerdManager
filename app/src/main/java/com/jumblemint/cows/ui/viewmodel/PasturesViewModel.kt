package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Pasture // Ensure this import is present if not already
// It's in the same package, so PastureWithCowCount should be available without an explicit import
// However, if PastureWithCowCount.kt was in a sub-package, an import would be:
// import com.jumblemint.cows.ui.viewmodel.pasture.PastureWithCowCount 
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Removed the duplicate PastureWithCowCount data class definition from here

data class PasturesUiState(
    val pastures: List<PastureWithDetails> = emptyList(), // Use PastureWithDetails for UI
    val unassignedCowCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PasturesViewModel(private val cattleRepository: CattleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PasturesUiState(isLoading = true))
    val uiState: StateFlow<PasturesUiState> = _uiState.asStateFlow()

    private var recentlyDeletedPasture: Pasture? = null

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

    fun deletePasture(pastureToDelete: Pasture): Result<Unit> {
        return try {
            viewModelScope.launch {
                recentlyDeletedPasture = pastureToDelete
                cattleRepository.deletePasture(pastureToDelete)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun undoDeletePasture() {
        recentlyDeletedPasture?.let { pastureToRestore ->
            viewModelScope.launch {
                cattleRepository.insertPasture(pastureToRestore) 
                recentlyDeletedPasture = null
            }
        }
    }

    // MARKER_FOR_PASTURE_INSERT_METHOD
    fun insertNewPasture(pasture: Pasture) {
        viewModelScope.launch {
            cattleRepository.insertPasture(pasture)
        }
    }
}
