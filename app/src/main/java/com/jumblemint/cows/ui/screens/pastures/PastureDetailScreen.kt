package com.jumblemint.cows.ui.screens.pastures

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CowCard
import com.jumblemint.cows.ui.components.SimpleTopAppBar
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.PastureDetailViewModel
import com.jumblemint.cows.ui.viewmodel.PastureDetailViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastureDetailScreen(
    pastureId: String,
    onNavigateBack: () -> Unit,
    onCowClick: (Long) -> Unit,
    onCowEdit: (Long) -> Unit,
    onEditPasture: () -> Unit = {},
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
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

    val viewModel: PastureDetailViewModel = viewModel(
        factory = PastureDetailViewModelFactory(application, repository, pastureId)
    )

    val uiState by viewModel.uiState.collectAsState()
    val tagColorMap = rememberTagColorMap(repository)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // TODO: Communicate pasture name (uiState.pasture?.name) to MainActivity's TopAppBar.
    // This could be done via a callback to update a shared state or ViewModel.
    // Example: LaunchedEffect(uiState.pasture?.name) { newName -> /* update MainActivity's title */ }

    // TODO: Ensure MainActivity's TopAppBar shows an Edit icon that calls `onEditPasture`
    // when this screen is active and uiState.pasture is not null.

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                title = uiState.pasture?.name ?: "Pasture Details",
                onBack = onNavigateBack,
                actions = {
                    if (uiState.pasture != null) {
                        IconButton(onClick = onEditPasture) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Pasture")
                        }
                    }
                }
            )
        },
        // contentWindowInsets = WindowInsets(0, 0, 0, 0) // Commented out
    ) { localScaffoldPadding -> // This padding is from THIS Scaffold (if it had a FAB, BottomBar, etc.)
                                // The `modifier` applied above already contains padding from MainActivity.

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(localScaffoldPadding), // Apply this Scaffold's padding
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.pasture == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(localScaffoldPadding), // Apply this Scaffold's padding
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Pasture not found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(localScaffoldPadding), // Apply this Scaffold's padding
                contentPadding = PaddingValues(all = 16.dp), // Overall padding for the content list
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pasture Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.pasture!!.name, // Title is now part of the content
                                style = MaterialTheme.typography.headlineMedium, // Emphasize title more
                                fontWeight = FontWeight.Bold
                            )

                            if (!uiState.pasture!!.description.isNullOrBlank()) {
                                Text(
                                    text = uiState.pasture!!.description!!,
                                    style = MaterialTheme.typography.bodyLarge, // Slightly larger for description
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp)) // Additional space before stats

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom // Align stats nicely
                            ) {
                                Column {
                                    Text(
                                        text = "Total Head",
                                        style = MaterialTheme.typography.labelLarge, // Slightly larger label
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${uiState.activeCows.size}",
                                        style = MaterialTheme.typography.headlineSmall, // Emphasize count
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (uiState.pasture!!.sizeAcres != null) {
                                    Column(horizontalAlignment = Alignment.End) { // Align to end
                                        Text(
                                            text = "Size",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${uiState.pasture!!.sizeAcres} acres",
                                            style = MaterialTheme.typography.titleLarge, // Match Total Head style better
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (uiState.classificationBreakdown.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Animal Types",
                                    style = MaterialTheme.typography.titleSmall, // Consistent small title for subsections
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val breakdownText = uiState.classificationBreakdown
                                    .entries
                                    .sortedByDescending { it.value }
                                    .joinToString(" • ") {
                                        "${it.key.name.lowercase().replaceFirstChar { char -> char.uppercase() }}: ${it.value}"
                                    }
                                Text(
                                    text = breakdownText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Animals Section Header
                if (uiState.activeCows.isNotEmpty()) {
                    item {
                        Text(
                            text = "Animals in this Pasture",
                            style = MaterialTheme.typography.titleLarge, // More prominent header
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp) // Add some space above this header
                        )
                    }
                }

                // Animals List
                if (uiState.activeCows.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // More subtle
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 16.dp), // Adjust padding
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No active animals in this pasture.", // Slightly friendlier message
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.activeCows, key = { it.id }) { cow ->
                        CowCard(
                            cow = cow,
                            onClick = { onCowClick(cow.id) },
                            onToggleWatch = { viewModel.toggleWatch(cow) },
                            onEdit = { onCowEdit(cow.id) },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteCow(cow) // Assumes ViewModel handles this
                                    val result = snackbarHostState.showSnackbar(
                                        message = "${cow.name ?: cow.tagNumber ?: "Cow"} deleted",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeleteCow(cow) // Assumes ViewModel handles this
                                    }
                                }
                            },
                            resolvedTagColor = resolveTagColor(cow.tagColor, tagColorMap)
                        )
                    }
                }
            }
        }
    }
}
