package com.jumblemint.cows.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.model.Note
import com.jumblemint.cows.ui.viewmodel.NotesViewModel
// import com.jumblemint.cows.ui.components.SwipeToDeleteContainer // Import removed
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var viewingNote by remember { mutableStateOf<Note?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    // snackbarHostState and scope are used by NoteCard's onDelete
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    onNavigateBack?.let { callback ->
                        IconButton(onClick = callback) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // Added SnackbarHost
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No notes yet")
            }
        } else {
            // snackbarHostState and scope are defined above and used by NoteCard
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.notes, key = { it.id }) { note ->
                    // SwipeToDeleteContainer has been removed.
                    // NoteCard is now a direct child.
                    NoteCard(
                        note = note,
                        dateFormat = dateFormat,
                        onView = {
                            viewingNote = note
                            showFullScreen = true
                        },
                        onEdit = {
                            editingNote = note
                            showEdit = true
                        },
                        onDelete = {
                            // This onDelete is part of NoteCard and uses
                            // snackbarHostState and scope defined in NotesScreen
                            scope.launch {
                                viewModel.deleteNote(note)
                                val res = snackbarHostState.showSnackbar(
                                    message = "Note deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Long
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreNote(note)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Error message handling - This was a Snackbar, changed to use the Scaffold's snackbarHostState
        uiState.error?.let { error ->
            // LaunchedEffect to show snackbar when error changes
            LaunchedEffect(error) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = error,
                        duration = SnackbarDuration.Short
                    )
                    // Optionally, clear the error in ViewModel after showing
                    // viewModel.clearError() 
                }
            }
        }
    }

    if (showAdd) {
        AddNoteDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, text ->
                viewModel.addNote(title, text)
                showAdd = false
            }
        )
    }
    
    if (showEdit && editingNote != null) {
        EditNoteDialog(
            note = editingNote!!,
            onDismiss = { 
                showEdit = false
                editingNote = null
            },
            onConfirm = { title, text ->
                viewModel.updateNote(editingNote!!, title, text)
                showEdit = false
                editingNote = null
            }
        )
    }
    
    if (showFullScreen && viewingNote != null) {
        FullScreenNoteDialog(
            note = viewingNote!!,
            dateFormat = dateFormat,
            onDismiss = {
                showFullScreen = false
                viewingNote = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: Note,
    dateFormat: SimpleDateFormat,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onView,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) { // Added weight to Column
                    if (note.title.isNotEmpty()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(note.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis // Consider maxLines if text can be very long
            )
        }
    }
}

@Composable
private fun AddNoteDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleError = if (it.isBlank()) "Title is required" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title*") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text("Note (optional)") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, text)
                    } else {
                        titleError = "Title is required"
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditNoteDialog(
    note: Note,
    onDismiss: () -> Unit, 
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var text by remember { mutableStateOf(note.text) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleError = if (it.isBlank()) "Title is required" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title*") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text("Note (optional)") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, text)
                    } else {
                        titleError = "Title is required"
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenNoteDialog(
    note: Note,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar
                TopAppBar(
                    title = { Text("Note") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .transformable(state = transformableState)
                ) {
                    // Title
                    if (note.title.isNotEmpty()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * scale).sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Date
                    Text(
                        text = dateFormat.format(Date(note.timestamp)),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * scale).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Note text
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * scale).sp
                        ),
                        lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight.value * scale).sp
                    )
                }
            }
        }
    }
}
