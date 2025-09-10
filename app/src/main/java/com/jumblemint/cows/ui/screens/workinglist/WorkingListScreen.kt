package com.jumblemint.cows.ui.screens.workinglist

import androidx.compose.foundation.ExperimentalFoundationApi // Added for stickyHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Ensure this import is present
import androidx.compose.ui.graphics.luminance // <<< THE MISSING IMPORT >>>
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.WorkingListUiState
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModel
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkingListScreen(
    onNavigateBack: () -> Unit,
    onCowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
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
    var showFiltersState by remember { mutableStateOf(false) }

    val tagColorMap = rememberTagColorMap(repository)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Working List") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFiltersState = !showFiltersState }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Toggle Filters")
                    }
                    if (hasActiveFilters(uiState)) {
                        IconButton(onClick = { viewModel.clearAllFilters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Content Filters")
                        }
                    }
                    IconButton(onClick = { viewModel.clearAllChecks() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear All Content Checks")
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Screen")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                if (showFiltersState) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
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
                                    TextButton(onClick = { showFiltersState = false }) {
                                        Text("Done")
                                    }
                                }
                                FilterSection(
                                    title = "Status",
                                    items = Status.values().toList(),
                                    selectedItems = uiState.selectedStatuses,
                                    onToggle = { viewModel.toggleStatusFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
                                FilterSection(
                                    title = "Animal Type",
                                    items = Classification.values().toList(),
                                    selectedItems = uiState.selectedClassifications,
                                    onToggle = { viewModel.toggleClassificationFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
                                FilterSection(
                                    title = "Gender",
                                    items = Gender.values().toList(), selectedItems = uiState.selectedGenders,
                                    onToggle = { viewModel.toggleGenderFilter(it) },
                                    itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                                )
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

                if (filteredCows.isNotEmpty()) {
                    stickyHeader {
                        val checkedCount = checkedItems.size
                        val totalCount = filteredCows.size
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface) 
                                .padding(top = 8.dp, bottom = 8.dp) 
                        ) {
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
                } else if (!showFiltersState) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No cows match the current criteria.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

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
                        resolvedTagColor = resolveTagColor(cow.tagColor, tagColorMap),
                        onCowClick = { onCowClick(cow.id) }
                    )
                }
            }
        }
    }
}

private fun hasActiveFilters(uiState: WorkingListUiState): Boolean {
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
    onCheckedChange: (Boolean) -> Unit,
    onCowClick: (Long) -> Unit,
    resolvedTagColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onCowClick(cow.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "View Cow Info",
                    tint = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cow.name ?: cow.tagNumber ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onCowClick(cow.id) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cow.classification.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // TODO: Display pasture information (cow.pastureId) here when ready.
            }
            if (!cow.tagNumber.isNullOrBlank() && resolvedTagColor != null) {
                val tagBackgroundColor: androidx.compose.ui.graphics.Color = resolvedTagColor // Explicit non-null type
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(tagBackgroundColor, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cow.tagNumber,
                        color = if (tagBackgroundColor.luminance() > 0.5f) Color.Black else Color.White, // Using unqualified Color.Black/White
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
