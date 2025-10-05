package com.jumblemint.cows.ui.screens.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.model.Note
import com.jumblemint.cows.ui.viewmodel.NotesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel()
) {
    val todoNotes by viewModel.getTodoNotes().collectAsState(initial = emptyList())
    val completedNotes by viewModel.getCompletedTodos().collectAsState(initial = emptyList())
    
    var showCompleted by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf(TodoFilter.ALL) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyCompleted by remember { mutableStateOf<Note?>(null) }
    
    LaunchedEffect(recentlyCompleted) {
        recentlyCompleted?.let { note ->
            val result = snackbarHostState.showSnackbar(
                message = "Todo marked as complete",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.markTodoIncomplete(note)
            }
            recentlyCompleted = null
        }
    }
    
    val displayNotes = if (showCompleted) {
        completedNotes
    } else {
        when (filterType) {
            TodoFilter.ALL -> todoNotes
            TodoFilter.OVERDUE -> todoNotes.filter { it.isOverdue() }
            TodoFilter.TODAY -> todoNotes.filter { it.isDueToday() }
            TodoFilter.UPCOMING -> todoNotes.filter { it.isDueUpcoming() }
            TodoFilter.NO_DATE -> todoNotes.filter { it.dueDate == null }
        }.sortedWith(compareBy<Note> { !it.isOverdue() }.thenBy { it.dueDate ?: Long.MAX_VALUE })
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { showCompleted = false },
                        selected = !showCompleted,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Active")
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { showCompleted = true },
                        selected = showCompleted,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Completed")
                    }
                }
                
                if (!showCompleted) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TodoFilter.values().forEach { filter ->
                            FilterChip(
                                onClick = { filterType = filter },
                                label = { Text(filter.label, maxLines = 1) },
                                selected = filterType == filter,
                                modifier = Modifier.height(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayNotes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (showCompleted) "No completed todos" else "No todo items",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(displayNotes) { note ->
                        TodoItem(
                            note = note,
                            isCompleted = showCompleted,
                            onMarkComplete = {
                                if (showCompleted) {
                                    viewModel.markTodoIncomplete(note)
                                } else {
                                    viewModel.markTodoComplete(note)
                                    recentlyCompleted = note
                                }
                            },
                            onClick = { onNavigateToDetail(note.id) }
                        )
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(
    note: Note,
    isCompleted: Boolean = false,
    onMarkComplete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (note.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                    )
                }
                note.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    val localDate = Instant.ofEpochMilli(dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    val isOverdue = localDate.isBefore(LocalDate.now()) && !isCompleted
                    val isDueToday = localDate.isEqual(LocalDate.now())
                    Text(
                        text = "Due: ${localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isOverdue -> MaterialTheme.colorScheme.error
                            isDueToday -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = if (isOverdue || isDueToday) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            
            IconButton(onClick = onMarkComplete) {
                Icon(
                    if (isCompleted) Icons.Default.Undo else Icons.Default.CheckCircle,
                    contentDescription = if (isCompleted) "Mark as incomplete" else "Mark as complete",
                    tint = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

enum class TodoFilter(val label: String) {
    ALL("All"),
    OVERDUE("Past Due"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    NO_DATE("No Date")
}

private fun Note.isOverdue(): Boolean {
    return dueDate?.let { 
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now())
    } ?: false
}

private fun Note.isDueToday(): Boolean {
    return dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().isEqual(LocalDate.now())
    } ?: false
}

private fun Note.isDueUpcoming(): Boolean {
    return dueDate?.let {
        val dueLocalDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        dueLocalDate.isAfter(LocalDate.now())
    } ?: false
}