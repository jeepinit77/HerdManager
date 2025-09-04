package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Note
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    // Corrected: Single argument for getDatabase and explicit type for database
    private val database: CattleDatabase = CattleDatabase.getDatabase(application)
    // This should now resolve if CattleDatabase has noteDao()
    private val noteDao = database.noteDao() 
    
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    init {
        loadNotes()
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            noteDao.getAllNotes()
                // Corrected: Explicit type for e
                .catch { e: Throwable -> 
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                // Corrected: Explicit type for notes
                .collect { notes: List<Note> -> 
                    _uiState.value = _uiState.value.copy(
                        notes = notes,
                        isLoading = false
                    )
                }
        }
    }
    
    fun addNote(title: String, text: String) {
        viewModelScope.launch {
            try {
                val note = Note(
                    title = title,
                    text = text,
                    timestamp = Date().time
                )
                val noteId = noteDao.insert(note)
                val savedNote = note.copy(id = noteId)
                
                // Sync the note immediately if user is signed in
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { currentUser ->
                    if (!currentUser.isLocalUser) {
                        application.syncService.syncItemImmediately(currentUser.uid, savedNote)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                noteDao.delete(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            try {
                // Re-insert the same note (NoteDao.insert uses REPLACE)
                noteDao.insert(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateNote(note: Note, newTitle: String, newText: String) {
        viewModelScope.launch {
            try {
                val updatedNote = note.copy(
                    title = newTitle,
                    text = newText,
                    timestamp = Date().time // Update timestamp when edited
                )
                noteDao.insert(updatedNote) // Using insert with REPLACE strategy
                
                // Sync the updated note immediately if user is signed in
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { currentUser ->
                    if (!currentUser.isLocalUser) {
                        application.syncService.syncItemImmediately(currentUser.uid, updatedNote)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
