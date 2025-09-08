package com.jumblemint.cows.ui.viewmodel

import android.app.Application // <<< IMPORT Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import com.jumblemint.cows.data.model.Settings // Not directly used
// import com.jumblemint.cows.data.model.SettingsKeys // Not directly used
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val application: Application, // <<< ADD Application context
    private val repository: CattleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load app version first as it's quick and doesn't involve DB/network
            try {
                val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                _uiState.value = _uiState.value.copy(appVersion = packageInfo.versionName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(appVersion = "N/A") // Fallback
            }

            // TODO: Replace with actual logic to get last sync time from SyncService or Repository
            // For now, using a placeholder. This might come from an observable flow.
            _uiState.value = _uiState.value.copy(lastSyncTime = "Sync status unavailable")


            // Load other settings
            try {
                val tagColors = repository.getAllTagColors().first().map { it.name }
                val activityTypes = repository.getAllActivityTypes().first().map { it.displayName }
                val isSampleDataInstalled = repository.isSampleDataInstalled()

                _uiState.value = _uiState.value.copy(
                    tagColors = tagColors,
                    activityTypes = activityTypes,
                    isSampleDataInstalled = isSampleDataInstalled,
                    isLoading = false // Settings loaded
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load settings: ${e.message}",
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
                _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)
                when (format) {
                    "CSV" -> exportToCSV()
                    "JSON" -> exportToJSON()
                }
                // isLoading should be set to false in exportToCSV/JSON or here if they are quick
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Export failed: ${e.message}", isLoading = false)
            }
        }
    }

    private suspend fun exportToCSV() {
        // TODO: Implement CSV export
        _uiState.value = _uiState.value.copy(
            message = "CSV export functionality coming soon.",
            isLoading = false
        )
    }

    private suspend fun exportToJSON() {
        // TODO: Implement JSON export
        _uiState.value = _uiState.value.copy(
            message = "JSON export functionality coming soon.",
            isLoading = false
        )
    }

    fun installSampleData() {
        viewModelScope.launch {
            if (_uiState.value.isSampleDataInstalled) {
                _uiState.value = _uiState.value.copy(
                    message = "Sample data is already installed.",
                    isLoading = false // Ensure loading is reset
                )
                return@launch
            }
            try {
                _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)
                repository.installSampleData() // This can still throw an error, which we need to fix in the repo
                _uiState.value = _uiState.value.copy(
                    isSampleDataInstalled = true,
                    message = "Sample data installed successfully.",
                    isLoading = false
                )
            } catch (e: Exception) {
                // The error message from the screenshot indicates the exception message is "Index 25 out of bounds for length 25"
                _uiState.value = _uiState.value.copy(
                    error = "Failed to install sample data: ${e.message}", // Keep original error reporting
                    isLoading = false
                )
            }
        }
    }

    fun deleteSampleData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)
                repository.deleteSampleData()
                _uiState.value = _uiState.value.copy(
                    isSampleDataInstalled = false,
                    message = "Sample data deleted successfully.",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete sample data: ${e.message}", isLoading = false)
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)
                repository.deleteAllData()
                _uiState.value = _uiState.value.copy(
                    isSampleDataInstalled = repository.isSampleDataInstalled(),
                    message = "All application data deleted successfully.",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete all data: ${e.message}", isLoading = false)
            }
        }
    }

    fun deleteSelectedData(selection: DeleteSelection) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)
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
                if (selection.settings) repository.deleteAllSettings() // Potentially risky

                _uiState.value = _uiState.value.copy(
                    message = "Selected data categories deleted successfully.",
                    isLoading = false,
                    isSampleDataInstalled = if (selection.cows || selection.pastures) repository.isSampleDataInstalled() else _uiState.value.isSampleDataInstalled
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete selected data: ${e.message}", isLoading = false)
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
    val defaultCalfPasture: String? = null,
    val isSampleDataInstalled: Boolean = false,
    val appVersion: String = "", // <<< ADDED appVersion
    val lastSyncTime: String? = null, // <<< ADDED lastSyncTime
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null
)
