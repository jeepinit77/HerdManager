package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CowCard
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel // Keep for now, might be useful for other filters
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.Period
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowListScreen(
    type: String?,
    value: String?,
    onCowClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
        CattleRepository(database.cowDao(), database.pastureDao(), database.activityDao(), database.settingsDao())
    }

    // ViewModel for all cows, potentially useful for some filter types if needed later
    val reportsViewModel: ReportsViewModel = viewModel(factory = ReportsViewModelFactory(repository))
    val allCowsState by reportsViewModel.uiState.collectAsState() // This contains all cows from the db

    // Pull full lists from repository for accurate filtering based on allCowsState or direct query
    val cowsFlow by remember { mutableStateOf(repository.getAllCows()) } // Or use allCowsState.cows if appropriate
    val cows by cowsFlow.collectAsState(initial = emptyList())

    // Build filtered list based on type/value
    val list: List<Cow> = remember(cows, type, value) {
        val active = cows.filter { it.status == Status.ACTIVE }
        when (type) {
            "status" -> {
                when (value) {
                    null -> cows // Show all cows if no specific status is requested
                    "ACTIVE" -> active
                    "SOLD" -> cows.filter { it.status == Status.SOLD }
                    "DECEASED" -> cows.filter { it.status == Status.DECEASED }
                    else -> active
                }
            }
            "classification" -> active.filter { it.classification.name == value }
            "pasture" -> { // FIX FOR ERROR 1 (around line 64)
                // it.pastureId is String?, value is String?
                active.filter { it.pastureId == value }
            }
            "pastureName" -> {
                if (value == "Unassigned") {
                    active.filter { it.pastureId == null }
                } else {
                    active
                }
            }
            "notCalved" -> {
                val nineMonthsAgo = LocalDate.now().minusMonths(9)
                val female = active.filter { it.gender == Gender.FEMALE && it.classification in listOf(Classification.COW, Classification.HEIFER) }
                // Consider if 'cows' (all statuses) or 'active' (active only) is correct for finding recent calves
                val calvesInPast9 = cows.filter { it.classification == Classification.CALF && it.birthDate?.isAfter(nineMonthsAgo) == true && it.motherId != null }
                val mothers = calvesInPast9.mapNotNull { it.motherId }.toSet()
                female.filter { it.id !in mothers }
            }
            "calved" -> {
                val nineMonthsAgo = LocalDate.now().minusMonths(9)
                val calvesInPast9 = cows.filter { it.classification == Classification.CALF && it.birthDate?.isAfter(nineMonthsAgo) == true && it.motherId != null }
                val mothers = calvesInPast9.mapNotNull { it.motherId }.toSet()
                active.filter { it.id in mothers }
            }
            "age" -> filterByAgeGroup(active, value)
            "watching" -> cows.filter { it.isWatched && it.status == Status.ACTIVE }
            else -> active
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
            "pasture" -> { // FIX FOR ERROR 2 (around line 112)
                // value is String? pasture ID. repository.getPastureById() expects String.
                if (value != null) {
                    val pasture = repository.getPastureById(value)
                    newTitle = pasture?.name?.let { "Pasture: $it" } ?: "Pasture Details"
                } else {
                    newTitle = "Cows by Pasture"
                }
            }
            "pastureName" -> {
                newTitle = if (value == "Unassigned") {
                    "Unassigned Cows"
                } else {
                    value?.let { "Pasture: $it" } ?: "Cows by Pasture"
                }
            }
            "notCalved" -> {
                newTitle = "Not Calved (9+ Months)"
            }
            "calved" -> {
                newTitle = "Calved (Past 9 Months)"
            }
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
            "watching" -> {
                newTitle = "Watched Cows"
            }
        }
        screenTitle = newTitle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back") 
                    }
                }
            )
        }
    ) { padding ->
        if (allCowsState.isLoading && list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { cow ->
                    CowCard(cow = cow, onClick = { onCowClick(cow.id) })
                }
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
                "1_5" -> years in 1..4
                "5_10" -> years in 5..9
                "10_PLUS" -> years >= 10
                else -> true
            }
        } ?: false
    }
}
