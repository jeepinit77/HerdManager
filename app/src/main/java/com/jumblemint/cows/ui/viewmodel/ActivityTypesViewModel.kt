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
        iconName: String? = null // <<< ADDED iconName parameter
    ) {
        viewModelScope.launch {
            val activityType = ActivityTypeConfig(
                id = UUID.randomUUID().toString(), // Ensure new ID for brand new types
                name = name,
                displayName = displayName,
                description = description,
                iconName = iconName, // <<< USE iconName
                isActive = true,
                isDefault = false,
                updatedAt = System.currentTimeMillis(),
                updatedBy = getUserId() // Set who created/updated it
            )
            repository.upsertActivityType(activityType)
            syncService.syncItemImmediately(getUserId(), activityType)
        }
    }

    fun updateActivityType(activityType: ActivityTypeConfig) {
        viewModelScope.launch {
            val updated = activityType.copy(
                updatedAt = System.currentTimeMillis(),
                updatedBy = getUserId() // Set who updated it
            )
            repository.upsertActivityType(updated)
            syncService.syncItemImmediately(getUserId(), updated)
        }
    }

    fun deleteActivityType(activityType: ActivityTypeConfig) {
        viewModelScope.launch {
            val deletedType = activityType.copy(
                isDeleted = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = getUserId()
            )
            repository.upsertActivityType(deletedType) // <<< USE UPSERT FOR SOFT DELETE
            syncService.syncItemImmediately(getUserId(), deletedType)
        }
    }

    // <<< NEW METHOD TO RESTORE (UNDO DELETE)
    fun restoreDeletedActivityType(activityTypeConfig: ActivityTypeConfig) {
        viewModelScope.launch {
            val restoredType = activityTypeConfig.copy(
                isDeleted = false, // Unmark as deleted
                updatedAt = System.currentTimeMillis(),
                updatedBy = getUserId()
            )
            repository.upsertActivityType(restoredType)
            syncService.syncItemImmediately(getUserId(), restoredType)
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            val userId = getUserId()
            val changeTimestamp = System.currentTimeMillis()
            repository.restoreDefaultActivityTypes()
            val updatedTypes = repository.getAllActivityTypesSync()
                .filter { it.updatedAt >= changeTimestamp }
            updatedTypes.forEach { type ->
                syncService.syncItemImmediately(userId, type)
            }
        }
    }
}
