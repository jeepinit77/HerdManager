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

    // Note: Removed automatic default insertion from init block
    // Defaults are now only inserted during:
    // 1. Initial app setup (repository initialization)
    // 2. Explicit user action (restoreDefaults())
    // 3. Sync operations when no data exists

    fun addActivityType(name: String, displayName: String, description: String? = null) {
        viewModelScope.launch {
            val activityType = ActivityTypeConfig(
                id = UUID.randomUUID().toString(),
                name = name,
                displayName = displayName,
                description = description,
                isActive = true,
                isDefault = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertActivityType(activityType)
            syncService.syncItemImmediately(getUserId(), activityType)
        }
    }

    fun updateActivityType(activityType: ActivityTypeConfig) {
        viewModelScope.launch {
            val updated = activityType.copy(updatedAt = System.currentTimeMillis())
            repository.upsertActivityType(updated)
            syncService.syncItemImmediately(getUserId(), updated)
        }
    }

    fun deleteActivityType(activityType: ActivityTypeConfig) {
        viewModelScope.launch {
            repository.deleteActivityType(activityType)
            syncService.syncItemImmediately(getUserId(), activityType.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            val existing = repository.getAllActivityTypesSync()
            // Delete all non-default custom activity types
            existing.filter { !it.isDefault }.forEach { custom ->
                repository.deleteActivityType(custom)
                syncService.syncItemImmediately(getUserId(), custom.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
            }
            // Restore defaults (re-insert if missing)
            repository.ensureDefaultActivityTypesExist()
            // Also sync default activity types up
            repository.getAllActivityTypesSync().filter { it.isDefault }.forEach { def ->
                syncService.syncItemImmediately(getUserId(), def)
            }
        }
    }
}