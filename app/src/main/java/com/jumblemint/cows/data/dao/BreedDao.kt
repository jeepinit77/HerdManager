package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.Breed
import kotlinx.coroutines.flow.Flow

@Dao
interface BreedDao {
    @Query("SELECT * FROM breeds WHERE isActive = 1 AND isDeleted = 0 ORDER BY name ASC")
    fun getAllBreeds(): Flow<List<Breed>>

    @Query("SELECT * FROM breeds WHERE id = :id")
    suspend fun getBreedById(id: String): Breed?

    @Query("SELECT * FROM breeds")
    suspend fun getAllBreedsIncludingDeleted(): List<Breed>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreed(breed: Breed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreeds(breeds: List<Breed>)

    @Update
    suspend fun updateBreed(breed: Breed)

    @Delete
    suspend fun deleteBreed(breed: Breed)

    @Query("DELETE FROM breeds WHERE isDefault = 0")
    suspend fun deleteCustomBreeds()

    @Query("DELETE FROM breeds")
    suspend fun deleteAllBreeds()
}