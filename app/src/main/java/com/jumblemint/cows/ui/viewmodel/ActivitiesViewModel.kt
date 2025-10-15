package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ActivitiesViewModel(
    application: Application,
    private val repository: CattleRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()
    
    init {
        loadActivitiesWithFilters()
    }
    
    private fun loadActivitiesWithFilters() {
        viewModelScope.launch {
            combine(
                repository.getAllActivities(),
                repository.getAllCows(),
                repository.getAllPastures()
            ) { activities, allCows, pastures ->
                val currentState = _uiState.value
                val pastureNames = pastures.map { it.name }
                
                // Filter activities based on search and filters
                val filteredActivities = activities.filter { activity ->
                    val cow = allCows.find { it.id == activity.cowId }
                    
                    // Search filter
                    val searchMatch = if (currentState.searchQuery.isBlank()) {
                        true
                    } else {
                        val query = currentState.searchQuery.lowercase()
                        activity.activityType.displayName.lowercase().contains(query) ||
                        activity.notes?.lowercase()?.contains(query) == true ||
                        cow?.name?.lowercase()?.contains(query) == true ||
                        cow?.tagNumber?.toString()?.contains(query) == true
                    }
                    
                    cow?.let { c ->
                        // Status filter
                        val statusMatch = currentState.selectedStatuses.isEmpty() || 
                                        currentState.selectedStatuses.contains(c.status)
                        
                        // Classification filter
                        val classificationMatch = currentState.selectedClassifications.isEmpty() || 
                                                currentState.selectedClassifications.contains(c.classification)
                        
                        // Gender filter
                        val genderMatch = currentState.selectedGenders.isEmpty() || 
                                        currentState.selectedGenders.contains(c.gender)
                        
                        // Pasture filter
                        val pastureMatch = if (currentState.selectedPastures.isEmpty()) {
                            true
                        } else {
                            val cowPastureName = c.pastureId?.let { pastureId ->
                                pastures.find { it.id == pastureId }?.name
                            }
                            cowPastureName?.let { currentState.selectedPastures.contains(it) } ?: false
                        }
                        
                        // Activity type filter
                        val activityTypeMatch = currentState.selectedActivityTypes.isEmpty() ||
                                              currentState.selectedActivityTypes.contains(activity.activityType)
                        
                        searchMatch && statusMatch && classificationMatch && genderMatch && pastureMatch && activityTypeMatch
                    } ?: false
                }

                // Group activities by groupId (or fallback to old grouping for legacy data)
                val groups = filteredActivities.groupBy { act ->
                    act.groupId ?: "legacy_${act.date}_${act.activityType}_${act.notes}_${act.fromPastureId}_${act.toPastureId}_${act.details}"
                }

                val grouped = groups.map { (groupId, acts) ->
                    // representative activity (first) for metadata/id when editing a single instance
                    val sample = acts.first()
                    val cowNames = acts.map { it.cowId }.map { id -> allCows.find { cow -> cow.id == id }?.name }
                    ActivityGroup(
                        groupId = groupId,
                        sample = sample,
                        activities = acts,
                        cowNames = cowNames
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    activityGroups = grouped.sortedWith(compareByDescending<ActivityGroup> { it.sample.date }.thenByDescending { it.sample.id }),
                    availablePastures = pastureNames,
                    isLoading = false
                )
            }.collect { }
        }
    }
    
    fun toggleStatusFilter(status: Status) {
        val currentStatuses = _uiState.value.selectedStatuses.toMutableSet()
        if (currentStatuses.contains(status)) {
            currentStatuses.remove(status)
        } else {
            currentStatuses.add(status)
        }
        // Empty set means show all statuses (no filter)
        _uiState.value = _uiState.value.copy(selectedStatuses = currentStatuses)
        loadActivitiesWithFilters() // Reload with new filters
    }
    
    fun toggleClassificationFilter(classification: Classification) {
        val currentClassifications = _uiState.value.selectedClassifications.toMutableSet()
        if (currentClassifications.contains(classification)) {
            currentClassifications.remove(classification)
        } else {
            currentClassifications.add(classification)
        }
        _uiState.value = _uiState.value.copy(selectedClassifications = currentClassifications)
        loadActivitiesWithFilters() // Reload with new filters
    }
    
    fun toggleGenderFilter(gender: Gender) {
        val currentGenders = _uiState.value.selectedGenders.toMutableSet()
        if (currentGenders.contains(gender)) {
            currentGenders.remove(gender)
        } else {
            currentGenders.add(gender)
        }
        _uiState.value = _uiState.value.copy(selectedGenders = currentGenders)
        loadActivitiesWithFilters() // Reload with new filters
    }
    
    fun togglePastureFilter(pastureName: String) {
        val currentPastures = _uiState.value.selectedPastures.toMutableSet()
        if (currentPastures.contains(pastureName)) {
            currentPastures.remove(pastureName)
        } else {
            currentPastures.add(pastureName)
        }
        _uiState.value = _uiState.value.copy(selectedPastures = currentPastures)
        loadActivitiesWithFilters() // Reload with new filters
    }
    
    fun toggleActivityTypeFilter(activityType: ActivityType) {
        val currentTypes = _uiState.value.selectedActivityTypes.toMutableSet()
        if (currentTypes.contains(activityType)) {
            currentTypes.remove(activityType)
        } else {
            currentTypes.add(activityType)
        }
        _uiState.value = _uiState.value.copy(selectedActivityTypes = currentTypes)
        loadActivitiesWithFilters() // Reload with new filters
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadActivitiesWithFilters()
    }
    
    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedStatuses = emptySet(),
            selectedClassifications = emptySet(),
            selectedGenders = emptySet(),
            selectedPastures = emptySet(),
            selectedActivityTypes = emptySet(),
            searchQuery = ""
        )
        loadActivitiesWithFilters()
    }

    // Deletion + undo helpers
    suspend fun deleteActivities(activities: List<Activity>) {
        activities.forEach { 
            val deletedActivity = it.copy(
                isDeleted = true,
                lastSyncAt = System.currentTimeMillis() // Update timestamp for sync
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
    }

    suspend fun undoDeleteActivities(activities: List<Activity>) {
        activities.forEach { 
            val restoredActivity = it.copy(
                isDeleted = false,
                lastSyncAt = System.currentTimeMillis() // Update timestamp for sync
            )
            repository.updateActivity(restoredActivity)
            
            // Sync the restoration immediately if user is signed in
            val application = getApplication<CattleApplication>()
            application.authService.currentUser.first()?.let { currentUser ->
                if (!currentUser.isLocalUser) {
                    application.syncService.syncItemImmediately(currentUser.uid, restoredActivity)
                }
            }
        }
    }
}

data class ActivitiesUiState(
    val activityGroups: List<ActivityGroup> = emptyList(),
    val selectedStatuses: Set<Status> = emptySet(),
    val selectedClassifications: Set<Classification> = emptySet(),
    val selectedGenders: Set<Gender> = emptySet(),
    val selectedPastures: Set<String> = emptySet(),
    val selectedActivityTypes: Set<ActivityType> = emptySet(),
    val availablePastures: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ActivityGroup(
    val groupId: String,
    val sample: Activity,
    val activities: List<Activity>,
    val cowNames: List<String?>
)