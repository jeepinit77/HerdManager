package com.jumblemint.cows.ui.screens.workinglist

import androidx.compose.foundation.ExperimentalFoundationApi // Added for stickyHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.components.AnimalFilterScreen
import com.jumblemint.cows.ui.components.AnimalFilterState
import com.jumblemint.cows.ui.viewmodel.WorkingListUiState
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModel
import com.jumblemint.cows.ui.viewmodel.WorkingListViewModelFactory
import com.jumblemint.cows.util.primaryIdentifier
import com.jumblemint.cows.util.usesTags

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
    var showFilterScreen by remember { mutableStateOf(false) }

    val tagColorMap = rememberTagColorMap(repository)
    val identifierMode by repository.getAnimalIdentifierModeFlow().collectAsState(initial = AnimalIdentifierMode.BOTH)

    if (showFilterScreen) {
        AnimalFilterScreen(
            initialFilterState = AnimalFilterState(
                classifications = uiState.selectedClassifications.toList(),
                genders = uiState.selectedGenders.toList(),
                pastures = uiState.selectedPastures.toList(),
                breeds = uiState.selectedBreeds.toList(),
                statuses = uiState.selectedStatuses.toList(),
                tagColors = uiState.selectedTagColors.toList(),
                isWatched = uiState.selectedIsWatched,
                selectedAgeRanges = uiState.selectedAgeRanges.toList()
            ),
            availablePastures = uiState.availablePastures,
            availableBreeds = uiState.availableBreeds,
            availableTagColors = uiState.availableTagColors,
            onApplyFilters = { newState ->
                viewModel.applyFilterState(newState)
                showFilterScreen = false
            },
            onDismiss = { showFilterScreen = false }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                onClick = { showFilterScreen = true },
                label = {
                    val activeFilterCount = getActiveFilterCount(uiState)
                    if (activeFilterCount > 0) {
                        Text("($activeFilterCount) Filters")
                    } else {
                        Text("Filters")
                    }
                },
                selected = hasActiveFilters(uiState),
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
            )
            if (hasActiveFilters(uiState)) {
                IconButton(onClick = { viewModel.clearAllFilters() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear All Filters")
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
        ) {
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
            } else {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                    onCowClick = { onCowClick(cow.id) },
                    identifierMode = identifierMode
                )
            }
        }
    }
}

private fun hasActiveFilters(uiState: WorkingListUiState): Boolean {
    return uiState.selectedStatuses.isNotEmpty() ||
            uiState.selectedClassifications.isNotEmpty() ||
            uiState.selectedGenders.isNotEmpty() ||
            uiState.selectedPastures.isNotEmpty() ||
            uiState.selectedBreeds.isNotEmpty() ||
            uiState.selectedTagColors.isNotEmpty() ||
            uiState.selectedIsWatched != null ||
            uiState.selectedAgeRanges.isNotEmpty()
}

private fun getActiveFilterCount(uiState: WorkingListUiState): Int {
    var count = 0
    if (uiState.selectedStatuses.isNotEmpty()) count++
    if (uiState.selectedClassifications.isNotEmpty()) count++
    if (uiState.selectedGenders.isNotEmpty()) count++
    if (uiState.selectedPastures.isNotEmpty()) count++
    if (uiState.selectedBreeds.isNotEmpty()) count++
    if (uiState.selectedTagColors.isNotEmpty()) count++
    if (uiState.selectedIsWatched != null) count++
    if (uiState.selectedAgeRanges.isNotEmpty()) count++
    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingListItem(
    cow: Cow,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onCowClick: (Long) -> Unit,
    identifierMode: AnimalIdentifierMode,
    resolvedTagColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val displayIdentifier = identifierMode.primaryIdentifier(cow.name, cow.tagNumber, fallback = "Unknown")
    val showTagBadge = identifierMode.usesTags() && !cow.tagNumber.isNullOrBlank() && resolvedTagColor != null
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
                    text = displayIdentifier,
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
            if (showTagBadge) {
                val tagBackgroundColor: androidx.compose.ui.graphics.Color = resolvedTagColor!! // Explicit non-null type
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
