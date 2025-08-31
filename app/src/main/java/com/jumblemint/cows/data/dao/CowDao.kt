package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import kotlinx.coroutines.flow.Flow

@Dao
interface CowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCow(cow: Cow): Long

    @Update
    suspend fun updateCow(cow: Cow)

    @Delete
    suspend fun deleteCow(cow: Cow)

    @Query("SELECT * FROM cows ORDER BY name ASC")
    fun getAllCows(): Flow<List<Cow>>

    @Query("SELECT * FROM cows WHERE id = :id")
    suspend fun getCowById(id: Long): Cow?

    @Query("SELECT * FROM cows WHERE tagNumber = :tagNumber LIMIT 1")
    suspend fun getCowByTagNumber(tagNumber: String): Cow?

    @Query("SELECT * FROM cows WHERE status = :status ORDER BY name ASC")
    fun getCowsByStatus(status: Status): Flow<List<Cow>>

    // MODIFIED: pastureId parameter changed from Long to String
    @Query("SELECT * FROM cows WHERE pastureId = :pastureId ORDER BY name ASC")
    fun getCowsByPasture(pastureId: String): Flow<List<Cow>>

    @Query("SELECT * FROM cows WHERE gender = 'FEMALE' AND status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveFemales(): Flow<List<Cow>>

    @Query("SELECT * FROM cows WHERE gender = 'MALE' AND status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveMales(): Flow<List<Cow>>
    
    @Query("SELECT * FROM cows WHERE motherId = :motherId ORDER BY birthDate DESC")
    fun getCalvesByMother(motherId: Long): Flow<List<Cow>>

    @Query("SELECT * FROM cows WHERE fatherId = :fatherId ORDER BY birthDate DESC")
    fun getCalvesByFather(fatherId: Long): Flow<List<Cow>>

    // MODIFIED: pastureId parameter changed from Long? to String?
    @Query("UPDATE cows SET pastureId = :pastureId WHERE id = :cowId")
    suspend fun updateCowPasture(cowId: Long, pastureId: String?)

    @Query("UPDATE cows SET classification = :classification WHERE id = :cowId")
    suspend fun updateCowClassification(cowId: Long, classification: String)

    @Query("UPDATE cows SET isWatched = :isWatched WHERE id = :cowId")
    suspend fun updateCowWatchStatus(cowId: Long, isWatched: Boolean)

    @Query("SELECT * FROM cows WHERE isWatched = 1 ORDER BY name ASC")
    fun getWatchedCows(): Flow<List<Cow>>

    // Add other queries as needed, e.g., for search, filtering by multiple criteria
}
