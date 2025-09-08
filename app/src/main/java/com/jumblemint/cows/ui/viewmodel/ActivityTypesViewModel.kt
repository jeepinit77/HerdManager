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
            val existingTypes = repository.getAllActivityTypesSync()

            // Soft delete all custom (non-default) activity types
            existingTypes.filter { !it.isDefault }.forEach { customType ->
                if (!customType.isDeleted) { // Only delete if not already deleted
                    val deletedCustomType = customType.copy(
                        isDeleted = true,
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = userId
                    )
                    repository.upsertActivityType(deletedCustomType) // <<< USE UPSERT
                    syncService.syncItemImmediately(userId, deletedCustomType)
                }
            }

            // Get the standard default types
            val defaultTypes = ActivityTypeConfig.getDefaultActivityTypes()
            defaultTypes.forEach { defaultType ->
                // Check if this default type already exists (by name, as ID might differ if user deleted and restored)
                val existingDefault = existingTypes.find { it.name == defaultType.name && it.isDefault }
                val typeToUpsert = if (existingDefault != null) {
                    // If it exists, ensure it's not deleted and has correct properties
                    existingDefault.copy(
                        displayName = defaultType.displayName, // Ensure display name is current
                        iconName = defaultType.iconName, // Ensure icon is current
                        description = defaultType.description ?: existingDefault.description, // Keep existing desc if new is null
                        isDeleted = false, // Ensure it's not marked as deleted
                        isActive = true, // Ensure it's active
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = userId
                    )
                } else {
                    // If it doesn't exist, prepare it for insertion (ID will be new if not found)
                    defaultType.copy(
                        id = UUID.randomUUID().toString(), // New ID for a fresh default
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