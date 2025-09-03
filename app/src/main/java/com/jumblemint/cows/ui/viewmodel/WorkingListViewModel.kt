package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Gender
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
        // Update available pastures
        val pastureNames = pastures.map { it.name } + "Unassigned"
        _uiState.value = state.copy(availablePastures = pastureNames)
        
        // Filter by status first (default to active only)
        val statusFilteredCows = allCows.filter { cow ->
            state.selectedStatuses.isEmpty() || state.selectedStatuses.contains(cow.status)
        }
        
        // Apply other filters
        statusFilteredCows.filter { cow ->
            // Pasture filter
            val pastureMatch = if (state.selectedPastures.isEmpty()) {
                true
            } else {
                state.selectedPastures.any { selectedPasture ->
                    if (selectedPasture == "Unassigned") {
                        cow.pastureId == null
                    } else {
                        val pasture = pastures.find { it.name == selectedPasture }
                        cow.pastureId == pasture?.id?.toString()
                    }
                }
            }
            
            // Classification filter
            val classificationMatch = state.selectedClassifications.isEmpty() || 
                                    state.selectedClassifications.contains(cow.classification)
            
            // Gender filter
            val genderMatch = state.selectedGenders.isEmpty() || 
                            state.selectedGenders.contains(cow.gender)
            
            pastureMatch && classificationMatch && genderMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun toggleStatusFilter(status: Status) {
        val currentStatuses = _uiState.value.selectedStatuses.toMutableSet()
        if (currentStatuses.contains(status)) {
            currentStatuses.remove(status)
        } else {
            currentStatuses.add(status)
        }
        // If no statuses selected, default back to active
        val newStatuses = if (currentStatuses.isEmpty()) setOf(Status.ACTIVE) else currentStatuses
        _uiState.value = _uiState.value.copy(selectedStatuses = newStatuses)
        _checkedItems.value = emptySet()
    }
    
    fun togglePastureFilter(pasture: String) {
        val currentPastures = _uiState.value.selectedPastures.toMutableSet()
        if (currentPastures.contains(pasture)) {
            currentPastures.remove(pasture)
        } else {
            currentPastures.add(pasture)
        }
        _uiState.value = _uiState.value.copy(selectedPastures = currentPastures)
        _checkedItems.value = emptySet()
    }
    
    fun toggleClassificationFilter(classification: Classification) {
        val currentClassifications = _uiState.value.selectedClassifications.toMutableSet()
        if (currentClassifications.contains(classification)) {
            currentClassifications.remove(classification)
        } else {
            currentClassifications.add(classification)
        }
        _uiState.value = _uiState.value.copy(selectedClassifications = currentClassifications)
        _checkedItems.value = emptySet()
    }
    
    fun toggleGenderFilter(gender: Gender) {
        val currentGenders = _uiState.value.selectedGenders.toMutableSet()
        if (currentGenders.contains(gender)) {
            currentGenders.remove(gender)
        } else {
            currentGenders.add(gender)
        }
        _uiState.value = _uiState.value.copy(selectedGenders = currentGenders)
        _checkedItems.value = emptySet()
    }
    
    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedStatuses = setOf(Status.ACTIVE), // Reset to active only
            selectedPastures = emptySet(),
            selectedClassifications = emptySet(),
            selectedGenders = emptySet()
        )
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
    val selectedStatuses: Set<Status> = setOf(Status.ACTIVE), // Default to active only
    val selectedPastures: Set<String> = emptySet(),
    val selectedClassifications: Set<Classification> = emptySet(),
    val selectedGenders: Set<Gender> = emptySet(),
    val availablePastures: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)