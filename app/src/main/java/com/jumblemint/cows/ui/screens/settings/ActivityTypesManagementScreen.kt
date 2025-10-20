package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.ui.components.LocalGlobalSnackbarState
import com.jumblemint.cows.ui.theme.contrastingTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypesManagementScreen(
    onNavigateBack: () -> Unit,
    resetTriggered: Boolean = false,
    onResetHandled: () -> Unit = {},
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
    var viewingType by remember { mutableStateOf<ActivityTypeConfig?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val globalSnackbarState = LocalGlobalSnackbarState.current
    
    LaunchedEffect(resetTriggered) {
        if (resetTriggered) {
            showResetConfirm = true
            onResetHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (activityTypes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        "Add types using the '+' button or reset to defaults.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activityTypes, key = { it.id }) { activityType ->
                    ActivityTypeItem(
                        activityType = activityType,
                        onClick = { viewingType = it },
                        onEdit = { editingType = it },
                        onDelete = { typeToDelete ->
                            viewModel.deleteActivityType(typeToDelete)
                            scope.launch {
                                globalSnackbarState?.let { snackbarState ->
                                    val result = snackbarState.showSnackbar(
                                        message = "Type '${typeToDelete.displayName}' deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreDeletedActivityType(typeToDelete)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingType = null
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Activity Type",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    if (showAddDialog || editingType != null) {
        ActivityTypeDialog(
            activityTypeToEdit = editingType,
            isViewOnly = false,
            onDismiss = {
                showAddDialog = false
                editingType = null
            },
            onSave = { internalName, displayName, description, iconName ->
                if (editingType != null) {
                    viewModel.updateActivityType(
                        editingType!!.copy(
                            displayName = displayName,
                            description = description.takeIf { it.isNotBlank() },
                            iconName = iconName,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    viewModel.addActivityType(
                        name = internalName,
                        displayName = displayName,
                        description = description.takeIf { it.isNotBlank() },
                        iconName = iconName
                    )
                }
                showAddDialog = false
                editingType = null
            }
        )
    }

    if (viewingType != null) {
        ActivityTypeDialog(
            activityTypeToEdit = viewingType,
            isViewOnly = true,
            onDismiss = { viewingType = null },
            onSave = { _, _, _, _ -> }
        )
    }

    if (showResetConfirm) {
        com.jumblemint.cows.ui.components.AppAlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { 
                Icon(
                    Icons.Default.WarningAmber, 
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                ) 
            },
            title = { Text("Reset Activity Types?") },
            text = { Text("This will remove ALL custom activity types and restore the original default types. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDefaults()
                        showResetConfirm = false
                        scope.launch {
                            globalSnackbarState?.showSnackbar("Activity types reset to defaults.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Reset") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { showResetConfirm = false }) { 
                    Text("Cancel") 
                }
            }
        )
    }
}

@Composable
fun ActivityTypeItem(
    activityType: ActivityTypeConfig,
    onClick: (ActivityTypeConfig) -> Unit,
    onEdit: (ActivityTypeConfig) -> Unit,
    onDelete: (ActivityTypeConfig) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(activityType) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForActivityType(activityType.iconName ?: activityType.name),
                contentDescription = activityType.displayName,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                activityType.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(onClick = { onEdit(activityType) }) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit ${activityType.displayName}",
                    tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
                )
            }
            IconButton(onClick = { onDelete(activityType) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${activityType.displayName}",
                    tint = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor()
                )
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
    "Info" to Icons.Default.Info,
    "Handyman" to Icons.Default.Handyman
).sortedBy { it.first }

fun getIconForActivityType(iconNameOrActivityName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val directMatch = availableActivityIcons.find { it.first.equals(iconNameOrActivityName, ignoreCase = true) }
    if (directMatch != null) return directMatch.second

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
    isViewOnly: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (internalName: String, displayName: String, description: String, iconName: String) -> Unit
) {
    var displayName by remember(activityTypeToEdit) { mutableStateOf(activityTypeToEdit?.displayName ?: "") }
    var description by remember(activityTypeToEdit) { mutableStateOf(activityTypeToEdit?.description ?: "") }
    var selectedIconName by remember(activityTypeToEdit) {
        val iconName = activityTypeToEdit?.iconName 
            ?: activityTypeToEdit?.name?.let { name ->
                // Try to find icon based on activity type name
                when (name.uppercase()) {
                    "MOVED" -> "DriveFileMove"
                    "WEANED" -> "ChildCare"
                    "SOLD" -> "Sell"
                    "DECEASED" -> "Dangerous"
                    "CASTRATED" -> "MedicalServices"
                    "BRED" -> "Favorite"
                    "CALVED" -> "BabyChangingStation"
                    "VACCINATED" -> "Vaccines"
                    "TREATED" -> "LocalHospital"
                    "WEIGHED" -> "Scale"
                    "PURCHASED" -> "Pets"
                    "HEALTH_CHECK" -> "Healing"
                    "TAGGED" -> "ContentCut"
                    "NOTE" -> "EditNote"
                    "WORKED" -> "Handyman"
                    else -> null
                }
            }
            ?: availableActivityIcons.firstOrNull()?.first 
            ?: "Assignment"
        mutableStateOf(iconName)
    }
    var showIconPicker by remember { mutableStateOf(false) }
    var displayNameError by remember { mutableStateOf<String?>(null) }

    com.jumblemint.cows.ui.components.AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                if (isViewOnly) Icons.Filled.Info 
                else if (activityTypeToEdit != null) Icons.Filled.Edit 
                else Icons.Filled.AddBusiness,
                contentDescription = null
            )
        },
        title = { 
            Text(
                if (isViewOnly) "View Activity Type"
                else if (activityTypeToEdit != null) "Edit Activity Type"
                else "Add New Activity Type"
            )
        },
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
                    readOnly = isViewOnly,
                    colors = defaultOutlinedTextFieldColors()
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
                    readOnly = isViewOnly,
                    colors = defaultOutlinedTextFieldColors()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Icon:", style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = { if (!isViewOnly) showIconPicker = true },
                        enabled = !isViewOnly
                    ) {
                        Icon(getIconForActivityType(selectedIconName), contentDescription = "Selected: $selectedIconName", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isViewOnly) "Icon" 
                            else "Change Icon"
                        )
                    }
                }

                if (showIconPicker && !isViewOnly) {
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
            if (!isViewOnly) {
                Button(
                    onClick = {
                        if (displayName.isNotBlank()) {
                            val internalName = activityTypeToEdit?.name ?: run {
                                val base = displayName.trim().uppercase()
                                    .replace(Regex("\\s+"), "_")
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
            FilledTonalButton(onClick = onDismiss) {
                Text(if (isViewOnly) "Close" else "Cancel")
            }
        }
    )
}
