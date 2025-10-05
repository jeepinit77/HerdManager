package com.jumblemint.cows.ui.screens.activities

import android.app.Application
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
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.*
import com.jumblemint.cows.ui.components.AnimalFilterScreen
import com.jumblemint.cows.ui.components.AnimalFilterState
import com.jumblemint.cows.ui.viewmodel.AddActivityViewModel
import com.jumblemint.cows.ui.viewmodel.AddActivityViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    editId: Long? = null,
    onNavigateBack: () -> Unit,
    onEditPasture: () -> Unit = {},
    modifier: Modifier = Modifier,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
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
            database.breedDao()
        )
    }
    val viewModel: AddActivityViewModel = viewModel(
        factory = AddActivityViewModelFactory(application, repository, editId)
    )

    val uiState by viewModel.uiState.collectAsState()
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showAnimalFilterDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterState by remember { mutableStateOf(AnimalFilterState()) }

    val filteredCows = remember(uiState.availableCows, filterState, searchQuery) {
        uiState.availableCows.filter { cow ->
            val matchesGender = filterState.genders.isEmpty() || cow.gender in filterState.genders
            val matchesClassification = filterState.classifications.isEmpty() || cow.classification in filterState.classifications
            val matchesPasture = filterState.pastures.isEmpty() || 
                (cow.pastureId in filterState.pastures) || 
                (cow.pastureId == null && "Unassigned" in filterState.pastures)
            val matchesSearch = searchQuery.isBlank() ||
                cow.name?.contains(searchQuery, ignoreCase = true) == true ||
                cow.tagNumber?.contains(searchQuery, ignoreCase = true) == true
            matchesGender && matchesClassification && matchesPasture && matchesSearch
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Notify parent about unsaved changes
    LaunchedEffect(uiState.hasUnsavedChanges) {
        onUnsavedChangesChanged(uiState.hasUnsavedChanges)
    }

    // Handle back press with unsaved changes warning
    LaunchedEffect(backPressed) {
        if (backPressed) {
            if (uiState.hasUnsavedChanges) {
                showUnsavedChangesDialog = true
            } else {
                onNavigateBack()
            }
            onBackHandled()
        }
    }

    // Handle save trigger from top bar
    LaunchedEffect(saveTriggered) {
        if (saveTriggered) {
            viewModel.saveActivity()
            onSaveHandled()
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onSave = {
                showUnsavedChangesDialog = false
                viewModel.saveActivity()
            },
            onDiscard = {
                showUnsavedChangesDialog = false
                onNavigateBack()
            }
        )
    }

    // Animal filter dialog
    if (showAnimalFilterDialog) {
        AnimalFilterScreen(
            initialFilterState = filterState,
            availablePastures = listOf("Unassigned") + uiState.availablePastures.map { it.name },
            availableBreeds = emptyList(),
            availableTagColors = emptyList(),
            onApplyFilters = { newState ->
                filterState = newState
                showAnimalFilterDialog = false
            },
            onDismiss = { showAnimalFilterDialog = false }
        )
    }

    if (uiState.isLoading && editId != null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier
                .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show all errors at the very top
                uiState.error?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Activity Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            DropdownField(
                                value = uiState.activityType?.name ?: "",
                                onValueChange = { typeName ->
                                    val type = ActivityType.entries.find { it.name == typeName }
                                    viewModel.updateActivityType(type)
                                },
                                label = "Activity Type",
                                options = ActivityType.entries.map { it.name },
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.error?.contains("Activity Type") == true // <<< USING isError
                            )
                            // Supporting text for DropdownField can be added if needed,
                            // or rely on the DropdownField's internal isError visual cue.
                            if (uiState.error?.contains("Activity Type") == true && uiState.activityType == null) {
                                Text(
                                    "Activity Type is required.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                )
                            }

                            DatePickerField(
                                value = uiState.date,
                                onValueChange = viewModel::updateDate,
                                label = "Date",
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (uiState.activityType == ActivityType.MOVED) {
                                DropdownField(
                                    value = uiState.toPastureName ?: "",
                                    onValueChange = { name ->
                                        val pasture = uiState.availablePastures.find { it.name == name }
                                        viewModel.updateToPasture(pasture?.id)
                                    },
                                    label = "Move to Pasture",
                                    options = listOf("") + uiState.availablePastures.map { it.name },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = uiState.error?.contains("Pasture") == true // <<< USING isError
                                )
                                if (uiState.error?.contains("Pasture") == true && uiState.toPastureId == null) {
                                     Text(
                                        "Pasture selection is required for MOVED activity.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = viewModel::updateNotes,
                                label = { Text("Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 5,
                                placeholder = {
                                    Text(
                                        if (uiState.activityType in listOf(ActivityType.WORKED, ActivityType.OTHER))
                                            "Notes required for this activity type"
                                        else
                                            "Optional notes"
                                    )
                                },
                                isError = (uiState.activityType in listOf(ActivityType.WORKED, ActivityType.OTHER) && uiState.notes.isBlank()) || uiState.error?.contains("Notes") == true,
                                supportingText = if (uiState.activityType in listOf(ActivityType.WORKED, ActivityType.OTHER) && uiState.notes.isBlank()) {
                                    { Text("Notes are required for ${uiState.activityType?.name?.lowercase()} activity.")}
                                } else if (uiState.error?.contains("Notes") == true && uiState.notes.isBlank()) { // Be more specific for general notes error
                                     { Text("Notes field has an error.") } // Or uiState.error specific to notes
                                } else null
                            )
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Animals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.selectedCows.size} selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search by name or tag") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear search") } }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val filteredCowIds = filteredCows.map { it.id }.toSet()
                                        filteredCowIds.forEach { viewModel.selectCow(it) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = filteredCows.isNotEmpty()
                                ) {
                                    Text("Select All Filtered")
                                }
                                Button(
                                    onClick = { viewModel.clearSelection() },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.selectedCows.isNotEmpty()
                                ) {
                                    Text("Clear Selection")
                                }
                                FilterChip(
                                    onClick = { showAnimalFilterDialog = true },
                                    label = { 
                                        val activeFilterCount = getActiveFilterCount(filterState)
                                        if (activeFilterCount > 0) {
                                            Text("($activeFilterCount)")
                                        } else {
                                            Text("Filters")
                                        }
                                    },
                                    selected = hasActiveFilters(filterState),
                                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                                )
                            }
                            
                            if (hasActiveFilters(filterState) || searchQuery.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        filterState = AnimalFilterState()
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear All Filters")
                                    }
                                }
                            }
                            if (uiState.error?.contains("cows") == true && uiState.selectedCows.isEmpty()) {
                                Text(
                                    text = "At least one animal must be selected.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }



                if (filteredCows.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("No animals match current filters.")
                        }
                    }
                } else {
                    items(filteredCows, key = { it.id }) { cow ->
                        CowSelectionCard(
                            cow = cow,
                            isSelected = cow.id in uiState.selectedCows,
                            onSelectionChanged = { isSelected ->
                                if (isSelected) {
                                    viewModel.selectCow(cow.id)
                                } else {
                                    viewModel.deselectCow(cow.id)
                                }
                            }
                        )
                    }
                }




            }
    }
}

private fun hasActiveFilters(filterState: AnimalFilterState): Boolean {
    return filterState.classifications.isNotEmpty() ||
           filterState.genders.isNotEmpty() ||
           filterState.pastures.isNotEmpty()
}

private fun getActiveFilterCount(filterState: AnimalFilterState): Int {
    var count = 0
    if (filterState.classifications.isNotEmpty()) count++
    if (filterState.genders.isNotEmpty()) count++
    if (filterState.pastures.isNotEmpty()) count++
    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowSelectionCard(
    cow: Cow,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelectionChanged(!isSelected) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChanged,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cow.name ?: cow.tagNumber ?: "Unnamed Animal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                val details = mutableListOf<String>()
                details.add(cow.gender.name.lowercase().replaceFirstChar { it.uppercase() })
                details.add(cow.classification.name.lowercase().replaceFirstChar { it.uppercase() })
                if (cow.tagNumber != null && cow.tagNumber != cow.name) details.add("Tag: ${cow.tagNumber}")
                Text(
                    text = details.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                )
            }
        }
    }
}
