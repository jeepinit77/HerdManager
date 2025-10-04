package com.jumblemint.cows.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.ui.viewmodel.NotesViewModel
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.ui.components.DatePickerField
import java.time.LocalDate
import java.time.ZoneId

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
    var isTodo by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }

    val originalTitle = remember(note) { note?.title ?: "" }
    val originalText = remember(note) { note?.text ?: "" }
    val originalIsTodo = remember(note) { note?.isTodo ?: false }
    val originalDueDate = remember(note) { 
        note?.dueDate?.let { 
            java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() 
        }
    }
    val hasChanges = title != originalTitle || text != originalText || isTodo != originalIsTodo || dueDate != originalDueDate

    LaunchedEffect(note) {
        note?.let {
            title = it.title
            text = it.text
            isTodo = it.isTodo
            dueDate = it.dueDate?.let { timestamp ->
                java.time.Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
    }

    LaunchedEffect(hasChanges) {
        onUnsavedChangesChanged(hasChanges)
    }

    LaunchedEffect(saveTriggered) {
        if (saveTriggered) {
            if (title.isNotBlank()) {
                val dueDateMillis = dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                if (note != null) {
                    viewModel.updateNote(note, title, text, isTodo, dueDateMillis)
                } else {
                    viewModel.addNote(title, text, isTodo, dueDateMillis)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTodo,
                onCheckedChange = { 
                    isTodo = it
                    if (!it) dueDate = null
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mark as Todo")
        }
        
        if (isTodo) {
            DatePickerField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = "Due Date (optional)",
                modifier = Modifier.fillMaxWidth()
            )
        }

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
                    val dueDateMillis = dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    if (note != null) {
                        viewModel.updateNote(note, title, text, isTodo, dueDateMillis)
                    } else {
                        viewModel.addNote(title, text, isTodo, dueDateMillis)
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