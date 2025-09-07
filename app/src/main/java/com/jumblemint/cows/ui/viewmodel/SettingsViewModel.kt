package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: CattleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load tag colors from the new TagColor table
                val tagColors = repository.getAllTagColors().first().map { it.name }
                
                // Load activity types from the new ActivityTypeConfig table
                val activityTypes = repository.getAllActivityTypes().first().map { it.displayName }
                
                // Check if sample data is installed
                val isSampleDataInstalled = repository.isSampleDataInstalled()
                
                _uiState.value = _uiState.value.copy(
                    tagColors = tagColors,
                    activityTypes = activityTypes,
                    isSampleDataInstalled = isSampleDataInstalled,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    data class DeleteSelection(
        val cows: Boolean = false,
        val pastures: Boolean = false,
        val activities: Boolean = false,
        val notes: Boolean = false,
        val tagColors: Boolean = false,
        val activityTypes: Boolean = false,
        val settings: Boolean = false
    )
    
    fun exportData(format: String) {
        viewModelScope.launch {
            try {
                when (format) {
                    "CSV" -> exportToCSV()
                    "JSON" -> exportToJSON()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    private suspend fun exportToCSV() {
        // TODO: Implement CSV export
        _uiState.value = _uiState.value.copy(
            message = "CSV export functionality coming soon"
        )
    }
    
    private suspend fun exportToJSON() {
        // TODO: Implement JSON export
        _uiState.value = _uiState.value.copy(
            message = "JSON export functionality coming soon"
        )
    }
    
    fun installSampleData() {
        viewModelScope.launch {
            try {
                repository.installSampleData()
                _uiState.value = _uiState.value.copy(
                    isSampleDataInstalled = true,
                    message = "Sample data installed successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteSampleData() {
        viewModelScope.launch {
            try {
                repository.deleteSampleData()
                _uiState.value = _uiState.value.copy(
                    isSampleDataInstalled = false,
                    message = "Sample data deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteAllData() {
        viewModelScope.launch {
            try {
                repository.deleteAllData()
                _uiState.value = _uiState.value.copy(
                    isSampleDataInstalled = false,
                    message = "All data deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteSelectedData(selection: DeleteSelection) {
        viewModelScope.launch {
            try {
                if (selection.activities) repository.deleteAllActivities()
                if (selection.cows) repository.deleteAllCows()
                if (selection.pastures) repository.deleteAllPastures()
                if (selection.notes) repository.deleteAllNotes()
                if (selection.tagColors) {
                    repository.deleteAllTagColors()
                    repository.ensureDefaultTagColorsExist()
                }
                if (selection.activityTypes) {
                    repository.deleteAllActivityTypeConfigs()
                    repository.ensureDefaultActivityTypesExist()
                }
                if (selection.settings) repository.deleteAllSettings()
                _uiState.value = _uiState.value.copy(message = "Selected data deleted successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SettingsUiState(
    val tagColors: List<String> = emptyList(),
    val activityTypes: List<String> = emptyList(),
    val defaultCalfPasture: String? = null, // This will remain null by default
    val isSampleDataInstalled: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null
)
