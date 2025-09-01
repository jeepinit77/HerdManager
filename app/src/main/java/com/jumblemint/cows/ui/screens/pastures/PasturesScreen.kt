package com.jumblemint.cows.ui.screens.pastures

import android.app.Application
import androidx.compose.foundation.layout.*
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
    // pasturesViewModel: PasturesViewModel = viewModel( // Old way
    //     factory = PasturesViewModelFactory(LocalContext.current.applicationContext as Application)
    // ),
    onNavigateToAddPasture: () -> Unit,
    onNavigateToPastureDetails: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // ViewModel setup with Repository
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context.applicationContext as Application)
    val repository = remember {
        CattleRepository(
            cowDao = database.cowDao(),
            pastureDao = database.pastureDao(),
            activityDao = database.activityDao(),
            settingsDao = database.settingsDao(),
            noteDao = database.noteDao()
        )
    }
    val pasturesViewModel: PasturesViewModel = viewModel(
        factory = PasturesViewModelFactory(repository) // Pass repository here
    )
    // MARKER_VIEWMODEL_REPO_INIT_NOV21

    val uiState by pasturesViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
            // Unassigned cows banner
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
                                    text = "Cows: ${uiState.unassignedCowCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            items(uiState.pastures, key = { it.pasture.id }) { pastureWithCowCount ->
                PastureCard( // PASTURE_CARD_NOV21_EDIT_BUTTON_ATTEMPT
                    pastureWithCowCount = pastureWithCowCount,
                    onClick = {
                        onNavigateToPastureDetails(pastureWithCowCount.pasture.id)
                    },
                    onEdit = {
                        onNavigateToPastureDetails(pastureWithCowCount.pasture.id)
                    },
                    onDelete = { pasture ->
                        coroutineScope.launch {
                            val deleteResult = pasturesViewModel.deletePasture(pasture)
                            deleteResult.fold(
                                onSuccess = {
                                    val snackbarResult = snackbarHostState.showSnackbar(
                                        message = "${pasture.name} deleted",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                                        pasturesViewModel.undoDeletePasture()
                                    }
                                },
                                onFailure = { exception ->
                                    snackbarHostState.showSnackbar(
                                        message = "Error deleting pasture: ${exception.message}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            )
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
    pastureWithCowCount: com.jumblemint.cows.ui.viewmodel.PastureWithCowCount,
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
                Text(text = pastureWithCowCount.pasture.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Cows: ${pastureWithCowCount.cowCount}", style = MaterialTheme.typography.bodyMedium)
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
                    onClick = { onDelete(pastureWithCowCount.pasture) }
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
