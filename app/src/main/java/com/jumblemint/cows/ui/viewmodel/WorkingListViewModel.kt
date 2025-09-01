package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkingListViewModel(
    private val repository: CattleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkingListUiState())
    val uiState: StateFlow<WorkingListUiState> = _uiState.asStateFlow()
    
    private val _checkedItems = MutableStateFlow<Set<Long>>(emptySet())
    val checkedItems: StateFlow<Set<Long>> = _checkedItems.asStateFlow()
    
    val filteredCows: StateFlow<List<Cow>> = combine(
        repository.getAllCows(),
        repository.getAllPastures(),
        _uiState
    ) { allCows, pastures, state ->
        val activeCows = allCows.filter { it.status == Status.ACTIVE }
        
        // Update available pastures
        val pastureNames = pastures.map { it.name } + "Unassigned"
        _uiState.value = state.copy(availablePastures = pastureNames)
        
        // Apply filters
        activeCows.filter { cow ->
            val pastureMatch = state.selectedPasture?.let { selectedPasture ->
                if (selectedPasture == "Unassigned") {
                    cow.pastureId == null
                } else {
                    val pasture = pastures.find { it.name == selectedPasture }
                    cow.pastureId == pasture?.id?.toString()
                }
            } ?: true
            
            val classificationMatch = state.selectedClassification?.let { selectedClassification ->
                cow.classification == selectedClassification
            } ?: true
            
            pastureMatch && classificationMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun updatePastureFilter(pasture: String?) {
        _uiState.value = _uiState.value.copy(selectedPasture = pasture)
        // Clear checked items when filter changes
        _checkedItems.value = emptySet()
    }
    
    fun updateClassificationFilter(classification: Classification?) {
        _uiState.value = _uiState.value.copy(selectedClassification = classification)
        // Clear checked items when filter changes
        _checkedItems.value = emptySet()
    }
    
    fun checkItem(cowId: Long) {
        _checkedItems.value = _checkedItems.value + cowId
    }
    
    fun uncheckItem(cowId: Long) {
        _checkedItems.value = _checkedItems.value - cowId
    }
    
    fun clearAllChecks() {
        _checkedItems.value = emptySet()
    }
}

data class WorkingListUiState(
    val selectedPasture: String? = null,
    val selectedClassification: Classification? = null,
    val availablePastures: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)