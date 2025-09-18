package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CowsViewModel(
    application: CattleApplication,
    private val repository: CattleRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(CowsUiState())
    val uiState: StateFlow<CowsUiState> = _uiState.asStateFlow()
    
    init {
        loadCowsWithFilters()
    }
    
    private fun loadCowsWithFilters() {
        viewModelScope.launch {
            combine(
                repository.getAllCows(),
                repository.getAllPastures(),
                _uiState // Add UI state to the combine so filtering reacts to state changes
            ) { allCows, pastures, currentState ->
                val pastureNames = pastures.map { it.name }
                val availableBreeds = allCows.mapNotNull { it.breed }.distinct().sorted()
                
                // Apply filters
                val filteredCows = allCows.filter { cow ->
                    val statusMatch = currentState.selectedStatuses.contains(cow.status)
                    
                    val classificationMatch = currentState.selectedClassifications.isEmpty() || 
                                            currentState.selectedClassifications.contains(cow.classification)
                    
                    val genderMatch = currentState.selectedGenders.isEmpty() || 
                                    currentState.selectedGenders.contains(cow.gender)
                    
                    val pastureMatch = if (currentState.selectedPastures.isEmpty()) {
                        true
                    } else {
                        val cowPastureName = cow.pastureId?.let { pastureId ->
                            pastures.find { it.id == pastureId }?.name
                        }
                        cowPastureName?.let { currentState.selectedPastures.contains(it) } ?: false
                    }

                    val breedMatch = currentState.selectedBreeds.isEmpty() ||
                                     cow.breed?.let { currentState.selectedBreeds.contains(it) } == true
                    
                    val searchMatch = if (currentState.searchQuery.isBlank()) {
                        true
                    } else {
                        val query = currentState.searchQuery.lowercase()
                        (cow.name?.lowercase()?.contains(query) == true) ||
                        (cow.tagNumber?.lowercase()?.contains(query) == true) ||
                        cow.classification.name.lowercase().contains(query) ||
                        cow.gender.name.lowercase().contains(query) ||
                        (cow.breed?.lowercase()?.contains(query) == true)
                    }
                    
                    statusMatch && classificationMatch && genderMatch && pastureMatch && breedMatch && searchMatch
                }
                // Pass availableBreeds to the Triple, and then to uiState.copy
                Triple(filteredCows, pastureNames, availableBreeds) 
            }.collect { (filteredCows, pastureNames, availableBreeds) ->
                _uiState.value = _uiState.value.copy(
                    cows = filteredCows,
                    availablePastures = pastureNames,
                    availableBreeds = availableBreeds,
                    isLoading = false // Set isLoading to false once data is processed
                )
            }
        }
    }
        
    fun toggleStatusFilter(status: Status) {
        val currentStatuses = _uiState.value.selectedStatuses.toMutableSet()
        if (currentStatuses.contains(status)) {
            currentStatuses.remove(status)
        } else {
            currentStatuses.add(status)
        }
        _uiState.value = _uiState.value.copy(selectedStatuses = currentStatuses)
    }
    
    fun toggleClassificationFilter(classification: Classification) {
        val currentClassifications = _uiState.value.selectedClassifications.toMutableSet()
        if (currentClassifications.contains(classification)) {
            currentClassifications.remove(classification)
        } else {
            currentClassifications.add(classification)
        }
        _uiState.value = _uiState.value.copy(selectedClassifications = currentClassifications)
    }
    
    fun toggleGenderFilter(gender: Gender) {
        val currentGenders = _uiState.value.selectedGenders.toMutableSet()
        if (currentGenders.contains(gender)) {
            currentGenders.remove(gender)
        } else {
            currentGenders.add(gender)
        }
        _uiState.value = _uiState.value.copy(selectedGenders = currentGenders)
    }
    
    fun togglePastureFilter(pastureName: String) {
        val currentPastures = _uiState.value.selectedPastures.toMutableSet()
        if (currentPastures.contains(pastureName)) {
            currentPastures.remove(pastureName)
        } else {
            currentPastures.add(pastureName)
        }
        _uiState.value = _uiState.value.copy(selectedPastures = currentPastures)
    }

    fun toggleBreedFilter(breed: String) {
        val currentBreeds = _uiState.value.selectedBreeds.toMutableSet()
        if (currentBreeds.contains(breed)) {
            currentBreeds.remove(breed)
        } else {
            currentBreeds.add(breed)
        }
        _uiState.value = _uiState.value.copy(selectedBreeds = currentBreeds)
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedStatuses = setOf(Status.ACTIVE), 
            selectedClassifications = emptySet(),
            selectedGenders = emptySet(),
            selectedPastures = emptySet(),
            selectedBreeds = emptySet(), // Clear selected breeds
            searchQuery = ""
        )
    }

    suspend fun deleteCow(cow: Cow) {
        val deletedCow = cow.copy(
            isDeleted = true,
            lastSyncAt = 0L 
        )
        repository.updateCow(deletedCow)
        
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
        val restoredCow = cow.copy(
            isDeleted = false,
            lastSyncAt = 0L
        )
        repository.updateCow(restoredCow)
        
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
    val selectedStatuses: Set<Status> = setOf(Status.ACTIVE),
    val selectedClassifications: Set<Classification> = emptySet(),
    val selectedGenders: Set<Gender> = emptySet(), 
    val selectedPastures: Set<String> = emptySet(),
    val availablePastures: List<String> = emptyList(),
    val selectedBreeds: Set<String> = emptySet(), // Added selectedBreeds
    val availableBreeds: List<String> = emptyList(), // Added availableBreeds
    val error: String? = null
)
