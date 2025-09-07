package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.SettingsViewModel
import com.jumblemint.cows.ui.viewmodel.SettingsViewModelFactory

// Helper data class for quadruple values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSignIn: (() -> Unit)? = null,
    onNavigateToHerds: (() -> Unit)? = null,
    onNavigateToAccountManagement: (() -> Unit)? = null,
    onNavigateToTagColors: (() -> Unit)? = null,
    onNavigateToActivityTypes: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
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
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository)
    )
    
    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val isSignedIn by application.authService.isSignedIn.collectAsState(initial = false)
    val syncStatus by application.syncService.syncStatus.collectAsState(initial = com.jumblemint.cows.sync.SyncStatus.IDLE)
    
    val uiState by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showSampleDataDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                onNavigateBack?.let { callback ->
                    IconButton(onClick = callback) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Customization Section
            item {
                Text(
                    text = "Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = "Tag Colors",
                    subtitle = "Manage available tag colors",
                    icon = Icons.Default.ColorLens,
                    onClick = { onNavigateToTagColors?.invoke() }
                )
            }
            
            item {
                SettingsCard(
                    title = "Activity Types",
                    subtitle = "Manage activity types",
                    icon = Icons.Default.Assignment,
                    onClick = { onNavigateToActivityTypes?.invoke() }
                )
            }
            

            
            // Account & Sync Section
            item {
                Text(
                    text = "Account & Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                val (title, subtitle, icon, onClick: () -> Unit) = when {
                    currentUser?.isLocalUser == false -> {
                        val syncStatusText = when (syncStatus) {
                            com.jumblemint.cows.sync.SyncStatus.SYNCING -> "Syncing..."
                            com.jumblemint.cows.sync.SyncStatus.SUCCESS -> "Synced"
                            com.jumblemint.cows.sync.SyncStatus.ERROR -> "Sync error"
                            else -> "Sync enabled"
                        }
                        Quadruple(
                            "Account: ${currentUser?.displayName ?: "Signed In"}",
                            "$syncStatusText • Tap to manage account",
                            when (syncStatus) {
                                com.jumblemint.cows.sync.SyncStatus.SYNCING -> Icons.Default.CloudSync
                                com.jumblemint.cows.sync.SyncStatus.ERROR -> Icons.Default.CloudOff
                                else -> Icons.Default.CloudDone
                            },
                            { onNavigateToAccountManagement?.invoke() ?: Unit } // Navigate to account management screen
                        )
                    }
                    currentUser?.isLocalUser == true -> Quadruple(
                        "Sign In & Sync",
                        "Currently using local storage only",
                        Icons.Default.CloudOff,
                        { onNavigateToSignIn?.invoke() ?: Unit }
                    )
                    else -> Quadruple(
                        "Sign In & Sync",
                        "Sync data across devices and collaborate",
                        Icons.Default.CloudSync,
                        { onNavigateToSignIn?.invoke() ?: Unit }
                    )
                }
                
                SettingsCard(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    onClick = onClick
                )
            }
            
            // Manual sync option for signed-in users
            if (isSignedIn && currentUser?.isLocalUser == false) {
                item {
                    SettingsCard(
                        title = "Sync Now",
                        subtitle = when (syncStatus) {
                            com.jumblemint.cows.sync.SyncStatus.SYNCING -> "Syncing in progress..."
                            com.jumblemint.cows.sync.SyncStatus.SUCCESS -> "Last sync successful"
                            com.jumblemint.cows.sync.SyncStatus.ERROR -> "Last sync failed - tap to retry"
                            else -> "Manually sync your data"
                        },
                        icon = when (syncStatus) {
                            com.jumblemint.cows.sync.SyncStatus.SYNCING -> Icons.Default.CloudSync
                            com.jumblemint.cows.sync.SyncStatus.ERROR -> Icons.Default.CloudOff
                            else -> Icons.Default.Refresh
                        },
                        onClick = {
                            if (syncStatus != com.jumblemint.cows.sync.SyncStatus.SYNCING) {
                                // Trigger manual sync
                                coroutineScope.launch {
                                    application.authService.startUserSync(application.syncService)
                                }
                            }
                        }
                    )
                }
            }

            
//            // Debug: Show current user info
//            item {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(16.dp)
//                    ) {
//                        Text(
//                            text = "Debug Info:",
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 14.sp
//                        )
//                        Text(
//                            text = "User: ${currentUser?.displayName ?: "None"}",
//                            fontSize = 12.sp
//                        )
//                        Text(
//                            text = "User ID: ${currentUser?.uid ?: "None"}",
//                            fontSize = 12.sp
//                        )
//                        Text(
//                            text = "Is Local: ${currentUser?.isLocalUser}",
//                            fontSize = 12.sp
//                        )
//                        Text(
//                            text = "Is Signed In: $isSignedIn",
//                            fontSize = 12.sp
//                        )
//                    }
//                }
//            }
            
            // Data Management Section
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = "Export Data",
                    subtitle = "Export your cattle data",
                    icon = Icons.Default.Download,
                    onClick = { showExportDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = "Import Data",
                    subtitle = "Import cattle data from file",
                    icon = Icons.Default.Upload,
                    onClick = { /* TODO: Implement import */ }
                )
            }
            
            item {
                SettingsCard(
                    title = if (uiState.isSampleDataInstalled) "Remove Sample Data" else "Add Sample Data",
                    subtitle = if (uiState.isSampleDataInstalled) "Delete sample cattle and pastures" else "Add sample cattle and pastures for testing",
                    icon = if (uiState.isSampleDataInstalled) Icons.Default.DeleteSweep else Icons.Default.Add,
                    onClick = { showSampleDataDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = "Delete Data",
                    subtitle = "Select which data groups to delete",
                    icon = Icons.Default.Warning,
                    onClick = { showDeleteDataDialog = true }
                )
            }
            
            // App Information Section
            item {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = "Version",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
            
            item {
                SettingsCard(
                    title = "About",
                    subtitle = "Cattle Manager App",
                    icon = Icons.Default.Help,
                    onClick = { }
                )
            }
        }
    }
    
    // Dialogs
    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.exportData(format)
                showExportDialog = false
            }
        )
    }
    
    if (showSampleDataDialog) {
        SampleDataDialog(
            isSampleDataInstalled = uiState.isSampleDataInstalled,
            onDismiss = { showSampleDataDialog = false },
            onInstall = {
                viewModel.installSampleData()
                // Trigger sync after installing sample data if signed in
                if (isSignedIn && currentUser?.isLocalUser == false) {
                    coroutineScope.launch { application.authService.startUserSync(application.syncService) }
                }
                showSampleDataDialog = false
            },
            onRemove = {
                viewModel.deleteSampleData()
                // Trigger sync after deleting sample data if signed in
                if (isSignedIn && currentUser?.isLocalUser == false) {
                    coroutineScope.launch { application.authService.startUserSync(application.syncService) }
                }
                showSampleDataDialog = false
            }
        )
    }
    
    if (showDeleteDataDialog) {
        DeleteDataSelectiveDialog(
            onDismiss = { showDeleteDataDialog = false },
            onConfirm = { selection ->
                if (isSignedIn && currentUser?.isLocalUser == false) {
                    // When signed in, clear corresponding server collections first and pause realtime
                    val uid = currentUser?.uid
                    if (uid != null) {
                        coroutineScope.launch {
                            try {
                                // Stop realtime to avoid re-downloading while wiping
                                application.syncService.stopRealtimeSync(uid)
                                val collections = mutableListOf<String>().apply {
                                    if (selection.cows) add("cows")
                                    if (selection.pastures) add("pastures")
                                    if (selection.activities) add("activities")
                                    if (selection.notes) add("notes")
                                    if (selection.tagColors) add("tagColors")
                                    if (selection.activityTypes) add("activityTypes")
                                    if (selection.settings) add("settings")
                                }
                                if (collections.isNotEmpty()) {
                                    application.syncService.clearServerCollections(uid, collections)
                                }
                            } catch (e: Exception) {
                                // Log and continue with local deletion to avoid being stuck
                                println("Failed to clear some server collections before local delete: ${e.message}")
                            } finally {
                                // Perform local deletion and then restart sync (which will see empty server state)
                                viewModel.deleteSelectedData(selection)
                                application.authService.startUserSync(application.syncService)
                            }
                        }
                    } else {
                        // Fallback: just delete locally
                        viewModel.deleteSelectedData(selection)
                    }
                } else {
                    // Not signed in: just delete locally
                    viewModel.deleteSelectedData(selection)
                }
                showDeleteDataDialog = false
            }
        )
    }
    
    // Show messages and errors
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // You could show a snackbar here if you have access to it
            // For now, we'll just clear the message after a delay
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }
    
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // You could show an error snackbar here if you have access to it
            // For now, we'll just clear the error after a delay
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text(
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onExport("CSV") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CSV")
                    }
                    Button(
                        onClick = { onExport("JSON") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("JSON")
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SampleDataDialog(
    isSampleDataInstalled: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isSampleDataInstalled) "Remove Sample Data" else "Add Sample Data") 
        },
        text = {
            Column {
                Text(
                    text = if (isSampleDataInstalled) {
                        "This will remove all sample cattle, pastures, and activities from your database. This action cannot be undone."
                    } else {
                        "This will add sample cattle, pastures, and activities to help you explore the app's features. You can remove this data later."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (!isSampleDataInstalled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sample data includes:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Multiple sample cattle across ages and genders with family relationships (mothers, fathers, siblings, calves)\n" +
                               "• 4 pastures with different acreages\n" +
                               "• Rich activities (births, moves, sales, work) and helpful notes to explore app features",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (isSampleDataInstalled) onRemove else onInstall,
                colors = if (isSampleDataInstalled) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (isSampleDataInstalled) "Remove" else "Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteDataSelectiveDialog(
    onDismiss: () -> Unit,
    onConfirm: (com.jumblemint.cows.ui.viewmodel.SettingsViewModel.DeleteSelection) -> Unit
) {
    var cows by remember { mutableStateOf(false) }
    var pastures by remember { mutableStateOf(false) }
    var activities by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(false) }
    var tagColors by remember { mutableStateOf(false) }
    var activityTypes by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Delete Data",
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select the types of data to delete:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = cows, onCheckedChange = { cows = it })
                    Text("Cows")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pastures, onCheckedChange = { pastures = it })
                    Text("Pastures")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activities, onCheckedChange = { activities = it })
                    Text("Activities")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notes, onCheckedChange = { notes = it })
                    Text("Notes")
                }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tagColors, onCheckedChange = { tagColors = it })
                    Text("Tag Color Settings")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activityTypes, onCheckedChange = { activityTypes = it })
                    Text("Activity Type Settings")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = settings, onCheckedChange = { settings = it })
                    Text("Other Settings (app preferences only)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Details:\n• Tag Color Settings: deletes all custom tag colors, then restores the default set.\n• Activity Type Settings: deletes all custom activity types, then restores the default set.\n• Other Settings: deletes only app preferences (like sample data flag, default calf pasture preference, filters). Your cows, pastures, activities, notes remain untouched.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Warning: This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        com.jumblemint.cows.ui.viewmodel.SettingsViewModel.DeleteSelection(
                            cows = cows,
                            pastures = pastures,
                            activities = activities,
                            notes = notes,
                            tagColors = tagColors,
                            activityTypes = activityTypes,
                            settings = settings
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}