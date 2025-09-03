package com.jumblemint.cows.ui.screens.workinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModel
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingListScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao(),
        database.noteDao()
    )
    val viewModel: WorkingListViewModel = viewModel(
        factory = WorkingListViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val filteredCows by viewModel.filteredCows.collectAsState()
    val checkedItems by viewModel.checkedItems.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Working List") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
                    
                    IconButton(onClick = { viewModel.clearAllChecks() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear All Checks")
                    }
                }
            )
        }
    ) { paddingValues ->
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
            }
            
            // Progress indicator
            if (filteredCows.isNotEmpty()) {
                item {
                    val checkedCount = checkedItems.size
                    val totalCount = filteredCows.size
                    Column {
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
                    }
                )
            }
        }
    }
}

// Helper function to check if any filters are active (beyond the default active status)
private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.WorkingListUiState): Boolean {
    return uiState.selectedStatuses != setOf(Status.ACTIVE) ||
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingListItem(
    cow: Cow,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cow.name ?: cow.tagNumber ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    Text(
                        text = cow.classification.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    cow.tagNumber?.let { tag ->
                        Text(
                            text = " • Tag: $tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                cow.pastureId?.let { pastureId ->
                    Text(
                        text = "Pasture: $pastureId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}