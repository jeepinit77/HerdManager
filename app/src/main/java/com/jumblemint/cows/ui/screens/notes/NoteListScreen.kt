package com.jumblemint.cows.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.model.Note
import com.jumblemint.cows.ui.components.AppAlertDialog
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.theme.contrastingTextColor
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.viewmodel.NotesUiState
import com.jumblemint.cows.ui.viewmodel.NotesViewModel
import com.jumblemint.cows.ui.viewmodel.TodoStatusFilter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onAddNote: () -> Unit,
    onEditNote: (Long) -> Unit,
    onViewNote: (Long) -> Unit,
    viewModel: NotesViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    val globalSnackbarState = com.jumblemint.cows.ui.components.LocalGlobalSnackbarState.current
    val scope = rememberCoroutineScope()
    var showFilterDialog by remember { mutableStateOf(false) }

    val hasNotes = uiState.allNotes.isNotEmpty()
    val hasFilters = hasActiveFilters(uiState)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotesSearchAndFilterRow(
                uiState = uiState,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onShowFilters = { showFilterDialog = true },
                onClearAll = {
                    viewModel.clearAllFilters()
                    viewModel.updateSearchQuery("")
                }
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !hasNotes && uiState.searchQuery.isBlank() && !hasFilters -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Nothing here yet", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Add notes using the + button to get started", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                uiState.filteredNotes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No notes match your search",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                viewModel.clearAllFilters()
                                viewModel.updateSearchQuery("")
                            }) {
                                Text("Clear search & filters")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                dateFormat = dateFormat,
                                onView = {
                                    onViewNote(note.id)
                                },
                                onEdit = {
                                    onEditNote(note.id)
                                },
                                onDelete = {
                                    scope.launch {
                                        viewModel.deleteNote(note)
                                        val res = globalSnackbarState?.showSnackbar(
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
            }
        }

        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Note")
        }

    }

    if (showFilterDialog) {
        NoteFiltersDialog(
            initialStartDate = uiState.startDateMillis,
            initialEndDate = uiState.endDateMillis,
            initialTodoFilter = uiState.todoFilter,
            onApply = { start, end, todo ->
                viewModel.updateStartDate(start)
                viewModel.updateEndDate(end)
                viewModel.updateTodoFilter(todo)
                showFilterDialog = false
            },
            onClear = {
                viewModel.clearAllFilters()
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            scope.launch {
                globalSnackbarState?.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
            }
        }
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
        modifier = Modifier.fillMaxWidth(),
        colors = getCardColors()
    ) {
        val dueDateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    if (note.title.isNotEmpty()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(note.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (note.isTodo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (note.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (note.isCompleted) "Todo completed" else "Todo active",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (note.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }

                note.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Due: ${dueDateFormat.format(Date(dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NotesSearchAndFilterRow(
    uiState: NotesUiState,
    onSearchChange: (String) -> Unit,
    onShowFilters: () -> Unit,
    onClearAll: () -> Unit
) {
    val activeFilterCount = getActiveFilterCount(uiState)
    val hasFilters = hasActiveFilters(uiState)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 56.dp)
                .alignBy { it.measuredHeight / 2 },
            colors = defaultOutlinedTextFieldColors()
        )

        FilterChip(
            onClick = onShowFilters,
            label = {
                if (activeFilterCount > 0) {
                    Text("($activeFilterCount)")
                } else {
                    Text("Filters")
                }
            },
            selected = hasFilters,
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") },
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .alignBy { it.measuredHeight / 2 },
            colors = FilterChipDefaults.filterChipColors(
                labelColor = MaterialTheme.colorScheme.background.contrastingTextColor(),
                iconColor = MaterialTheme.colorScheme.background.contrastingTextColor(),
                selectedLabelColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor(),
                selectedLeadingIconColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor(),
                containerColor = MaterialTheme.colorScheme.background,
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.12f)
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = hasFilters,
                borderColor = MaterialTheme.colorScheme.background.contrastingTextColor().copy(alpha = 0.3f),
                selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor().copy(alpha = 0.3f)
            )
        )

        if (hasFilters || uiState.searchQuery.isNotBlank()) {
            IconButton(onClick = onClearAll) {
                Icon(Icons.Default.Clear, contentDescription = "Clear search and filters")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteFiltersDialog(
    initialStartDate: Long?,
    initialEndDate: Long?,
    initialTodoFilter: TodoStatusFilter,
    onApply: (Long?, Long?, TodoStatusFilter) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    var startDate by remember(initialStartDate) { mutableStateOf(initialStartDate?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }) }
    var endDate by remember(initialEndDate) { mutableStateOf(initialEndDate?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }) }
    var todoFilter by remember(initialTodoFilter) { mutableStateOf(initialTodoFilter) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val startMillis = startDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
                val endMillis = endDate?.plusDays(1)?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()?.minus(1)
                onApply(startMillis, endMillis, todoFilter)
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    startDate = null
                    endDate = null
                    todoFilter = TodoStatusFilter.ALL
                    onClear()
                }) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        title = { Text("Filter notes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date range", style = MaterialTheme.typography.titleMedium)
                    DatePickerField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            if (endDate != null && it != null && it.isAfter(endDate)) {
                                endDate = it
                            }
                        },
                        label = "Start date (optional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    DatePickerField(
                        value = endDate,
                        onValueChange = {
                            endDate = it
                            if (startDate != null && it != null && it.isBefore(startDate)) {
                                startDate = it
                            }
                        },
                        label = "End date (optional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Show", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow {
                        TodoStatusFilter.values().forEachIndexed { index, filter ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TodoStatusFilter.values().size),
                                onClick = { todoFilter = filter },
                                selected = todoFilter == filter,
                                modifier = Modifier.defaultMinSize(minHeight = SegmentedButtonMinHeight),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(filter.label, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    )
}

private val SegmentedButtonMinHeight = 44.dp

private fun hasActiveFilters(uiState: NotesUiState): Boolean {
    return uiState.startDateMillis != null || uiState.endDateMillis != null || uiState.todoFilter != TodoStatusFilter.ALL
}

private fun getActiveFilterCount(uiState: NotesUiState): Int {
    var count = 0
    if (uiState.startDateMillis != null) count++
    if (uiState.endDateMillis != null) count++
    if (uiState.todoFilter != TodoStatusFilter.ALL) count++
    return count
}


