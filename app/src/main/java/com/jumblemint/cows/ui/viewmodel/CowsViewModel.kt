package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.util.AgeRangeKeys // Import centralized keys
import com.jumblemint.cows.util.AgeUtils // Import centralized utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

// Local AgeRangeKeys object removed, will use the one from com.jumblemint.cows.util

class CowsViewModel(
    application: CattleApplication,
    private val repository: CattleRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(CowsUiState())
    val uiState: StateFlow<CowsUiState> = _uiState.asStateFlow()
    
    init {
        loadCowsWithFilters()
    }

    private fun cowMatchesSelectedAgeRanges(cow: Cow, selectedKeys: Set<String>, today: LocalDate): Boolean {
        if (selectedKeys.isEmpty()) return true
        for (key in selectedKeys) {
            if (AgeUtils.cowMatchesAgeRangeKey(cow, key, today)) {
                return true // Cow matches at least one of the selected age ranges
            }
        }
        return false // Cow does not match any of the selected age ranges
    }
    
    private fun loadCowsWithFilters() {
        viewModelScope.launch {
            combine(
                repository.getAllCows(),
                repository.getAllPastures(),
                _uiState 
            ) { allCows, pastures, currentState ->
                val pastureNames = pastures.map { it.name }
                val availableBreeds = allCows.mapNotNull { it.breed }.distinct().sorted()
                val availableTagColors = allCows.mapNotNull { it.tagColor }.distinct().sorted()
                val today = LocalDate.now()
                
                val filteredCows = allCows.filter { cow ->
                    val statusMatch = currentState.selectedStatuses.isEmpty() || currentState.selectedStatuses.contains(cow.status)
                    val classificationMatch = currentState.selectedClassifications.isEmpty() || currentState.selectedClassifications.contains(cow.classification)
                    val genderMatch = currentState.selectedGenders.isEmpty() || currentState.selectedGenders.contains(cow.gender)
                    val pastureMatch = if (currentState.selectedPastures.isEmpty()) {
                        true
                    } else {
                        val cowPastureName = cow.pastureId?.let { pastureId -> pastures.find { it.id == pastureId }?.name }
                        cowPastureName?.let { currentState.selectedPastures.contains(it) } ?: false
                    }
                    val breedMatch = currentState.selectedBreeds.isEmpty() || cow.breed?.let { currentState.selectedBreeds.contains(it) } == true
                    val tagColorMatch = currentState.selectedTagColors.isEmpty() || cow.tagColor?.let { currentState.selectedTagColors.contains(it) } == true
                    val watchedMatch = currentState.selectedIsWatched == null || cow.isWatched == currentState.selectedIsWatched
                    val ageMatch = cowMatchesSelectedAgeRanges(cow, currentState.selectedAgeRanges, today) // Updated to use new helper
                    
                    val searchMatch = if (currentState.searchQuery.isBlank()) {
                        true
                    } else {
                        val query = currentState.searchQuery.lowercase()
                        (cow.name?.lowercase()?.contains(query) == true) ||
                        (cow.tagNumber?.lowercase()?.contains(query) == true) ||
                        cow.classification.name.lowercase().contains(query) ||
                        cow.gender.name.lowercase().contains(query) ||
                        (cow.breed?.lowercase()?.contains(query) == true) ||
                        (cow.status.name.lowercase().contains(query) == true) || 
                        (cow.tagColor?.lowercase()?.contains(query) == true)
                    }
                    
                    statusMatch && classificationMatch && genderMatch && pastureMatch && breedMatch && tagColorMatch && watchedMatch && ageMatch && searchMatch
                }
                
                Quartet(filteredCows, pastureNames, availableBreeds, availableTagColors)
            }.collect { (filteredCows, pastureNames, availableBreeds, availableTagColors) ->
                _uiState.value = _uiState.value.copy(
                    cows = filteredCows,
                    availablePastures = pastureNames,
                    availableBreeds = availableBreeds,
                    availableTagColors = availableTagColors,
                    isLoading = false
                )
            }
        }
    }
        
    fun toggleStatusFilter(status: Status) {
        val current = _uiState.value.selectedStatuses.toMutableSet()
        if (current.contains(status)) current.remove(status) else current.add(status)
        _uiState.value = _uiState.value.copy(selectedStatuses = current)
    }
    
    fun toggleClassificationFilter(classification: Classification) {
        val current = _uiState.value.selectedClassifications.toMutableSet()
        if (current.contains(classification)) current.remove(classification) else current.add(classification)
        _uiState.value = _uiState.value.copy(selectedClassifications = current)
    }
    
    fun toggleGenderFilter(gender: Gender) {
        val current = _uiState.value.selectedGenders.toMutableSet()
        if (current.contains(gender)) current.remove(gender) else current.add(gender)
        _uiState.value = _uiState.value.copy(selectedGenders = current)
    }
    
    fun togglePastureFilter(pastureName: String) {
        val current = _uiState.value.selectedPastures.toMutableSet()
        if (current.contains(pastureName)) current.remove(pastureName) else current.add(pastureName)
        _uiState.value = _uiState.value.copy(selectedPastures = current)
    }

    fun toggleBreedFilter(breed: String) {
        val current = _uiState.value.selectedBreeds.toMutableSet()
        if (current.contains(breed)) current.remove(breed) else current.add(breed)
        _uiState.value = _uiState.value.copy(selectedBreeds = current)
    }

    fun toggleTagColorFilter(tagColor: String) { 
        val current = _uiState.value.selectedTagColors.toMutableSet()
        if (current.contains(tagColor)) current.remove(tagColor) else current.add(tagColor)
        _uiState.value = _uiState.value.copy(selectedTagColors = current)
    }

    fun setWatchedFilter(isWatched: Boolean?) { 
        _uiState.value = _uiState.value.copy(selectedIsWatched = isWatched)
    }

    fun toggleAgeRangeFilter(ageRangeKey: String) { 
        val current = _uiState.value.selectedAgeRanges.toMutableSet()
        if (current.contains(ageRangeKey)) current.remove(ageRangeKey) else current.add(ageRangeKey)
        _uiState.value = _uiState.value.copy(selectedAgeRanges = current)
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedStatuses = emptySet(), 
            selectedClassifications = emptySet(),
            selectedGenders = emptySet(),
            selectedPastures = emptySet(),
            selectedBreeds = emptySet(),
            selectedTagColors = emptySet(), 
            selectedIsWatched = null, 
            selectedAgeRanges = emptySet()
        )
    }

    suspend fun deleteCow(cow: Cow) {
        val deletedCow = cow.copy(isDeleted = true, lastSyncAt = 0L)
        repository.updateCow(deletedCow)
        viewModelScope.launch {
            val application = getApplication<CattleApplication>()
            application.authService.currentUser.first()?.let { user ->
                if (!user.isLocalUser) {
                    try { application.syncService.syncItemImmediately(user.uid, deletedCow) } catch (e: Exception) { println("Failed to sync deletion: ${e.message}") }
                }
            }
        }
    }

    suspend fun undoDeleteCow(cow: Cow) {
        val restoredCow = cow.copy(isDeleted = false, lastSyncAt = 0L)
        repository.updateCow(restoredCow)
        viewModelScope.launch {
            val application = getApplication<CattleApplication>()
            application.authService.currentUser.first()?.let { user ->
                if (!user.isLocalUser) {
                    try { application.syncService.syncItemImmediately(user.uid, restoredCow) } catch (e: Exception) { println("Failed to sync restoration: ${e.message}") }
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

data class Quartet<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class CowsUiState(
    val cows: List<Cow> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatuses: Set<Status> = emptySet(),
    val selectedClassifications: Set<Classification> = emptySet(),
    val selectedGenders: Set<Gender> = emptySet(), 
    val selectedPastures: Set<String> = emptySet(),
    val availablePastures: List<String> = emptyList(),
    val selectedBreeds: Set<String> = emptySet(),
    val availableBreeds: List<String> = emptyList(),
    val selectedTagColors: Set<String> = emptySet(),
    val availableTagColors: List<String> = emptyList(),
    val selectedIsWatched: Boolean? = null,
    val selectedAgeRanges: Set<String> = emptySet(),
    val error: String? = null
)
