package com.jumblemint.cows.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_preferences")

class BackupPreferences(private val context: Context) {

    companion object {
        val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val KEY_BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency") // DAILY, WEEKLY, CUSTOM
        val KEY_CUSTOM_INTERVAL_HOURS = longPreferencesKey("custom_interval_hours") // Kept for worker compatibility/caching
        val KEY_LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        
        // Backup Location
        val KEY_LOCAL_BACKUP_ENABLED = booleanPreferencesKey("local_backup_enabled")
        val KEY_LOCAL_BACKUP_URI = stringPreferencesKey("local_backup_uri")
        val KEY_GOOGLE_DRIVE_BACKUP_ENABLED = booleanPreferencesKey("google_drive_backup_enabled")
        
        // Triggers
        val KEY_BACKUP_ON_EVENT_ENABLED = booleanPreferencesKey("backup_on_event_enabled")
        
        // Custom Frequency UI State
        val KEY_CUSTOM_INTERVAL_VALUE = intPreferencesKey("custom_interval_value")
        val KEY_CUSTOM_INTERVAL_UNIT = stringPreferencesKey("custom_interval_unit") // HOURS, DAYS, WEEKS, MONTHS
        
        // Detailed Schedule
        val KEY_BACKUP_HOUR = intPreferencesKey("backup_hour") // 0-23
        val KEY_BACKUP_MINUTE = intPreferencesKey("backup_minute") // 0-59
        val KEY_BACKUP_DAY_OF_WEEK = intPreferencesKey("backup_day_of_week") // 1 (Mon) - 7 (Sun)
    }

    val autoBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_AUTO_BACKUP_ENABLED] ?: false }

    val backupFrequency: Flow<String> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_BACKUP_FREQUENCY] ?: "DAILY" }
        
    val customIntervalHours: Flow<Long> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_CUSTOM_INTERVAL_HOURS] ?: 24L }

    val lastBackupTimestamp: Flow<Long> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_LAST_BACKUP_TIMESTAMP] ?: 0L }

    val localBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_LOCAL_BACKUP_ENABLED] ?: true } // Default to true

    val localBackupUri: Flow<String?> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_LOCAL_BACKUP_URI] }

    val googleDriveBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_GOOGLE_DRIVE_BACKUP_ENABLED] ?: false }

    val backupOnEventEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_BACKUP_ON_EVENT_ENABLED] ?: false }

    val customIntervalValue: Flow<Int> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_CUSTOM_INTERVAL_VALUE] ?: 1 }
        
    val customIntervalUnit: Flow<String> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_CUSTOM_INTERVAL_UNIT] ?: "DAYS" }
        
    val backupHour: Flow<Int> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_BACKUP_HOUR] ?: 2 } // Default 2 AM
        
    val backupMinute: Flow<Int> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_BACKUP_MINUTE] ?: 0 } // Default 00
        
    val backupDayOfWeek: Flow<Int> = context.backupDataStore.data
        .map { preferences -> preferences[KEY_BACKUP_DAY_OF_WEEK] ?: 1 } // Default Monday (1)

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setBackupFrequency(frequency: String) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun setCustomIntervalHours(hours: Long) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_CUSTOM_INTERVAL_HOURS] = hours
        }
    }
    
    suspend fun setCustomInterval(value: Int, unit: String) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_CUSTOM_INTERVAL_VALUE] = value
            preferences[KEY_CUSTOM_INTERVAL_UNIT] = unit
            
            // Calculate hours for worker usage
            val hours = when (unit) {
                "HOURS" -> value.toLong()
                "DAYS" -> value * 24L
                "WEEKS" -> value * 24L * 7L
                "MONTHS" -> value * 24L * 30L // Approx
                else -> 24L
            }
            preferences[KEY_CUSTOM_INTERVAL_HOURS] = hours.coerceAtLeast(1L) // Minimum 1 hour
        }
    }
    
    suspend fun setBackupTime(hour: Int, minute: Int) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_BACKUP_HOUR] = hour
            preferences[KEY_BACKUP_MINUTE] = minute
        }
    }
    
    suspend fun setBackupDayOfWeek(day: Int) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_BACKUP_DAY_OF_WEEK] = day
        }
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }
    
    suspend fun setLocalBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_LOCAL_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun setLocalBackupUri(uri: String?) {
        context.backupDataStore.edit { preferences ->
            if (uri != null) {
                preferences[KEY_LOCAL_BACKUP_URI] = uri
            } else {
                preferences.remove(KEY_LOCAL_BACKUP_URI)
            }
        }
    }

    suspend fun setGoogleDriveBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_GOOGLE_DRIVE_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun setBackupOnEventEnabled(enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[KEY_BACKUP_ON_EVENT_ENABLED] = enabled
        }
    }
}
