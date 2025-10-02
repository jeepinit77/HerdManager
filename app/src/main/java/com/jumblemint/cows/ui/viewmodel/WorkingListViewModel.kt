package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.screens.AnimalFilterState
import com.jumblemint.cows.util.AgeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

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
        val pastureNames = pastures.map { it.name } + "Unassigned"
        val breedNames = allCows.mapNotNull { it.breed }.distinct()
        val tagColors = allCows.mapNotNull { it.tagColor }.distinct()

        _uiState.value = state.copy(
            availablePastures = pastureNames,
            availableBreeds = breedNames,
            availableTagColors = tagColors
        )

        val today = LocalDate.now()
        allCows.filter { cow ->
            val statusMatch = state.selectedStatuses.isEmpty() || state.selectedStatuses.contains(cow.status)
            val classificationMatch = state.selectedClassifications.isEmpty() || state.selectedClassifications.contains(cow.classification)
            val genderMatch = state.selectedGenders.isEmpty() || state.selectedGenders.contains(cow.gender)
            val pastureMatch = state.selectedPastures.isEmpty() || state.selectedPastures.any { selectedPasture ->
                if (selectedPasture == "Unassigned") cow.pastureId == null
                else pastures.find { it.name == selectedPasture }?.id?.toString() == cow.pastureId
            }
            val breedMatch = state.selectedBreeds.isEmpty() || state.selectedBreeds.contains(cow.breed)
            val tagColorMatch = state.selectedTagColors.isEmpty() || state.selectedTagColors.contains(cow.tagColor)
            val watchedMatch = state.selectedIsWatched == null || state.selectedIsWatched == cow.isWatched
            val ageMatch = state.selectedAgeRanges.isEmpty() || state.selectedAgeRanges.any { ageRangeKey ->
                AgeUtils.cowMatchesAgeRangeKey(cow, ageRangeKey, today)
            }

            statusMatch && classificationMatch && genderMatch && pastureMatch && breedMatch && tagColorMatch && watchedMatch && ageMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun applyFilterState(filterState: AnimalFilterState) {
        _uiState.value = _uiState.value.copy(
            selectedStatuses = filterState.statuses.toSet(),
            selectedClassifications = filterState.classifications.toSet(),
            selectedGenders = filterState.genders.toSet(),
            selectedPastures = filterState.pastures.toSet(),
            selectedBreeds = filterState.breeds.toSet(),
            selectedTagColors = filterState.tagColors.toSet(),
            selectedIsWatched = filterState.isWatched,
            selectedAgeRanges = filterState.selectedAgeRanges.toSet()
        )
        _checkedItems.value = emptySet()
    }

    fun clearAllFilters() {
        _uiState.value = WorkingListUiState()
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
    val selectedStatuses: Set<Status> = emptySet(),
    val selectedPastures: Set<String> = emptySet(),
    val selectedClassifications: Set<Classification> = emptySet(),
    val selectedGenders: Set<Gender> = emptySet(),
    val selectedBreeds: Set<String> = emptySet(),
    val selectedTagColors: Set<String> = emptySet(),
    val selectedIsWatched: Boolean? = null,
    val selectedAgeRanges: Set<String> = emptySet(),
    val availablePastures: List<String> = emptyList(),
    val availableBreeds: List<String> = emptyList(),
    val availableTagColors: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
