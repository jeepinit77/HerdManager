package com.jumblemint.cows.data.dao

import androidx.room.*
import com.jumblemint.cows.data.model.TagColor
import kotlinx.coroutines.flow.Flow

@Dao
interface TagColorDao {
    @Query("SELECT * FROM tag_colors WHERE isActive = 1 AND isDeleted = 0 ORDER BY name ASC")
    fun getAllActiveTagColors(): Flow<List<TagColor>>

    @Query("SELECT * FROM tag_colors WHERE isDeleted = 0 ORDER BY isDefault DESC, name ASC")
    suspend fun getAllSync(): List<TagColor>

    @Query("SELECT * FROM tag_colors WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllTagColors(): Flow<List<TagColor>>

    @Query("SELECT * FROM tag_colors")
    suspend fun getAllTagColorsSync(): List<TagColor>

    @Query("SELECT * FROM tag_colors WHERE id = :id")
    suspend fun getTagColorById(id: String): TagColor?

    @Query("SELECT * FROM tag_colors WHERE name = :name AND isActive = 1 AND isDeleted = 0")
    suspend fun getTagColorByName(name: String): TagColor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagColor(tagColor: TagColor): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagColors(tagColors: List<TagColor>)

    @Upsert
    suspend fun upsert(tagColor: TagColor)

    @Upsert
    suspend fun upsertAll(tagColors: List<TagColor>)

    @Update
    suspend fun updateTagColor(tagColor: TagColor)

    @Delete
    suspend fun deleteTagColor(tagColor: TagColor)

    @Query("UPDATE tag_colors SET isActive = :isActive WHERE id = :id")
    suspend fun updateTagColorActiveStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM tag_colors WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM tag_colors")
    suspend fun count(): Int

    @Query("DELETE FROM tag_colors")
    suspend fun clearAllTagColors()
}
