package com.jumblemint.cows.ui.screens.activities

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.theme.SmartText
import com.jumblemint.cows.ui.theme.contrastingTextColor
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModel
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModelFactory
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onAddActivityClick: () -> Unit = {},
    onEditActivityClick: (Activity) -> Unit = {},
    onActivityClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = remember(context) { context.applicationContext as? CattleApplication }
    val repository = remember(application) {
        application?.repository ?: CattleDatabase.getDatabase(context).let { database ->
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
                activityTypeConfigDao = database.activityTypeConfigDao(),
                breedDao = database.breedDao()
            )
        }
    }

    val viewModel: ActivitiesViewModel = viewModel(
        factory = ActivitiesViewModelFactory(
            (context.applicationContext as? Application)
                ?: throw IllegalStateException("Application context is required"),
            repository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val globalSnackbarState = com.jumblemint.cows.ui.components.LocalGlobalSnackbarState.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivityClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Activity")
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val hasActiveFilters = hasActiveFilters(uiState)
            val isQueryActive = uiState.searchQuery.isNotBlank()
            val showEmptyState = uiState.activityGroups.isEmpty() && !showFilters && !hasActiveFilters && !isQueryActive

            if (showEmptyState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nothing here yet",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add activities using the + button to get started",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
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
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )

                        val activeFilterCount = getActiveFilterCount(uiState)
                        FilterChip(
                            onClick = { showFilters = !showFilters },
                            label = {
                                Text(
                                    text = if (activeFilterCount > 0) {
                                        "Filters ($activeFilterCount)"
                                    } else {
                                        "Filters"
                                    }
                                )
                            },
                            selected = showFilters || hasActiveFilters,
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                        )

                        if (hasActiveFilters || isQueryActive) {
                            IconButton(onClick = {
                                viewModel.clearAllFilters()
                                showFilters = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear All Filters")
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        if (showFilters) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = getCardColors()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
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

                                        FilterSection(
                                            title = "Status",
                                            items = Status.values().toList(),
                                            selectedItems = uiState.selectedStatuses,
                                            onToggle = { viewModel.toggleStatusFilter(it) },
                                            itemLabel = { it.name.toDisplayName() }
                                        )

                                        FilterSection(
                                            title = "Classification",
                                            items = Classification.values().toList(),
                                            selectedItems = uiState.selectedClassifications,
                                            onToggle = { viewModel.toggleClassificationFilter(it) },
                                            itemLabel = { it.name.toDisplayName() }
                                        )

                                        FilterSection(
                                            title = "Gender",
                                            items = Gender.values().toList(),
                                            selectedItems = uiState.selectedGenders,
                                            onToggle = { viewModel.toggleGenderFilter(it) },
                                            itemLabel = { it.name.toDisplayName() }
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

                        if (uiState.activityGroups.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No activities match your current filters.",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.activityGroups,
                                key = { it.groupId }
                            ) { group ->
                                ActivityCard(
                                    activity = group.sample,
                                    cowNames = group.cowNames.filterNotNull(),
                                    onClick = { onActivityClick(group.sample.id) },
                                    onEdit = { onEditActivityClick(group.sample) },
                                    onDelete = {
                                        scope.launch {
                                            viewModel.deleteActivities(group.activities)
                                            val result = globalSnackbarState?.showSnackbar(
                                                message = "Activity deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCard(
    activity: Activity,
    cowNames: List<String>,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
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
                        text = activity.activityType.displayName,
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
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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
    return uiState.selectedStatuses.isNotEmpty() ||
        uiState.selectedClassifications.isNotEmpty() ||
        uiState.selectedGenders.isNotEmpty() ||
        uiState.selectedPastures.isNotEmpty() ||
        uiState.selectedActivityTypes.isNotEmpty()
}

private fun getActiveFilterCount(uiState: com.jumblemint.cows.ui.viewmodel.ActivitiesUiState): Int {
    return listOf(
        uiState.selectedStatuses.size,
        uiState.selectedClassifications.size,
        uiState.selectedGenders.size,
        uiState.selectedPastures.size,
        uiState.selectedActivityTypes.size
    ).sum()
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

private fun String.toDisplayName(): String {
    return lowercase(Locale.getDefault())
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(Locale.getDefault())
                } else {
                    char.toString()
                }
            }
        }
}
