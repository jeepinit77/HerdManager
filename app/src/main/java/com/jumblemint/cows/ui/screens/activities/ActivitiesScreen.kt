package com.jumblemint.cows.ui.screens.activities

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModel
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModelFactory
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ActivitiesScreen(
    onAddActivityClick: () -> Unit = {},
    onEditActivityClick: (Activity) -> Unit = {}
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao()
    )
    val viewModel: ActivitiesViewModel = viewModel(
        factory = ActivitiesViewModelFactory(context.applicationContext as Application, repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivityClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Activity")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Activities") },
                actions = {
                    FilterChip(
                        onClick = { showFilters = !showFilters },
                        label = { Text("Filters") },
                        selected = showFilters || hasActiveFilters(uiState),
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                    )
                    
                    if (hasActiveFilters(uiState)) {
                        IconButton(onClick = { viewModel.clearAllFilters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Filters")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.activityGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "No activities found", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Activities will appear here as you manage your cattle", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expandable Filter Section
                if (showFilters) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Status Filters
                                FilterSection(
                                    title = "Cattle Status",
                                    items = Status.values().toList(),
                                    selectedItems = uiState.selectedStatuses,
                                    onToggle = { viewModel.toggleStatusFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
                                
                                // Classification Filters
                                FilterSection(
                                    title = "Animal Type",
                                    items = Classification.values().toList(),
                                    selectedItems = uiState.selectedClassifications,
                                    onToggle = { viewModel.toggleClassificationFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
                                
                                // Gender Filters
                                FilterSection(
                                    title = "Gender",
                                    items = Gender.values().toList(),
                                    selectedItems = uiState.selectedGenders,
                                    onToggle = { viewModel.toggleGenderFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
                                
                                // Pasture Filters
                                if (uiState.availablePastures.isNotEmpty()) {
                                    FilterSection(
                                        title = "Pasture",
                                        items = uiState.availablePastures,
                                        selectedItems = uiState.selectedPastures,
                                        onToggle = { viewModel.togglePastureFilter(it) },
                                        itemLabel = { it }
                                    )
                                }
                                
                                // Activity Type Filters
                                FilterSection(
                                    title = "Activity Type",
                                    items = ActivityType.values().toList(),
                                    selectedItems = uiState.selectedActivityTypes,
                                    onToggle = { viewModel.toggleActivityTypeFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
                            }
                        }
                    }
                }
                
                items(uiState.activityGroups, key = { it.sample.id }) { group ->
                    ActivityCard(
                        activity = group.sample,
                        cowNames = group.cowNames.filterNotNull(),
                        onEdit = { onEditActivityClick(group.sample) },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteActivities(group.activities)
                                val res = snackbarHostState.showSnackbar(
                                    message = "Activity deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Long
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDeleteActivities(group.activities)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCard(
    activity: Activity,
    cowNames: List<String>,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.activityType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Show all cows affected
                    if (cowNames.isNotEmpty()) {
                        Text(
                            text = cowNames.joinToString(separator = ", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    activity.notes?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = activity.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        onEdit?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Activity")
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Activity",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to check if any filters are active (beyond the default active status)
private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.ActivitiesUiState): Boolean {
    return uiState.selectedStatuses != setOf(Status.ACTIVE) ||
           uiState.selectedClassifications.isNotEmpty() ||
           uiState.selectedGenders.isNotEmpty() ||
           uiState.selectedPastures.isNotEmpty() ||
           uiState.selectedActivityTypes.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterSection(
    title: String,
    items: List<T>,
    selectedItems: Set<T>,
    onToggle: (T) -> Unit,
    itemLabel: (T) -> String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                FilterChip(
                    onClick = { onToggle(item) },
                    label = { Text(itemLabel(item)) },
                    selected = selectedItems.contains(item)
                )
            }
        }
    }
}
