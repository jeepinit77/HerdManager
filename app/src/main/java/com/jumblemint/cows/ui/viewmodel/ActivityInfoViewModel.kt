package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Activity 
import com.jumblemint.cows.data.model.Cow 
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.screens.activities.ActivityInfoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ActivityInfoViewModel(
    private val repository: CattleRepository,
    private val activityId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityInfoUiState(isLoading = true))
    val uiState: StateFlow<ActivityInfoUiState> = _uiState.asStateFlow()

    init {
        loadActivityDetails()
    }

    private fun loadActivityDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val primaryActivity: Activity? = repository.getActivityById(activityId)

                if (primaryActivity != null) {
                    val idsToFetch = mutableSetOf<Long>()

                    if (!primaryActivity.groupId.isNullOrBlank()) {
                        // Grouped activity: aggregate IDs from all activities in the group
                        val groupActivities: List<Activity> = repository.getActivitiesByGroupId(primaryActivity.groupId)
                        if (groupActivities.isNotEmpty()) {
                            groupActivities.forEach { activityInGroup ->
                                if (activityInGroup.cowIds.isNotEmpty()) {
                                    idsToFetch.addAll(activityInGroup.cowIds)
                                } else if (activityInGroup.cowId != 0L) { // Fallback for individual record in group
                                    idsToFetch.add(activityInGroup.cowId)
                                }
                            }
                        }
                        // Safety net if group processing yielded no IDs (e.g. groupActivities was empty)
                        if (idsToFetch.isEmpty()) {
                            if (primaryActivity.cowIds.isNotEmpty()) {
                                idsToFetch.addAll(primaryActivity.cowIds)
                            } else if (primaryActivity.cowId != 0L) {
                                idsToFetch.add(primaryActivity.cowId)
                            }
                        }
                    } else {
                        // Single, non-grouped activity
                        if (primaryActivity.cowIds.isNotEmpty()) {
                            idsToFetch.addAll(primaryActivity.cowIds)
                        } else if (primaryActivity.cowId != 0L) {
                            idsToFetch.add(primaryActivity.cowId)
                        }
                    }

                    val associatedCows: List<Cow> = if (idsToFetch.isNotEmpty()) {
                        repository.getCowsByIds(idsToFetch.toList()).firstOrNull() ?: emptyList()
                    } else {
                        emptyList()
                    }

                    _uiState.value = ActivityInfoUiState(
                        activity = primaryActivity,
                        associatedCows = associatedCows,
                        isLoading = false
                    )
                } else {
                    _uiState.value = ActivityInfoUiState(error = "Activity not found", isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = ActivityInfoUiState(error = e.message ?: "An unknown error occurred", isLoading = false)
            }
        }
    }
}