package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CowInfoViewModel(
    private val repository: CattleRepository,
    private val cowId: Long
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CowInfoUiState())
    val uiState: StateFlow<CowInfoUiState> = _uiState.asStateFlow()
    
    init {
        loadCowInfo()
    }
    
    private fun loadCowInfo() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                combine(
                    repository.getCowByIdFlow(cowId),
                    repository.getAllCows(),
                    repository.getAllPastures(),
                    repository.getActivitiesForCow(cowId)
                ) { cow: Cow?, allCows: List<Cow>, pastures: List<Pasture>, activities: List<Activity> ->
                    CowInfoData(cow, allCows, pastures, activities)
                }.collect { data: CowInfoData ->
                    val cow = data.cow
                    val allCows = data.allCows
                    val pastures = data.pastures
                    val activities = data.activities
                    
                    if (cow != null) {
                        // Find mother and father
                        val mother = cow.motherId?.let { motherId ->
                            allCows.find { it.id == motherId }
                        }
                        val father = cow.fatherId?.let { fatherId ->
                            allCows.find { it.id == fatherId }
                        }
                        
                        // Find children
                        val children = allCows.filter { it.motherId == cowId || it.fatherId == cowId }
                            .sortedByDescending { it.birthDate }
                        
                        // Find pasture name
                        val pastureName = cow.pastureId?.let { pastureId ->
                            pastures.find { it.id == pastureId }?.name
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            cow = cow,
                            mother = mother,
                            father = father,
                            children = children,
                            pastureName = pastureName,
                            activities = activities.sortedByDescending { it.date },
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Cow not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading cow information: ${e.message}"
                )
            }
        }
    }
    
    fun toggleWatch() {
        viewModelScope.launch {
            _uiState.value.cow?.let { cow ->
                try {
                    repository.updateCow(cow.copy(isWatched = !cow.isWatched))
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error updating watch status: ${e.message}"
                    )
                }
            }
        }
    }
}

data class CowInfoUiState(
    val cow: Cow? = null,
    val mother: Cow? = null,
    val father: Cow? = null,
    val children: List<Cow> = emptyList(),
    val pastureName: String? = null,
    val activities: List<Activity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// Helper data class for combine
data class CowInfoData(val cow: Cow?, val allCows: List<Cow>, val pastures: List<Pasture>, val activities: List<Activity>)