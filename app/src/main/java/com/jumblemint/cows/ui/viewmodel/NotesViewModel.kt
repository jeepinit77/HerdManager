package com.jumblemint.cows.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Note
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val database: CattleDatabase = CattleDatabase.getDatabase(application)
    private val noteDao = database.noteDao()
    
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    init {
        loadNotes()
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            noteDao.getAllNotes()
                .catch { e: Throwable ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { notes: List<Note> ->
                    _uiState.update { current ->
                        val updated = current.copy(
                            allNotes = notes,
                            isLoading = false
                        )
                        updated.copy(
                            filteredNotes = applyFilters(
                                notes = notes,
                                searchQuery = updated.searchQuery,
                                startDateMillis = updated.startDateMillis,
                                endDateMillis = updated.endDateMillis,
                                todoFilter = updated.todoFilter,
                                todoCompletionFilter = updated.todoCompletionFilter
                            )
                        )
                    }
                }
        }
    }

    fun addNote(
        title: String,
        text: String,
        isTodo: Boolean = false,
        dueDate: Long? = null,
        isCompleted: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val note = Note(
                    title = title,
                    text = text,
                    timestamp = Date().time,
                    isTodo = isTodo,
                    dueDate = dueDate,
                    isCompleted = if (isTodo) isCompleted else false
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
                // Soft delete: mark as deleted instead of physically removing
                val deletedNote = note.copy(
                    isDeleted = true,
                    timestamp = System.currentTimeMillis() // Update timestamp for sync
                )
                noteDao.insert(deletedNote) // Using insert with REPLACE strategy
                
                // Sync the deletion immediately if user is signed in
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { currentUser ->
                    if (!currentUser.isLocalUser) {
                        application.syncService.syncItemImmediately(currentUser.uid, deletedNote)
                    }
                }
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
    
    fun updateNote(
        note: Note,
        newTitle: String,
        newText: String,
        isTodo: Boolean = false,
        dueDate: Long? = null,
        isCompleted: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val updatedNote = note.copy(
                    title = newTitle,
                    text = newText,
                    timestamp = Date().time, // Update timestamp when edited
                    isTodo = isTodo,
                    dueDate = dueDate,
                    isCompleted = if (isTodo) isCompleted else false
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
    
    fun markTodoComplete(note: Note) {
        viewModelScope.launch {
            try {
                val completedNote = note.copy(
                    isCompleted = true,
                    timestamp = Date().time
                )
                noteDao.insert(completedNote)
                
                // Sync the updated note immediately if user is signed in
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { currentUser ->
                    if (!currentUser.isLocalUser) {
                        application.syncService.syncItemImmediately(currentUser.uid, completedNote)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun getTodoNotes(): Flow<List<Note>> = noteDao.getTodoNotes()
    
    fun getCompletedTodos(): Flow<List<Note>> = noteDao.getCompletedTodos()
    
    fun markTodoIncomplete(note: Note) {
        viewModelScope.launch {
            try {
                val incompleteNote = note.copy(
                    isCompleted = false,
                    timestamp = Date().time
                )
                noteDao.insert(incompleteNote)

                // Sync the updated note immediately if user is signed in
                val application = getApplication<CattleApplication>()
                application.authService.currentUser.first()?.let { currentUser ->
                    if (!currentUser.isLocalUser) {
                        application.syncService.syncItemImmediately(currentUser.uid, incompleteNote)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            val trimmed = query.trim()
            current.copy(
                searchQuery = trimmed,
                filteredNotes = applyFilters(
                    notes = current.allNotes,
                    searchQuery = trimmed,
                    startDateMillis = current.startDateMillis,
                    endDateMillis = current.endDateMillis,
                    todoFilter = current.todoFilter,
                    todoCompletionFilter = current.todoCompletionFilter
                )
            )
        }
    }

    fun updateStartDate(millis: Long?) {
        _uiState.update { current ->
            val adjustedEnd = current.endDateMillis
            val newStart = millis
            val finalEnd = if (newStart != null && adjustedEnd != null && newStart > adjustedEnd) newStart else adjustedEnd
            current.copy(
                startDateMillis = newStart,
                endDateMillis = finalEnd,
                filteredNotes = applyFilters(
                    notes = current.allNotes,
                    searchQuery = current.searchQuery,
                    startDateMillis = newStart,
                    endDateMillis = finalEnd,
                    todoFilter = current.todoFilter,
                    todoCompletionFilter = current.todoCompletionFilter
                )
            )
        }
    }

    fun updateEndDate(millis: Long?) {
        _uiState.update { current ->
            val newEnd = millis
            val finalStart = if (newEnd != null && current.startDateMillis != null && newEnd < current.startDateMillis) {
                newEnd
            } else {
                current.startDateMillis
            }
            current.copy(
                startDateMillis = finalStart,
                endDateMillis = newEnd,
                filteredNotes = applyFilters(
                    notes = current.allNotes,
                    searchQuery = current.searchQuery,
                    startDateMillis = finalStart,
                    endDateMillis = newEnd,
                    todoFilter = current.todoFilter,
                    todoCompletionFilter = current.todoCompletionFilter
                )
            )
        }
    }

    fun updateTodoFilter(filter: TodoStatusFilter) {
        _uiState.update { current ->
            val completionFilter = if (filter == TodoStatusFilter.NON_TODO) {
                TodoCompletionFilter.ALL
            } else {
                current.todoCompletionFilter
            }
            current.copy(
                todoFilter = filter,
                todoCompletionFilter = completionFilter,
                filteredNotes = applyFilters(
                    notes = current.allNotes,
                    searchQuery = current.searchQuery,
                    startDateMillis = current.startDateMillis,
                    endDateMillis = current.endDateMillis,
                    todoFilter = filter,
                    todoCompletionFilter = completionFilter
                )
            )
        }
    }

    fun updateTodoCompletionFilter(filter: TodoCompletionFilter) {
        _uiState.update { current ->
            val appliedFilter = if (current.todoFilter == TodoStatusFilter.NON_TODO) {
                TodoCompletionFilter.ALL
            } else {
                filter
            }
            current.copy(
                todoCompletionFilter = appliedFilter,
                filteredNotes = applyFilters(
                    notes = current.allNotes,
                    searchQuery = current.searchQuery,
                    startDateMillis = current.startDateMillis,
                    endDateMillis = current.endDateMillis,
                    todoFilter = current.todoFilter,
                    todoCompletionFilter = appliedFilter
                )
            )
        }
    }

    fun clearAllFilters() {
        _uiState.update { current ->
            current.copy(
                startDateMillis = null,
                endDateMillis = null,
                todoFilter = TodoStatusFilter.ALL,
                todoCompletionFilter = TodoCompletionFilter.ALL,
                filteredNotes = applyFilters(
                    notes = current.allNotes,
                    searchQuery = current.searchQuery,
                    startDateMillis = null,
                    endDateMillis = null,
                    todoFilter = TodoStatusFilter.ALL,
                    todoCompletionFilter = TodoCompletionFilter.ALL
                )
            )
        }
    }

    private fun applyFilters(
        notes: List<Note>,
        searchQuery: String,
        startDateMillis: Long?,
        endDateMillis: Long?,
        todoFilter: TodoStatusFilter,
        todoCompletionFilter: TodoCompletionFilter
    ): List<Note> {
        if (notes.isEmpty()) return emptyList()

        val query = searchQuery.lowercase(Locale.getDefault())
        val timestampFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        return notes.asSequence()
            .filter { note ->
                val matchesSearch = if (query.isBlank()) {
                    true
                } else {
                    val titleMatch = note.title.lowercase(Locale.getDefault()).contains(query)
                    val textMatch = note.text.lowercase(Locale.getDefault()).contains(query)
                    val timestampMatch = timestampFormatter
                        .format(Date(note.timestamp))
                        .lowercase(Locale.getDefault())
                        .contains(query)
                    val dueDateMatch = note.dueDate?.let {
                        dateFormatter
                            .format(Date(it))
                            .lowercase(Locale.getDefault())
                            .contains(query)
                    } ?: false
                    titleMatch || textMatch || timestampMatch || dueDateMatch
                }

                val matchesStart = startDateMillis?.let { note.timestamp >= it } ?: true
                val matchesEnd = endDateMillis?.let { note.timestamp <= it } ?: true
                val matchesTodo = when (todoFilter) {
                    TodoStatusFilter.ALL -> true
                    TodoStatusFilter.TODO_ONLY -> note.isTodo
                    TodoStatusFilter.NON_TODO -> !note.isTodo
                }

                val matchesTodoCompletion = if (!note.isTodo || todoFilter == TodoStatusFilter.NON_TODO) {
                    true
                } else {
                    when (todoCompletionFilter) {
                        TodoCompletionFilter.ALL -> true
                        TodoCompletionFilter.ACTIVE -> !note.isCompleted
                        TodoCompletionFilter.DONE -> note.isCompleted
                    }
                }

                matchesSearch && matchesStart && matchesEnd && matchesTodo && matchesTodoCompletion
            }
            .sortedByDescending { it.timestamp }
            .toList()
    }
}

data class NotesUiState(
    val allNotes: List<Note> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val todoFilter: TodoStatusFilter = TodoStatusFilter.ALL,
    val todoCompletionFilter: TodoCompletionFilter = TodoCompletionFilter.ALL
)

enum class TodoStatusFilter(val label: String) {
    ALL("All"),
    TODO_ONLY("Todos"),
    NON_TODO("Notes")
}

enum class TodoCompletionFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    DONE("Done")
}
