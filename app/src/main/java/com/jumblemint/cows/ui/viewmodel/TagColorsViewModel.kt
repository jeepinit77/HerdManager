package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.TagColor
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TagColorsViewModel(
    private val repository: CattleRepository,
    private val syncService: SyncService,
    private val getUserId: () -> String
) : ViewModel() {

    val tagColors: StateFlow<List<TagColor>> = repository.getAllTagColors()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    fun addTagColor(name: String, argb: Int) {
        viewModelScope.launch {
            val tagColor = TagColor(
                id = UUID.randomUUID().toString(),
                name = name,
                colorValue = argb,
                isDefault = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertTagColor(tagColor)
            syncService.syncItemImmediately(getUserId(), tagColor)
        }
    }

    fun updateTagColor(tagColor: TagColor) {
        viewModelScope.launch {
            val updated = tagColor.copy(updatedAt = System.currentTimeMillis())
            repository.upsertTagColor(updated)
            syncService.syncItemImmediately(getUserId(), updated)
        }
    }

    fun deleteTagColor(tagColor: TagColor) {
        viewModelScope.launch {
            repository.deleteTagColor(tagColor)
            syncService.syncItemImmediately(getUserId(), tagColor.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun restoreTagColor(tagColor: TagColor) {
        viewModelScope.launch {
            val restored = tagColor.copy(isDeleted = false, updatedAt = System.currentTimeMillis())
            repository.upsertTagColor(restored)
            syncService.syncItemImmediately(getUserId(), restored)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val existing = repository.getAllTagColorsSync()
            // Delete all non-default custom colors
            existing.filter { !it.isDefault }.forEach { custom ->
                repository.deleteTagColor(custom)
                syncService.syncItemImmediately(getUserId(), custom.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
            }
            // Ensure defaults exist (re-insert if missing)
            repository.ensureDefaultTagColorsExist()
            // Also sync default colors up
            repository.getAllTagColorsSync().filter { it.isDefault }.forEach { def ->
                syncService.syncItemImmediately(getUserId(), def)
            }
        }
    }

    fun toggleTagColorActive(tagColor: TagColor) {
        updateTagColor(tagColor.copy(isActive = !tagColor.isActive))
    }
}