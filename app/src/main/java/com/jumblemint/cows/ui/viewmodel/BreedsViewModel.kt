package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Breed
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.sync.SyncService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BreedsViewModel(
    private val repository: CattleRepository,
    private val syncService: SyncService,
    private val getUserId: () -> String
) : ViewModel() {

    val breeds: StateFlow<List<Breed>> = repository.getAllBreeds().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addBreed(name: String) {
        viewModelScope.launch {
            val userId = getUserId()
            val timestamp = System.currentTimeMillis()
            val breed = Breed(
                name = name,
                createdAt = timestamp,
                updatedAt = timestamp,
                updatedBy = userId
            )
            repository.insertBreed(breed)
            syncService.syncItemImmediately(userId, breed)
        }
    }

    fun updateBreed(breed: Breed) {
        viewModelScope.launch {
            val userId = getUserId()
            val updated = breed.copy(
                updatedAt = System.currentTimeMillis(),
                updatedBy = userId,
                isDeleted = false,
                isActive = true
            )
            repository.insertBreed(updated)
            syncService.syncItemImmediately(userId, updated)
        }
    }

    fun deleteBreed(breed: Breed) {
        viewModelScope.launch {
            if (breed.isDeleted) return@launch
            val userId = getUserId()
            val deletionTimestamp = System.currentTimeMillis()
            val deleted = breed.copy(
                isDeleted = true,
                isActive = false,
                updatedAt = deletionTimestamp,
                firestoreId = breed.firestoreId ?: breed.id,
                updatedBy = userId
            )
            repository.insertBreed(deleted)
            syncService.syncItemImmediately(userId, deleted)
        }
    }

    fun restoreBreed(breed: Breed) {
        viewModelScope.launch {
            val userId = getUserId()
            val restored = breed.copy(
                isDeleted = false,
                isActive = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = userId
            )
            repository.insertBreed(restored)
            syncService.syncItemImmediately(userId, restored)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val userId = getUserId()
            val (deletedBreeds, defaultBreeds) = repository.restoreDefaultBreeds(updatedBy = userId)
            deletedBreeds.forEach { syncService.syncItemImmediately(userId, it) }
            defaultBreeds.forEach { syncService.syncItemImmediately(userId, it) }
        }
    }
}