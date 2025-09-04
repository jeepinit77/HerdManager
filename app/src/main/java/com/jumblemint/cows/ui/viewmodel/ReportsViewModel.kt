package com.jumblemint.cows.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine // Ensure combine is imported
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

class ReportsViewModel(
    private val repository: CattleRepository,
    private val authService: com.jumblemint.cows.auth.AuthService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
    
    init {
        loadReports()
    }
    
    private fun loadReports() {
        viewModelScope.launch {
            // Simplified: just get all data without herd filtering
            combine(
                authService.currentUser,
                repository.getAllCows(),
                repository.getAllPastures(),
                repository.getAllActivities(),
                repository.getWatchedCows()
            ) { currentUser, allCows, pastures, activities, watchedCows ->
                ReportsData(currentUser, allCows, activities, pastures, watchedCows)
            }.collect { data ->
                val currentUser = data.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@collect
                }
                
                val allCows = data.cows
                val activities = data.activities
                val pastures = data.pastures
                val watchedCows = data.watchedCows
                
                val activeCows = allCows.filter { it.status == Status.ACTIVE }
                val soldCows = allCows.filter { it.status == Status.SOLD }
                val deceasedCows = allCows.filter { it.status == Status.DECEASED }

                // Classification breakdown
                val classificationBreakdown = activeCows.groupBy { it.classification.name }
                    .mapValues { it.value.size }

                // Age-based breakdown
                val today = LocalDate.now()
                var cowsUnder1Year = 0
                var cowsBetween1And5Years = 0
                var cowsBetween5And10Years = 0
                var cowsOver10Years = 0

                activeCows.forEach { cow ->
                    cow.birthDate?.let { birthDate ->
                        val age = Period.between(birthDate, today)
                        when {
                            age.years < 1 -> cowsUnder1Year++
                            age.years in 1..4 -> cowsBetween1And5Years++
                            age.years in 5..9 -> cowsBetween5And10Years++
                            age.years >= 10 -> cowsOver10Years++
                        }
                    }
                }

                // Pasture breakdown computed from cows and pastures snapshot
                val pastureNameById = pastures.associate { it.id to it.name }
                val cowsByPastureId = activeCows.groupBy { it.pastureId }
                val pastureBreakdown = mutableMapOf<String, Int>()
                cowsByPastureId.forEach { (pastureId, cowsInPasture) ->
                    val name = pastureId?.let { pastureNameById[it] } ?: "Unassigned"
                    pastureBreakdown[name] = cowsInPasture.size
                }

                // Breeding: cows with calves (not sold or weaned)
                val femaleCows = activeCows.filter {
                    it.gender == Gender.FEMALE &&
                    it.classification in listOf(Classification.COW, Classification.HEIFER)
                }
                
                // Find cows that have active calves (not sold, not weaned)
                val activeCalves = allCows.filter { cow ->
                    cow.classification == Classification.CALF &&
                    cow.status == Status.ACTIVE &&
                    cow.motherId != null
                }
                val mothersWithActiveCalves = activeCalves.mapNotNull { it.motherId }.toSet()
                val cowsWithCalves = femaleCows.count { it.id in mothersWithActiveCalves }
                
                // Cows not calved in 9+ months (existing logic)
                val nineMonthsAgo = today.minusMonths(9)
                val calvesInPast9Months = allCows.filter { cow ->
                    cow.classification == Classification.CALF &&
                    cow.birthDate?.isAfter(nineMonthsAgo) == true &&
                    cow.motherId != null
                }
                val mothersWhoCalvedIn9Months = calvesInPast9Months.mapNotNull { it.motherId }.toSet()
                val cowsNotCalvedIn9Months = femaleCows.count { it.id !in mothersWhoCalvedIn9Months }

                _uiState.value = _uiState.value.copy(
                    totalCows = activeCows.size, // Changed to show only active cattle
                    activeCows = activeCows.size,
                    soldCows = soldCows.size,
                    deceasedCows = deceasedCows.size,
                    watchedCowsCount = watchedCows.size, // Added watched cows count
                    classificationBreakdown = classificationBreakdown,
                    pastureBreakdown = pastureBreakdown,
                    cowsUnder1Year = cowsUnder1Year,
                    cowsBetween1And5Years = cowsBetween1And5Years,
                    cowsBetween5And10Years = cowsBetween5And10Years,
                    cowsOver10Years = cowsOver10Years,
                    cowsNotCalvedIn9Months = cowsNotCalvedIn9Months,
                    cowsCalvedInPast9Months = cowsWithCalves,
                    isLoading = false
                )
            }
        }
    }
}

// Helper data class for combine
data class ReportsData(val currentUser: com.jumblemint.cows.data.model.User?, val cows: List<Cow>, val activities: List<Activity>, val pastures: List<Pasture>, val watchedCows: List<Cow>)

data class ReportsUiState(
    val totalCows: Int = 0,
    val activeCows: Int = 0,
    val soldCows: Int = 0,
    val deceasedCows: Int = 0,
    val watchedCowsCount: Int = 0, // Added watched cows count
    val classificationBreakdown: Map<String, Int> = emptyMap(),
    val pastureBreakdown: Map<String, Int> = emptyMap(),
    val cowsUnder1Year: Int = 0,
    val cowsBetween1And5Years: Int = 0,
    val cowsBetween5And10Years: Int = 0,
    val cowsOver10Years: Int = 0,
    val cowsNotCalvedIn9Months: Int = 0,
    val cowsCalvedInPast9Months: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)