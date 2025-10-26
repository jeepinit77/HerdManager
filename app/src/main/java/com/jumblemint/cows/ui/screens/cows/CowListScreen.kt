package com.jumblemint.cows.ui.screens.cows

import android.app.Application // Required for ViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.jumblemint.cows.ui.components.LocalGlobalSnackbarState
import com.jumblemint.cows.ui.components.AnimalFilterScreen
import com.jumblemint.cows.ui.components.AnimalFilterState
import com.jumblemint.cows.util.AgeUtils // Centralized
import com.jumblemint.cows.ui.viewmodel.CowsViewModel
import com.jumblemint.cows.ui.viewmodel.CowsViewModelFactory
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory
import com.jumblemint.cows.ui.theme.contrastingTextColor
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import java.time.LocalDate
// Period is not directly used here anymore, AgeUtils handles it.
import java.util.Locale

private fun Cow.isUnassignedPasture(): Boolean = pastureId.isNullOrBlank()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowListScreen(
    type: String? = null,
    value: String? = null,
    pastureId: String? = null,
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
    val identifierModeFlow by repository.getAnimalIdentifierModeFlow().collectAsState(initial = AnimalIdentifierMode.BOTH)
    val identifierMode = cowsUiState?.value?.identifierMode ?: identifierModeFlow

    var showAnimalFilterDialog by remember { mutableStateOf(false) }
    val globalSnackbarState = LocalGlobalSnackbarState.current
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
            val today = LocalDate.now()
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
                "pasture" -> {
                    val targetPastureId = value?.takeUnless { it.isBlank() }
                    active.filter { cow -> cow.pastureId == targetPastureId }
                }
                "pastureName" -> {
                    if (value == "Unassigned") {
                        active.filter { it.isUnassignedPasture() }
                    } else {
                        active
                    }
                }
                "unassigned" -> active.filter { it.isUnassignedPasture() }
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
                "age" -> value?.let { ageKey -> active.filter { AgeUtils.cowMatchesAgeRangeKey(it, ageKey, today) } } ?: active // Use AgeUtils
                "watching" -> allCows.filter { it.isWatched && it.status == Status.ACTIVE }
                else -> active
            }
        }
    }

    var screenTitle by remember { mutableStateOf("Cattle") }

    LaunchedEffect(type, value) { // Removed repository dependency as it's not used for title
        var newTitle = "Cattle"
        when (type) {
            "status" -> {
                newTitle = value?.let {
                    val statusName = it.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                    "Status: $statusName"
                } ?: "All Cattle"
            }
            "classification" -> {
                newTitle = value?.let { "Classification: $it" } ?: "Cattle by Classification"
            }
            "pasture" -> {
                newTitle = value?.let { "Pasture: $it" } ?: "Cattle by Pasture"
            }
            "pastureName" -> {
                newTitle = if (value == "Unassigned") {
                    "Unassigned Animals"
                } else {
                    value?.let { "Pasture: $it" } ?: "Cattle by Pasture"
                }
            }
            "unassigned" -> newTitle = "Unassigned Animals"
            "notCalved" -> newTitle = "Not Calved (9+ Months)"
            "calved" -> newTitle = "Cattle with Active Calves"
            "age" -> {
                newTitle = "Age: ${AgeUtils.getLabel(value)}" // Use AgeUtils for label
            }
            "watching" -> newTitle = "Watched Cattle"
        }
        screenTitle = newTitle
    }

    val isLoading = if (showSearchAndFilters) {
        cowsUiState?.value?.isLoading == true
    } else {
        reportsUiState?.value?.isLoading == true && list.isEmpty()
    }

    if (showSearchAndFilters && cowsViewModel != null && cowsUiState?.value != null) {
        val currentUiState = cowsUiState.value
        if (showAnimalFilterDialog) {
            AnimalFilterScreen(
                initialFilterState = AnimalFilterState(
                    classifications = currentUiState.selectedClassifications.toList(),
                    genders = currentUiState.selectedGenders.toList(),
                    pastures = currentUiState.selectedPastures.toList(),
                    breeds = currentUiState.selectedBreeds.toList(),
                    statuses = currentUiState.selectedStatuses.toList(),
                    tagColors = currentUiState.selectedTagColors.toList(),
                    isWatched = currentUiState.selectedIsWatched,
                    selectedAgeRanges = currentUiState.selectedAgeRanges.toList()
                ),
                availablePastures = currentUiState.availablePastures,
                availableBreeds = currentUiState.availableBreeds,
                availableTagColors = currentUiState.availableTagColors,
                onApplyFilters = { newState ->
                    Status.entries.forEach { status ->
                        val shouldBeSelected = newState.statuses.contains(status)
                        if (currentUiState.selectedStatuses.contains(status) != shouldBeSelected) {
                            cowsViewModel.toggleStatusFilter(status)
                        }
                    }
                    Gender.entries.forEach { gender ->
                        val shouldBeSelected = newState.genders.contains(gender)
                        if (currentUiState.selectedGenders.contains(gender) != shouldBeSelected) {
                            cowsViewModel.toggleGenderFilter(gender)
                        }
                    }
                    Classification.entries.forEach { classification ->
                        val shouldBeSelected = newState.classifications.contains(classification)
                        if (currentUiState.selectedClassifications.contains(classification) != shouldBeSelected) {
                            cowsViewModel.toggleClassificationFilter(classification)
                        }
                    }
                    (currentUiState.availablePastures + newState.pastures).distinct().forEach { pasture ->
                        val shouldBeSelected = newState.pastures.contains(pasture)
                        if (currentUiState.selectedPastures.contains(pasture) != shouldBeSelected) {
                            cowsViewModel.togglePastureFilter(pasture)
                        }
                    }
                    (currentUiState.availableBreeds + newState.breeds).distinct().forEach { breed ->
                        val shouldBeSelected = newState.breeds.contains(breed)
                        if (currentUiState.selectedBreeds.contains(breed) != shouldBeSelected) {
                            cowsViewModel.toggleBreedFilter(breed)
                        }
                    }
                    (currentUiState.availableTagColors + newState.tagColors).distinct().forEach { tagColor ->
                        val shouldBeSelected = newState.tagColors.contains(tagColor)
                        if (currentUiState.selectedTagColors.contains(tagColor) != shouldBeSelected) {
                            cowsViewModel.toggleTagColorFilter(tagColor)
                        }
                    }
                    if (currentUiState.selectedIsWatched != newState.isWatched) {
                        cowsViewModel.setWatchedFilter(newState.isWatched)
                    }
                    // Update Age Ranges - uses the centralized AgeRangeKeys
                    AgeUtils.ageRanges.map { it.key }.forEach { ageRangeKey -> // Iterate through all possible keys
                        val shouldBeSelected = newState.selectedAgeRanges.contains(ageRangeKey)
                        if (currentUiState.selectedAgeRanges.contains(ageRangeKey) != shouldBeSelected) {
                            cowsViewModel.toggleAgeRangeFilter(ageRangeKey)
                        }
                    }

                    showAnimalFilterDialog = false
                },
                onDismiss = { showAnimalFilterDialog = false },
                viewModel = cowsViewModel
            )
        }
    }

    Box(modifier = modifier.padding(horizontal = if (showSearchAndFilters) 0.dp else 8.dp)) { // Add padding only when not in main pager
        CowListContent(
            modifier = Modifier.fillMaxSize(), // CowListContent will fill this Box
            showSearchAndFilters = showSearchAndFilters,
            onShowAnimalFilterDialog = { showAnimalFilterDialog = true },
            cowsUiState = cowsUiState?.value,
            cowsViewModel = cowsViewModel,
            list = list,
            isLoading = isLoading,
            onCowClick = onCowClick,
            onCowEdit = onCowEdit,
            tagColorMap = tagColorMap,
            scope = scope,
            globalSnackbarState = globalSnackbarState, // Pass global snackbar state
            identifierMode = identifierMode
        )

        // Conditionally display FAB and SnackbarHost if they are part of this screen's features
        if (showSearchAndFilters && showFab) {
            if (onAddCowClick != null) {
                FloatingActionButton(
                    onClick = onAddCowClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Animal")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CowListContent(
    modifier: Modifier,
    showSearchAndFilters: Boolean,
    onShowAnimalFilterDialog: () -> Unit,
    cowsUiState: com.jumblemint.cows.ui.viewmodel.CowsUiState?,
    cowsViewModel: CowsViewModel?,
    list: List<Cow>,
    isLoading: Boolean,
    onCowClick: (Long) -> Unit,
    onCowEdit: ((Long) -> Unit)?,
    tagColorMap: Map<String, androidx.compose.ui.graphics.Color>,
    scope: kotlinx.coroutines.CoroutineScope,
    globalSnackbarState: com.jumblemint.cows.ui.components.GlobalSnackbarState?,
    identifierMode: AnimalIdentifierMode
) {
    Column(
        modifier = modifier // This modifier is Modifier.fillMaxSize() from the Box above
    ) {
        if (showSearchAndFilters && cowsUiState != null && cowsViewModel != null) {

            // ---- Search + Filters (stable, theme-friendly, aligned) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cowsUiState.searchQuery,
                    onValueChange = { cowsViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search cattle...") }, // <<-- use placeholder not label
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .alignBy { it.measuredHeight / 2 }, // center-align with chip
                    colors = defaultOutlinedTextFieldColors()
                )

                val activeFilterCount = getActiveFilterCount(cowsUiState)
                FilterChip(
                    onClick = { onShowAnimalFilterDialog() },
                    label = {
                        if (activeFilterCount > 0) {
                            Text("($activeFilterCount)")
                        } else {
                            Text("Filters")
                        }
                    },
                    selected = hasActiveFilters(cowsUiState),
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
                        selected = hasActiveFilters(cowsUiState),
                        borderColor = MaterialTheme.colorScheme.background.contrastingTextColor().copy(alpha = 0.3f),
                        selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer.contrastingTextColor().copy(alpha = 0.3f)
                    )
                )

                if (hasActiveFilters(cowsUiState) || cowsUiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        cowsViewModel.clearAllFilters()
                        cowsViewModel.updateSearchQuery("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear all filters and search")
                    }
                }
            }
            // ---- End search row ----
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showSearchAndFilters) "Nothing here yet" else "No cattle match the criteria.", style = MaterialTheme.typography.headlineSmall)
                    if (showSearchAndFilters) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add cattle using the + button to get started", style = MaterialTheme.typography.bodyMedium)
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
                        identifierMode = identifierMode,
                        onClick = { onCowClick(cow.id) },
                        onToggleWatch = if (showSearchAndFilters && cowsViewModel != null) {{ cowsViewModel.toggleWatch(cow) }} else null,
                        onEdit = onCowEdit?.let { { onCowEdit(cow.id) } },
                        onDelete = if (showSearchAndFilters && cowsViewModel != null && globalSnackbarState != null) {{
                            scope.launch {
                                cowsViewModel.deleteCow(cow)
                                val res = globalSnackbarState.showSnackbar(
                                    message = "Animal deleted",
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

private fun hasActiveFilters(uiState: com.jumblemint.cows.ui.viewmodel.CowsUiState): Boolean {
    return uiState.selectedStatuses.isNotEmpty() ||
            uiState.selectedClassifications.isNotEmpty() ||
            uiState.selectedGenders.isNotEmpty() ||
            uiState.selectedPastures.isNotEmpty() ||
            uiState.selectedBreeds.isNotEmpty() ||
            uiState.selectedTagColors.isNotEmpty() ||
            uiState.selectedIsWatched != null ||
            uiState.selectedAgeRanges.isNotEmpty()
}

private fun getActiveFilterCount(uiState: com.jumblemint.cows.ui.viewmodel.CowsUiState): Int {
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

// filterByAgeGroup is no longer needed here as the logic is now within the main list generation for `type == "age"` using AgeUtils
