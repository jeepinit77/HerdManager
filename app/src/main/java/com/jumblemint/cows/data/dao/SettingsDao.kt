package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<Settings>>
    
    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSettingByKey(key: String): Settings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: Settings)
    
    @Delete
    suspend fun deleteSetting(setting: Settings)
}