package com.jumblemint.cows.ui.screens.activities

import android.app.Application
import androidx.compose.foundation.clickable // Added for Card click
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModel
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModelFactory
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.theme.SmartText
import com.jumblemint.cows.ui.theme.contrastingTextColor
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onAddActivityClick: () -> Unit = {},
    onEditActivityClick: (Activity) -> Unit = {},
    onActivityClick: (Long) -> Unit = {}, // <<< ADDED PARAMETER
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
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
            database.activityTypeConfigDao(),
            database.breedDao() // Added missing breedDao
        )
    }
    val viewModel: ActivitiesViewModel = viewModel(
        factory = ActivitiesViewModelFactory(context.applicationContext as Application, repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    val globalSnackbarState = com.jumblemint.cows.ui.components.LocalGlobalSnackbarState.current
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.activityGroups.isEmpty() && !showFilters) {
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search and Filter bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Search activities...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            modifier = Modifier.weight(1f).height(56.dp)
                        )
                        FilterChip(
                            onClick = { showFilters = !showFilters },
                            label = {
                                val activeFilterCount = getActiveFilterCount(uiState)
                                if (activeFilterCount > 0) {
                                    Text("($activeFilterCount)")
                                } else {
                                    Text("Filters")
                                }
                            },
                            selected = hasActiveFilters(uiState),
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") },
                            modifier = Modifier.height(56.dp)
                        )
                        if (hasActiveFilters(uiState) || uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.clearAllFilters()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear All Filters")
                            }
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                    if (showFilters) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = getCardColors()
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
                                    FilterSection(
                                        title = "Activity Type",
                                        items = ActivityType.values().toList(),
                                        selectedItems = uiState.selectedActivityTypes,
                                        onToggle = { viewModel.toggleActivityTypeFilter(it) },
                                        itemLabel = { it.displayName }
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
                                onClick = { onActivityClick(group.sample.id) }, // <<< PASSING CLICK HANDLER
                                onEdit = { onEditActivityClick(group.sample) },
                                onDelete = {
                                    scope.launch {
                                        viewModel.deleteActivities(group.activities)
                                        val res = globalSnackbarState?.showSnackbar(
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
                    } else if (showFilters) {
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
        
        // Floating Action Button positioned manually
        FloatingActionButton(
            onClick = onAddActivityClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Activity")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCard(
    activity: Activity,
    cowNames: List<String>,
    onClick: () -> Unit, // <<< ADDED onClick PARAMETER
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // <<< MADE CARD CLICKABLE
        colors = getCardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    SmartText(
                        text = activity.activityType.displayName, // Using displayName
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (cowNames.isNotEmpty()) {
                        SmartText(
                            text = cowNames.joinToString(separator = ", "),
                            style = MaterialTheme.typography.bodyMedium,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    activity.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        SmartText(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            maxLines = 2, // Limit notes preview if needed
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis // Added for long notes
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    SmartText(
                        text = activity.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.labelMedium,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row {
                        onEdit?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    Icons.Filled.Edit, 
                                    contentDescription = "Edit Activity",
                                    tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
                                )
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) { 
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Activity",
                                    tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.ActivitiesUiState): Boolean {
    return uiState.selectedActivityTypes.isNotEmpty()
}

private fun getActiveFilterCount(uiState: com.jumblemint.cows.ui.viewmodel.ActivitiesUiState): Int {
    var count = 0
    if (uiState.selectedActivityTypes.isNotEmpty()) count++
    return count
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
