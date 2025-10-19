package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Breed
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.BreedsViewModel
import com.jumblemint.cows.ui.viewmodel.BreedsViewModelFactory
import kotlinx.coroutines.launch
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedsManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val repository = remember {
        CattleRepository(
            database.cowDao(), database.pastureDao(), database.activityDao(),
            database.settingsDao(), database.noteDao(), database.userDao(),
            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
            database.activityTypeConfigDao(), database.breedDao()
        )
    }

    val viewModel: BreedsViewModel = viewModel(
        factory = BreedsViewModelFactory(
            repository = repository,
            getUserId = { currentUser?.uid ?: "" }
        )
    )

    val breeds by viewModel.breeds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBreed by remember { mutableStateOf<Breed?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastDeleted by remember { mutableStateOf<Breed?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // Snackbar Host positioned manually
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        var showResetConfirm by remember { mutableStateOf(false) }

        if (breeds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Pets, contentDescription = "No breeds", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No breeds found.", style = MaterialTheme.typography.headlineSmall)
                    Text("Add breeds using the '+' button above.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(breeds, key = { it.id }) { breed ->
                    BreedItem(
                        breed = breed,
                        onEdit = { editingBreed = it; showAddDialog = true },
                        onDelete = { breedToDelete ->
                            lastDeleted = breedToDelete
                            viewModel.deleteBreed(breedToDelete)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Breed '${breedToDelete.name}' deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    lastDeleted?.let { viewModel.restoreBreed(it) }
                                }
                                lastDeleted = null
                            }
                        }
                    )
                }
            }
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                icon = { Icon(Icons.Default.WarningAmber, contentDescription = "Warning") },
                title = { Text("Reset Breeds?") },
                text = { Text("This will remove all custom breeds and reinstall the default set. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            viewModel.resetToDefaults()
                            scope.launch {
                                snackbarHostState.showSnackbar("Breeds reset to defaults.")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }

    if (showAddDialog || editingBreed != null) {
        BreedDialog(
            breedToEdit = editingBreed,
            onDismiss = {
                showAddDialog = false
                editingBreed = null
            },
            onSave = { name, id ->
                if (id != null && editingBreed != null) {
                    viewModel.updateBreed(
                        editingBreed!!.copy(
                            name = name,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    viewModel.addBreed(name)
                }
                showAddDialog = false
                editingBreed = null
            }
        )
    }
}

@Composable
fun BreedItem(
    breed: Breed,
    onEdit: (Breed) -> Unit,
    onDelete: (Breed) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = breed.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onEdit(breed) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${breed.name}", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(breed) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${breed.name}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedDialog(
    breedToEdit: Breed? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, id: String?) -> Unit
) {
    var name by remember(breedToEdit) { mutableStateOf(breedToEdit?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (breedToEdit != null) "Edit Breed" else "Add New Breed") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) "Name cannot be empty" else null
                    },
                    label = { Text("Breed Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    colors = defaultOutlinedTextFieldColors()
                )
                if (nameError != null) {
                    Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), breedToEdit?.id)
                    } else {
                        nameError = "Name cannot be empty"
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
