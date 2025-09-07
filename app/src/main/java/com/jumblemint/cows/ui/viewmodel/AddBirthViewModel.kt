package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.auth.AuthService // Corrected import
import com.jumblemint.cows.sync.SyncService // Correct import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID // Added import

class AddBirthViewModel(
    private val repository: CattleRepository,
    private val authService: AuthService,      // Added dependency
    private val syncService: SyncService       // Added dependency
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddBirthUiState())
    val uiState: StateFlow<AddBirthUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                val tagColors = repository.getAllTagColors().first().map { it.name }

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
            val state = _uiState.value
            val currentUser = authService.currentUser.first()

            if (currentUser == null) {
                _uiState.value = state.copy(error = "No authenticated user. Cannot record birth.")
                return@launch
            }
            val currentUserId = currentUser.uid

            // Validation
            if (state.motherId == null) {
                _uiState.value = state.copy(error = "Please select a mother")
                return@launch
            }
            if (state.birthDate == null) {
                _uiState.value = state.copy(error = "Please select a birth date")
                return@launch
            }

            try {
                val newFirestoreId = UUID.randomUUID().toString()
                val selectedMother = state.availableMothers.find { it.id == state.motherId }

                val newCalf = Cow(
                    id = 0L, // Room will auto-generate this
                    name = state.calfName.takeIf { it.isNotBlank() },
                    tagNumber = state.calfTagNumber.takeIf { it.isNotBlank() },
                    tagColor = state.calfTagColor.takeIf { it.isNotBlank() },
                    birthDate = state.birthDate,
                    gender = state.calfGender,
                    classification = Classification.CALF,
                    colorMarkings = state.calfColorMarkings.takeIf { it.isNotBlank() },
                    motherId = state.motherId,
                    fatherId = state.fatherId,
                    status = Status.ACTIVE,
                    pastureId = selectedMother?.pastureId, // Calf starts in mother's pasture, or null
                    photos = emptyList(),
                    isWatched = false,
                    createdAt = LocalDate.now(),
                    updatedAt = LocalDate.now(),
                    herdId = selectedMother?.herdId, // Calf belongs to mother's herd
                    firestoreId = newFirestoreId,
                    lastSyncAt = 0L,
                    isDeleted = false,
                    createdBy = currentUserId,
                    updatedBy = currentUserId
                )

                repository.insertCow(newCalf)

                if (!currentUser.isLocalUser) {
                    // Pass the newCalf itself, which already has the firestoreId set
                    syncService.syncItemImmediately(currentUserId, newCalf) 
                        .onFailure {
                             _uiState.value = state.copy(error = "Failed to sync new calf: ${it.message}")
                             println("Error immediately syncing new calf: ${it.message}")
                        }
                }
                
                _uiState.value = state.copy(isSaved = true, error = null)
                
            } catch (e: Exception) {
                _uiState.value = state.copy(error = "Error recording birth: ${e.message}", isSaved = false)
                e.printStackTrace()
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
