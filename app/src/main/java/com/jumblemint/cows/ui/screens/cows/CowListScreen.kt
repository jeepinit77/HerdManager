package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// import androidx.compose.material.icons.Icons // Not needed if TopAppBar is removed
// import androidx.compose.material.icons.filled.ArrowBack // Not needed if TopAppBar is removed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.graphics.Color // Not directly used in this file anymore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CowCard
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.Period
import java.util.Locale

// @OptIn(ExperimentalMaterial3Api::class) // Not strictly needed if TopAppBar is removed
@Composable
fun CowListScreen(
    type: String?,
    value: String?,
    onCowClick: (Long) -> Unit,
    onBack: () -> Unit, // Still needed for MainActivity's TopAppBar
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val application = context.applicationContext as com.jumblemint.cows.CattleApplication
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

    val reportsViewModel: ReportsViewModel = viewModel(factory = ReportsViewModelFactory(repository, application.authService))
    val allCowsState by reportsViewModel.uiState.collectAsState()

    val cowsFlow by remember { mutableStateOf(repository.getAllCows()) }
    val cows by cowsFlow.collectAsState(initial = emptyList())
    val tagColorMap = rememberTagColorMap(repository)

    val list: List<Cow> = remember(cows, type, value) {
        val active = cows.filter { it.status == Status.ACTIVE }
        when (type) {
            "status" -> {
                when (value) {
                    null -> cows
                    "ACTIVE" -> active
                    "SOLD" -> cows.filter { it.status == Status.SOLD }
                    "DECEASED" -> cows.filter { it.status == Status.DECEASED }
                    else -> active
                }
            }
            "classification" -> active.filter { it.classification.name == value }
            "pasture" -> active.filter { it.pastureId == value }
            "pastureName" -> {
                if (value == "Unassigned") {
                    active.filter { it.pastureId == null }
                } else { // Assuming if pastureName is not "Unassigned", it's a specific (though currently unfilterable by name) pasture
                    active // This case might need refinement if direct name filtering is desired without ID
                }
            }
            "notCalved" -> {
                val nineMonthsAgo = LocalDate.now().minusMonths(9)
                val female = active.filter { it.gender == Gender.FEMALE && it.classification in listOf(Classification.COW, Classification.HEIFER) }
                val calvesInPast9 = cows.filter { it.classification == Classification.CALF && it.birthDate?.isAfter(nineMonthsAgo) == true && it.motherId != null }
                val mothers = calvesInPast9.mapNotNull { it.motherId }.toSet()
                female.filter { it.id !in mothers }
            }
            "calved" -> {
                val activeCalves = cows.filter { cow ->
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
            "watching" -> cows.filter { it.isWatched && it.status == Status.ACTIVE }
            else -> active
        }
    }

    var screenTitle by remember { mutableStateOf("Cows") }

    // This LaunchedEffect sets the screenTitle.
    // MainActivity's TopAppBarWithMenu would need to observe this or a similar state.
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
                    val pasture = repository.getPastureByIdSuspend(value) // Assumes value is pasture ID
                    newTitle = pasture?.name?.let { "Pasture: $it" } ?: "Pasture Details"
                } else {
                    newTitle = "Cows by Pasture"
                }
            }
            "pastureName" -> { // This case might need a lookup if 'value' is a name that needs to resolve to an ID for filtering
                newTitle = if (value == "Unassigned") {
                    "Unassigned Cows"
                } else {
                    value?.let { "Pasture: $it" } ?: "Cows by Pasture"
                }
            }
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
        // TODO: Update MainActivity's TopAppBar title state here or via a shared ViewModel / callback
    }

    if (allCowsState.isLoading && list.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (list.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No cows match the criteria.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.id }) { cow ->
                CowCard(
                    cow = cow,
                    onClick = { onCowClick(cow.id) },
                    resolvedTagColor = resolveTagColor(cow.tagColor, tagColorMap)
                )
            }
        }
    }
}

private fun filterByAgeGroup(active: List<Cow>, value: String?): List<Cow> {
    val today = LocalDate.now()
    return active.filter { cow ->
        cow.birthDate?.let { bd ->
            val years = Period.between(bd, today).years
            when (value) {
                "UNDER_1" -> years < 1
                "1_5" -> years in 1..4 // Corrected range to 1-4 for "1-5 years" assuming 5 is exclusive upper bound of a category
                "5_10" -> years in 5..9 // Corrected range to 5-9
                "10_PLUS" -> years >= 10
                else -> true // Should not happen if value is one of the defined keys
            }
        } ?: false // Cows without a birthdate are excluded from age filters
    }
}
