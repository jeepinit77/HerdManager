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
// import androidx.compose.material.icons.filled.FilterList // Not directly used in the main Scaffold
// import androidx.compose.material.icons.filled.Clear // Not directly used in the main Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview // Keep Preview for now
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
// @Preview(showBackground = true) // Preview might not work well with ViewModel and context-based setup
@Composable
fun ActivitiesScreen(
    onAddActivityClick: () -> Unit = {},
    onEditActivityClick: (Activity) -> Unit = {},
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    // Consider injecting repository if this screen is complex, or ensure ViewModel handles most logic
    val repository = remember { // Encapsulate repository creation in remember
        CattleRepository(
            database.cowDao(),
            database.pastureDao(),
            database.activityDao(),
            database.settingsDao(),
            database.noteDao(),
            database.userDao(),
            database.herdDao(),
            database.herdMemberDao(),
            database.tagColorDao(),
            database.activityTypeConfigDao()
        )
    }
    val viewModel: ActivitiesViewModel = viewModel(
        factory = ActivitiesViewModelFactory(context.applicationContext as Application, repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) } // This state is for the filter section inside the screen

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivityClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Activity")
            }
        }
        // contentWindowInsets = WindowInsets(0, 0, 0, 0) // Commented out
    ) { paddingValues -> // This paddingValues is from THIS screen's Scaffold
                         // It will be used by the LazyColumn to position itself correctly
                         // relative to THIS Scaffold's FAB, if any.
                         // The `modifier` passed to this Scaffold already contains padding
                         // from MainActivity's Scaffold (for the TopAppBarWithMenu).

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from THIS screen's Scaffold
        ) {
            // TODO: Add a FilterChip or similar button here to toggle `showFilters`
            // This button would be part of THIS screen's content, below MainActivity's TopAppBar.
            // Example:
            // Button(onClick = { showFilters = !showFilters }) { Text("Toggle Filters") }


            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.activityGroups.isEmpty() && !showFilters) { // Don't show "Nothing here" if filters are open
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Nothing here yet", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Add activities using the + button to get started", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize() // LazyColumn fills the space given by the parent Column
                        .padding(horizontal = 16.dp), // Add horizontal padding for content list
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp) // Padding for the list items themselves
                ) {
                    // Expandable Filter Section
                    if (showFilters) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp) // Space below filter card
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Filters",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { showFilters = false }) {
                                            Text("Done")
                                        }
                                    }

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

                    if (uiState.activityGroups.isNotEmpty()){
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
                    } else if (showFilters) { // If filters are shown but result is empty
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No activities match your filters.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
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
                verticalAlignment = Alignment.Top // Changed to Top for better alignment with multiline text
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { // Added padding to prevent text touching icons
                    Text(
                        text = activity.activityType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (cowNames.isNotEmpty()) {
                        Text(
                            text = cowNames.joinToString(separator = ", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    activity.notes?.takeIf { it.isNotBlank() }?.let { notes -> // Added takeIf
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
                    // Removed Spacer(modifier = Modifier.height(4.dp)) as IconButtons have their own touch target size
                    Row {
                        onEdit?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) { // Standardized IconButton size
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Activity")
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) { // Standardized IconButton size
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
// This function might need to be removed or adapted if filter toggling is done via MainActivity's TopAppBar
private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.ActivitiesUiState): Boolean {
    return uiState.selectedStatuses != setOf(Status.ACTIVE) || // Example: Assuming ACTIVE is default
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
        LazyRow( // Changed from FlowRow to LazyRow for simplicity, can be FlowRow if needed
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
