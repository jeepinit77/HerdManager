package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Herd
import kotlinx.coroutines.flow.Flow

@Dao
interface HerdDao {
    
    @Query("SELECT * FROM herds WHERE id = :id LIMIT 1")
    suspend fun getHerdById(id: String): Herd?
    
    @Query("SELECT * FROM herds WHERE id = :id LIMIT 1")
    fun getHerdByIdFlow(id: String): Flow<Herd?>
    
    @Query("SELECT * FROM herds WHERE isActive = 1")
    fun getAllActiveHerds(): Flow<List<Herd>>
    
    @Query("SELECT * FROM herds WHERE ownerId = :ownerId AND isActive = 1")
    fun getHerdsByOwner(ownerId: String): Flow<List<Herd>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHerd(herd: Herd)
    
    @Update
    suspend fun updateHerd(herd: Herd)
    
    @Delete
    suspend fun deleteHerd(herd: Herd)
    
    @Query("UPDATE herds SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteHerd(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM herds")
    suspend fun deleteAllHerds()
}