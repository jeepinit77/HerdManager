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
                isActive = false,
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
                isActive = true,
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
            val existingTypes = repository.getAllActivityTypesSync()
            
            // Create a map of existing types by name for quick lookup
            val existingByName = existingTypes.associateBy { it.name }

            // Soft delete all custom (non-default) activity types
            existingTypes.filter { !it.isDefault }.forEach { customType ->
                if (!customType.isDeleted) {
                    val deletedCustomType = customType.copy(
                        isActive = false,
                        isDeleted = true,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = userId
                    )
                    repository.upsertActivityType(deletedCustomType)
                    syncService.syncItemImmediately(userId, deletedCustomType)
                }
            }

            // Get the standard default types
            val defaultTypes = ActivityTypeConfig.getDefaultActivityTypes()
            defaultTypes.forEach { defaultType ->
                // Check if this default type already exists by name
                val existing = existingByName[defaultType.name]
                val typeToUpsert = if (existing != null) {
                    // Update existing record to ensure it has current default properties
                    existing.copy(
                        displayName = defaultType.displayName,
                        iconName = defaultType.iconName,
                        description = defaultType.description,
                        isDeleted = false,
                        isActive = true,
                        isDefault = true,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = userId
                    )
                } else {
                    // Create new default with unique ID
                    defaultType.copy(
                        id = UUID.randomUUID().toString(),
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = userId,
                        isDeleted = false
                    )
                }
                repository.upsertActivityType(typeToUpsert)
                syncService.syncItemImmediately(userId, typeToUpsert)
            }
        }
    }
}
