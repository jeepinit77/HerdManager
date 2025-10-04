package com.jumblemint.cows.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.ui.viewmodel.NotesViewModel
import com.jumblemint.cows.ui.components.UnsavedChangesDialog

@Composable
fun NoteDetailScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {},
    viewModel: NotesViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val note = if (noteId == 0L) null else uiState.notes.find { it.id == noteId }
    
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }

    val originalTitle = remember(note) { note?.title ?: "" }
    val originalText = remember(note) { note?.text ?: "" }
    val hasChanges = title != originalTitle || text != originalText

    LaunchedEffect(note) {
        note?.let {
            title = it.title
            text = it.text
        }
    }

    LaunchedEffect(hasChanges) {
        onUnsavedChangesChanged(hasChanges)
    }

    LaunchedEffect(saveTriggered) {
        if (saveTriggered) {
            if (title.isNotBlank()) {
                if (note != null) {
                    viewModel.updateNote(note, title, text)
                } else {
                    viewModel.addNote(title, text)
                }
                onNavigateBack()
            } else {
                titleError = "Title is required"
            }
            onSaveHandled()
        }
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(backPressed) {
        if (backPressed) {
            if (hasChanges) {
                showUnsavedDialog = true
            } else {
                onNavigateBack()
            }
            onBackHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                titleError = if (it.isBlank()) "Title is required" else null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title*") },
            isError = titleError != null,
            supportingText = titleError?.let { { Text(it) } },
            singleLine = true
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Note (optional)") },
            minLines = 5
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedDialog = false },
            onSave = {
                showUnsavedDialog = false
                if (title.isNotBlank()) {
                    if (note != null) {
                        viewModel.updateNote(note, title, text)
                    } else {
                        viewModel.addNote(title, text)
                    }
                    onNavigateBack()
                } else {
                    titleError = "Title is required"
                }
            },
            onDiscard = {
                showUnsavedDialog = false
                onNavigateBack()
            }
        )
    }
}