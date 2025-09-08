package com.jumblemint.cows.ui.screens.workinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
// import androidx.compose.foundation.selection.selectable // Not directly used in the modified version
// import androidx.compose.material.icons.Icons // Imports for specific icons will be kept if actions are hoisted
// import androidx.compose.material.icons.filled.* // Or removed if actions are fully handled by MainActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModel
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModelFactory
import androidx.compose.material.icons.Icons // Keep for FilterList, Clear, ClearAll if used in hoisted actions
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier, // <<< ADDED MODIFIER PARAMETER
    // TODO: Add parameters for hoisted actions if MainActivity will provide them:
    // onToggleFilters: () -> Unit,
    // onClearAllFilters: () -> Unit,
    // onClearAllChecks: () -> Unit,
    // showFiltersInAppBar: Boolean, // To control visibility of filter chip in AppBar
    // hasActiveFiltersInAppBar: Boolean // To control visibility of clear filter icon in AppBar
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember { // Encapsulate repository creation in remember
        CattleRepository(
            cowDao = database.cowDao(),
            pastureDao = database.pastureDao(),
            activityDao = database.activityDao(),
            settingsDao = database.settingsDao(),
            noteDao = database.noteDao(),
            userDao = database.userDao(),
            herdDao = database.herdDao(),
            herdMemberDao = database.herdMemberDao(),
            tagColorDao = database.tagColorDao(),
            activityTypeConfigDao = database.activityTypeConfigDao()
        )
    }
    val viewModel: WorkingListViewModel = viewModel(
        factory = WorkingListViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val filteredCows by viewModel.filteredCows.collectAsState()
    val checkedItems by viewModel.checkedItems.collectAsState()
    var showFiltersState by remember { mutableStateOf(false) } // Renamed to avoid conflict if a prop is passed

    val tagColorMap = rememberTagColorMap(repository)

    // TODO: Communicate screen title "Working List" to MainActivity's TopAppBar.
    // This could be done via a callback, a shared ViewModel, or by passing it to NavHost.
    // Example: LaunchedEffect(Unit) { /* call to update MainActivity's title */ }

    // TODO: The actions (FilterChip, Clear Filters, Clear All Checks) previously in the TopAppBar
    // need to be handled by MainActivity's TopAppBarWithMenu.
    // This might involve:
    // 1. Hoisting the state (`showFilters`, `hasActiveFilters`) and the event handlers
    //    (`viewModel.clearAllFilters()`, `viewModel.clearAllChecks()`) to `CattleNavigation`
    //    and then to `MainActivity`.
    // 2. Or, `MainActivity`'s `TopAppBarWithMenu` could observe the `WorkingListViewModel` directly (if appropriate).
    // For now, these actions are removed from this screen's Scaffold.
    // The `showFiltersState` variable will control the visibility of the filter section *within* the content.
    // A separate button might be needed in the content if the AppBar's filter toggle is not used.

    Scaffold(
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        // topBar = { ... } // TopAppBar REMOVED
        // contentWindowInsets = WindowInsets(0, 0, 0, 0) // Commented out
    ) { paddingValues -> // These paddingValues are from THIS Scaffold (e.g., if it had a FAB)
                         // The `modifier` passed to this Scaffold ALREADY includes padding
                         // from MainActivity's Scaffold (for the TopAppBarWithMenu).

        Column( // Added a parent Column to manage layout of potential in-content filter toggle and list
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from THIS screen's Scaffold (if any elements like FAB were present)
        ) {

            // Optional: In-content button to toggle filters if AppBar doesn't handle it
            // This is a placeholder if the filter toggle is not part of the main AppBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    onClick = { showFiltersState = !showFiltersState },
                    label = { Text("Filters") },
                    selected = showFiltersState || hasActiveFilters(uiState), // Use local state for chip selection
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                )
                 if (hasActiveFilters(uiState)) {
                    IconButton(onClick = { viewModel.clearAllFilters() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Content Filters")
                    }
                }
                IconButton(onClick = { viewModel.clearAllChecks() }) {
                     Icon(Icons.Default.ClearAll, contentDescription = "Clear All Content Checks")
                }
            }


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize() // Fills the remaining space in the parent Column
                    .padding(horizontal = 16.dp), // Keep horizontal padding for the list items
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp) // Padding at the bottom of the list
            ) {
                // Expandable Filter Section
                if (showFiltersState) { // Controlled by local state
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
                                    TextButton(onClick = { showFiltersState = false }) { // Use local state
                                        Text("Done")
                                    }
                                }

                                // Status Filters
                                FilterSection(
                                    title = "Status",
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
                                    items = Gender.values().toList(), selectedItems = uiState.selectedGenders,
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
                            }
                        }
                    }
                }

                // Progress indicator
                if (filteredCows.isNotEmpty()) {
                    item {
                        val checkedCount = checkedItems.size
                        val totalCount = filteredCows.size
                        Column(modifier = Modifier.padding(top = 8.dp)) { // Add padding if filters are not shown
                            LinearProgressIndicator(
                                progress = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "$checkedCount of $totalCount checked",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } else if (!showFiltersState) { // Show empty message only if not loading and filters are not open
                     item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No cows match the current criteria.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }


                // Cow List
                items(filteredCows, key = { it.id }) { cow ->
                    WorkingListItem(
                        cow = cow,
                        isChecked = checkedItems.contains(cow.id),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                viewModel.checkItem(cow.id)
                            } else {
                                viewModel.uncheckItem(cow.id)
                            }
                        },
                        resolvedTagColor = resolveTagColor(cow.tagColor, tagColorMap)
                    )
                }
            }
        }
    }
}

// Helper function to check if any filters are active (beyond the default active status)
private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.WorkingListUiState): Boolean {
    // Assuming Status.ACTIVE is the default and other filters being empty is default.
    // This logic might need adjustment based on what's considered "default" vs "active filter".
    val hasNonDefaultStatusFilters = uiState.selectedStatuses.isNotEmpty() && (uiState.selectedStatuses.size != 1 || !uiState.selectedStatuses.contains(Status.ACTIVE))
    return hasNonDefaultStatusFilters ||
           uiState.selectedClassifications.isNotEmpty() ||
           uiState.selectedGenders.isNotEmpty() ||
           uiState.selectedPastures.isNotEmpty()
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
        LazyRow( // Changed from FlowRow for consistency if preferred, can be FlowRow if wrapping is desired
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingListItem(
    cow: Cow,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    resolvedTagColor: androidx.compose.ui.graphics.Color? = null // Keep type explicit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp) // Adjusted padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp) // Explicit size for checkbox
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cow.name ?: cow.tagNumber ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    Text(
                        text = cow.classification.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    cow.tagNumber?.let { tag ->
                        Text(
                            text = " • Tag: $tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Display resolvedTagColor if available and different from default
                 resolvedTagColor?.let { color ->
                    if (color != MaterialTheme.colorScheme.surfaceVariant && color != MaterialTheme.colorScheme.primaryContainer) { // Avoid showing default colors
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Box(modifier = Modifier.size(12.dp).background(color, shape = MaterialTheme.shapes.small))
                             Spacer(modifier = Modifier.width(4.dp))
                             Text("Tag Color", style = MaterialTheme.typography.bodySmall, color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                     }
                 }


                cow.pastureId?.let { pastureId -> // This would ideally be pastureName
                    Text(
                        text = "Pasture: $pastureId", // TODO: Resolve pasture name if possible
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
