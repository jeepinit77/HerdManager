package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                // Load tag colors
                val tagColorsSetting = repository.getSettingByKey(SettingsKeys.TAG_COLORS)
                val tagColors = tagColorsSetting?.value?.split(",")?.map { it.trim() } ?: listOf(
                    "Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Pink", "White", "Black", "Brown"
                )
                
                // Load activity types
                val activityTypesSetting = repository.getSettingByKey(SettingsKeys.ACTIVITY_TYPES)
                val activityTypes = activityTypesSetting?.value?.split(",")?.map { it.trim() } ?: listOf(
                    "MOVED", "WEANED", "SOLD", "DECEASED", "WORKED", "CASTRATED", "BIRTH", "OTHER"
                )
                
                // REMOVED: val calfPasture = repository.getCalfPasture()
                
                _uiState.value = _uiState.value.copy(
                    tagColors = tagColors,
                    activityTypes = activityTypes,
                    // REMOVED: defaultCalfPasture = calfPasture?.name,
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
    
    fun updateTagColors(colors: List<String>) {
        viewModelScope.launch {
            try {
                val setting = Settings(
                    key = SettingsKeys.TAG_COLORS,
                    value = colors.joinToString(",")
                )
                repository.insertOrUpdateSetting(setting)
                
                _uiState.value = _uiState.value.copy(tagColors = colors)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateActivityTypes(types: List<String>) {
        viewModelScope.launch {
            try {
                val setting = Settings(
                    key = SettingsKeys.ACTIVITY_TYPES,
                    value = types.joinToString(",")
                )
                repository.insertOrUpdateSetting(setting)
                
                _uiState.value = _uiState.value.copy(activityTypes = types)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
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
}

data class SettingsUiState(
    val tagColors: List<String> = emptyList(),
    val activityTypes: List<String> = emptyList(),
    val defaultCalfPasture: String? = null, // This will remain null by default
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null
)
