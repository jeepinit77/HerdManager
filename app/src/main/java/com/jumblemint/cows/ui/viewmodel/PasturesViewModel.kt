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
    val pastures: List<PastureWithCowCount> = emptyList(), // This will now refer to the one in PastureWithCowCount.kt
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
            cattleRepository.getPasturesWithCowCount().collect { pastureData: List<PastureWithCowCount> ->
                _uiState.update { currentState ->
                    currentState.copy(pastures = pastureData, isLoading = false)
                }
            }
        }
        
        viewModelScope.launch {
            cattleRepository.getUnassignedCowCount().collect { unassignedCount ->
                _uiState.update { currentState ->
                    currentState.copy(unassignedCowCount = unassignedCount)
                }
            }
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
