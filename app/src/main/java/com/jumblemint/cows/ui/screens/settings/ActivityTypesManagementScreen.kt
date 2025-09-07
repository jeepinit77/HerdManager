package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState // Added explicit import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.ActivityTypeConfig
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.ActivityTypesViewModel
import com.jumblemint.cows.ui.viewmodel.ActivityTypesViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypesManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val repository = CattleRepository(
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
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Manage Activity Types") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showResetConfirm = true }) {
                    Icon(Icons.Default.Restore, contentDescription = "Reset to defaults")
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Activity Type")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activityTypes) { activityType ->
                ActivityTypeItem(
                    activityType = activityType,
                    onEdit = { editingType = it },
                    onDelete = { viewModel.deleteActivityType(it) }
                )
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog || editingType != null) {
        ActivityTypeDialog(
            activityType = editingType,
            onDismiss = {
                showAddDialog = false
                editingType = null
            },
            onSave = { name, displayName, description ->
                if (editingType != null) {
                    viewModel.updateActivityType(editingType!!.copy(
                        name = name,
                        displayName = displayName,
                        description = description.takeIf { it.isNotBlank() },
                        updatedAt = System.currentTimeMillis()
                    ))
                } else {
                    viewModel.addActivityType(
                        name = name,
                        displayName = displayName,
                        description = description.takeIf { it.isNotBlank() }
                    )
                }
                showAddDialog = false
                editingType = null
            }
        )
    }
    
    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Reset activity types?") },
            text = { Text("This will remove all custom activity types and restore the default types. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.restoreDefaults()
                }) { Text("Reset") }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Icon(
                getIconForActivityType(activityType.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Type info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activityType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                activityType.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Actions
            Row {
                IconButton(onClick = { onEdit(activityType) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = { onDelete(activityType) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Available icons for activity types
private val availableIcons = listOf(
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
    "Agriculture" to Icons.Default.Agriculture,
    "Pets" to Icons.Default.Pets,
    "LocalFlorist" to Icons.Default.LocalFlorist,
    "Healing" to Icons.Default.Healing,
    "MonitorWeight" to Icons.Default.MonitorWeight,
    "Medication" to Icons.Default.Medication,
    "Science" to Icons.Default.Science,
    "Biotech" to Icons.Default.Biotech,
    "HealthAndSafety" to Icons.Default.HealthAndSafety
)

private fun getIconForActivityType(activityTypeName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (activityTypeName.uppercase()) {
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
        else -> {
            // For custom activity types, try to find a matching icon by name
            val iconName = activityTypeName.split("_").firstOrNull()
            availableIcons.find { it.first.uppercase().contains(iconName?.uppercase() ?: "") }?.second
                ?: Icons.Default.Assignment
        }
    }
}

@Composable
fun ActivityTypeDialog(
    activityType: ActivityTypeConfig? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var displayName by remember { mutableStateOf(activityType?.displayName ?: "") }
    var description by remember { mutableStateOf(activityType?.description ?: "") }
    var selectedIconName by remember { 
        mutableStateOf(
            // Try to find the current icon name for existing activity types
            availableIcons.find { (_, icon) -> 
                icon == getIconForActivityType(activityType?.name ?: "")
            }?.first ?: "Assignment"
        )
    }
    var showIconPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (activityType != null) "Edit Activity Type" else "Add Activity Type") 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Activity Type Name") },
                    placeholder = { Text("Custom Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Description of this activity type") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                // Icon selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Icon:")
                    
                    val selectedIcon = availableIcons.find { it.first == selectedIconName }?.second ?: Icons.Default.Assignment
                    
                    IconButton(
                        onClick = { showIconPicker = !showIconPicker },
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            selectedIcon,
                            contentDescription = "Selected icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    TextButton(onClick = { showIconPicker = !showIconPicker }) {
                        Text("Choose Icon")
                    }
                }
                
                // Icon picker grid
                if (showIconPicker) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableIcons) { (iconName, icon) ->
                                IconButton(
                                    onClick = {
                                        selectedIconName = iconName
                                        showIconPicker = false
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(
                                            width = if (selectedIconName == iconName) 2.dp else 1.dp,
                                            color = if (selectedIconName == iconName) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = iconName,
                                        tint = if (selectedIconName == iconName) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (displayName.isNotBlank()) {
                        // For existing activity types, keep the original name; for new ones, generate from display name and icon
                        val codeName = activityType?.name ?: run {
                            val baseName = displayName.trim().uppercase().replace(Regex("[^A-Z0-9]"), "_")
                            // Prefix with icon name for better icon matching
                            "${selectedIconName.uppercase()}_$baseName"
                        }
                        onSave(codeName, displayName.trim(), description.trim())
                    }
                },
                enabled = displayName.isNotBlank()
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