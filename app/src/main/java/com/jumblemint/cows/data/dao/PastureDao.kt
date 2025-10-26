package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.ui.viewmodel.PastureWithCowCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PastureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pasture: Pasture): Long

    @Update
    suspend fun update(pasture: Pasture)

    @Delete
    suspend fun delete(pasture: Pasture)

    @Query("""
        SELECT
            pasture.id AS p_id,
            pasture.name AS p_name,
            pasture.description AS p_description,
            pasture.sizeAcres AS p_sizeAcres,
            pasture.herdId AS p_herdId,
            pasture.firestoreId AS p_firestoreId,
            pasture.lastSyncAt AS p_lastSyncAt,
            pasture.isDeleted AS p_isDeleted,
            pasture.createdBy AS p_createdBy,
            pasture.updatedBy AS p_updatedBy,
            SUM(CASE WHEN cow.pastureId = pasture.id AND cow.status = 'ACTIVE' THEN 1 ELSE 0 END) as cowCount
        FROM pastures AS pasture
        LEFT JOIN cows AS cow ON pasture.id = cow.pastureId
        WHERE pasture.isDeleted = 0
        GROUP BY
            pasture.id,
            pasture.name,
            pasture.description,
            pasture.sizeAcres,
            pasture.herdId,
            pasture.firestoreId,
            pasture.lastSyncAt,
            pasture.isDeleted,
            pasture.createdBy,
            pasture.updatedBy
        ORDER BY p_name ASC
    """)
    fun getAllPasturesWithCowCounts(): Flow<List<PastureWithCowCount>>

    @Query(
        "SELECT COUNT(*) FROM cows " +
            "WHERE (pastureId IS NULL OR pastureId = '') " +
            "AND status = 'ACTIVE' " +
            "AND isDeleted = 0"
    )
    fun getUnassignedCowCount(): Flow<Int>


    @Query("SELECT * FROM pastures WHERE id = :id AND isDeleted = 0")
    fun getPastureById(id: String): Flow<Pasture?>

    @Query("SELECT * FROM pastures WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllPastures(): Flow<List<Pasture>>

    @Query("DELETE FROM pastures")
    suspend fun deleteAllPastures()

}

