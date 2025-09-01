package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
// import com.jumblemint.cows.data.model.Cow // Not needed if CowCard takes care of its own Cow import
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CowCard
// import com.jumblemint.cows.ui.components.SwipeToDeleteContainer // Import removed
import com.jumblemint.cows.ui.viewmodel.CowsViewModel
import com.jumblemint.cows.ui.viewmodel.CowsViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowsScreen(
    pastureId: Long? = null,
    onCowClick: (Long) -> Unit,
    onAddCowClick: () -> Unit
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao()
    )
    val viewModel: CowsViewModel = viewModel(
        factory = CowsViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showStatusFilter by remember { mutableStateOf(false) }

    LaunchedEffect(pastureId) {
        if (pastureId != null) {
            // MODIFIED: Convert Long? to String?
            viewModel.filterCowsByPasture(pastureId.toString())
        }
    }

    val snackbarHostState = remember { SnackbarHostState() } // Used by CowCard's onDelete
    val scope = rememberCoroutineScope() // Used by CowCard's onDelete

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCowClick
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cow")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.selectedPastureId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtered by pasture",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.filterCowsByPasture(null) }
                        ) {
                            Text("Clear Filter")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchCows(it)
                    },
                    label = { Text("Search cows...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    onClick = { showStatusFilter = !showStatusFilter },
                    label = { Text("Filter") },
                    selected = uiState.selectedStatus != null
                )
            }

            if (showStatusFilter) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Filter by Status", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                onClick = { viewModel.filterCowsByStatus(Status.ACTIVE) },
                                label = { Text("Active") },
                                selected = uiState.selectedStatus == Status.ACTIVE
                            )
                            FilterChip(
                                onClick = { viewModel.filterCowsByStatus(Status.SOLD) },
                                label = { Text("Sold") },
                                selected = uiState.selectedStatus == Status.SOLD
                            )
                            FilterChip(
                                onClick = { viewModel.filterCowsByStatus(Status.DECEASED) },
                                label = { Text("Deceased") },
                                selected = uiState.selectedStatus == Status.DECEASED
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.cows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No cows found", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Add your first cow to get started", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddCowClick) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Cow")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.cows, key = { it.id }) { cow ->
                        // SwipeToDeleteContainer has been removed.
                        // CowCard is now a direct child of items.
                        CowCard(
                            cow = cow,
                            onClick = { onCowClick(cow.id) },
                            onToggleWatch = { viewModel.toggleWatch(cow) },
                            onDelete = {
                                // This onDelete is part of CowCard itself and uses the
                                // snackbarHostState and scope defined in CowsScreen
                                scope.launch {
                                    viewModel.deleteCow(cow)
                                    val res = snackbarHostState.showSnackbar(
                                        message = "Cow deleted",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeleteCow(cow)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
