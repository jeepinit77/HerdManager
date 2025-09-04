package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): User?
    
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun getUserByIdFlow(uid: String): Flow<User?>
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("UPDATE users SET lastSyncAt = :syncTime WHERE uid = :uid")
    suspend fun updateLastSync(uid: String, syncTime: Long)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}