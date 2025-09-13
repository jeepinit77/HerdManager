package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Keep this import
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items // Keep this import
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete // Specific import
import androidx.compose.material.icons.outlined.Edit // Specific import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.text.font.FontWeight // <<< REMOVED UNUSED IMPORT
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.ActivityTypeConfig
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ActivityTypesViewModel
import com.jumblemint.cows.ui.viewmodel.ActivityTypesViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypesManagementScreen(
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

    val viewModel: ActivityTypesViewModel = viewModel(
        factory = ActivityTypesViewModelFactory(
            repository = repository,
            syncService = application.syncService,
            getUserId = { currentUser?.uid ?: "" }
        )
    )

    val activityTypes by viewModel.activityTypes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingType by remember { mutableStateOf<ActivityTypeConfig?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastDeleted by remember { mutableStateOf<ActivityTypeConfig?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Activity Types") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset to Default Activity Types")
                    }
                    IconButton(onClick = { editingType = null; showAddDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add New Activity Type")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0,0,0,0)
    ) { paddingValues ->

        if (activityTypes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.ListAlt,
                        contentDescription = "No activity types",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No custom activity types found.", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Add types using the '+' button above or reset to defaults.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activityTypes, key = { it.id }) { activityType ->
                    ActivityTypeItem(
                        activityType = activityType,
                        onEdit = { editingType = it },
                        onDelete = { typeToDelete ->
                            lastDeleted = typeToDelete
                            viewModel.deleteActivityType(typeToDelete)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Type '${typeToDelete.displayName}' deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    // <<< USE NEW RESTORE METHOD
                                    lastDeleted?.let { viewModel.restoreDeletedActivityType(it) }
                                }
                                lastDeleted = null // Clear after action or dismissal
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog || editingType != null) {
        ActivityTypeDialog(
            activityTypeToEdit = editingType,
            onDismiss = {
                showAddDialog = false
                editingType = null
            },
            onSave = { internalName, displayName, description, iconName ->
                if (editingType != null) {
                    viewModel.updateActivityType(
                        editingType!!.copy(
                            // name = internalName, // Internal name should generally not change for existing items
                            displayName = displayName,
                            description = description.takeIf { it.isNotBlank() },
                            iconName = iconName, // <<< PASS iconName
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    viewModel.addActivityType(
                        name = internalName, // Generated internal name for new types
                        displayName = displayName,
                        description = description.takeIf { it.isNotBlank() },
                        iconName = iconName // <<< PASS iconName
                    )
                }
                showAddDialog = false
                editingType = null
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = "Warning") },
            title = { Text("Reset Activity Types?") },
            text = { Text("This will remove ALL custom activity types and restore the original default types. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreDefaults()
                        showResetConfirm = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Activity types reset to defaults.")
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

@Composable
fun ActivityTypeItem(
    activityType: ActivityTypeConfig,
    onEdit: (ActivityTypeConfig) -> Unit,
    onDelete: (ActivityTypeConfig) -> Unit
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
            Icon(
                // <<< USE activityType.iconName (now exists)
                imageVector = getIconForActivityType(activityType.iconName ?: activityType.name),
                contentDescription = activityType.displayName,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityType.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                activityType.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onEdit(activityType) }) {
                // <<< CORRECTED ICON
                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${activityType.displayName}", tint = MaterialTheme.colorScheme.primary)
            }
            // <<< USE isDefault instead of isSystemType
            if (!activityType.isDefault) {
                IconButton(onClick = { onDelete(activityType) }) {
                    // <<< CORRECTED ICON
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete ${activityType.displayName}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

val availableActivityIcons = listOf(
    "Assignment" to Icons.Default.Assignment,
    "DriveFileMove" to Icons.Default.DriveFileMove,
    "ChildCare" to Icons.Default.ChildCare,
    "Sell" to Icons.Default.Sell,
    "Dangerous" to Icons.Default.Dangerous,
    "MedicalServices" to Icons.Default.MedicalServices,
    "Favorite" to Icons.Default.Favorite,
    "BabyChangingStation" to Icons.Default.BabyChangingStation,
    "Vaccines" to Icons.Default.Vaccines,
    "LocalHospital" to Icons.Default.LocalHospital,
    "Scale" to Icons.Default.Scale,
    "Pets" to Icons.Default.Pets,
    "Healing" to Icons.Default.Healing,
    "ContentCut" to Icons.Default.ContentCut,
    "Grass" to Icons.Default.Grass,
    "WaterDrop" to Icons.Default.WaterDrop,
    "Flare" to Icons.Default.Flare,
    "QrCodeScanner" to Icons.Default.QrCodeScanner,
    "EditNote" to Icons.Default.EditNote,
    "Event" to Icons.Default.Event,
    "Biotech" to Icons.Default.Biotech,
    "Science" to Icons.Default.Science,
    "WarningAmber" to Icons.Default.WarningAmber,
    "Info" to Icons.Default.Info
).sortedBy { it.first }

fun getIconForActivityType(iconNameOrActivityName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val directMatch = availableActivityIcons.find { it.first.equals(iconNameOrActivityName, ignoreCase = true) }
    if (directMatch != null) return directMatch.second

    // Fallback logic, can be simplified if iconName is always expected to be in availableActivityIcons
    return when (iconNameOrActivityName.uppercase()) {
        "MOVED" -> Icons.Default.DriveFileMove
        "WEANED" -> Icons.Default.ChildCare
        "SOLD" -> Icons.Default.Sell
        "DECEASED" -> Icons.Default.Dangerous
        "CASTRATED" -> Icons.Default.MedicalServices
        "BRED" -> Icons.Default.Favorite
        "CALVED" -> Icons.Default.BabyChangingStation
        "VACCINATED" -> Icons.Default.Vaccines
        "TREATED" -> Icons.Default.LocalHospital
        "WEIGHED" -> Icons.Default.Scale
        "PURCHASED" -> Icons.Default.Pets
        "HEALTH_CHECK" -> Icons.Default.Healing
        "TAGGED" -> Icons.Default.ContentCut
        "NOTE" -> Icons.Default.EditNote
        else -> availableActivityIcons.find { it.first.contains(iconNameOrActivityName, ignoreCase = true) }?.second ?: Icons.Default.Assignment
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypeDialog(
    activityTypeToEdit: ActivityTypeConfig? = null,
    onDismiss: () -> Unit,
    onSave: (internalName: String, displayName: String, description: String, iconName: String) -> Unit
) {
    var displayName by remember(activityTypeToEdit) { mutableStateOf(activityTypeToEdit?.displayName ?: "") }
    var description by remember(activityTypeToEdit) { mutableStateOf(activityTypeToEdit?.description ?: "") }
    var selectedIconName by remember(activityTypeToEdit) {
        // <<< USE activityTypeToEdit.iconName
        mutableStateOf(activityTypeToEdit?.iconName ?: availableActivityIcons.firstOrNull()?.first ?: "Assignment")
    }
    var showIconPicker by remember { mutableStateOf(false) }
    var displayNameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // <<< USE activityTypeToEdit.isDefault
        icon = { Icon(if (activityTypeToEdit != null && !activityTypeToEdit.isDefault) Icons.Filled.Edit else if (activityTypeToEdit == null) Icons.Filled.AddBusiness else Icons.Filled.Info, contentDescription = null)},
        // <<< USE activityTypeToEdit.isDefault
        title = { Text(if (activityTypeToEdit != null) (if(activityTypeToEdit.isDefault) "View Activity Type" else "Edit Activity Type") else "Add New Activity Type") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        displayNameError = if (it.isBlank()) "Display name cannot be empty" else null
                    },
                    label = { Text("Display Name*") },
                    placeholder = { Text("e.g., Vaccinated, Moved to Pasture X") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = displayNameError != null,
                    // <<< USE activityTypeToEdit.isDefault
                    readOnly = activityTypeToEdit?.isDefault == true
                )
                if (displayNameError != null) {
                    Text(displayNameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Short explanation of this activity type") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    // <<< USE activityTypeToEdit.isDefault
                    readOnly = activityTypeToEdit?.isDefault == true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Icon:", style = MaterialTheme.typography.bodyLarge)
                    Button(
                        // <<< USE activityTypeToEdit.isDefault
                        onClick = { if (activityTypeToEdit?.isDefault != true) showIconPicker = true },
                        // <<< USE activityTypeToEdit.isDefault
                        enabled = activityTypeToEdit?.isDefault != true
                    ) {
                        Icon(getIconForActivityType(selectedIconName), contentDescription = "Selected: $selectedIconName", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        // <<< USE activityTypeToEdit.isDefault
                        Text(if (activityTypeToEdit?.isDefault == true) "Default Icon" else "Change Icon")
                    }
                }

                // <<< USE activityTypeToEdit.isDefault
                if (showIconPicker && activityTypeToEdit?.isDefault != true) {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 48.dp),
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableActivityIcons, key = {it.first}) { (iconKey, iconVector) ->
                                IconButton(
                                    onClick = {
                                        selectedIconName = iconKey
                                        showIconPicker = false
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (selectedIconName == iconKey) 2.dp else 1.dp,
                                            color = if (selectedIconName == iconKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(if (selectedIconName == iconKey) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else Color.Transparent)
                                ) {
                                    Icon(
                                        iconVector,
                                        contentDescription = iconKey,
                                        tint = if (selectedIconName == iconKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // <<< USE activityTypeToEdit.isDefault
            if (activityTypeToEdit?.isDefault != true) {
                Button(
                    onClick = {
                        if (displayName.isNotBlank()) {
                            val internalName = activityTypeToEdit?.name ?: run {
                                // <<< CORRECTED REGEX AND QUOTE
                                val base = displayName.trim().uppercase()
                                    .replace(Regex("\\s+"), "_") // Use double backslash for \s
                                    .replace(Regex("[^A-Z0-9_]"), "")
                                "${selectedIconName.uppercase()}_${base}_${System.currentTimeMillis().toString().takeLast(4)}".take(50)
                            }
                            onSave(internalName, displayName.trim(), description.trim(), selectedIconName)
                        } else {
                            displayNameError = "Display name cannot be empty"
                        }
                    },
                    enabled = displayName.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                // <<< USE activityTypeToEdit.isDefault
                Text(if (activityTypeToEdit?.isDefault == true) "Close" else "Cancel")
            }
        }
    )
}
