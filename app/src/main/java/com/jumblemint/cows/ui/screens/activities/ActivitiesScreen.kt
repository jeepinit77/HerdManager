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
import androidx.compose.material.icons.filled.Check
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
import java.time.LocalDate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.ui.draw.alpha

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

    val dateRangeFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
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
            } else if (uiState.activityGroups.isEmpty() && !showFilters && uiState.searchQuery.isBlank() && !hasActiveFilters(uiState)) {
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
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Search activities...", color = MaterialTheme.colorScheme.onBackground) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )
                        FilterChip(
                            onClick = { showFilters = !showFilters },
                            label = {
                                val activeFilterCount = getActiveFilterCount(uiState)
                                if (activeFilterCount > 0) {
                                    Text("($activeFilterCount)", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                                Icon(Icons.Default.Clear, contentDescription = "Clear All Filters", tint = MaterialTheme.colorScheme.onBackground)
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
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        DateRangeFilterSection(
                                            currentRange = uiState.dateRange,
                                            onRangeSelected = { viewModel.updateDateRange(it) },
                                            formatter = dateRangeFormatter
                                        )

                                        ActivityTypeFilterSection(
                                            selectedTypes = uiState.selectedActivityTypes,
                                            onToggleType = { viewModel.toggleActivityTypeFilter(it) }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.activityGroups.isNotEmpty()) {
                            items(uiState.activityGroups, key = { it.sample.id }) { group ->
                                ActivityCard(
                                    activity = group.sample,
                                    cowNames = group.cowNames.filterNotNull(),
                                    onClick = { onActivityClick(group.sample.id) },
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
                        } else if (showFilters || uiState.searchQuery.isNotBlank() || hasActiveFilters(uiState)) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "No activities match your search.",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = {
                                            viewModel.clearAllFilters()
                                            showFilters = false
                                        }) {
                                            Text("Clear search & filters")
                                        }
                                    }
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
fun DateRangeFilterSection(
    currentRange: Pair<LocalDate, LocalDate>?,
    onRangeSelected: (Pair<LocalDate, LocalDate>?) -> Unit,
    formatter: DateTimeFormatter
) {
    Column {
        Text(
            text = "Date Range",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Quick select buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val today = LocalDate.now()
            val thisMonth = Pair(today.withDayOfMonth(1), today)
            val lastMonth = Pair(today.minusMonths(1).withDayOfMonth(1), today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth()))
            val thisYear = Pair(LocalDate.of(today.year, 1, 1), today)
            val lastYear = Pair(LocalDate.of(today.year - 1, 1, 1), LocalDate.of(today.year - 1, 12, 31))

            FilterChip(
                selected = currentRange == thisMonth,
                onClick = { onRangeSelected(thisMonth) },
                label = { Text("This Month") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = currentRange == lastMonth,
                onClick = { onRangeSelected(lastMonth) },
                label = { Text("Last Month") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = currentRange == thisYear,
                onClick = { onRangeSelected(thisYear) },
                label = { Text("This Year") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = currentRange == lastYear,
                onClick = { onRangeSelected(lastYear) },
                label = { Text("Last Year") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start date button
            DateSelectorButton(
                selectedDate = currentRange?.first,
                placeholderText = "Start date",
                onDateSelected = { newStartDate ->
                    if (newStartDate != null) {
                        val endDate = currentRange?.second ?: newStartDate
                        onRangeSelected(if (newStartDate.isBefore(endDate) || newStartDate.isEqual(endDate)) {
                            Pair(newStartDate, endDate)
                        } else {
                            Pair(newStartDate, newStartDate)
                        })
                    } else {
                        onRangeSelected(null)
                    }
                },
                formatter = formatter,
                modifier = Modifier.weight(1f)
            )

            // End date button
            DateSelectorButton(
                selectedDate = currentRange?.second,
                placeholderText = "End date",
                onDateSelected = { newEndDate ->
                    if (newEndDate != null) {
                        val startDate = currentRange?.first ?: newEndDate
                        onRangeSelected(if (startDate.isBefore(newEndDate) || startDate.isEqual(newEndDate)) {
                            Pair(startDate, newEndDate)
                        } else {
                            Pair(newEndDate, newEndDate)
                        })
                    } else {
                        onRangeSelected(null)
                    }
                },
                formatter = formatter,
                modifier = Modifier.weight(1f)
            )
        }

        // Clear button if range is set
        if (currentRange != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { onRangeSelected(null) }) {
                Text("Clear date range")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorButton(
    selectedDate: LocalDate?,
    placeholderText: String,
    onDateSelected: (LocalDate?) -> Unit,
    formatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier
    ) {
        Text(
            text = selectedDate?.format(formatter) ?: placeholderText,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        val localDate = java.time.Instant.ofEpochMilli(selectedDateMillis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(localDate)
                    } else {
                        onDateSelected(null)
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContentColor = MaterialTheme.colorScheme.primary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                )
            )
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
    return uiState.selectedActivityTypes.isNotEmpty() || uiState.dateRange != null
}

private fun getActiveFilterCount(uiState: com.jumblemint.cows.ui.viewmodel.ActivitiesUiState): Int {
    var count = 0
    if (uiState.selectedActivityTypes.isNotEmpty()) count++
    if (uiState.dateRange != null) count++
    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTypeFilterSection(
    selectedTypes: Set<ActivityType>,
    onToggleType: (ActivityType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Activity Type",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = {
                    // This button clears all selected types by toggling off the ones that are selected
                    selectedTypes.forEach { onToggleType(it) }
                },
                enabled = selectedTypes.isNotEmpty(),
                modifier = Modifier
                    .alpha(if (selectedTypes.isNotEmpty()) 1f else 0f)
                    .heightIn(min = 0.dp)
            ) {
                Text("Clear")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ActivityType.entries.toList()) { activityType ->
                val isSelected = selectedTypes.contains(activityType)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleType(activityType) },
                    label = { Text(activityType.displayName) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
    }
}
