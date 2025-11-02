package com.jumblemint.cows.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jumblemint.cows.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<Note>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    suspend fun getAllNotesForSync(): List<Note>
    
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isTodo = 1 AND isCompleted = 0 ORDER BY dueDate ASC, timestamp DESC")
    fun getTodoNotes(): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isTodo = 1 AND isCompleted = 1 ORDER BY timestamp DESC")
    fun getCompletedTodos(): Flow<List<Note>>
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}