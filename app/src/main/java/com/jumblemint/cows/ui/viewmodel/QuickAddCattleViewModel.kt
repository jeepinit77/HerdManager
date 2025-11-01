package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickAddCattleViewModel(
    application: CattleApplication,
    private val repository: CattleRepository
) : AndroidViewModel(application) {

    private val entryIdGenerator = AtomicLong(0L)

    private fun createEmptyEntry(): QuickAddEntry = QuickAddEntry(id = entryIdGenerator.incrementAndGet())

    private fun initialSections(): Map<QuickAddSection, List<QuickAddEntry>> =
        QuickAddSection.entries.associateWith { listOf(createEmptyEntry()) }

    private val _uiState = MutableStateFlow(
        QuickAddCattleUiState(
            sections = initialSections(),
            expandedSection = QuickAddSection.COWS
        )
    )
    val uiState: StateFlow<QuickAddCattleUiState> = _uiState.asStateFlow()

    fun setExpandedSection(section: QuickAddSection) {
        _uiState.update { current ->
            val newExpanded = if (current.expandedSection == section) null else section
            current.copy(expandedSection = newExpanded)
        }
    }

    fun setLimitTagIdsToNumeric(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(limitTagIdsToNumeric = enabled)
        }
    }

    fun updateName(section: QuickAddSection, entryId: Long, value: String) {
        updateEntry(section, entryId) { it.copy(name = value) }
    }

    fun updateTag(section: QuickAddSection, entryId: Long, value: String) {
        updateEntry(section, entryId) { it.copy(tagNumber = value) }
    }

    fun removeEntry(section: QuickAddSection, entryId: Long) {
        _uiState.update { current ->
            val updatedEntries = current.sections[section].orEmpty().filterNot { it.id == entryId }
            val normalized = ensurePlaceholder(updatedEntries)
            current.copy(
                sections = current.sections + (section to normalized),
                isSaved = false,
                errorMessage = null
            )
        }
    }

    fun saveAnimals() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.totalAnimals == 0) {
            _uiState.update { it.copy(errorMessage = "Add at least one animal before saving.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val application = getApplication<CattleApplication>()
            val currentUser = application.authService.currentUser.first()
            if (currentUser == null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No authenticated user. Unable to save animals."
                    )
                }
                return@launch
            }

            val userId = currentUser.uid
            val animalsToSave = buildList {
                state.sections.forEach { (section, entries) ->
                    entries.filter { it.hasContent() }.forEach { entry ->
                        add(
                            Cow(
                                name = entry.name.trim().takeIf { name -> name.isNotBlank() },
                                tagNumber = entry.tagNumber.trim().takeIf { tag -> tag.isNotBlank() },
                                gender = section.gender,
                                classification = section.classification,
                                status = Status.ACTIVE,
                                pastureId = null,
                                photos = emptyList(),
                                isWatched = false,
                                firestoreId = UUID.randomUUID().toString(),
                                lastSyncAt = 0L,
                                isDeleted = false,
                                createdAt = LocalDate.now(),
                                updatedAt = LocalDate.now(),
                                createdBy = userId,
                                updatedBy = userId,
                                herdId = null
                            )
                        )
                    }
                }
            }

            if (animalsToSave.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Each animal needs at least a name or tag number."
                    )
                }
                return@launch
            }

            try {
                val insertedIds = withContext(Dispatchers.IO) {
                    repository.insertCows(animalsToSave)
                }
                val savedCows = animalsToSave.zip(insertedIds) { cow, id -> cow.copy(id = id) }

                if (!currentUser.isLocalUser) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val syncService = application.syncService
                        savedCows.forEach { cow ->
                            syncService.syncItemImmediately(userId, cow).onFailure { throwable ->
                                println("QuickAddCattleViewModel: Failed to sync cow ${cow.id}: ${throwable.message}")
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        sections = initialSections(),
                        expandedSection = QuickAddSection.COWS,
                        isSaving = false,
                        isSaved = true,
                        errorMessage = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = t.localizedMessage ?: "Unable to save animals",
                        isSaved = false
                    )
                }
            }
        }
    }

    fun acknowledgeSave() {
        _uiState.update { it.copy(isSaved = false) }
    }

    private fun updateEntry(
        section: QuickAddSection,
        entryId: Long,
        transform: (QuickAddEntry) -> QuickAddEntry
    ) {
        _uiState.update { current ->
            val updatedEntries = current.sections[section].orEmpty().map { entry ->
                if (entry.id == entryId) transform(entry) else entry
            }
            val normalized = ensurePlaceholder(updatedEntries)
            current.copy(
                sections = current.sections + (section to normalized),
                isSaved = false,
                errorMessage = null
            )
        }
    }

    private fun ensurePlaceholder(entries: List<QuickAddEntry>): List<QuickAddEntry> {
        if (entries.isEmpty()) return listOf(createEmptyEntry())
        val sanitized = entries.filterIndexed { index, entry ->
            entry.hasContent() || index == entries.lastIndex
        }
        val base = if (sanitized.isEmpty()) listOf(createEmptyEntry()) else sanitized
        return if (base.last().hasContent()) base + createEmptyEntry() else base
    }
}

data class QuickAddCattleUiState(
    val sections: Map<QuickAddSection, List<QuickAddEntry>>,
    val expandedSection: QuickAddSection? = QuickAddSection.COWS,
    val limitTagIdsToNumeric: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val totalAnimals: Int = sections.values.sumOf { entries -> entries.count { it.hasContent() } }
    val hasUnsavedChanges: Boolean = totalAnimals > 0 && !isSaved
}

data class QuickAddEntry(
    val id: Long,
    val name: String = "",
    val tagNumber: String = ""
) {
    fun hasContent(): Boolean = name.isNotBlank() || tagNumber.isNotBlank()
}

enum class QuickAddSection(
    val displayName: String,
    val gender: Gender,
    val classification: Classification
) {
    BULLS("Bulls", Gender.MALE, Classification.BULL),
    COWS("Cows", Gender.FEMALE, Classification.COW),
    HEIFERS("Heifers", Gender.FEMALE, Classification.HEIFER),
    STEERS("Steers", Gender.MALE, Classification.STEER),
    MALE_CALVES("Male Calves", Gender.MALE, Classification.CALF),
    FEMALE_CALVES("Female Calves", Gender.FEMALE, Classification.CALF)
}
