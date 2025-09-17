package com.jumblemint.cows.ui.screens.cows

import android.app.Application // Required for ViewModelFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// import androidx.compose.foundation.lazy.LazyRow // No longer needed by FilterSection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
// import androidx.compose.material.icons.filled.Check // No longer needed by FilterSection
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CowCard
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.components.FocusAwareLiveSync
import com.jumblemint.cows.ui.screens.AnimalFilterScreen // Import the new screen
import com.jumblemint.cows.ui.screens.AnimalFilterState // Import the state
import com.jumblemint.cows.ui.viewmodel.CowsViewModel
import com.jumblemint.cows.ui.viewmodel.CowsViewModelFactory
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate
import java.time.Period
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowListScreen(
    type: String? = null,
    value: String? = null,
    pastureId: Long? = null, // This might be String according to Cow.pastureId
    onCowClick: (Long) -> Unit,
    onCowEdit: ((Long) -> Unit)? = null,
    onAddCowClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    showSearchAndFilters: Boolean = true,
    showFab: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext as Application
    val cattleApplication = context.applicationContext as CattleApplication

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

    val cowsViewModel: CowsViewModel? = if (showSearchAndFilters) {
        viewModel(factory = CowsViewModelFactory(cattleApplication, repository))
    } else null
    
    val reportsViewModel: ReportsViewModel? = if (!showSearchAndFilters) {
        viewModel(factory = ReportsViewModelFactory(applicationContext, repository, cattleApplication.authService))
    } else null

    val cowsUiState = cowsViewModel?.uiState?.collectAsState()
    val reportsUiState = reportsViewModel?.uiState?.collectAsState()
    
    val cowsFlow by remember { mutableStateOf(repository.getAllCows()) }
    val allCows by cowsFlow.collectAsState(initial = emptyList())
    val tagColorMap = rememberTagColorMap(repository)
    
    var showAnimalFilterSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showSearchAndFilters) {
        FocusAwareLiveSync(
            orchestrator = cattleApplication.syncOrchestrator,
            screenKey = "Cows",
            intervalMs = 20_000L,
            leadingRun = true
        )
    }

    val list: List<Cow> = if (showSearchAndFilters) {
        cowsUiState?.value?.cows ?: emptyList()
    } else {
        remember(allCows, type, value) {
            val active = allCows.filter { it.status == Status.ACTIVE }
            when (type) {
                "status" -> {
                    when (value) {
                        null -> allCows
                        "ACTIVE" -> active
                        "SOLD" -> allCows.filter { it.status == Status.SOLD }
                        "DECEASED" -> allCows.filter { it.status == Status.DECEASED }
                        else -> active
                    }
                }
                "classification" -> active.filter { it.classification.name == value }
                "pasture" -> active.filter { it.pastureId == value } // Assuming value is String or null
                "pastureName" -> {
                    if (value == "Unassigned") {
                        active.filter { it.pastureId == null }
                    } else {
                        active // Needs refinement as in original
                    }
                }
                "unassigned" -> active.filter { it.pastureId == null }
                "notCalved" -> {
                    val nineMonthsAgo = LocalDate.now().minusMonths(9)
                    val female = active.filter { it.gender == Gender.FEMALE && it.classification in listOf(Classification.COW, Classification.HEIFER) }
                    val calvesInPast9 = allCows.filter { it.classification == Classification.CALF && it.birthDate?.isAfter(nineMonthsAgo) == true && it.motherId != null }
                    val mothers = calvesInPast9.mapNotNull { it.motherId }.toSet()
                    female.filter { it.id !in mothers }
                }
                "calved" -> {
                    val activeCalves = allCows.filter { cow ->
                        cow.classification == Classification.CALF &&
                        cow.status == Status.ACTIVE &&
                        cow.motherId != null
                    }
                    val mothersWithActiveCalves = activeCalves.mapNotNull { it.motherId }.toSet()
                    active.filter {
                        it.gender == Gender.FEMALE &&
                        it.classification in listOf(Classification.COW, Classification.HEIFER) &&
                        it.id in mothersWithActiveCalves
                    }
                }
                "age" -> filterByAgeGroup(active, value)
                "watching" -> allCows.filter { it.isWatched && it.status == Status.ACTIVE }
                else -> active
            }
        }
    }

    var screenTitle by remember { mutableStateOf("Cows") }

    LaunchedEffect(type, value, repository) {
        var newTitle = "Cows"
        when (type) {
            "status" -> {
                newTitle = value?.let {
                    val statusName = it.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                    "Status: $statusName"
                } ?: "All Cows"
            }
            "classification" -> {
                newTitle = value?.let { "Classification: $it" } ?: "Cows by Classification"
            }
            "pasture" -> {
                if (value != null) {
                     // Assuming value is pasture Name, not ID here for title consistency with pastureName type
                    newTitle = "Pasture: $value"
                } else {
                    newTitle = "Cows by Pasture"
                }
            }
            "pastureName" -> { 
                newTitle = if (value == "Unassigned") {
                    "Unassigned Animals"
                } else {
                    value?.let { "Pasture: $it" } ?: "Cows by Pasture"
                }
            }
            "unassigned" -> newTitle = "Unassigned Animals"
            "notCalved" -> newTitle = "Not Calved (9+ Months)"
            "calved" -> newTitle = "Cows with Active Calves"
            "age" -> {
                val ageDesc = when (value) {
                    "UNDER_1" -> "Under 1 Year"
                    "1_5" -> "1-5 Years"
                    "5_10" -> "5-10 Years"
                    "10_PLUS" -> "Over 10 Years"
                    else -> "by Age"
                }
                newTitle = "Age: $ageDesc"
            }
            "watching" -> newTitle = "Watched Cows"
        }
        screenTitle = newTitle
    }

    val isLoading = if (showSearchAndFilters) {
        cowsUiState?.value?.isLoading == true
    } else {
        reportsUiState?.value?.isLoading == true && list.isEmpty()
    }

    if (showSearchAndFilters && cowsViewModel != null && cowsUiState?.value != null) {
        if (showAnimalFilterSheet) {
            AnimalFilterScreen(
                initialFilterState = AnimalFilterState(
                    searchTerm = cowsUiState.value.searchQuery,
                    classifications = cowsUiState.value.selectedClassifications.toList(),
                    gender = cowsUiState.value.selectedGenders.firstOrNull(),
                    pasture = cowsUiState.value.selectedPastures.firstOrNull()
                ),
                availablePastures = cowsUiState.value.availablePastures,
                onApplyFilters = { newState ->
                    cowsViewModel.updateSearchQuery(newState.searchTerm)

                    val currentVmGenders = cowsUiState.value.selectedGenders
                    if (newState.gender == null) { // "Any Gender" selected
                        currentVmGenders.forEach { cowsViewModel.toggleGenderFilter(it) }
                    } else {
                        currentVmGenders.forEach { if (it != newState.gender) cowsViewModel.toggleGenderFilter(it) }
                        if (!currentVmGenders.contains(newState.gender)) {
                            newState.gender?.let { cowsViewModel.toggleGenderFilter(it) }
                        }
                    }

                    val currentVmClassifications = cowsUiState.value.selectedClassifications
                    newState.classifications.forEach { classif ->
                        if (!currentVmClassifications.contains(classif)) {
                            cowsViewModel.toggleClassificationFilter(classif)
                        }
                    }
                    currentVmClassifications.forEach { classif ->
                        if (!newState.classifications.contains(classif)) {
                            cowsViewModel.toggleClassificationFilter(classif)
                        }
                    }

                    val currentVmPastures = cowsUiState.value.selectedPastures
                    if (newState.pasture == null) { // "Any Pasture" selected
                        currentVmPastures.forEach { cowsViewModel.togglePastureFilter(it) }
                    } else {
                        currentVmPastures.forEach { if (it != newState.pasture) cowsViewModel.togglePastureFilter(it) }
                        if (!currentVmPastures.contains(newState.pasture)) {
                            newState.pasture?.let { cowsViewModel.togglePastureFilter(it) }
                        }
                    }
                    showAnimalFilterSheet = false
                },
                onDismiss = { showAnimalFilterSheet = false }
            )
        }
    }

    if (showSearchAndFilters && showFab) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (onAddCowClick != null) {
                    FloatingActionButton(
                        onClick = onAddCowClick
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Cow")
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            CowListContent(
                modifier = modifier.padding(paddingValues),
                showSearchAndFilters = showSearchAndFilters,
                onShowAnimalFilterSheet = { showAnimalFilterSheet = true }, // Updated here
                cowsUiState = cowsUiState?.value,
                cowsViewModel = cowsViewModel,
                list = list,
                isLoading = isLoading,
                onCowClick = onCowClick,
                onCowEdit = onCowEdit,
                tagColorMap = tagColorMap,
                scope = scope,
                snackbarHostState = snackbarHostState
            )
        }
    } else {
        CowListContent(
            modifier = modifier,
            showSearchAndFilters = showSearchAndFilters,
            onShowAnimalFilterSheet = { showAnimalFilterSheet = true }, // Updated here
            cowsUiState = cowsUiState?.value,
            cowsViewModel = cowsViewModel,
            list = list,
            isLoading = isLoading,
            onCowClick = onCowClick,
            onCowEdit = onCowEdit,
            tagColorMap = tagColorMap,
            scope = scope,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun CowListContent(
    modifier: Modifier,
    showSearchAndFilters: Boolean,
    onShowAnimalFilterSheet: () -> Unit, // Changed from onShowFiltersChange and showFilters
    cowsUiState: com.jumblemint.cows.ui.viewmodel.CowsUiState?,
    cowsViewModel: CowsViewModel?,
    list: List<Cow>,
    isLoading: Boolean,
    onCowClick: (Long) -> Unit,
    onCowEdit: ((Long) -> Unit)?,
    tagColorMap: Map<String, androidx.compose.ui.graphics.Color>,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (showSearchAndFilters && cowsUiState != null && cowsViewModel != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), // Added padding for the row
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cowsUiState.searchQuery,
                    onValueChange = { cowsViewModel.updateSearchQuery(it) },
                    label = { Text("Search cows...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    onClick = { onShowAnimalFilterSheet() }, // Changed here
                    label = {
                        val activeFilterCount = getActiveFilterCount(cowsUiState) // This function should still work
                        if (activeFilterCount > 0) {
                            Text("($activeFilterCount)")
                        } else {
                            Text("Filters") // Default text when no filters active
                        }
                    },
                    selected = hasActiveFilters(cowsUiState), // This function should still work
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                )
                if (hasActiveFilters(cowsUiState)) {
                    IconButton(onClick = { cowsViewModel.clearAllFilters() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Filters")
                    }
                }
            }
            // Removed the old inline filter Card and FilterSection calls
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showSearchAndFilters) "Nothing here yet" else "No cows match the criteria.", style = MaterialTheme.typography.headlineSmall)
                    if (showSearchAndFilters) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add cows using the + button to get started", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (showSearchAndFilters) 8.dp else 12.dp)
            ) {
                items(list, key = { it.id }) { cow ->
                    CowCard(
                        cow = cow,
                        onClick = { onCowClick(cow.id) },
                        onToggleWatch = if (showSearchAndFilters && cowsViewModel != null) {{ cowsViewModel.toggleWatch(cow) }} else null,
                        onEdit = onCowEdit?.let { { onCowEdit(cow.id) } },
                        onDelete = if (showSearchAndFilters && cowsViewModel != null) {{
                            scope.launch {
                                cowsViewModel.deleteCow(cow)
                                val res = snackbarHostState.showSnackbar(
                                    message = "Cow deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Long
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    cowsViewModel.undoDeleteCow(cow)
                                }
                            }
                        }} else null,
                        resolvedTagColor = resolveTagColor(cow.tagColor, tagColorMap)
                    )
                }
            }
        }
    }
}

// hasActiveFilters and getActiveFilterCount should still work as they reflect the ViewModel's state.
private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.CowsUiState): Boolean {
    // Consider if default status filter (ACTIVE) should count if user hasn't explicitly opened filters.
    // For now, matching original logic.
    val hasNonDefaultStatus = uiState.selectedStatuses.isNotEmpty() && (uiState.selectedStatuses.size > 1 || !uiState.selectedStatuses.contains(Status.ACTIVE))
    return hasNonDefaultStatus || uiState.selectedClassifications.isNotEmpty() || uiState.selectedGenders.isNotEmpty() || uiState.selectedPastures.isNotEmpty() || uiState.searchQuery.isNotBlank()
}

private fun getActiveFilterCount(uiState: com.jumblemint.cows.ui.viewmodel.CowsUiState): Int {
    var count = 0
    // Count status if it's not just 'ACTIVE' or if it's empty (meaning user cleared it, which is a filter action)
    if (uiState.selectedStatuses.isNotEmpty() && (uiState.selectedStatuses.size > 1 || !uiState.selectedStatuses.contains(Status.ACTIVE))) {
        count++
    }
    if (uiState.selectedClassifications.isNotEmpty()) count++
    if (uiState.selectedGenders.isNotEmpty()) count++
    if (uiState.selectedPastures.isNotEmpty()) count++
    if (uiState.searchQuery.isNotBlank()) count++
    return count
}

// FilterSection composable is no longer used here.

private fun filterByAgeGroup(active: List<Cow>, value: String?): List<Cow> {
    val today = LocalDate.now()
    return active.filter { cow ->
        cow.birthDate?.let { bd ->
            val years = Period.between(bd, today).years
            when (value) {
                "UNDER_1" -> years < 1
                "1_5" -> years in 1..4
                "5_10" -> years in 5..9
                "10_PLUS" -> years >= 10
                else -> true
            }
        } ?: false
    }
}
