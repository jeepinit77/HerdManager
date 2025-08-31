package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddActivityViewModel(
    private val repository: CattleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddActivityUiState())
    val uiState: StateFlow<AddActivityUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val allCows = repository.getAllCows().first()
                val allPastures = repository.getAllPastures().first()
                
                val activeCows = allCows.filter { it.status == Status.ACTIVE }
                
                _uiState.value = _uiState.value.copy(
                    availableCows = activeCows,
                    availablePastures = allPastures,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun updateActivityType(activityType: ActivityType?) {
        _uiState.value = _uiState.value.copy(activityType = activityType)
    }

    fun updateDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    // Changed pastureId to String?
    fun updateToPasture(pastureId: String?) {
        val pasture = _uiState.value.availablePastures.find { it.id == pastureId } // Now String == String
        _uiState.value = _uiState.value.copy(
            toPastureId = pastureId,
            toPastureName = pasture?.name
        )
    }

    fun selectCow(cowId: Long) {
        val currentSelection = _uiState.value.selectedCows.toMutableSet()
        currentSelection.add(cowId)
        _uiState.value = _uiState.value.copy(selectedCows = currentSelection)
    }

    fun deselectCow(cowId: Long) {
        val currentSelection = _uiState.value.selectedCows.toMutableSet()
        currentSelection.remove(cowId)
        _uiState.value = _uiState.value.copy(selectedCows = currentSelection)
    }

    fun selectAllCows() {
        val allCowIds = _uiState.value.availableCows.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedCows = allCowIds)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedCows = emptySet())
    }

    fun saveActivity() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                
                if (state.activityType == null) {
                    _uiState.value = state.copy(error = "Please select an activity type")
                    return@launch
                }
                
                if (state.selectedCows.isEmpty()) {
                    _uiState.value = state.copy(error = "Please select at least one cow")
                    return@launch
                }
                
                if (state.date == null) {
                    _uiState.value = state.copy(error = "Please select a date")
                    return@launch
                }
                
                if (state.activityType in listOf(ActivityType.WORKED, ActivityType.OTHER) && state.notes.isBlank()) {
                    _uiState.value = state.copy(error = "Notes are required for this activity type")
                    return@launch
                }
                
                if (state.activityType == ActivityType.MOVED && state.toPastureId == null) {
                    _uiState.value = state.copy(error = "Please select a destination pasture")
                    return@launch
                }
                
                repository.createBulkActivity(
                    cowIds = state.selectedCows.toList(),
                    activityType = state.activityType,
                    date = state.date,
                    notes = state.notes.takeIf { it.isNotBlank() },
                    toPastureId = state.toPastureId // This will now be String?
                )
                
                _uiState.value = state.copy(isSaved = true)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

data class AddActivityUiState(
    val activityType: ActivityType? = null,
    val date: LocalDate? = LocalDate.now(),
    val notes: String = "",
    val toPastureId: String? = null, // Changed to String?
    val toPastureName: String? = null,
    val selectedCows: Set<Long> = emptySet(),
    val availableCows: List<Cow> = emptyList(),
    val availablePastures: List<Pasture> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)