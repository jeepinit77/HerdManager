package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID // Added import

// CowDetailUiState is defined here or imported
data class CowDetailUiState(
    val id: Long = 0L,
    val name: String = "",
    val tagNumber: String = "",
    val tagColor: String? = null,
    val birthDate: LocalDate? = LocalDate.now(),
    val gender: Gender? = null,
    val classification: Classification? = null,
    val colorMarkings: String = "",
    val registrationNumber: String = "",
    val breed: String? = null,
    val motherId: Long? = null,
    val fatherId: Long? = null,
    val motherName: String? = null,
    val fatherName: String? = null,
    val status: Status = Status.ACTIVE,
    val pastureId: String? = null, 
    val pastureName: String? = null,
    val photos: List<String> = emptyList(),
    val isWatched: Boolean = false,
    val createdAt: LocalDate? = null,
    val updatedAt: LocalDate? = null,
    val availableMothers: List<Cow> = emptyList(),
    val availableFathers: List<Cow> = emptyList(),
    val availablePastures: List<Pasture> = emptyList(),
    val tagColors: List<String> = emptyList(),
    val breeds: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isNameTagLinked: Boolean = false
)

class CowDetailViewModel(
    application: CattleApplication,
    private val repository: CattleRepository,
    private val cowId: Long
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CowDetailUiState())
    val uiState: StateFlow<CowDetailUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val mothers = repository.getActiveFemales().first()
                val fathers = repository.getActiveMales().first()
                val pastures = repository.getAllPastures().first()
                val tagColors = repository.getAllTagColors().first().map { it.name }
                val breeds = repository.getAllBreeds().first().map { it.name }

                if (cowId == 0L) { // New cow
                    _uiState.update {
                        it.copy(
                            id = 0L,
                            availableMothers = mothers,
                            availableFathers = fathers,
                            availablePastures = pastures,
                            tagColors = tagColors,
                            breeds = breeds,
                            isLoading = false,
                            birthDate = LocalDate.now() // Default birthdate for new cow
                        )
                    }
                } else { // Existing cow
                    val cow = repository.getCowById(cowId)
                    if (cow != null) {
                        val motherName = cow.motherId?.let { repository.getCowById(it)?.name }
                        val fatherName = cow.fatherId?.let { repository.getCowById(it)?.name }
                        val currentPasture = pastures.find { it.id == cow.pastureId } 

                        _uiState.update {
                            it.copy(
                                id = cow.id,
                                name = cow.name ?: "",
                                tagNumber = cow.tagNumber ?: "",
                                tagColor = cow.tagColor,
                                birthDate = cow.birthDate,
                                gender = cow.gender,
                                classification = cow.classification,
                                colorMarkings = cow.colorMarkings ?: "",
                                registrationNumber = cow.registrationNumber ?: "",
                                breed = cow.breed,
                                motherId = cow.motherId,
                                fatherId = cow.fatherId,
                                motherName = motherName,
                                fatherName = fatherName,
                                status = cow.status,
                                pastureId = cow.pastureId, 
                                pastureName = currentPasture?.name,
                                photos = cow.photos,
                                isWatched = cow.isWatched,
                                createdAt = cow.createdAt,
                                updatedAt = cow.updatedAt,
                                availableMothers = mothers,
                                availableFathers = fathers,
                                availablePastures = pastures,
                                tagColors = tagColors,
                                breeds = breeds,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(error = "Cow not found", isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateName(name: String) { 
        _uiState.update { 
            if (it.isNameTagLinked) {
                it.copy(name = name, tagNumber = name)
            } else {
                it.copy(name = name)
            }
        }
    }
    
    fun updateTagNumber(tagNumber: String) { 
        _uiState.update { 
            if (it.isNameTagLinked) {
                it.copy(name = tagNumber, tagNumber = tagNumber)
            } else {
                it.copy(tagNumber = tagNumber)
            }
        }
    }
    
    fun toggleNameTagLink() {
        _uiState.update { currentState ->
            val newLinkedState = !currentState.isNameTagLinked
            if (newLinkedState) {
                // When linking, sync the non-empty field to the empty one, or name to tag if both have values
                when {
                    currentState.name.isNotBlank() && currentState.tagNumber.isBlank() -> 
                        currentState.copy(isNameTagLinked = true, tagNumber = currentState.name)
                    currentState.tagNumber.isNotBlank() && currentState.name.isBlank() -> 
                        currentState.copy(isNameTagLinked = true, name = currentState.tagNumber)
                    else -> 
                        currentState.copy(isNameTagLinked = true, tagNumber = currentState.name)
                }
            } else {
                currentState.copy(isNameTagLinked = false)
            }
        }
    }
    fun updateTagColor(tagColor: String?) { _uiState.update { it.copy(tagColor = tagColor) } }
    fun updateBirthDate(birthDate: LocalDate?) { _uiState.update { it.copy(birthDate = birthDate) } }
    fun updateGender(gender: Gender?) { 
        _uiState.update { currentState ->
            val newClassification = when (gender) {
                Gender.FEMALE -> when (currentState.classification) {
                    Classification.BULL, Classification.STEER -> null
                    else -> currentState.classification
                }
                Gender.MALE -> when (currentState.classification) {
                    Classification.COW, Classification.HEIFER -> null
                    else -> currentState.classification
                }
                else -> currentState.classification
            }
            currentState.copy(gender = gender, classification = newClassification)
        }
    }
    
    fun updateClassification(classification: Classification?) { 
        _uiState.update { currentState ->
            val newGender = when (classification) {
                Classification.COW, Classification.HEIFER -> Gender.FEMALE
                Classification.BULL, Classification.STEER -> Gender.MALE
                else -> currentState.gender
            }
            currentState.copy(classification = classification, gender = newGender)
        }
    }
    fun updateColorMarkings(colorMarkings: String) { _uiState.update { it.copy(colorMarkings = colorMarkings) } }
    fun updateRegistrationNumber(registrationNumber: String) { _uiState.update { it.copy(registrationNumber = registrationNumber) } }
    fun updateBreed(breed: String?) { _uiState.update { it.copy(breed = breed) } }
    fun updateStatus(status: Status) { _uiState.update { it.copy(status = status) } }
    fun updateIsWatched(isWatched: Boolean) { _uiState.update { it.copy(isWatched = isWatched) } }

    fun updateMother(motherId: Long?) {
        viewModelScope.launch {
            val motherName = motherId?.let { repository.getCowById(it)?.name }
            _uiState.update { it.copy(motherId = motherId, motherName = motherName) }
        }
    }

    fun updateFather(fatherId: Long?) {
        viewModelScope.launch {
            val fatherName = fatherId?.let { repository.getCowById(it)?.name }
            _uiState.update { it.copy(fatherId = fatherId, fatherName = fatherName) }
        }
    }

    fun updatePasture(pastureId: String?) {
        val pasture = _uiState.value.availablePastures.find { it.id == pastureId }
        _uiState.update { it.copy(pastureId = pastureId, pastureName = pasture?.name) }
    }

    fun saveCow() {
        viewModelScope.launch { 
            val currentState = _uiState.value
            // Validation: Either name OR tag number is required
            if (currentState.name.isBlank() && currentState.tagNumber.isBlank()) {
                _uiState.update { it.copy(error = "Please enter a Name or a Tag Number.") }
                return@launch
            }
            
            // Validation: Gender and Classification are required
            if (currentState.gender == null) {
                _uiState.update { it.copy(error = "Please select a Gender.") }
                return@launch
            }
            
            if (currentState.classification == null) {
                _uiState.update { it.copy(error = "Please select a Classification.") }
                return@launch
            }

            val application = getApplication<CattleApplication>()
            val currentUser = application.authService.currentUser.first()

            if (currentUser == null) {
                _uiState.update { it.copy(error = "No authenticated user. Cannot save cow.") }
                return@launch
            }
            val currentUserId = currentUser.uid

            val cowToSave: Cow
            var isNewCow = false

            if (currentState.id == 0L) { // New Cow
                isNewCow = true
                val newFirestoreId = UUID.randomUUID().toString()
                cowToSave = Cow(
                    id = 0L, // Room will auto-generate
                    name = currentState.name.takeIf { it.isNotBlank() },
                    tagNumber = currentState.tagNumber,
                    tagColor = currentState.tagColor,
                    birthDate = currentState.birthDate ?: LocalDate.now(),
                    gender = currentState.gender!!,
                    classification = currentState.classification!!,
                    colorMarkings = currentState.colorMarkings.takeIf { it.isNotBlank() },
                    registrationNumber = currentState.registrationNumber.takeIf { it.isNotBlank() },
                    breed = currentState.breed,
                    motherId = currentState.motherId,
                    fatherId = currentState.fatherId,
                    status = currentState.status,
                    pastureId = currentState.pastureId,
                    photos = currentState.photos,
                    isWatched = currentState.isWatched,
                    firestoreId = newFirestoreId,
                    lastSyncAt = 0L,
                    isDeleted = false,
                    createdAt = LocalDate.now(),
                    updatedAt = LocalDate.now(),
                    createdBy = currentUserId,
                    updatedBy = currentUserId,
                    herdId = null // TODO: Determine herdId for new cows if not part of birth screen
                )
            } else { // Existing Cow
                val existingCow = repository.getCowById(currentState.id)
                if (existingCow == null) {
                    _uiState.update { it.copy(error = "Failed to load existing cow for update.") }
                    return@launch
                }
                cowToSave = existingCow.copy(
                    name = currentState.name.takeIf { it.isNotBlank() },
                    tagNumber = currentState.tagNumber,
                    tagColor = currentState.tagColor,
                    birthDate = currentState.birthDate ?: existingCow.birthDate,
                    gender = currentState.gender!!,
                    classification = currentState.classification!!,
                    colorMarkings = currentState.colorMarkings.takeIf { it.isNotBlank() },
                    registrationNumber = currentState.registrationNumber.takeIf { it.isNotBlank() },
                    breed = currentState.breed,
                    motherId = currentState.motherId,
                    fatherId = currentState.fatherId,
                    status = currentState.status,
                    pastureId = currentState.pastureId,
                    photos = currentState.photos, // Assuming photos are managed and updated in UI state
                    isWatched = currentState.isWatched,
                    // firestoreId, createdAt, createdBy are preserved from existingCow
                    updatedAt = LocalDate.now(),
                    updatedBy = currentUserId,
                    lastSyncAt = 0L, // Mark for re-sync
                    isDeleted = existingCow.isDeleted // Preserve soft delete status
                    // herdId is preserved from existingCow
                )
            }

            try {
                val savedCowForSync: Cow
                if (isNewCow) {
                    val newId = repository.insertCow(cowToSave)
                    savedCowForSync = cowToSave.copy(id = newId)
                } else {
                    repository.updateCow(cowToSave)
                    savedCowForSync = cowToSave
                }
                
                _uiState.update { it.copy(isSaved = true, error = null, isLoading = false) }

                if (!currentUser.isLocalUser) {
                    viewModelScope.launch(Dispatchers.IO) { 
                        application.syncService.syncItemImmediately(currentUserId, savedCowForSync)
                            .onFailure {
                                // Handle immediate sync failure if necessary on a background thread
                                println("CowDetailViewModel: Error immediately syncing cow ${savedCowForSync.id}: ${it.message}")
                            }
                    }
                }

            } catch (e: Exception) { 
                _uiState.update { it.copy(error = "Failed to save cow: ${e.message}", isSaved = false, isLoading = false) }
            }
        }
    }
}
