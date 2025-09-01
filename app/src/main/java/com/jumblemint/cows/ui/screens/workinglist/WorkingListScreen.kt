package com.jumblemint.cows.ui.screens.workinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var isFilterExpanded by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { isFilterExpanded = !isFilterExpanded }) {
                        Icon(
                            if (isFilterExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = if (isFilterExpanded) "Hide Filters" else "Show Filters"
                        )
                    }
                    IconButton(onClick = { viewModel.clearAllChecks() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear All")
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
            // Collapsible Filter Section
            if (isFilterExpanded) {
                item {
                    FilterSection(
                        selectedPasture = uiState.selectedPasture,
                        selectedClassification = uiState.selectedClassification,
                        onPastureChange = { viewModel.updatePastureFilter(it) },
                        onClassificationChange = { viewModel.updateClassificationFilter(it) },
                        pastures = uiState.availablePastures
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    selectedPasture: String?,
    selectedClassification: Classification?,
    onPastureChange: (String?) -> Unit,
    onClassificationChange: (Classification?) -> Unit,
    pastures: List<String>
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pasture Filter
            var pastureExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = pastureExpanded,
                onExpandedChange = { pastureExpanded = !pastureExpanded }
            ) {
                OutlinedTextField(
                    value = selectedPasture ?: "All Pastures",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Pasture") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = pastureExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = pastureExpanded,
                    onDismissRequest = { pastureExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Pastures") },
                        onClick = {
                            onPastureChange(null)
                            pastureExpanded = false
                        }
                    )
                    pastures.forEach { pasture ->
                        DropdownMenuItem(
                            text = { Text(pasture) },
                            onClick = {
                                onPastureChange(pasture)
                                pastureExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Classification Filter
            var classificationExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = classificationExpanded,
                onExpandedChange = { classificationExpanded = !classificationExpanded }
            ) {
                OutlinedTextField(
                    value = selectedClassification?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All Types",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Animal Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = classificationExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = classificationExpanded,
                    onDismissRequest = { classificationExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Types") },
                        onClick = {
                            onClassificationChange(null)
                            classificationExpanded = false
                        }
                    )
                    Classification.values().forEach { classification ->
                        DropdownMenuItem(
                            text = { Text(classification.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onClassificationChange(classification)
                                classificationExpanded = false
                            }
                        )
                    }
                }
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