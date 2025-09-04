package com.jumblemint.cows.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Added for WatchingCard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onShowList: (type: String, value: String?) -> Unit,
    onNavigateToAddBirth: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as com.jumblemint.cows.CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao(),
        database.noteDao()
    )
    val viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(repository, application.authService)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val pasturesFlow = remember { repository.getAllPastures() }
    val pastures by pasturesFlow.collectAsState(initial = emptyList())
    val pastureIdByName = remember(pastures) { pastures.associate { it.name to it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Herd Overview (with Watching included)
                item {
                    HerdOverviewCard(
                        totalCows = uiState.totalCows,
                        watchedCows = uiState.watchedCowsCount,
                        onClick = { type -> onShowList("status", type) },
                        onWatchingClick = { onShowList("watching", null) }
                    )
                }

                // Tools Section
                item {
                    ToolsCard(
                        onAddCalfClick = onNavigateToAddBirth,
                        onWorkingListClick = { onShowList("workingList", null) }
                    )
                }
                
                // Classification Breakdown
                item {
                    ClassificationBreakdownCard(
                        classifications = uiState.classificationBreakdown,
                        onRowClick = { classification ->
                            onShowList("classification", classification)
                        }
                    )
                }
                
                // Pasture Breakdown
                item {
                    PastureBreakdownCard(
                        pastures = uiState.pastureBreakdown,
                        onRowClick = { pastureName ->
                            val id = pastureIdByName[pastureName]
                            if (id != null) onShowList("pasture", id.toString()) else onShowList("pastureName", pastureName)
                        }
                    )
                }
                
                // Age-based Reports
                item {
                    AgeBasedReportsCard(
                        under1Year = uiState.cowsUnder1Year,
                        between1And5Years = uiState.cowsBetween1And5Years,
                        between5And10Years = uiState.cowsBetween5And10Years,
                        over10Years = uiState.cowsOver10Years,
                        onRowClick = { rangeKey -> onShowList("age", rangeKey) }
                    )
                }
                
                // Breeding Reports
                item {
                    BreedingReportsCard(
                        cowsNotCalvedIn9Months = uiState.cowsNotCalvedIn9Months,
                        cowsCalvedIn9Months = uiState.cowsCalvedInPast9Months, // Corrected name, uncommented
                        onNotCalvedClick = { onShowList("notCalved", null) },   // Uncommented
                        onCalvedClick = { onShowList("calved", null) }          // Uncommented
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsCard(
    onAddCalfClick: () -> Unit,
    onWorkingListClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolItem(
                    icon = Icons.Default.Add,
                    label = "Add Calf",
                    onClick = onAddCalfClick
                )
                ToolItem(
                    icon = Icons.Default.List,
                    label = "Working List",
                    onClick = onWorkingListClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerdOverviewCard(
    totalCows: Int,
    watchedCows: Int,
    onClick: (type: String?) -> Unit,
    onWatchingClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Herd Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ClickableStatItem("Total Head", totalCows, MaterialTheme.colorScheme.primary) { onClick(null) }
                ClickableStatItem("Watching", watchedCows, MaterialTheme.colorScheme.tertiary) { onWatchingClick() }
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableStatItem(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationBreakdownCard(
    classifications: Map<String, Int>,
    onRowClick: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "By Classification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            classifications.forEach { (classification, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(classification) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = classification,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastureBreakdownCard(
    pastures: Map<String, Int>,
    onRowClick: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Landscape,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "By Pasture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (pastures.isEmpty()) {
                Text(
                    text = "No pastures assigned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pastures.forEach { (pasture, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRowClick(pasture) },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pasture,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeBasedReportsCard(
    under1Year: Int,
    between1And5Years: Int,
    between5And10Years: Int,
    over10Years: Int,
    onRowClick: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "By Age",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val ageGroups = listOf(
                "Under 1 year" to under1Year,
                "1-5 years" to between1And5Years,
                "5-10 years" to between5And10Years,
                "Over 10 years" to over10Years
            )
            
            ageGroups.forEach { (ageGroup, count) ->
                val key = when (ageGroup) {
                    "Under 1 year" -> "UNDER_1"
                    "1-5 years" -> "1_5"
                    "5-10 years" -> "5_10"
                    else -> "10_PLUS"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(key) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ageGroup,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingReportsCard(
    cowsNotCalvedIn9Months: Int,
    cowsCalvedIn9Months: Int,
    onNotCalvedClick: () -> Unit,
    onCalvedClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Breeding Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNotCalvedClick() },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cows not calved in 9+ months",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = cowsNotCalvedIn9Months.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (cowsNotCalvedIn9Months > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCalvedClick() },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cows with calves",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = cowsCalvedIn9Months.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
