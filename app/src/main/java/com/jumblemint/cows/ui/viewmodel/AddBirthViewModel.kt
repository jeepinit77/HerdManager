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

class AddBirthViewModel(
    private val repository: CattleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddBirthUiState())
    val uiState: StateFlow<AddBirthUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load tag colors from settings
                val tagColorsSetting = repository.getSettingByKey(SettingsKeys.TAG_COLORS)
                val tagColors = tagColorsSetting?.value?.split(",")?.map { it.trim() } ?: listOf(
                    "Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Pink", "White", "Black", "Brown"
                )
                
                // Get the current values from the flows
                val mothers = repository.getActiveFemales().first()
                val fathers = repository.getActiveMales().first()
                
                _uiState.value = _uiState.value.copy(
                    tagColors = tagColors,
                    availableMothers = mothers,
                    availableFathers = fathers,
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
    
    fun updateBirthDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(birthDate = date)
    }
    
    fun updateMother(motherId: Long?) {
        val mother = _uiState.value.availableMothers.find { it.id == motherId }
        _uiState.value = _uiState.value.copy(
            motherId = motherId,
            motherName = mother?.name
        )
    }
    
    fun updateFather(fatherId: Long?) {
        val father = _uiState.value.availableFathers.find { it.id == fatherId }
        _uiState.value = _uiState.value.copy(
            fatherId = fatherId,
            fatherName = father?.name
        )
    }
    
    fun updateCalfName(name: String) {
        _uiState.value = _uiState.value.copy(calfName = name)
    }
    
    fun updateCalfGender(gender: Gender) {
        _uiState.value = _uiState.value.copy(calfGender = gender)
    }
    
    fun updateCalfTagNumber(tagNumber: String) {
        _uiState.value = _uiState.value.copy(calfTagNumber = tagNumber)
    }
    
    fun updateCalfTagColor(tagColor: String) {
        _uiState.value = _uiState.value.copy(calfTagColor = tagColor)
    }
    
    fun updateCalfColorMarkings(colorMarkings: String) {
        _uiState.value = _uiState.value.copy(calfColorMarkings = colorMarkings)
    }
    
    fun recordBirth() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                
                // Validation
                if (state.motherId == null) {
                    _uiState.value = state.copy(error = "Please select a mother")
                    return@launch
                }
                
                if (state.birthDate == null) {
                    _uiState.value = state.copy(error = "Please select a birth date")
                    return@launch
                }
                
                // Create the calf with all provided details
                val calfId = repository.recordBirth(
                    motherId = state.motherId,
                    fatherId = state.fatherId,
                    calfName = state.calfName.takeIf { it.isNotBlank() },
                    calfGender = state.calfGender,
                    birthDate = state.birthDate
                )
                
                // Update the calf with additional details if provided
                if (state.calfTagNumber.isNotBlank() || state.calfTagColor.isNotBlank() || state.calfColorMarkings.isNotBlank()) {
                    val calf = repository.getCowById(calfId)
                    calf?.let { existingCalf ->
                        val updatedCalf = existingCalf.copy(
                            tagNumber = state.calfTagNumber.takeIf { it.isNotBlank() },
                            tagColor = state.calfTagColor.takeIf { it.isNotBlank() },
                            colorMarkings = state.calfColorMarkings.takeIf { it.isNotBlank() },
                            updatedAt = LocalDate.now()
                        )
                        repository.updateCow(updatedCalf)
                    }
                }
                
                _uiState.value = state.copy(isSaved = true)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

data class AddBirthUiState(
    val birthDate: LocalDate? = LocalDate.now(),
    val motherId: Long? = null,
    val motherName: String? = null,
    val fatherId: Long? = null,
    val fatherName: String? = null,
    val calfName: String = "",
    val calfGender: Gender = Gender.TBD,
    val calfTagNumber: String = "",
    val calfTagColor: String = "",
    val calfColorMarkings: String = "",
    val tagColors: List<String> = emptyList(),
    val availableMothers: List<Cow> = emptyList(),
    val availableFathers: List<Cow> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)