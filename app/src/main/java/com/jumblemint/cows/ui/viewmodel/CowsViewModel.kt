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
        initializeDefaultData()
    }
    
    private fun loadCowsWithFilters() {
        viewModelScope.launch {
            combine(
                repository.getAllCows(),
                repository.getAllPastures(),
                _uiState // Add UI state to the combine so filtering reacts to state changes
            ) { allCows, pastures, currentState ->
                val pastureNames = pastures.map { it.name }
                
                // Apply filters
                val filteredCows = allCows.filter { cow ->
                    // Status filter - always apply since we always have at least ACTIVE selected
                    val statusMatch = currentState.selectedStatuses.contains(cow.status)
                    
                    // Classification filter
                    val classificationMatch = currentState.selectedClassifications.isEmpty() || 
                                            currentState.selectedClassifications.contains(cow.classification)
                    
                    // Gender filter
                    val genderMatch = currentState.selectedGenders.isEmpty() || 
                                    currentState.selectedGenders.contains(cow.gender)
                    
                    // Pasture filter
                    val pastureMatch = if (currentState.selectedPastures.isEmpty()) {
                        true
                    } else {
                        val cowPastureName = cow.pastureId?.let { pastureId ->
                            pastures.find { it.id == pastureId }?.name
                        }
                        cowPastureName?.let { currentState.selectedPastures.contains(it) } ?: false
                    }
                    
                    // Search query filter
                    val searchMatch = if (currentState.searchQuery.isBlank()) {
                        true
                    } else {
                        val query = currentState.searchQuery.lowercase()
                        (cow.name?.lowercase()?.contains(query) == true) ||
                        (cow.tagNumber?.lowercase()?.contains(query) == true) ||
                        cow.classification.name.lowercase().contains(query) ||
                        cow.gender.name.lowercase().contains(query)
                    }
                    
                    statusMatch && classificationMatch && genderMatch && pastureMatch && searchMatch
                }
                
                Triple(filteredCows, pastureNames, false) // Return filtered data
            }.collect { (filteredCows, pastureNames, isLoading) ->
                _uiState.value = _uiState.value.copy(
                    cows = filteredCows,
                    availablePastures = pastureNames,
                    isLoading = isLoading
                )
            }
        }
    }
    
    private fun initializeDefaultData() {
        viewModelScope.launch {
            repository.initializeDefaultData()
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
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedStatuses = setOf(Status.ACTIVE), // Reset to ACTIVE only
            selectedClassifications = emptySet(),
            selectedGenders = emptySet(),
            selectedPastures = emptySet(),
            searchQuery = ""
        )
    }

    // Deletion with undo helpers
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
    val selectedStatuses: Set<Status> = setOf(Status.ACTIVE), // Default to ACTIVE selected
    val selectedClassifications: Set<Classification> = emptySet(), // Multi-select classifications
    val selectedGenders: Set<Gender> = emptySet(), // Multi-select genders
    val selectedPastures: Set<String> = emptySet(), // Multi-select pastures
    val availablePastures: List<String> = emptyList(), // Available pasture names for filtering
    val error: String? = null
)
