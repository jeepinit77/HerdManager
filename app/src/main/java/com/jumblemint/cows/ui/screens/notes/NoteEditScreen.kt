package com.jumblemint.cows.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.ui.viewmodel.NotesViewModel
import java.time.LocalDate
import java.time.ZoneId
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.ui.components.FocusAwareLiveSync

@Composable
fun NoteEditScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {},
    viewModel: NotesViewModel = viewModel(),
    modifier: Modifier = Modifier,
    defaultIsTodo: Boolean = false
) {
    val application = LocalContext.current.applicationContext as CattleApplication
    val uiState by viewModel.uiState.collectAsState()
    val note = if (noteId == 0L) null else uiState.allNotes.find { it.id == noteId }

    var title by remember(noteId) { mutableStateOf("") }
    var text by remember(noteId) { mutableStateOf("") }
    var isTodo by remember(noteId, defaultIsTodo) { mutableStateOf(if (noteId == 0L) defaultIsTodo else false) }
    var isCompleted by remember(noteId) { mutableStateOf(false) }
    var dueDate by remember(noteId) { mutableStateOf<LocalDate?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }

    FocusAwareLiveSync(
        orchestrator = application.syncOrchestrator,
        screenKey = "NoteEdit:$noteId",
        intervalMs = 20_000L,
        leadingRun = true
    )

    val originalTitle = remember(note) { note?.title ?: "" }
    val originalText = remember(note) { note?.text ?: "" }
    val originalIsTodo = remember(note) { note?.isTodo ?: defaultIsTodo }
    val originalIsCompleted = remember(note) { note?.isCompleted ?: false }
    val originalDueDate = remember(note) {
        note?.dueDate?.let {
            java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val hasChanges = title != originalTitle || text != originalText || isTodo != originalIsTodo || dueDate != originalDueDate || (if (isTodo) isCompleted else false) != originalIsCompleted

    LaunchedEffect(note, defaultIsTodo) {
        if (note != null) {
            title = note.title
            text = note.text
            isTodo = note.isTodo
            isCompleted = note.isCompleted
            dueDate = note.dueDate?.let { timestamp ->
                java.time.Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        } else {
            isTodo = defaultIsTodo
            isCompleted = false
            dueDate = null
        }
    }

    LaunchedEffect(hasChanges) {
        onUnsavedChangesChanged(hasChanges)
    }

    LaunchedEffect(saveTriggered) {
            if (saveTriggered) {
                if (title.isNotBlank()) {
                    val dueDateMillis = dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    val finalIsCompleted = if (isTodo) isCompleted else false
                    if (note != null) {
                        viewModel.updateNote(note, title, text, isTodo, dueDateMillis, finalIsCompleted)
                    } else {
                        viewModel.addNote(title, text, isTodo, dueDateMillis, finalIsCompleted)
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
            .verticalScroll(rememberScrollState())
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
            singleLine = true,
            colors = defaultOutlinedTextFieldColors()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTodo,
                onCheckedChange = {
                    isTodo = it
                    if (!it) {
                        dueDate = null
                        isCompleted = false
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mark as Todo",
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (isTodo) {
            DatePickerField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = "Due Date (optional)",
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { isCompleted = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) "Marked as done" else "Mark as done",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = { Text("Note (optional)") },
            colors = defaultOutlinedTextFieldColors()
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedDialog = false },
            onSave = {
                showUnsavedDialog = false
                if (title.isNotBlank()) {
                    val dueDateMillis = dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    val finalIsCompleted = if (isTodo) isCompleted else false
                    if (note != null) {
                        viewModel.updateNote(note, title, text, isTodo, dueDateMillis, finalIsCompleted)
                    } else {
                        viewModel.addNote(title, text, isTodo, dueDateMillis, finalIsCompleted)
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