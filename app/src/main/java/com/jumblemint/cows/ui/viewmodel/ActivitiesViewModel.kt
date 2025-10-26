package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.util.primaryIdentifier
import com.jumblemint.cows.util.usesNames
import com.jumblemint.cows.util.usesTags
import java.time.LocalDate
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
                repository.getAllPastures(),
                repository.getAnimalIdentifierModeFlow()
            ) { activities, allCows, pastures, identifierMode ->
                val currentState = _uiState.value
                val pastureNames = pastures.map { it.name }

                // Use selected date range when set, fall back to entire list otherwise
                val activitiesInRange = currentState.dateRange?.let { (startDate, endDate) ->
                    activities.filter { it.date in startDate..endDate }
                } ?: activities

                // Filter activities based on search and filters
                val filteredActivities = activitiesInRange.filter { activity ->
                    val cow = allCows.find { it.id == activity.cowId }
                    val associatedCows = if (activity.cowIds.isNotEmpty()) {
                        activity.cowIds.mapNotNull { cowId -> allCows.find { it.id == cowId } }
                    } else {
                        listOfNotNull(cow)
                    }

                    // Search filter
                    val searchMatch = if (currentState.searchQuery.isBlank()) {
                        true
                    } else {
                        val query = currentState.searchQuery.lowercase()
                        activity.activityType.displayName.lowercase().contains(query) ||
                            activity.notes?.lowercase()?.contains(query) == true ||
                            associatedCows.any { matchesIdentifierQuery(it, query, identifierMode) } ||
                            associatedCows.any { it.classification.name.lowercase().contains(query) } ||
                            associatedCows.any { it.gender.name.lowercase().contains(query) } ||
                            associatedCows.any { it.breed?.lowercase()?.contains(query) == true } ||
                            associatedCows.any { it.tagColor?.lowercase()?.contains(query) == true }
                    }

                    associatedCows.firstOrNull()?.let { c ->
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
                    act.groupId
                        ?: "legacy_${act.date}_${act.activityType}_${act.notes}_${act.fromPastureId}_${act.toPastureId}_${act.details}"
                }

                val grouped = groups.map { (groupId, acts) ->
                    // representative activity (first) for metadata/id when editing a single instance
                    val sample = acts.first()
                    val identifiers = acts.flatMap { act ->
                        when {
                            act.cowIds.isNotEmpty() -> act.cowIds.mapNotNull { id ->
                                allCows.find { cow -> cow.id == id }
                            }
                            act.cowId != 0L -> listOfNotNull(allCows.find { cow -> cow.id == act.cowId })
                            else -> emptyList()
                        }
                    }.distinctBy { it.id }
                        .map { cow -> identifierMode.primaryIdentifier(cow.name, cow.tagNumber, fallback = "Unnamed Animal") }
                    ActivityGroup(
                        groupId = groupId,
                        sample = sample,
                        activities = acts,
                        cowIdentifiers = identifiers
                    )
                }

                val usedTypes = activities.map { it.activityType }.distinct().sortedBy { it.displayName }

                _uiState.value = _uiState.value.copy(
                    activityGroups = grouped.sortedWith(compareByDescending<ActivityGroup> { it.sample.date }.thenByDescending { it.sample.id }),
                    availablePastures = pastureNames,
                    usedActivityTypes = usedTypes,
                    identifierMode = identifierMode,
                    isLoading = false
                )
            }.collect { }
        }
    }

    private fun matchesIdentifierQuery(cow: com.jumblemint.cows.data.model.Cow, query: String, identifierMode: AnimalIdentifierMode): Boolean {
        val identifiers = when {
            identifierMode.usesNames() && identifierMode.usesTags() -> listOfNotNull(cow.name, cow.tagNumber)
            identifierMode.usesNames() -> listOfNotNull(cow.name)
            identifierMode.usesTags() -> listOfNotNull(cow.tagNumber)
            else -> emptyList()
        }
        return identifiers.any { it.lowercase().contains(query) }
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

    fun updateDateRange(range: Pair<LocalDate, LocalDate>?) {
        _uiState.value = _uiState.value.copy(dateRange = range)
        loadActivitiesWithFilters()
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
            dateRange = null,
            searchQuery = ""
        )
        loadActivitiesWithFilters()
    }

    suspend fun getPreviewResultCount(
        previewDateRange: Pair<LocalDate, LocalDate>?,
        previewActivityTypes: Set<ActivityType>
    ): Int {
        val activities = repository.getAllActivities().first()
        val allCows = repository.getAllCows().first()
        val pastures = repository.getAllPastures().first()
        val currentState = _uiState.value

        // Use preview date range when set, fall back to entire list otherwise
        val activitiesInRange = previewDateRange?.let { (startDate, endDate) ->
            activities.filter { it.date in startDate..endDate }
        } ?: activities

        // Filter activities based on search and filters (using preview activity types)
        val filteredActivities = activitiesInRange.filter { activity ->
            val cow = allCows.find { it.id == activity.cowId }

            // Search filter (keep current search query)
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
                // Status filter (keep current filters)
                val statusMatch = currentState.selectedStatuses.isEmpty() ||
                    currentState.selectedStatuses.contains(c.status)

                // Classification filter (keep current filters)
                val classificationMatch = currentState.selectedClassifications.isEmpty() ||
                    currentState.selectedClassifications.contains(c.classification)

                // Gender filter (keep current filters)
                val genderMatch = currentState.selectedGenders.isEmpty() ||
                    currentState.selectedGenders.contains(c.gender)

                // Pasture filter (keep current filters)
                val pastureMatch = if (currentState.selectedPastures.isEmpty()) {
                    true
                } else {
                    val cowPastureName = c.pastureId?.let { pastureId ->
                        pastures.find { it.id == pastureId }?.name
                    }
                    cowPastureName?.let { currentState.selectedPastures.contains(it) } ?: false
                }

                // Activity type filter (use PREVIEW types instead of current)
                val activityTypeMatch = previewActivityTypes.isEmpty() ||
                    previewActivityTypes.contains(activity.activityType)

                searchMatch && statusMatch && classificationMatch && genderMatch && pastureMatch && activityTypeMatch
            } ?: false
        }

        // Group by groupId to count activity groups, not individual activities
        val groups = filteredActivities.groupBy { act ->
            act.groupId
                ?: "legacy_${act.date}_${act.activityType}_${act.notes}_${act.fromPastureId}_${act.toPastureId}_${act.details}"
        }

        return groups.size
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
    val dateRange: Pair<LocalDate, LocalDate>? = null,
    val availablePastures: List<String> = emptyList(),
    val usedActivityTypes: List<ActivityType> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val identifierMode: AnimalIdentifierMode = AnimalIdentifierMode.BOTH
)

data class ActivityGroup(
    val groupId: String,
    val sample: Activity,
    val activities: List<Activity>,
    val cowIdentifiers: List<String>
)
