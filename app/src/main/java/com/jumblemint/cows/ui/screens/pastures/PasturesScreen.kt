package com.jumblemint.cows.ui.screens.pastures

import android.app.Application
import androidx.compose.foundation.layout.*
import com.jumblemint.cows.CattleApplication
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.ui.viewmodel.PasturesViewModel
import com.jumblemint.cows.ui.viewmodel.PasturesViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasturesScreen(
    onNavigateToAddPasture: () -> Unit,
    onNavigateToPastureDetails: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(application)
    val repository = remember {
        CattleRepository(
            cowDao = database.cowDao(),
            pastureDao = database.pastureDao(),
            activityDao = database.activityDao(),
            settingsDao = database.settingsDao(),
            noteDao = database.noteDao(),
            userDao = database.userDao(),
            herdDao = database.herdDao(),
            herdMemberDao = database.herdMemberDao()
        )
    }
    val pasturesViewModel: PasturesViewModel = viewModel(
        factory = PasturesViewModelFactory(application, repository)
    )

    val uiState by pasturesViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle errors from the ViewModel
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            // Consider adding: pasturesViewModel.onErrorShown() to clear the error in ViewModel
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddPasture) {
                Icon(Icons.Filled.Add, contentDescription = "Add Pasture")
            }
        },
        topBar = {
            TopAppBar(title = { Text("Pastures") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.unassignedCowCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unassigned",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Total Head: ${uiState.unassignedCowCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            items(uiState.pastures, key = { it.pastureWithCount.pasture.id }) { pastureWithDetails ->
                PastureCard(
                    pastureWithDetails = pastureWithDetails,
                    onClick = {
                        onNavigateToPastureDetails(pastureWithDetails.pastureWithCount.pasture.id)
                    },
                    onEdit = {
                        onNavigateToPastureDetails(pastureWithDetails.pastureWithCount.pasture.id)
                    },
                    onDelete = { pasture ->
                        coroutineScope.launch {
                            pasturesViewModel.deletePasture(pasture) // Call ViewModel method
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "${pasture.name} deleted",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Long
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                pasturesViewModel.undoDeletePasture()
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastureCard(
    pastureWithDetails: com.jumblemint.cows.ui.viewmodel.PastureWithDetails,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Pasture) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pastureWithDetails.pastureWithCount.pasture.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Head: ${pastureWithDetails.pastureWithCount.cowCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (pastureWithDetails.classificationBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val breakdownText = pastureWithDetails.classificationBreakdown
                        .entries
                        .sortedByDescending { it.value }
                        .joinToString(" • ") { "${it.key.name.lowercase().replaceFirstChar { char -> char.uppercase() }}: ${it.value}" }
                    Text(
                        text = breakdownText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row { 
                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Pasture"
                    )
                }
                IconButton(
                    onClick = { onDelete(pastureWithDetails.pastureWithCount.pasture) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Pasture",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
