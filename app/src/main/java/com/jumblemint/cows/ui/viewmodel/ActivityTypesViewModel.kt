package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.ActivityTypeConfig
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ActivityTypesViewModel(
    private val repository: CattleRepository,
    private val syncService: SyncService,
    private val getUserId: () -> String
) : ViewModel() {

    val activityTypes: StateFlow<List<ActivityTypeConfig>> = repository.getAllActivityTypes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addActivityType(
        name: String,
        displayName: String,
        description: String? = null,
        iconName: String? = null
    ) {
        viewModelScope.launch {
            val userId = getUserId()
            val timestamp = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val activityType = ActivityTypeConfig(
                id = id,
                name = name,
                displayName = displayName,
                description = description,
                iconName = iconName,
                isActive = true,
                isDefault = false,
                createdAt = timestamp,
                updatedAt = timestamp,
                firestoreId = id,
                updatedBy = userId
            )
            repository.insertActivityType(activityType)
            syncService.syncItemImmediately(userId, activityType)
        }
    }

    fun updateActivityType(activityType: ActivityTypeConfig) {
        viewModelScope.launch {
            val userId = getUserId()
            val updated = activityType.copy(
                updatedAt = System.currentTimeMillis(),
                updatedBy = userId,
                isDeleted = false,
                isActive = true
            )
            repository.insertActivityType(updated)
            syncService.syncItemImmediately(userId, updated)
        }
    }

    fun deleteActivityType(activityType: ActivityTypeConfig) {
        viewModelScope.launch {
            if (activityType.isDeleted) return@launch
            val userId = getUserId()
            val deletedType = activityType.copy(
                isDeleted = true,
                isActive = false,
                updatedAt = System.currentTimeMillis(),
                firestoreId = activityType.firestoreId ?: activityType.id,
                updatedBy = userId
            )
            repository.insertActivityType(deletedType)
            syncService.syncItemImmediately(userId, deletedType)
        }
    }

    fun restoreDeletedActivityType(activityTypeConfig: ActivityTypeConfig) {
        viewModelScope.launch {
            val userId = getUserId()
            val restoredType = activityTypeConfig.copy(
                isDeleted = false,
                isActive = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = userId
            )
            repository.insertActivityType(restoredType)
            syncService.syncItemImmediately(userId, restoredType)
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            val userId = getUserId()
            val (deletedTypes, defaultTypes) = repository.restoreDefaultActivityTypes(updatedBy = userId)
            deletedTypes.forEach { syncService.syncItemImmediately(userId, it) }
            defaultTypes.forEach { syncService.syncItemImmediately(userId, it) }
            val idsToKeep = defaultTypes.mapNotNull { it.firestoreId ?: it.id }.toSet()
            syncService.markRemoteActivityTypesDeletedExcept(userId, idsToKeep)
        }
    }
}
