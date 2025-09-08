package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CowCard
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.CowsViewModel
import com.jumblemint.cows.ui.viewmodel.CowsViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.jumblemint.cows.ui.components.FocusAwareLiveSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowsScreen(
    pastureId: Long? = null,
    onCowClick: (Long) -> Unit,
    onCowEdit: (Long) -> Unit,
    onAddCowClick: () -> Unit,
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
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
    val viewModel: CowsViewModel = viewModel(
        factory = CowsViewModelFactory(application, repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    // Get tag color map for resolving tag colors
    val tagColorMap = rememberTagColorMap(repository)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    FocusAwareLiveSync(
        orchestrator = application.syncOrchestrator,
        screenKey = "Cows",
        intervalMs = 20_000L,
        leadingRun = true
    )

    Scaffold(
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCowClick
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cow")
            }
        },
        // contentWindowInsets = WindowInsets(0, 0, 0, 0) // This might be overridden by the modifier from CattleNavigation
                                                        // It's generally safer to let the modifier handle all padding.
                                                        // If mainScaffoldPadding is correctly applied by the modifier,
                                                        // this specific contentWindowInsets might not be needed or could conflict.
                                                        // Let's keep it commented for now and see the effect.
    ) { paddingValues -> // This paddingValues is from THIS screen's Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from THIS Scaffold
                .padding(horizontal = 16.dp) // Keep additional horizontal padding for content
        ) {
            // Search and Filter Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search cows...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    onClick = { showFilters = !showFilters },
                    label = {
                        val activeFilterCount = getActiveFilterCount(uiState)
                        if (activeFilterCount > 0) {
                            Text("($activeFilterCount)")
                        } else {
                            Text("")
                        }
                    },
                    selected = showFilters || hasActiveFilters(uiState),
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                )

                if (hasActiveFilters(uiState)) {
                    IconButton(onClick = { viewModel.clearAllFilters() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Filters")
                    }
                }
            }

            // Expandable Filter Section
            if (showFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Visual indicator for dismissible panel
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    MaterialTheme.shapes.small
                                )
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                            TextButton(
                                onClick = { showFilters = false }
                            ) {
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.cows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Nothing here yet", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Add cows using the + button to get started", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.cows, key = { it.id }) { cow ->
                        CowCard(
                            cow = cow,
                            onClick = { onCowClick(cow.id) },
                            onToggleWatch = { viewModel.toggleWatch(cow) },
                            onEdit = { onCowEdit(cow.id) },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteCow(cow)
                                    val res = snackbarHostState.showSnackbar(
                                        message = "Cow deleted",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeleteCow(cow)
                                    }
                                }
                            },
                            resolvedTagColor = resolveTagColor(cow.tagColor, tagColorMap)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to check if any filters are active (excluding default ACTIVE status)
private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.CowsUiState): Boolean {
    val hasNonDefaultStatusFilters = uiState.selectedStatuses != setOf(Status.ACTIVE)
    return hasNonDefaultStatusFilters ||
           uiState.selectedClassifications.isNotEmpty() ||
           uiState.selectedGenders.isNotEmpty() ||
           uiState.selectedPastures.isNotEmpty() ||
           uiState.searchQuery.isNotBlank()
}

// Helper function to count active filters (excluding default ACTIVE status)
private fun getActiveFilterCount(uiState: com.jumblemint.cows.ui.viewmodel.CowsUiState): Int {
    var count = 0
    if (uiState.selectedStatuses != setOf(Status.ACTIVE)) count++
    if (uiState.selectedClassifications.isNotEmpty()) count++
    if (uiState.selectedGenders.isNotEmpty()) count++
    if (uiState.selectedPastures.isNotEmpty()) count++
    if (uiState.searchQuery.isNotBlank()) count++
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
                val isSelected = selectedItems.contains(item)
                FilterChip(
                    onClick = { onToggle(item) },
                    label = {
                        Text(
                            text = itemLabel(item),
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    selected = isSelected,
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}
