package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.ActivityTypeConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTypeConfigDao {
    @Query("SELECT * FROM activity_type_configs WHERE isActive = 1 ORDER BY isDefault DESC, displayName ASC")
    fun getAllActiveActivityTypes(): Flow<List<ActivityTypeConfig>>

    @Query("SELECT * FROM activity_type_configs ORDER BY isDefault DESC, displayName ASC")
    fun getAllActivityTypes(): Flow<List<ActivityTypeConfig>>

    @Query("SELECT * FROM activity_type_configs")
    suspend fun getAllActivityTypesSync(): List<ActivityTypeConfig>

    @Query("SELECT * FROM activity_type_configs WHERE id = :id")
    suspend fun getActivityTypeById(id: String): ActivityTypeConfig?

    @Query("SELECT * FROM activity_type_configs WHERE name = :name AND isActive = 1")
    suspend fun getActivityTypeByName(name: String): ActivityTypeConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityType(activityType: ActivityTypeConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityTypes(activityTypes: List<ActivityTypeConfig>)

    @Upsert
    suspend fun upsert(activityType: ActivityTypeConfig)

    @Upsert
    suspend fun upsertAll(activityTypes: List<ActivityTypeConfig>)

    @Update
    suspend fun updateActivityType(activityType: ActivityTypeConfig)

    @Delete
    suspend fun deleteActivityType(activityType: ActivityTypeConfig)

    @Query("UPDATE activity_type_configs SET isActive = :isActive WHERE id = :id")
    suspend fun updateActivityTypeActiveStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM activity_type_configs")
    suspend fun deleteAllActivityTypes()

    @Query("SELECT COUNT(*) FROM activity_type_configs")
    suspend fun count(): Int
}