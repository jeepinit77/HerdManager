package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.auth.AuthService
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class AddBirthViewModel(
    private val repository: CattleRepository,
    private val authService: AuthService,
    private val syncService: SyncService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBirthUiState())
    val uiState: StateFlow<AddBirthUiState> = _uiState.asStateFlow()

    private var initialSnapshot: BirthFormSnapshot? = null

    init {
        initialSnapshot = currentSnapshot(_uiState.value)
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val tagColors = repository.getAllTagColors().first().map { it.name }
                val cutoffDate = LocalDate.now().minusMonths(9)
                val mothers = repository.getEligibleMothers(cutoffDate).first()
                val fathers = repository.getActiveMales().first()
                    .filter { it.classification == Classification.BULL }
                val pastures = repository.getAllPastures().first()
                val recentSires = repository.getRecentSires()
                    .filter { sire -> fathers.any { it.id == sire.id } }

                setState(
                    _uiState.value.copy(
                        tagColors = tagColors,
                        availableMothers = mothers,
                        availableFathers = fathers,
                        availablePastures = pastures,
                        recentSires = recentSires,
                        isLoading = false,
                        isSaved = false,
                        error = null
                    ),
                    resetInitial = true
                )
            } catch (e: Exception) {
                setState(
                    _uiState.value.copy(
                        error = e.message,
                        isLoading = false,
                        isSaved = false
                    )
                )
            }
        }
    }

    fun updateBirthDate(date: LocalDate?) {
        val state = _uiState.value
        val updatedState = state.copy(birthDate = date, error = null, isSaved = false)
        val newCalfName = if (updatedState.calfName.isBlank() && updatedState.motherId != null) {
            val mother = updatedState.availableMothers.find { it.id == updatedState.motherId }
            mother?.let {
                val motherIdentifier = it.name?.takeIf { name -> name.isNotBlank() } ?: it.tagNumber ?: ""
                if (motherIdentifier.isNotBlank()) {
                    val birthYear = updatedState.birthDate?.year?.toString() ?: LocalDate.now().year.toString()
                    "$motherIdentifier $birthYear"
                } else {
                    updatedState.calfName
                }
            } ?: updatedState.calfName
        } else {
            updatedState.calfName
        }

        setState(updatedState.copy(calfName = newCalfName))
    }

    fun updateMother(motherId: Long?) {
        val state = _uiState.value
        val mother = state.availableMothers.find { it.id == motherId }
        val newCalfName = if (state.calfName.isBlank()) {
            if (mother != null) {
                val motherIdentifier = mother.name?.takeIf { it.isNotBlank() } ?: mother.tagNumber ?: ""
                if (motherIdentifier.isNotBlank()) {
                    val birthYear = state.birthDate?.year?.toString() ?: LocalDate.now().year.toString()
                    "$motherIdentifier $birthYear"
                } else {
                    ""
                }
            } else {
                ""
            }
        } else state.calfName

        setState(
            state.copy(
                motherId = motherId,
                motherName = mother?.name,
                calfName = newCalfName,
                calfPastureId = mother?.pastureId,
                error = null,
                isSaved = false
            )
        )
    }

    fun updateFather(fatherId: Long?) {
        val state = _uiState.value
        val father = state.availableFathers.find { it.id == fatherId }
        setState(
            state.copy(
                fatherId = fatherId,
                fatherName = father?.name,
                error = null,
                isSaved = false
            )
        )

        if (fatherId != null) {
            viewModelScope.launch {
                repository.rememberRecentSire(fatherId)
                val refreshedSires = repository.getRecentSires()
                    .filter { sire -> _uiState.value.availableFathers.any { it.id == sire.id } }
                setState(_uiState.value.copy(recentSires = refreshedSires))
            }
        }
    }

    fun updateCalfName(name: String) {
        val state = _uiState.value
        setState(state.copy(calfName = name, isSaved = false))
    }

    fun updateCalfBirthWeight(weight: String) {
        val state = _uiState.value
        setState(state.copy(calfBirthWeight = weight, isSaved = false))
    }

    fun updateCalfGender(gender: Gender) {
        val state = _uiState.value
        setState(state.copy(calfGender = gender, error = null, isSaved = false))
    }

    fun updateCalfTagNumber(tagNumber: String) {
        val state = _uiState.value
        setState(state.copy(calfTagNumber = tagNumber, isSaved = false))
    }

    fun updateCalfTagColor(tagColor: String) {
        val state = _uiState.value
        setState(state.copy(calfTagColor = tagColor, isSaved = false))
    }

    fun updateCalfColorMarkings(colorMarkings: String) {
        val state = _uiState.value
        setState(state.copy(calfColorMarkings = colorMarkings, isSaved = false))
    }

    fun updateCalfPasture(pastureId: String?) {
        val state = _uiState.value
        setState(state.copy(calfPastureId = pastureId, isSaved = false))
    }

    fun recordBirth() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentUser = authService.currentUser.first()

            if (currentUser == null) {
                setState(state.copy(error = "No authenticated user. Cannot record birth.", isSaved = false))
                return@launch
            }
            val currentUserId = currentUser.uid

            if (state.motherId == null) {
                setState(state.copy(error = "Please select a mother", isSaved = false))
                return@launch
            }
            if (state.birthDate == null) {
                setState(state.copy(error = "Please select a birth date", isSaved = false))
                return@launch
            }

            try {
                val newFirestoreId = UUID.randomUUID().toString()
                val selectedMother = state.availableMothers.find { it.id == state.motherId }

                val newCalf = Cow(
                    id = 0L,
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
                    pastureId = state.calfPastureId ?: selectedMother?.pastureId,
                    photos = emptyList(),
                    isWatched = false,
                    createdAt = LocalDate.now(),
                    updatedAt = LocalDate.now(),
                    herdId = selectedMother?.herdId,
                    firestoreId = newFirestoreId,
                    lastSyncAt = 0L,
                    isDeleted = false,
                    createdBy = currentUserId,
                    updatedBy = currentUserId
                )

                val insertedId = repository.insertCow(newCalf)
                val newCalfRecord = newCalf.copy(id = insertedId)

                if (state.calfBirthWeight.isNotBlank()) {
                    state.calfBirthWeight.toDoubleOrNull()?.takeIf { it > 0 }?.let { weight ->
                        repository.insertActivity(
                            Activity(
                                cowId = newCalfRecord.id,
                                date = state.birthDate ?: LocalDate.now(),
                                activityType = ActivityType.WEIGHED,
                                notes = "Birth weight recorded",
                                quantity = weight,
                                cowIds = listOfNotNull(newCalfRecord.id.takeIf { it != 0L })
                            )
                        )
                    }
                }

                if (!currentUser.isLocalUser) {
                    syncService.syncItemImmediately(currentUserId, newCalfRecord)
                        .onFailure {
                            setState(state.copy(error = "Failed to sync new calf: ${it.message}", isSaved = false))
                            println("Error immediately syncing new calf: ${it.message}")
                        }
                }

                setState(state.copy(isSaved = true, error = null), resetInitial = true)
            } catch (e: Exception) {
                setState(state.copy(error = "Error recording birth: ${e.message}", isSaved = false))
                e.printStackTrace()
            }
        }
    }

    private fun currentSnapshot(state: AddBirthUiState): BirthFormSnapshot = BirthFormSnapshot(
        birthDate = state.birthDate,
        motherId = state.motherId,
        fatherId = state.fatherId,
        calfName = state.calfName,
        calfGender = state.calfGender,
        calfTagNumber = state.calfTagNumber,
        calfTagColor = state.calfTagColor,
        calfColorMarkings = state.calfColorMarkings,
        calfBirthWeight = state.calfBirthWeight,
        calfPastureId = state.calfPastureId
    )

    private fun setState(newState: AddBirthUiState, resetInitial: Boolean = false) {
        val snapshot = currentSnapshot(newState)
        if (initialSnapshot == null || resetInitial) {
            initialSnapshot = snapshot
        }
        val baseline = initialSnapshot ?: snapshot
        _uiState.value = newState.copy(hasUnsavedChanges = snapshot != baseline)
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
    val calfBirthWeight: String = "",
    val tagColors: List<String> = emptyList(),
    val availableMothers: List<Cow> = emptyList(),
    val availableFathers: List<Cow> = emptyList(),
    val availablePastures: List<Pasture> = emptyList(),
    val calfPastureId: String? = null,
    val recentSires: List<Cow> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false
)

private data class BirthFormSnapshot(
    val birthDate: LocalDate?,
    val motherId: Long?,
    val fatherId: Long?,
    val calfName: String,
    val calfGender: Gender,
    val calfTagNumber: String,
    val calfTagColor: String,
    val calfColorMarkings: String,
    val calfBirthWeight: String,
    val calfPastureId: String?
)
