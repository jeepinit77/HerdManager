package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CowsViewModel(
    private val repository: CattleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CowsUiState())
    val uiState: StateFlow<CowsUiState> = _uiState.asStateFlow()
    
    init {
        loadCows()
        initializeDefaultData()
    }
    
    private fun loadCows() {
        viewModelScope.launch {
            repository.getAllCows().collect { cows ->
                _uiState.value = _uiState.value.copy(
                    cows = cows,
                    isLoading = false
                )
            }
        }
    }
    
    private fun initializeDefaultData() {
        viewModelScope.launch {
            repository.initializeDefaultData()
        }
    }
    
    fun filterCowsByStatus(status: Status?) {
        viewModelScope.launch {
            if (status == null) {
                val cows = repository.getAllCows().first()
                _uiState.value = _uiState.value.copy(
                    cows = cows,
                    selectedStatus = null
                )
            } else {
                val cows = repository.getCowsByStatus(status).first()
                _uiState.value = _uiState.value.copy(
                    cows = cows,
                    selectedStatus = status
                )
            }
        }
    }
    
    // MODIFIED: pastureId parameter type changed to String?
    fun filterCowsByPasture(pastureId: String?) {
        viewModelScope.launch {
            if (pastureId == null) {
                val cows = repository.getAllCows().first()
                _uiState.value = _uiState.value.copy(
                    cows = cows,
                    selectedPastureId = null // This will now assign String? (null)
                )
            } else {
                // pastureId is now String?, repository.getCowsByPasture should expect String
                val cows = repository.getCowsByPasture(pastureId).first() // Error was here
                _uiState.value = _uiState.value.copy(
                    cows = cows,
                    selectedPastureId = pastureId // This will now assign String?
                )
            }
        }
    }
    
    fun searchCows(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // Filter cows based on search query
        // This could be enhanced with a proper search implementation
    }

    // Deletion with undo helpers
    suspend fun deleteCow(cow: Cow) {
        repository.deleteCow(cow)
    }

    suspend fun undoDeleteCow(cow: Cow) {
        // Simple undo: reinsert the previous item (id may autogenerate)
        repository.insertCow(cow.copy(id = 0))
        loadCows()
    }

    fun toggleWatch(cow: Cow) {
        viewModelScope.launch {
            repository.updateCowWatchStatus(cow.id, !cow.isWatched)
        }
    }
}

data class CowsUiState(
    val cows: List<Cow> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatus: Status? = null,
    // MODIFIED: selectedPastureId type changed to String?
    val selectedPastureId: String? = null,
    val error: String? = null
)
