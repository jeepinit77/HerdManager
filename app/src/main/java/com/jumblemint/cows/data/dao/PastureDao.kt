package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.ui.viewmodel.PastureWithCowCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PastureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pasture: Pasture): Long // MODIFIED: Added Long return type

    @Update
    suspend fun update(pasture: Pasture)

    @Delete
    suspend fun delete(pasture: Pasture)

    @Query("SELECT pasture.id AS p_id, pasture.name AS p_name, pasture.description AS p_description, pasture.sizeAcres AS p_sizeAcres, SUM(CASE WHEN cow.pastureId = pasture.id AND cow.status = 'ACTIVE' THEN 1 ELSE 0 END) as cowCount FROM pastures pasture LEFT JOIN cows cow ON pasture.id = cow.pastureId GROUP BY pasture.id ORDER BY p_name ASC")
    fun getAllPasturesWithCowCounts(): Flow<List<PastureWithCowCount>>

    @Query("SELECT COUNT(*) FROM cows WHERE pastureId IS NULL AND status = 'ACTIVE'")
    fun getUnassignedCowCount(): Flow<Int>


    @Query("SELECT * FROM pastures WHERE id = :id")
    fun getPastureById(id: String): Flow<Pasture?>

    @Query("SELECT * FROM pastures ORDER BY name ASC")
    fun getAllPastures(): Flow<List<Pasture>>

    @Query("DELETE FROM pastures")
    suspend fun deleteAllPastures()

}

