package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Breed
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BreedsViewModel(
    private val repository: CattleRepository,
    private val getUserId: () -> String
) : ViewModel() {

    val breeds: StateFlow<List<Breed>> = repository.getAllBreeds().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addBreed(name: String) {
        viewModelScope.launch {
            val breed = Breed(
                name = name,
                updatedBy = getUserId()
            )
            repository.insertBreed(breed)
        }
    }

    fun updateBreed(breed: Breed) {
        viewModelScope.launch {
            repository.updateBreed(breed.copy(
                updatedAt = System.currentTimeMillis(),
                updatedBy = getUserId()
            ))
        }
    }

    fun deleteBreed(breed: Breed) {
        viewModelScope.launch {
            repository.deleteBreed(breed)
        }
    }

    fun restoreBreed(breed: Breed) {
        viewModelScope.launch {
            repository.updateBreed(
                breed.copy(
                    isDeleted = false,
                    isActive = true,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = getUserId()
                )
            )
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.restoreDefaultBreeds()
        }
    }
}