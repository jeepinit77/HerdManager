package com.jumblemint.cows.ui.screens.reports

import android.app.Application // Required for ViewModelFactory
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.util.AgeRangeKeys // Import the centralized keys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onShowList: (type: String, value: String?) -> Unit,
    onNavigateToAddBirth: () -> Unit,
    onNavigateToTodoList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application // Get Application instance

    // ViewModel initialization
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
            database.activityTypeConfigDao()
        )
    }
    val cattleApplication = context.applicationContext as com.jumblemint.cows.CattleApplication
    val viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(application, repository, cattleApplication.authService)
    )

    val uiState by viewModel.uiState.collectAsState()
    val pasturesFlow = remember { repository.getAllPastures() }
    val pastures by pasturesFlow.collectAsState(initial = emptyList())
    val pastureIdByName = remember(pastures) { pastures.associate { it.name to it.id } }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(), // Use the modifier passed to ReportsScreen
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier // Use the modifier passed to ReportsScreen
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Add specific padding for this screen's content
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HerdOverviewCard(uiState.totalCows, uiState.watchedCowsCount, { type -> onShowList("status", type) }, { onShowList("watching", null) }) }
            item { ToolsCard(onNavigateToAddBirth, { onShowList("workingList", null) }, onNavigateToTodoList) }
            item { ClassificationBreakdownCard(uiState.classificationBreakdown) { classification -> onShowList("classification", classification) } }
            item { PastureBreakdownCard(uiState.pastureBreakdown) { pastureName -> val id = pastureIdByName[pastureName]; if (id != null) onShowList("pasture", id.toString()) else onShowList("pastureName", pastureName) } }
            item { AgeBasedReportsCard(uiState.cowsUnder1Year, uiState.cowsBetween1And5Years, uiState.cowsBetween5And10Years, uiState.cowsOver10Years) { rangeKey -> onShowList("age", rangeKey) } }
            item { BreedingReportsCard(uiState.cowsNotCalvedIn9Months, uiState.cowsCalvedInPast9Months, { onShowList("notCalved", null) }, { onShowList("calved", null) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsCard(onAddCalfClick: () -> Unit, onWorkingListClick: () -> Unit, onTodoListClick: () -> Unit) {
    Card(colors = getCardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolItem(Icons.Default.Add, "Add Calf", onAddCalfClick)
                ToolItem(Icons.Default.List, "Working List", onWorkingListClick)
                ToolItem(Icons.Default.CheckCircle, "ToDo List", onTodoListClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = getCardColors(), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = null) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerdOverviewCard(totalCows: Int, watchedCows: Int, onClick: (type: String?) -> Unit, onWatchingClick: () -> Unit) {
    Card(colors = getCardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Herd Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ClickableStatItem("Total Head", totalCows, MaterialTheme.colorScheme.primary) { onClick(null) }
                ClickableStatItem("Watching", watchedCows, MaterialTheme.colorScheme.tertiary) { onWatchingClick() }
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableStatItem(label: String, count: Int, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(onClick = onClick, colors = getCardColors(), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = null) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationBreakdownCard(classifications: Map<String, Int>, onRowClick: (String) -> Unit) {
    Card(colors = getCardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("By Classification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            classifications.forEach { (classification, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onRowClick(classification) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = classification, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastureBreakdownCard(pastures: Map<String, Int>, onRowClick: (String) -> Unit) {
    Card(colors = getCardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Landscape, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("By Pasture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (pastures.isEmpty()) {
                Text("No pastures assigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pastures.forEach { (pasture, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onRowClick(pasture) }.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = pasture, style = MaterialTheme.typography.bodyMedium)
                        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeBasedReportsCard(under1Year: Int, between1And5Years: Int, between5And10Years: Int, over10Years: Int, onRowClick: (String) -> Unit) {
    Card(colors = getCardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("By Age", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val ageGroups = listOf("Under 1 year" to under1Year, "1-5 years" to between1And5Years, "5-10 years" to between5And10Years, "Over 10 years" to over10Years)
            ageGroups.forEach { (ageGroup, count) ->
                val key = when (ageGroup) {
                    "Under 1 year" -> AgeRangeKeys.LEGACY_UNDER_1
                    "1-5 years" -> AgeRangeKeys.LEGACY_1_5
                    "5-10 years" -> AgeRangeKeys.LEGACY_5_10
                    else -> AgeRangeKeys.LEGACY_10_PLUS
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onRowClick(key) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = ageGroup, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingReportsCard(cowsNotCalvedIn9Months: Int, cowsCalvedIn9Months: Int, onNotCalvedClick: () -> Unit, onCalvedClick: () -> Unit) {
    Card(colors = getCardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PregnantWoman, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Breeding Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNotCalvedClick() }.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Cows not calved in 9+ months", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = cowsNotCalvedIn9Months.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (cowsNotCalvedIn9Months > 0) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onCalvedClick() }.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Cows with calves (last 9m)", style = MaterialTheme.typography.bodyMedium)
                Text(cowsCalvedIn9Months.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
