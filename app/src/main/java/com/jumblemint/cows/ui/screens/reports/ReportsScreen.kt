package com.jumblemint.cows.ui.screens.reports

import android.app.Application // Required for ViewModelFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer // Added for wobble
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ReportsViewModel
import com.jumblemint.cows.ui.viewmodel.ReportsViewModelFactory
import com.jumblemint.cows.ui.theme.getCardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random // Added for random delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onShowList: (type: String, value: String?) -> Unit,
    onNavigateToAddBirth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application // Get Application instance

    // ViewModel initialization with Application context for SharedPreferences
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- Hint Logic State ---
    var hasClickedGotItState by remember { mutableStateOf(viewModel.hasUserClickedGotIt()) }
    var hasShownCloseButtonState by remember { mutableStateOf(viewModel.hasShownCloseButton()) }
    var snackbarShownThisScreenInstance by remember { mutableStateOf(false) }
    var temporaryWobblePeriodActive by remember { mutableStateOf(false) }
    // --- End Hint Logic State ---

    LaunchedEffect(Unit) { 
        if (!hasClickedGotItState && !snackbarShownThisScreenInstance) {
            temporaryWobblePeriodActive = true 
            snackbarShownThisScreenInstance = true 

            val actionLabel = if (hasShownCloseButtonState) "GOT IT" else "Close"

            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Tip: Most items on this screen can be tapped for more details.",
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Short // Using Short duration
                )

                if (result == SnackbarResult.ActionPerformed) {
                    if (actionLabel == "GOT IT") {
                        hasClickedGotItState = true
                        viewModel.setHasUserClickedGotIt(true)
                    } else { // Clicked "Close"
                        hasShownCloseButtonState = true
                        viewModel.setHasShownCloseButton(true)
                    }
                } else { // Snackbar dismissed without action (e.g., timeout)
                    if (actionLabel == "Close") {
                        hasShownCloseButtonState = true
                        viewModel.setHasShownCloseButton(true)
                    }
                }
                 if (hasClickedGotItState) { 
                     temporaryWobblePeriodActive = false
                 }
            }
            delay(4000L) // Overall window for wobbles to occur
            temporaryWobblePeriodActive = false 
        } else {
            temporaryWobblePeriodActive = false 
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val showWobble = temporaryWobblePeriodActive && !hasClickedGotItState

                item { HerdOverviewCard(uiState.totalCows, uiState.watchedCowsCount, { type -> onShowList("status", type) }, { onShowList("watching", null) }) }
                item { ToolsCard(onNavigateToAddBirth, { onShowList("workingList", null) }) }
                item { ClassificationBreakdownCard(uiState.classificationBreakdown, { classification -> onShowList("classification", classification) }, showWobble) }
                item { PastureBreakdownCard(uiState.pastureBreakdown, { pastureName -> val id = pastureIdByName[pastureName]; if (id != null) onShowList("pasture", id.toString()) else onShowList("pastureName", pastureName) }, showWobble) }
                item { AgeBasedReportsCard(uiState.cowsUnder1Year, uiState.cowsBetween1And5Years, uiState.cowsBetween5And10Years, uiState.cowsOver10Years, { rangeKey -> onShowList("age", rangeKey) }, showWobble) }
                item { BreedingReportsCard(uiState.cowsNotCalvedIn9Months, uiState.cowsCalvedInPast9Months, { onShowList("notCalved", null) }, { onShowList("calved", null) }, showWobble) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsCard(onAddCalfClick: () -> Unit, onWorkingListClick: () -> Unit) {
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = getCardColors(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
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
    Card(onClick = onClick, colors = getCardColors(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WobbleTextHint(text: String, style: androidx.compose.ui.text.TextStyle, applyWobble: Boolean) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(applyWobble) {
        if (applyWobble) {
            // Each WobbleTextHint instance gets its own speed characteristic for this activation
            val baseDuration = 180L // Base duration for one half of a cycle
            // Randomize duration between 150ms and 210ms (baseDuration +/- 30ms)
            val randomDuration = Random.nextLong(baseDuration - 30, baseDuration + 31).coerceAtLeast(100L) // Ensure not too fast

            launch {
                delay(Random.nextLong(50, 250)) // Stagger start of wobble
                for (i in 0..4) { // 5 cycles
                    rotation.animateTo(5f, animationSpec = tween(durationMillis = randomDuration.toInt()))
                    rotation.animateTo(-5f, animationSpec = tween(durationMillis = randomDuration.toInt()))
                }
                rotation.animateTo(0f, animationSpec = tween(durationMillis = randomDuration.toInt())) // Settle back
            }
        } else {
             rotation.snapTo(0f)
        }
    }

    Text(
        text = text,
        style = style,
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation.value
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationBreakdownCard(classifications: Map<String, Int>, onRowClick: (String) -> Unit, applyHintEffect: Boolean) {
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
                    WobbleTextHint(text = classification, style = MaterialTheme.typography.bodyMedium, applyWobble = applyHintEffect)
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastureBreakdownCard(pastures: Map<String, Int>, onRowClick: (String) -> Unit, applyHintEffect: Boolean) {
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
                        WobbleTextHint(text = pasture, style = MaterialTheme.typography.bodyMedium, applyWobble = applyHintEffect)
                        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeBasedReportsCard(under1Year: Int, between1And5Years: Int, between5And10Years: Int, over10Years: Int, onRowClick: (String) -> Unit, applyHintEffect: Boolean) {
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
                    "Under 1 year" -> "UNDER_1"
                    "1-5 years" -> "1_5"
                    "5-10 years" -> "5_10"
                    else -> "10_PLUS"
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onRowClick(key) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WobbleTextHint(text = ageGroup, style = MaterialTheme.typography.bodyMedium, applyWobble = applyHintEffect)
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingReportsCard(cowsNotCalvedIn9Months: Int, cowsCalvedIn9Months: Int, onNotCalvedClick: () -> Unit, onCalvedClick: () -> Unit, applyHintEffect: Boolean) {
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
                WobbleTextHint(text = "Cows not calved in 9+ months", style = MaterialTheme.typography.bodyMedium, applyWobble = applyHintEffect)
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
                WobbleTextHint(text = "Cows with calves (last 9m)", style = MaterialTheme.typography.bodyMedium, applyWobble = applyHintEffect)
                Text(cowsCalvedIn9Months.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
