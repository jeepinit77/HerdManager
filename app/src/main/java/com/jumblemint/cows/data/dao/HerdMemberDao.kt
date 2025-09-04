package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.HerdMember
import com.jumblemint.cows.data.model.HerdRole
import kotlinx.coroutines.flow.Flow

@Dao
interface HerdMemberDao {
    
    @Query("SELECT * FROM herd_members WHERE herdId = :herdId AND isActive = 1")
    fun getMembersByHerd(herdId: String): Flow<List<HerdMember>>
    
    @Query("SELECT * FROM herd_members WHERE userId = :userId AND isActive = 1")
    fun getHerdsByUser(userId: String): Flow<List<HerdMember>>
    
    @Query("SELECT * FROM herd_members WHERE herdId = :herdId AND userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getMembership(herdId: String, userId: String): HerdMember?
    
    @Query("SELECT * FROM herd_members WHERE herdId = :herdId AND role = :role AND isActive = 1")
    fun getMembersByRole(herdId: String, role: HerdRole): Flow<List<HerdMember>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: HerdMember)
    
    @Update
    suspend fun updateMember(member: HerdMember)
    
    @Delete
    suspend fun deleteMember(member: HerdMember)
    
    @Query("UPDATE herd_members SET isActive = 0 WHERE herdId = :herdId AND userId = :userId")
    suspend fun removeMember(herdId: String, userId: String)
    
    @Query("DELETE FROM herd_members")
    suspend fun deleteAllMembers()
}