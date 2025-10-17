package com.jumblemint.cows.ui.screens.activities

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Activity
import com.jumblemint.cows.data.model.ActivityType
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModel
import com.jumblemint.cows.ui.viewmodel.ActivitiesViewModelFactory
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.theme.SmartText
import com.jumblemint.cows.ui.theme.contrastingTextColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onAddActivityClick: () -> Unit = {},
    onEditActivityClick: (Activity) -> Unit = {},
    onActivityClick: (Long) -> Unit = {},
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
            database.breedDao()
        )
    }
    val viewModel: ActivitiesViewModel = viewModel(
        factory = ActivitiesViewModelFactory(context.applicationContext as Application, repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val globalSnackbarState = com.jumblemint.cows.ui.components.LocalGlobalSnackbarState.current
    val scope = rememberCoroutineScope()

    // dialog open/close
    var showFilterDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.activityGroups.isEmpty() && !showFilterDialog && uiState.searchQuery.isBlank() && !hasActiveFilters(uiState)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing here yet", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Add activities using the + button to get started", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {

                    // ── Search + Filter bar (matches CowList; full width; aligned) ─────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text("Search Activities...",
                                    softWrap = false)
                                          },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 56.dp)
                                .alignBy { it.measuredHeight / 2 },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedLabelColor = MaterialTheme.colorScheme.background.contrastingTextColor().copy(alpha = 0.6f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLeadingIconColor = MaterialTheme.colorScheme.background.contrastingTextColor().copy(alpha = 0.6f),
                                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.background.contrastingTextColor().copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        val activeFilterCount = getActiveFilterCount(uiState)
                        FilterChip(
                            onClick = { showFilterDialog = true },
                            label = { if (activeFilterCount > 0) Text("($activeFilterCount)") else Text("Filters") },
                            selected = hasActiveFilters(uiState),
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 56.dp)
                                .alignBy { it.measuredHeight / 2 },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = MaterialTheme.colorScheme.background.contrastingTextColor(),
                                iconColor = MaterialTheme.colorScheme.background.contrastingTextColor(),
                                selectedLabelColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor(),
                                selectedLeadingIconColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor(),
                                containerColor = MaterialTheme.colorScheme.background,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.12f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = hasActiveFilters(uiState),
                                borderColor = MaterialTheme.colorScheme.background.contrastingTextColor().copy(alpha = 0.3f),
                                selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor().copy(alpha = 0.3f)
                            )
                        )

                        if (hasActiveFilters(uiState) || uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.clearAllFilters()
                                viewModel.updateSearchQuery("")
                                showFilterDialog = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear All Filters and Search", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                    // ───────────────────────────────────────────────────────────────────────────

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        if (uiState.activityGroups.isNotEmpty()) {
                            items(uiState.activityGroups, key = { it.sample.id }) { group ->
                                ActivityCard(
                                    activity = group.sample,
                                    cowNames = group.cowNames.filterNotNull(),
                                    onClick = { onActivityClick(group.sample.id) },
                                    onEdit = { onEditActivityClick(group.sample) },
                                    onDelete = {
                                        scope.launch {
                                            val acts = group.activities
                                            viewModel.deleteActivities(acts)
                                            val res = globalSnackbarState?.showSnackbar(
                                                message = "Activity deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Long
                                            )
                                            if (res == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDeleteActivities(acts)
                                            }
                                        }
                                    }
                                )
                            }
                        } else if (uiState.searchQuery.isNotBlank() || hasActiveFilters(uiState)) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("No activities match your search.", style = MaterialTheme.typography.bodyLarge)
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(onClick = {
                                            viewModel.clearAllFilters()
                                            viewModel.updateSearchQuery("")
                                            showFilterDialog = false
                                        }) { Text("Clear search & filters") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB
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

    // ===== FILTER DIALOG (styled like AnimalFilterScreen) =====
    if (showFilterDialog) {
        ActivityFilterDialog(
            initialRange = uiState.dateRange,
            initialTypes = uiState.selectedActivityTypes,
            onApply = { newRange, newTypes ->
                // Apply date range
                viewModel.updateDateRange(newRange)
                // Sync activity types via diff
                val current = uiState.selectedActivityTypes
                (current - newTypes).forEach { viewModel.toggleActivityTypeFilter(it) } // remove
                (newTypes - current).forEach { viewModel.toggleActivityTypeFilter(it) } // add
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ActivityFilterDialog(
    initialRange: Pair<LocalDate, LocalDate>?,
    initialTypes: Set<ActivityType>,
    onApply: (Pair<LocalDate, LocalDate>?, Set<ActivityType>) -> Unit,
    onDismiss: () -> Unit
) {
    // Match AnimalFilterScreen sizing & structure
    val dialogContentPadding = 16.dp
    var range by remember { mutableStateOf(initialRange) }
    var types by remember { mutableStateOf(initialTypes) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(dialogContentPadding)) {
                // Title centered with Close icon on right (like AnimalFilterScreen)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Filter Activities",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Filters")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Date Range (mirrors your animal filter quick-picks + selectors)
                    DateRangeSection(
                        currentRange = range,
                        onRangeSelected = { range = it }
                    )

                    // Activity Type section (FlowRow wrap, Clear button like your groups)
                    ActivityTypeSection(
                        selectedTypes = types,
                        onToggleType = { t ->
                            types = if (types.contains(t)) types - t else types + t
                        },
                        onClear = { types = emptySet() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Buttons row (Clear All / Apply) – same vibe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            range = null
                            types = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear All") }

                    Button(
                        onClick = { onApply(range, types) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply Filters") }
                }
            }
        }
    }
}

/* ---------- Sections used inside the dialog (styled to match your AnimalFilterScreen) ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSection(
    currentRange: Pair<LocalDate, LocalDate>?,
    onRangeSelected: (Pair<LocalDate, LocalDate>?) -> Unit
) {
    Column {
        Text(
            text = "Date Range",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))

        // Quick picks row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val today = LocalDate.now()
            val thisMonth = Pair(today.withDayOfMonth(1), today)
            val lastMonthStart = today.minusMonths(1).withDayOfMonth(1)
            val lastMonthEnd = today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth())
            val lastMonth = Pair(lastMonthStart, lastMonthEnd)
            val thisYear = Pair(LocalDate.of(today.year, 1, 1), today)
            val lastYear = Pair(LocalDate.of(today.year - 1, 1, 1), LocalDate.of(today.year - 1, 12, 31))

            FilterChip(selected = currentRange == thisMonth, onClick = { onRangeSelected(thisMonth) }, label = { Text("This Month") }, modifier = Modifier.weight(1f))
            FilterChip(selected = currentRange == lastMonth, onClick = { onRangeSelected(lastMonth) }, label = { Text("Last Month") }, modifier = Modifier.weight(1f))
            FilterChip(selected = currentRange == thisYear, onClick = { onRangeSelected(thisYear) }, label = { Text("This Year") }, modifier = Modifier.weight(1f))
            FilterChip(selected = currentRange == lastYear, onClick = { onRangeSelected(lastYear) }, label = { Text("Last Year") }, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateSelectorButton(
                selectedDate = currentRange?.first,
                placeholderText = "Start date",
                onDateSelected = { newStart ->
                    if (newStart != null) {
                        val end = currentRange?.second ?: newStart
                        onRangeSelected(if (!newStart.isAfter(end)) Pair(newStart, end) else Pair(newStart, newStart))
                    } else onRangeSelected(null)
                },
                formatter = formatter,
                modifier = Modifier.weight(1f)
            )
            DateSelectorButton(
                selectedDate = currentRange?.second,
                placeholderText = "End date",
                onDateSelected = { newEnd ->
                    if (newEnd != null) {
                        val start = currentRange?.first ?: newEnd
                        onRangeSelected(if (!start.isAfter(newEnd)) Pair(start, newEnd) else Pair(newEnd, newEnd))
                    } else onRangeSelected(null)
                },
                formatter = formatter,
                modifier = Modifier.weight(1f)
            )
        }

        if (currentRange != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onRangeSelected(null) }) { Text("Clear date range") }
        }

        Divider(
            modifier = Modifier
                .padding(top = 1.dp)
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTypeSection(
    selectedTypes: Set<ActivityType>,
    onToggleType: (ActivityType) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) {
        Row(
            Modifier
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
                onClick = onClear,
                enabled = selectedTypes.isNotEmpty(),
                modifier = Modifier
                    .alpha(if (selectedTypes.isNotEmpty()) 1f else 0f)
                    .heightIn(min = 0.dp)
            ) { Text("Clear") }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ActivityType.entries.forEach { t ->
                val isSelected = selectedTypes.contains(t)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleType(t) },
                    label = { Text(t.displayName) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
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

        Divider(
            modifier = Modifier
                .padding(top = 1.dp)
                .fillMaxWidth()
        )
    }
}

/* ---------- Shared bits (date picker) ---------- */

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

    OutlinedButton(onClick = { showDatePicker = true }, modifier = modifier) {
        Text(text = selectedDate?.format(formatter) ?: placeholderText, style = MaterialTheme.typography.bodyMedium)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val localDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onDateSelected(localDate)
                    } else onDateSelected(null)
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
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
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = getCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
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
                            text = cowNames.joinToString(", "),
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
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Activity", tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor())
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Activity", tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor())
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
