package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ActivityDao {
    
    @Query("SELECT * FROM activities WHERE isDeleted = 0 ORDER BY date DESC, id DESC")
    fun getAllActivities(): Flow<List<Activity>>

    @Query("SELECT * FROM activities ORDER BY date DESC, id DESC")
    suspend fun getAllActivitiesForSync(): List<Activity>
    
    @Query("SELECT * FROM activities WHERE cowId = :cowId AND isDeleted = 0 ORDER BY date DESC, id DESC")
    fun getActivitiesForCow(cowId: Long): Flow<List<Activity>>
    
    @Query("SELECT * FROM activities WHERE activityType = :activityType AND isDeleted = 0 ORDER BY date DESC, id DESC")
    fun getActivitiesByType(activityType: ActivityType): Flow<List<Activity>>
    
    @Query("SELECT * FROM activities WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date DESC, id DESC")
    fun getActivitiesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Activity>>
    
    @Insert
    suspend fun insertActivity(activity: Activity): Long
    
    @Insert
    suspend fun insertActivities(activities: List<Activity>)
    
    @Update
    suspend fun updateActivity(activity: Activity)
    
    @Delete
    suspend fun deleteActivity(activity: Activity)

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: Long): Activity?
    
    @Query("SELECT * FROM activities WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getActivityByFirestoreId(firestoreId: String): Activity?
    
    @Query("SELECT * FROM activities WHERE groupId = :groupId AND isDeleted = 0 ORDER BY date DESC, id DESC")
    suspend fun getActivitiesByGroupId(groupId: String): List<Activity>
    
    @Query("SELECT DISTINCT activityType FROM activities WHERE isDeleted = 0 ORDER BY activityType")
    suspend fun getDistinctActivityTypes(): List<ActivityType>

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()
}
