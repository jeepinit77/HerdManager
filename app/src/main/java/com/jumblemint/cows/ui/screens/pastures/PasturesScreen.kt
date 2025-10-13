package com.jumblemint.cows.ui.screens.pastures

import android.app.Application
import androidx.compose.foundation.layout.*
import com.jumblemint.cows.CattleApplication
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close // Added for TopAppBar
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
import com.jumblemint.cows.ui.theme.getCardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasturesScreen(
    onNavigateToAddPasture: () -> Unit,
    onNavigateToPastureDetails: (String) -> Unit,
    onNavigateToEditPasture: (String) -> Unit,
    onNavigateToUnassignedList: () -> Unit, // Added new navigation callback
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
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
            herdMemberDao = database.herdMemberDao(),
            tagColorDao = database.tagColorDao(),
            activityTypeConfigDao = database.activityTypeConfigDao()
        )
    }
    val pasturesViewModel: PasturesViewModel = viewModel(
        factory = PasturesViewModelFactory(application, repository)
    )

    val uiState by pasturesViewModel.uiState.collectAsState()
    val globalSnackbarState = com.jumblemint.cows.ui.components.LocalGlobalSnackbarState.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            globalSnackbarState?.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            pasturesViewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPasture,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Pasture")
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (uiState.pastures.isEmpty() && uiState.unassignedCowCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Nothing here yet", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Add fields using the + button to get started", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show Unassigned card only if there are unassigned cows AND at least one actual pasture exists
                if (uiState.unassignedCowCount > 0 && uiState.pastures.isNotEmpty()) {
                    item {
                        Card(
                            onClick = { onNavigateToUnassignedList() }, // Made card tappable
                            modifier = Modifier.fillMaxWidth(),
                            colors = getCardColors() // Changed to use getCardColors for consistency
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
                                        // color = MaterialTheme.colorScheme.onSecondaryContainer // Color will now come from getCardColors
                                    )
                                    Text(
                                        text = "Total Head: ${uiState.unassignedCowCount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        // color = MaterialTheme.colorScheme.onSecondaryContainer // Color will now come from getCardColors
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
                            onNavigateToEditPasture(pastureWithDetails.pastureWithCount.pasture.id)
                        },
                        onDelete = { pasture ->
                            coroutineScope.launch {
                                pasturesViewModel.deletePasture(pasture)
                                val snackbarResult = globalSnackbarState?.showSnackbar(
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
        modifier = Modifier.fillMaxWidth(),
        colors = getCardColors()
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
