package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jumblemint.cows.data.backup.BackupManager
import com.jumblemint.cows.data.backup.BackupWorker
import com.jumblemint.cows.data.preferences.BackupPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)
    private val backupPreferences = BackupPreferences(application)
    private val workManager = WorkManager.getInstance(application)

    val autoBackupEnabled = backupPreferences.autoBackupEnabled
    val backupFrequency = backupPreferences.backupFrequency
    val customIntervalHours = backupPreferences.customIntervalHours
    val lastBackupTimestamp = backupPreferences.lastBackupTimestamp
    val googleDriveBackupEnabled = backupPreferences.googleDriveBackupEnabled
    val backupOnEventEnabled = backupPreferences.backupOnEventEnabled
    
    val localBackupEnabled = backupPreferences.localBackupEnabled
    val localBackupUri = backupPreferences.localBackupUri
    val customIntervalValue = backupPreferences.customIntervalValue
    val customIntervalUnit = backupPreferences.customIntervalUnit
    
    val backupHour = backupPreferences.backupHour
    val backupMinute = backupPreferences.backupMinute
    val backupDayOfWeek = backupPreferences.backupDayOfWeek

    private val _driveBackups = MutableStateFlow<List<com.google.api.services.drive.model.File>>(emptyList())
    val driveBackups: StateFlow<List<com.google.api.services.drive.model.File>> = _driveBackups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    fun clearMessage() {
        _message.value = null
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setAutoBackupEnabled(enabled)
            scheduleBackupWorker()
        }
    }

    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            backupPreferences.setBackupFrequency(frequency)
            scheduleBackupWorker()
        }
    }

    fun setCustomInterval(value: Int, unit: String) {
        viewModelScope.launch {
            backupPreferences.setCustomInterval(value, unit)
            scheduleBackupWorker()
        }
    }
    
    fun setBackupTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            backupPreferences.setBackupTime(hour, minute)
            scheduleBackupWorker()
        }
    }
    
    fun setBackupDayOfWeek(day: Int) {
        viewModelScope.launch {
            backupPreferences.setBackupDayOfWeek(day)
            scheduleBackupWorker()
        }
    }
    
    fun setBackupOnEventEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setBackupOnEventEnabled(enabled)
        }
    }

    fun setGoogleDriveBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setGoogleDriveBackupEnabled(enabled)
        }
    }
    
    fun setLocalBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setLocalBackupEnabled(enabled)
        }
    }
    
    fun setLocalBackupUri(uri: Uri?) {
        viewModelScope.launch {
             // Take persistent permission if it's a new URI
             if (uri != null) {
                 try {
                     getApplication<Application>().contentResolver.takePersistableUriPermission(
                         uri,
                         android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                         android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                     )
                 } catch (e: Exception) {
                     // Might fail if not a document URI or already granted? 
                     // Just ignore or log
                     e.printStackTrace()
                 }
                 backupPreferences.setLocalBackupUri(uri.toString())
             } else {
                 backupPreferences.setLocalBackupUri(null)
             }
        }
    }

    fun performManualBackup(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val localEnabled = localBackupEnabled.first()
            val uriString = localBackupUri.first()
            val customUri = if (localEnabled && uriString != null) Uri.parse(uriString) else null
            
            val result = backupManager.createLocalBackup(customUri)
            
            if (result.isSuccess) {
                backupPreferences.updateLastBackupTimestamp(System.currentTimeMillis())
                _message.value = "Local backup created successfully"
                
                if (account != null && googleDriveBackupEnabled.first()) {
                    val driveResult = backupManager.createDriveBackup(account, result.getOrThrow())
                    if (driveResult.isSuccess) {
                         _message.value = "Backup uploaded to Google Drive"
                         fetchDriveBackups(account)
                    } else {
                        _message.value = "Local backup created, but Drive upload failed: ${driveResult.exceptionOrNull()?.message}"
                    }
                }
            } else {
                _message.value = "Backup failed: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun restoreFromLocal(uri: Uri) {
         viewModelScope.launch {
            _isLoading.value = true
            val result = backupManager.restoreFromLocalBackup(uri)
            if (result.isSuccess) {
                 _message.value = "Restore successful. Please restart the app."
            } else {
                 _message.value = "Restore failed: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
         }
    }
    
    fun fetchDriveBackups(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = backupManager.getDriveBackups(account)
            if (result.isSuccess) {
                _driveBackups.value = result.getOrThrow()
            } else {
                _message.value = "Failed to list Drive backups: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun restoreFromDrive(fileId: String, account: GoogleSignInAccount) {
         viewModelScope.launch {
            _isLoading.value = true
            val result = backupManager.restoreFromDriveBackup(fileId, account)
            if (result.isSuccess) {
                 _message.value = "Restore successful. Please restart the app."
            } else {
                 _message.value = "Restore failed: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
         }
    }

    private suspend fun scheduleBackupWorker() {
        val enabled = backupPreferences.autoBackupEnabled.first()
        if (!enabled) {
            workManager.cancelUniqueWork("AutoBackup")
            return
        }

        val frequency = backupPreferences.backupFrequency.first()
        
        var intervalHours = 24L
        var initialDelayMillis = 0L
        
        val now = LocalDateTime.now()
        val targetHour = backupPreferences.backupHour.first()
        val targetMinute = backupPreferences.backupMinute.first()
        var nextRun = now.with(LocalTime.of(targetHour, targetMinute))

        if (frequency == "CUSTOM") {
            intervalHours = backupPreferences.customIntervalHours.first()
            // For custom, we just start roughly now or next hour? 
            // Usually custom intervals are just periodic from "now".
            // We'll leave delay as 0 for custom to start immediately/next period.
        } else if (frequency == "DAILY") {
            intervalHours = 24L
            if (now.isAfter(nextRun)) {
                nextRun = nextRun.plusDays(1)
            }
            initialDelayMillis = Duration.between(now, nextRun).toMillis()
        } else if (frequency == "WEEKLY") {
            intervalHours = 24 * 7L
            val targetDayOfWeek = backupPreferences.backupDayOfWeek.first() // 1 (Mon) - 7 (Sun)
            
            // Adjust to next occurrence of that day
            while (nextRun.dayOfWeek.value != targetDayOfWeek) {
                nextRun = nextRun.plusDays(1)
            }
            
            if (now.isAfter(nextRun)) {
                nextRun = nextRun.plusWeeks(1)
            }
            initialDelayMillis = Duration.between(now, nextRun).toMillis()
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "AutoBackup",
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }
}

class BackupViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
