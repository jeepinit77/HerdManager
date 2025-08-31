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
                // Load cow names for each activity
                val activitiesWithCows = activities.map { activity ->
                    val cow = repository.getCowById(activity.cowId)
                    ActivityWithCow(activity, cow?.name)
                }
                
                _uiState.value = _uiState.value.copy(
                    activities = activitiesWithCows,
                    isLoading = false
                )
            }
        }
    }

    // Deletion + undo helpers
    suspend fun deleteActivity(activity: Activity) {
        repository.deleteActivity(activity)
    }

    suspend fun undoDeleteActivity(activity: Activity) {
        repository.insertActivity(activity.copy(id = 0))
        loadActivities()
    }
}

data class ActivitiesUiState(
    val activities: List<ActivityWithCow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ActivityWithCow(
    val activity: Activity,
    val cowName: String?
)