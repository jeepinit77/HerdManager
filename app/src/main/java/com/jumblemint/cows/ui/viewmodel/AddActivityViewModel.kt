package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddActivityViewModel(
    application: Application,
    private val repository: CattleRepository,
    private val editId: Long? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AddActivityUiState())
    val uiState: StateFlow<AddActivityUiState> = _uiState.asStateFlow()
    
    private var originalState: AddActivityUiState? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val allCows = repository.getAllCows().first()
                val allPastures = repository.getAllPastures().first()
                
                val activeCows = allCows.filter { it.status == Status.ACTIVE }
                
                var baseState = _uiState.value.copy(
                    availableCows = activeCows,
                    availablePastures = allPastures,
                    isLoading = false
                )

                // If editing, prefill from existing activity and load all cows from the same group
                editId?.let { id ->
                    repository.getActivityById(id)?.let { act ->
                        // If the activity has a groupId, load all activities from that group
                        val selectedCowIds = if (act.groupId != null) {
                            repository.getActivitiesByGroupId(act.groupId).map { it.cowId }.toSet()
                        } else {
                            // Fallback for legacy activities without groupId
                            setOf(act.cowId)
                        }
                        
                        baseState = baseState.copy(
                            activityType = act.activityType,
                            date = act.date,
                            notes = act.notes ?: "",
                            toPastureId = act.toPastureId,
                            selectedCows = selectedCowIds
                        )
                        originalState = baseState
                    }
                }

                _uiState.value = baseState
                if (originalState == null) {
                    originalState = baseState
                }
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
        updateUnsavedChanges()
    }

    fun updateDate(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(date = date)
        updateUnsavedChanges()
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
        updateUnsavedChanges()
    }

    // Changed pastureId to String?
    fun updateToPasture(pastureId: String?) {
        val pasture = _uiState.value.availablePastures.find { it.id == pastureId } // Now String == String
        _uiState.value = _uiState.value.copy(
            toPastureId = pastureId,
            toPastureName = pasture?.name
        )
        updateUnsavedChanges()
    }

    fun selectCow(cowId: Long) {
        val currentSelection = _uiState.value.selectedCows.toMutableSet()
        currentSelection.add(cowId)
        _uiState.value = _uiState.value.copy(selectedCows = currentSelection)
        updateUnsavedChanges()
    }

    fun deselectCow(cowId: Long) {
        val currentSelection = _uiState.value.selectedCows.toMutableSet()
        currentSelection.remove(cowId)
        _uiState.value = _uiState.value.copy(selectedCows = currentSelection)
        updateUnsavedChanges()
    }

    fun selectAllCows() {
        val allCowIds = _uiState.value.availableCows.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedCows = allCowIds)
        updateUnsavedChanges()
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedCows = emptySet())
        updateUnsavedChanges()
    }
    
    private fun updateUnsavedChanges() {
        val current = _uiState.value
        val original = originalState ?: AddActivityUiState()
        
        val hasChanges = current.activityType != original.activityType ||
                current.date != original.date ||
                current.notes != original.notes ||
                current.toPastureId != original.toPastureId ||
                current.selectedCows != original.selectedCows
        
        _uiState.value = current.copy(hasUnsavedChanges = hasChanges)
    }

    fun saveActivity() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val errors = mutableListOf<String>()
                
                if (state.activityType == null) {
                    errors.add("Please select an activity type")
                }
                
                if (state.selectedCows.isEmpty()) {
                    errors.add("Please select at least one animal")
                }
                
                if (state.date == null) {
                    errors.add("Please select a date")
                }
                
                if (state.activityType in listOf(ActivityType.WORKED, ActivityType.OTHER) && state.notes.isBlank()) {
                    errors.add("Notes are required for this activity type")
                }
                
                if (state.activityType == ActivityType.MOVED && state.toPastureId == null) {
                    errors.add("Please select a destination pasture")
                }
                
                if (errors.isNotEmpty()) {
                    _uiState.value = state.copy(error = errors.joinToString("\n• ", "• "))
                    return@launch
                }
                
                if (editId != null) {
                    // Editing existing activity group
                    val originalActivity = repository.getActivityById(editId)
                    if (originalActivity?.groupId != null) {
                        // Soft delete all activities in the group
                        val groupActivities = repository.getActivitiesByGroupId(originalActivity.groupId)
                        groupActivities.forEach { 
                            val deletedActivity = it.copy(
                                isDeleted = true,
                                lastSyncAt = System.currentTimeMillis()
                            )
                            repository.updateActivity(deletedActivity)
                            
                            // Sync the deletion immediately if user is signed in
                            val application = getApplication<CattleApplication>()
                            application.authService.currentUser.first()?.let { currentUser ->
                                if (!currentUser.isLocalUser) {
                                    application.syncService.syncItemImmediately(currentUser.uid, deletedActivity)
                                }
                            }
                        }
                        
                        // Create new activities with the same groupId
                        val createdActivities = repository.createBulkActivityWithGroupId(
                            cowIds = state.selectedCows.toList(),
                            activityType = state.activityType!!,
                            date = state.date!!,
                            notes = state.notes.takeIf { it.isNotBlank() },
                            toPastureId = state.toPastureId,
                            groupId = originalActivity.groupId
                        )
                        
                        // Sync the newly created activities immediately if user is signed in
                        val application = getApplication<CattleApplication>()
                        application.authService.currentUser.first()?.let { currentUser ->
                            if (!currentUser.isLocalUser) {
                                createdActivities.forEach { activity ->
                                    application.syncService.syncItemImmediately(currentUser.uid, activity)
                                }
                            }
                        }
                    } else if (originalActivity != null) {
                        // Legacy activity without groupId, just update the single activity
                        val updatedActivity = originalActivity.copy(
                            activityType = state.activityType!!,
                            date = state.date!!,
                            notes = state.notes.takeIf { it.isNotBlank() },
                            toPastureId = state.toPastureId
                        )
                        repository.updateActivity(updatedActivity)
                        
                        // Sync the updated activity immediately if user is signed in
                        val application = getApplication<CattleApplication>()
                        application.authService.currentUser.first()?.let { currentUser ->
                            if (!currentUser.isLocalUser) {
                                application.syncService.syncItemImmediately(currentUser.uid, updatedActivity)
                            }
                        }
                    }
                } else {
                    // Creating new activity
                    val createdActivities = repository.createBulkActivity(
                        cowIds = state.selectedCows.toList(),
                        activityType = state.activityType!!,
                        date = state.date!!,
                        notes = state.notes.takeIf { it.isNotBlank() },
                        toPastureId = state.toPastureId
                    )
                    
                    // Sync the newly created activities immediately if user is signed in
                    val application = getApplication<CattleApplication>()
                    application.authService.currentUser.first()?.let { currentUser ->
                        if (!currentUser.isLocalUser) {
                            createdActivities.forEach { activity ->
                                application.syncService.syncItemImmediately(currentUser.uid, activity)
                            }
                        }
                    }
                }
                
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
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false
)