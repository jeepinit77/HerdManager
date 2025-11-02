package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.export.DataExporter
import com.jumblemint.cows.data.import.DataImporter
import com.jumblemint.cows.data.import.ImportResult
import com.jumblemint.cows.data.import.ConflictResolution
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.text.format.DateUtils
import com.jumblemint.cows.sync.ReplaceServerProgressEvent
import com.jumblemint.cows.sync.ReplaceServerProgressPhase
import com.jumblemint.cows.sync.ReplaceServerProgressStatus
import com.jumblemint.cows.sync.SyncService
import java.util.Locale

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

            val lastSyncSetting = repository.getSettingByKey(SettingsKeys.LAST_SYNC_TIMESTAMP)
            val lastSyncTime = lastSyncSetting?.value?.toLongOrNull()
            _uiState.value = _uiState.value.copy(lastSyncTime = formatLastSyncTime(lastSyncTime))


            // Load other settings
            try {
                val tagColors = repository.getAllTagColors().first().map { it.name }
                val activityTypes = repository.getAllActivityTypes().first().map { it.displayName }
                val isSampleDataInstalled = repository.isSampleDataInstalled()
                val identifierMode = repository.getAnimalIdentifierMode()

                _uiState.value = _uiState.value.copy(
                    tagColors = tagColors,
                    activityTypes = activityTypes,
                    isSampleDataInstalled = isSampleDataInstalled,
                    identifierMode = identifierMode,
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

    private fun formatLastSyncTime(timestamp: Long?): String? {
        if (timestamp == null || timestamp <= 0L) return null
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(
            timestamp,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    fun updateAnimalIdentifierMode(mode: AnimalIdentifierMode) {
        viewModelScope.launch {
            try {
                repository.setAnimalIdentifierMode(mode)
                _uiState.value = _uiState.value.copy(identifierMode = mode)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update identification preference: ${e.message}"
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
        val breeds: Boolean = false,
        val settings: Boolean = false
    ) {
        fun hasAnySelection(): Boolean =
            cows || pastures || activities || notes || tagColors || activityTypes || settings

        fun toRemoteCollections(): List<String> {
            val collections = mutableListOf<String>()
            if (cows) collections += "cows"
            if (pastures) collections += "pastures"
            if (activities) collections += "activities"
            if (notes) collections += "notes"
            if (tagColors) collections += "tagColors"
            if (activityTypes) collections += "activityTypes"
            if (settings) collections += "settings"
            return collections
        }
    }

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

    suspend fun prepareExportData(format: String): Pair<String, String> {
        val exporter = DataExporter(application)
        val cows = repository.getAllCowsSync()
        val pastures = repository.getAllPasturesSync()
        val activities = repository.getAllActivitiesSync()
        val notes = repository.getAllNotesSync()
        
        val file = when (format.uppercase()) {
            "CSV" -> exporter.exportToCsv(cows, pastures, activities, notes)
            "JSON" -> exporter.exportToJson(cows, pastures, activities, notes)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
        
        return Pair(file.absolutePath, exporter.getFileName(format))
    }
    
    private suspend fun exportToCSV() {
        _uiState.value = _uiState.value.copy(
            message = "Use the export dialog to save your CSV file",
            isLoading = false
        )
    }

    private suspend fun exportToJSON() {
        _uiState.value = _uiState.value.copy(
            message = "Use the export dialog to save your JSON file",
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

    suspend fun deleteSelectedData(selection: DeleteSelection) {
        try {
            _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)

            val deletedCategories = mutableListOf<String>()

            if (selection.activities) {
                repository.deleteAllActivities()
                deletedCategories.add("Activities")
            }
            if (selection.cows) {
                repository.deleteAllCows()
                deletedCategories.add("Cattle")
            }
            if (selection.pastures) {
                repository.deleteAllPastures()
                deletedCategories.add("Pastures")
            }
            if (selection.notes) {
                repository.deleteAllNotes()
                deletedCategories.add("Notes")
            }
            if (selection.tagColors) {
                repository.deleteAllTagColors()
                repository.ensureDefaultTagColorsExist()
                deletedCategories.add("Tagging Colors")
            }
            if (selection.activityTypes) {
                repository.deleteAllActivityTypeConfigs()
                repository.ensureDefaultActivityTypesExist()
                deletedCategories.add("Activity Types")
            }
            if (selection.breeds) {
                repository.restoreDefaultBreeds()
                deletedCategories.add("Breeds")
            }
            if (selection.settings) {
                repository.deleteAllSettings()
                deletedCategories.add("Settings")
            }

            val message = if (deletedCategories.isNotEmpty()) {
                "Deleted: ${deletedCategories.joinToString(", ")}"
            } else {
                "No categories selected for deletion"
            }

            _uiState.value = _uiState.value.copy(
                message = message,
                isLoading = false,
                isSampleDataInstalled = if (selection.cows || selection.pastures) repository.isSampleDataInstalled() else _uiState.value.isSampleDataInstalled
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to delete selected data: ${e.message}", isLoading = false)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearResetCloudDataProgress() {
        _uiState.value = _uiState.value.copy(resetCloudProgress = null)
    }

    fun setPendingExportFormat(format: String) {
        _uiState.value = _uiState.value.copy(pendingExportFormat = format)
    }

    suspend fun replaceServerDataWithDeviceSnapshot(
        syncService: SyncService,
        userId: String
    ): Result<Unit> {
        val totalSteps = 1 + SyncService.CLOUD_COLLECTIONS.size + SyncService.FORCE_UPLOAD_COLLECTIONS.size
        var completedSteps = 0
        var currentMessage = "Pausing realtime sync…"

        fun publishProgress() {
            _uiState.value = _uiState.value.copy(
                resetCloudProgress = ResetCloudDataProgress.InProgress(
                    message = currentMessage,
                    completedSteps = completedSteps.coerceAtMost(totalSteps),
                    totalSteps = totalSteps
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            message = null,
            error = null,
            resetCloudProgress = ResetCloudDataProgress.InProgress(
                message = currentMessage,
                completedSteps = completedSteps,
                totalSteps = totalSteps
            )
        )

        val handleProgress: (ReplaceServerProgressEvent) -> Unit = { event ->
            val label = friendlyCollectionLabel(event.target)
            when (event.phase) {
                ReplaceServerProgressPhase.CLEARING_COLLECTION -> {
                    if (event.status == ReplaceServerProgressStatus.STARTED) {
                        currentMessage = "Clearing $label from the cloud…"
                    } else {
                        completedSteps++
                        currentMessage = "${capitalizeLabel(label)} cleared from the cloud."
                    }
                }
                ReplaceServerProgressPhase.UPLOADING_COLLECTION -> {
                    if (event.status == ReplaceServerProgressStatus.STARTED) {
                        currentMessage = "Uploading $label from this device…"
                    } else {
                        completedSteps++
                        currentMessage = "${capitalizeLabel(label)} uploaded to the cloud."
                    }
                }
            }
            publishProgress()
        }

        runCatching { syncService.stopRealtimeSync(userId) }
            .onFailure {
                println("SettingsViewModel: Unable to stop realtime sync before replacing server data: ${it.message}")
                completedSteps++
                currentMessage = "Couldn't pause realtime sync: ${it.localizedMessage ?: "unknown error"}."
                publishProgress()
            }
            .onSuccess {
                completedSteps++
                currentMessage = "Realtime sync paused."
                publishProgress()
            }

        val replaceResult = runCatching {
            syncService.clearServerData(userId, handleProgress)
            syncService.forceUploadAllData(userId, handleProgress)
        }

        val restartResult = runCatching { syncService.startRealtimeSync(userId) }
            .onFailure {
                println("SettingsViewModel: Unable to restart realtime sync after replacing server data: ${it.message}")
            }

        if (replaceResult.isSuccess) {
            val summary = if (restartResult.isSuccess) {
                "Cloud data replaced successfully."
            } else {
                val reason = restartResult.exceptionOrNull()?.localizedMessage ?: "unknown error"
                "Cloud data replaced, but realtime sync may need attention: $reason"
            }
            _uiState.value = _uiState.value.copy(
                resetCloudProgress = ResetCloudDataProgress.Success(
                    completedSteps = totalSteps,
                    totalSteps = totalSteps,
                    summary = summary,
                    finishedAt = System.currentTimeMillis()
                )
            )
        } else {
            val errorMessage = replaceResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"
            _uiState.value = _uiState.value.copy(
                resetCloudProgress = ResetCloudDataProgress.Error(
                    completedSteps = completedSteps.coerceAtMost(totalSteps),
                    totalSteps = totalSteps,
                    errorMessage = errorMessage
                )
            )
        }

        return replaceResult
    }

    private fun friendlyCollectionLabel(collectionKey: String): String {
        return when (collectionKey) {
            "cows" -> "cow records"
            "pastures" -> "pasture records"
            "activities" -> "activity history"
            "notes" -> "notes"
            "settings" -> "settings"
            "tagColors" -> "tag colors"
            "activityTypes" -> "activity types"
            "breeds" -> "breed library"
            else -> collectionKey
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                .replace('_', ' ')
                .lowercase(Locale.getDefault())
        }
    }

    private fun capitalizeLabel(label: String): String {
        if (label.isEmpty()) return label
        return label.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
    }

    fun importData(uri: Uri, format: String, conflictResolution: ConflictResolution? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(message = null, error = null, isLoading = true)
                val importer = DataImporter(application, repository)
                
                val result = when (format.uppercase()) {
                    "JSON" -> importer.importFromJson(uri, conflictResolution)
                    "CSV" -> importer.importFromCsv(uri)
                    else -> ImportResult.Error("Unsupported format: $format")
                }
                
                when (result) {
                    is ImportResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = result.message ?: "Successfully imported ${result.itemsImported} items",
                            isLoading = false,
                            isSampleDataInstalled = repository.isSampleDataInstalled()
                        )
                    }
                    is ImportResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is ImportResult.ConflictDetected -> {
                        _uiState.value = _uiState.value.copy(
                            conflictInfo = ConflictInfo(
                                uri = uri,
                                format = format,
                                conflictCount = result.conflictCount,
                                totalRecords = result.totalRecords
                            ),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Import failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun resolveConflict(resolution: ConflictResolution) {
        val conflictInfo = _uiState.value.conflictInfo ?: return
        _uiState.value = _uiState.value.copy(conflictInfo = null)
        importData(conflictInfo.uri, conflictInfo.format, resolution)
    }
    
    fun cancelConflictResolution() {
        _uiState.value = _uiState.value.copy(conflictInfo = null)
    }
}

data class ConflictInfo(
    val uri: Uri,
    val format: String,
    val conflictCount: Int,
    val totalRecords: Int
)

sealed interface ResetCloudDataProgress {
    val completedSteps: Int
    val totalSteps: Int

    data class InProgress(
        val message: String,
        override val completedSteps: Int,
        override val totalSteps: Int
    ) : ResetCloudDataProgress

    data class Success(
        override val completedSteps: Int,
        override val totalSteps: Int,
        val summary: String,
        val finishedAt: Long
    ) : ResetCloudDataProgress

    data class Error(
        override val completedSteps: Int,
        override val totalSteps: Int,
        val errorMessage: String
    ) : ResetCloudDataProgress
}

data class SettingsUiState(
    val tagColors: List<String> = emptyList(),
    val activityTypes: List<String> = emptyList(),
    val defaultCalfPasture: String? = null,
    val isSampleDataInstalled: Boolean = false,
    val identifierMode: AnimalIdentifierMode = AnimalIdentifierMode.BOTH,
    val appVersion: String = "",
    val lastSyncTime: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val pendingExportFormat: String? = null,
    val conflictInfo: ConflictInfo? = null,
    val resetCloudProgress: ResetCloudDataProgress? = null
)
