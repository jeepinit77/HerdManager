package com.jumblemint.cows.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.preferences.tipsDataStore
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.util.identifierRequirementMessage
import com.jumblemint.cows.util.isIdentifierSatisfied
import com.jumblemint.cows.util.usesNames
import com.jumblemint.cows.util.usesTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class CowDetailUiState(
    val id: Long = 0L,
    val name: String = "",
    val tagNumber: String = "",
    val tagColor: String? = null,
    val birthDate: LocalDate? = null,
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
    val isNameTagLinked: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val identifierMode: AnimalIdentifierMode = AnimalIdentifierMode.BOTH
)

class CowDetailViewModel(
    private val application: CattleApplication,
    private val repository: CattleRepository,
    private val cowId: Long,
    private val tipsDataStore: DataStore<Preferences>
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CowDetailUiState())
    val uiState: StateFlow<CowDetailUiState> = _uiState.asStateFlow()

    // Buffer 1 event to avoid losing the save-attempt signal if UI hasn't started collecting yet
    private val _saveAttemptSignal = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val saveAttemptSignal: SharedFlow<Unit> = _saveAttemptSignal.asSharedFlow()

    private object PreferencesKeys {
        val NAME_TAG_LINKED_KEY = booleanPreferencesKey("name_tag_linked")
    }

    init {
        viewModelScope.launch {
            repository.getAnimalIdentifierModeFlow().collect { mode ->
                _uiState.update { current ->
                    var updated = current.copy(identifierMode = mode)
                    if (mode != AnimalIdentifierMode.BOTH && current.isNameTagLinked) {
                        updated = updated.copy(isNameTagLinked = false)
                    }
                    updated
                }
            }
        }
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val initialLinkState = tipsDataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.NAME_TAG_LINKED_KEY] ?: false
                }
                .firstOrNull() ?: false
            
            _uiState.update { it.copy(isLoading = true, isNameTagLinked = initialLinkState) }
            
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
                            birthDate = null
                        )
                    }
                    if (initialLinkState && _uiState.value.name.isNotBlank()) {
                        _uiState.update { it.copy(tagNumber = it.name) }
                    } else if (initialLinkState && _uiState.value.tagNumber.isNotBlank()){
                         _uiState.update { it.copy(name = it.tagNumber) }
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
                        if (initialLinkState) {
                             if (_uiState.value.name.isNotBlank()) {
                                _uiState.update { it.copy(tagNumber = it.name) }
                            } else if (_uiState.value.tagNumber.isNotBlank()){
                                 _uiState.update { it.copy(name = it.tagNumber) }
                            }
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
            val updated = if (it.isNameTagLinked) {
                it.copy(name = name, tagNumber = name, hasUnsavedChanges = true)
            } else {
                it.copy(name = name, hasUnsavedChanges = true)
            }
            if (updated.identifierMode.isIdentifierSatisfied(updated.name, updated.tagNumber) &&
                updated.error == updated.identifierMode.identifierRequirementMessage()
            ) {
                updated.copy(error = null)
            } else {
                updated
            }
        }
    }

    fun updateTagNumber(tagNumber: String) {
        _uiState.update {
            val updated = if (it.isNameTagLinked) {
                it.copy(name = tagNumber, tagNumber = tagNumber, hasUnsavedChanges = true)
            } else {
                it.copy(tagNumber = tagNumber, hasUnsavedChanges = true)
            }
            if (updated.identifierMode.isIdentifierSatisfied(updated.name, updated.tagNumber) &&
                updated.error == updated.identifierMode.identifierRequirementMessage()
            ) {
                updated.copy(error = null)
            } else {
                updated
            }
        }
    }

    fun toggleNameTagLink() {
        if (_uiState.value.identifierMode != AnimalIdentifierMode.BOTH) return
        val newLinkedState = !_uiState.value.isNameTagLinked
        _uiState.update { currentState ->
            val updatedState = if (newLinkedState) {
                when {
                    currentState.name.isNotBlank() && currentState.tagNumber.isBlank() ->
                        currentState.copy(isNameTagLinked = true, tagNumber = currentState.name)
                    currentState.name.isBlank() && currentState.tagNumber.isNotBlank() -> 
                        currentState.copy(isNameTagLinked = true, name = currentState.tagNumber)
                    else -> 
                        currentState.copy(isNameTagLinked = true, tagNumber = currentState.name)
                }
            } else {
                currentState.copy(isNameTagLinked = false)
            }
            // Clear name/tag error if either is now populated due to linking
            if (updatedState.name.isNotBlank() || updatedState.tagNumber.isNotBlank()) {
                val requirementMessage = updatedState.identifierMode.identifierRequirementMessage()
                updatedState.copy(error = if (updatedState.error == requirementMessage) null else updatedState.error)
            } else {
                updatedState
            }
        }
        viewModelScope.launch {
            tipsDataStore.edit { settings ->
                settings[PreferencesKeys.NAME_TAG_LINKED_KEY] = newLinkedState
            }
        }
    }

    fun updateTagColor(tagColor: String?) { _uiState.update { it.copy(tagColor = tagColor, hasUnsavedChanges = true) } }
    fun updateBirthDate(birthDate: LocalDate?) { _uiState.update { it.copy(birthDate = birthDate, hasUnsavedChanges = true) } }

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
            currentState.copy(gender = gender, classification = newClassification, error = if (gender != null) null else currentState.error, hasUnsavedChanges = true)
        }
    }

    fun updateClassification(classification: Classification?) {
        _uiState.update { currentState ->
            val newGender = when (classification) {
                Classification.COW, Classification.HEIFER -> Gender.FEMALE
                Classification.BULL, Classification.STEER -> Gender.MALE
                else -> currentState.gender
            }
            currentState.copy(classification = classification, gender = newGender, error = if (classification != null) null else currentState.error, hasUnsavedChanges = true)
        }
    }

    fun updateColorMarkings(colorMarkings: String) { _uiState.update { it.copy(colorMarkings = colorMarkings, hasUnsavedChanges = true) } }
    fun updateRegistrationNumber(registrationNumber: String) { _uiState.update { it.copy(registrationNumber = registrationNumber, hasUnsavedChanges = true) } }
    fun updateBreed(breed: String?) { _uiState.update { it.copy(breed = breed, hasUnsavedChanges = true) } }
    fun updateStatus(status: Status) { _uiState.update { it.copy(status = status, error = null, hasUnsavedChanges = true) } } // Status always has a value, so error for status nullity isn't needed here.
    fun updateIsWatched(isWatched: Boolean) { _uiState.update { it.copy(isWatched = isWatched, hasUnsavedChanges = true) } }
    fun updatePhotos(photos: List<String>) { _uiState.update { it.copy(photos = photos, hasUnsavedChanges = true) } }

    fun updateMother(motherId: Long?) {
        viewModelScope.launch {
            val motherName = motherId?.let { repository.getCowById(it)?.name }
            _uiState.update { it.copy(motherId = motherId, motherName = motherName, hasUnsavedChanges = true) }
        }
    }

    fun updateFather(fatherId: Long?) {
        viewModelScope.launch {
            val fatherName = fatherId?.let { repository.getCowById(it)?.name }
            _uiState.update { it.copy(fatherId = fatherId, fatherName = fatherName, hasUnsavedChanges = true) }
        }
    }

    fun updatePasture(pastureId: String?) {
        val pasture = _uiState.value.availablePastures.find { it.id == pastureId }
        _uiState.update { it.copy(pastureId = pastureId, pastureName = pasture?.name, hasUnsavedChanges = true) }
    }

    fun saveCow() {
        _saveAttemptSignal.tryEmit(Unit) // Signal save attempt to UI
        
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) } // Clear previous general error messages
            val currentState = _uiState.value
            
            if (!currentState.identifierMode.isIdentifierSatisfied(currentState.name, currentState.tagNumber)) {
                val message = currentState.identifierMode.identifierRequirementMessage()
                _uiState.update { it.copy(error = message) }
                return@launch
            }
            if (currentState.gender == null) {
                _uiState.update { it.copy(error = "Please select a Gender.") }
                return@launch
            }
            if (currentState.classification == null) {
                _uiState.update { it.copy(error = "Please select a Classification.") }
                return@launch
            }
            // Removed incorrect status == null check as status is non-nullable and defaults to ACTIVE

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
                    id = 0L, 
                    name = currentState.name.takeIf { it.isNotBlank() },
                    tagNumber = currentState.tagNumber,
                    tagColor = currentState.tagColor,
                    birthDate = currentState.birthDate,
                    gender = currentState.gender!!,
                    classification = currentState.classification!!,
                    colorMarkings = currentState.colorMarkings.takeIf { it.isNotBlank() },
                    registrationNumber = currentState.registrationNumber.takeIf { it.isNotBlank() },
                    breed = currentState.breed,
                    motherId = currentState.motherId,
                    fatherId = currentState.fatherId,
                    status = currentState.status, // status is non-null
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
                    herdId = null 
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
                    birthDate = currentState.birthDate,
                    gender = currentState.gender!!,
                    classification = currentState.classification!!,
                    colorMarkings = currentState.colorMarkings.takeIf { it.isNotBlank() },
                    registrationNumber = currentState.registrationNumber.takeIf { it.isNotBlank() },
                    breed = currentState.breed,
                    motherId = currentState.motherId,
                    fatherId = currentState.fatherId,
                    status = currentState.status, // status is non-null
                    pastureId = currentState.pastureId,
                    photos = currentState.photos, 
                    isWatched = currentState.isWatched,
                    updatedAt = LocalDate.now(),
                    updatedBy = currentUserId,
                    lastSyncAt = 0L, 
                    isDeleted = existingCow.isDeleted 
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
                
                _uiState.update { it.copy(isSaved = true, error = null, isLoading = false, hasUnsavedChanges = false) }

                if (!currentUser.isLocalUser) {
                    viewModelScope.launch(Dispatchers.IO) {
                        application.syncService.syncItemImmediately(currentUserId, savedCowForSync)
                            .onFailure {
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
