package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PastureDetailViewModel(
    private val repository: CattleRepository,
    private val pastureId: String
) : ViewModel() {
    
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
        repository.deleteCow(cow)
    }
    
    suspend fun undoDeleteCow(cow: Cow) {
        // Simple undo: reinsert the previous item (id may autogenerate)
        repository.insertCow(cow.copy(id = 0))
    }
}

data class PastureDetailUiState(
    val pasture: Pasture? = null,
    val activeCows: List<Cow> = emptyList(),
    val classificationBreakdown: Map<Classification, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)