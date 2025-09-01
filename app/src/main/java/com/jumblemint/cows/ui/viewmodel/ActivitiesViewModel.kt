package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivitiesViewModel(
    private val repository: CattleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()
    
    init {
        loadActivities()
    }
    
    private fun loadActivities() {
        viewModelScope.launch {
            repository.getAllActivities().collect { activities ->
                // Group activities by groupId (or fallback to old grouping for legacy data)
                val groups = activities.groupBy { act ->
                    act.groupId ?: "legacy_${act.date}_${act.activityType}_${act.notes}_${act.fromPastureId}_${act.toPastureId}_${act.details}"
                }

                val grouped = groups.map { (groupId, acts) ->
                    // representative activity (first) for metadata/id when editing a single instance
                    val sample = acts.first()
                    val cowNames = acts.map { it.cowId }.map { id -> repository.getCowById(id)?.name }
                    ActivityGroup(
                        groupId = groupId,
                        sample = sample,
                        activities = acts,
                        cowNames = cowNames
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    activityGroups = grouped.sortedWith(compareByDescending<ActivityGroup> { it.sample.date }.thenByDescending { it.sample.id }),
                    isLoading = false
                )
            }
        }
    }

    // Deletion + undo helpers
    suspend fun deleteActivities(activities: List<Activity>) {
        activities.forEach { repository.deleteActivity(it) }
    }

    suspend fun undoDeleteActivities(activities: List<Activity>) {
        activities.forEach { repository.insertActivity(it.copy(id = 0)) }
        loadActivities()
    }
}

data class ActivitiesUiState(
    val activityGroups: List<ActivityGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ActivityGroup(
    val groupId: String,
    val sample: Activity,
    val activities: List<Activity>,
    val cowNames: List<String?>
)