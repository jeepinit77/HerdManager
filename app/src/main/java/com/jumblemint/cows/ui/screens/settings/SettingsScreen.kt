package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.items // items is not directly used, LazyColumn's items { } is used.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info // Corrected import
import androidx.compose.material.icons.outlined.HelpOutline // Re-added this import
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
import com.jumblemint.cows.sync.SyncStatus
import com.jumblemint.cows.ui.viewmodel.SettingsViewModel
import com.jumblemint.cows.ui.viewmodel.SettingsViewModelFactory
// import kotlinx.coroutines.delay // Not used
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape // Added for SettingsCard
import com.jumblemint.cows.ui.theme.getCardColors

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
    onNavigateToActivityTypes: (() -> Unit)? = null,
    onNavigateToThemeSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication // application context
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
        CattleRepository(
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
    }
    val viewModel: SettingsViewModel = viewModel(
        // <<< Pass application to the factory
        factory = SettingsViewModelFactory(application, repository)
    )

    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val isSignedIn by application.authService.isSignedIn.collectAsState(initial = false)
    val syncStatus by application.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)

    val uiState by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showSampleDataDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // <<< LaunchedEffect for showing Snackbars for errors and messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMsg,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError() // Clear error after showing
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearMessage() // Clear message after showing
            }
        }
    }


    Column(
        modifier = modifier // Modifier is applied to the root Column
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(), // Ensure LazyColumn takes available space
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Consistent spacing
        ) {
            // Customization Section
            item {
                Text(
                    text = "Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                )
            }
            item { // <<< CORRECTED SYNTAX HERE
                SettingsCard(
                    title = "Tag Colors",
                    subtitle = "Manage available tag colors",
                    icon = Icons.Filled.ColorLens,
                    onClick = { onNavigateToTagColors?.invoke() }
                )
            }
            item {
                SettingsCard(
                    title = "Activity Types",
                    subtitle = "Manage activity types and fields",
                    icon = Icons.Filled.Assignment,
                    onClick = { onNavigateToActivityTypes?.invoke() }
                )
            }
            item {
                SettingsCard(
                    title = "Theme Settings",
                    subtitle = "Customize app colors and appearance",
                    icon = Icons.Filled.Palette,
                    onClick = { onNavigateToThemeSettings?.invoke() }
                )
            }

            // Account & Sync Section
            item {
                Text(
                    text = "Account & Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)
                )
            }
            item {
                val (title, subtitle, icon, onClickAction) = when {
                    currentUser?.isLocalUser == false -> {
                        val syncStatusText = when (syncStatus) {
                            SyncStatus.SYNCING -> "Syncing..."
                            // <<< Use uiState.lastSyncTime
                            SyncStatus.SUCCESS -> "Last synced: ${uiState.lastSyncTime ?: "Recently"}"
                            SyncStatus.ERROR -> "Sync error occurred"
                            else -> uiState.lastSyncTime?.let { "Last synced: $it" } ?: "Sync enabled"
                        }
                        Quadruple(
                            "Account: ${currentUser?.displayName ?: currentUser?.email ?: "Signed In"}",
                            "$syncStatusText • Tap to manage",
                            when (syncStatus) {
                                SyncStatus.SYNCING -> Icons.Filled.CloudSync
                                SyncStatus.ERROR -> Icons.Filled.CloudOff
                                else -> Icons.Filled.CloudDone
                            },
                            { onNavigateToAccountManagement?.invoke() }
                        )
                    }
                    currentUser?.isLocalUser == true -> Quadruple(
                        "Sign In & Sync",
                        "Currently using local storage only",
                        Icons.Filled.CloudOff,
                        { onNavigateToSignIn?.invoke() }
                    )
                    else -> Quadruple( // Not signed in
                        "Sign In & Sync",
                        "Sync data across devices and collaborate",
                        Icons.Filled.CloudUpload, // Changed icon to suggest action
                        { onNavigateToSignIn?.invoke() }
                    )
                }
                SettingsCard(title = title, subtitle = subtitle, icon = icon, onClick = { onClickAction?.invoke() })
            }

            if (isSignedIn && currentUser?.isLocalUser == false) {
                item {
                    SettingsCard(
                        title = "Sync Now",
                        subtitle = when (syncStatus) {
                            SyncStatus.SYNCING -> "Syncing in progress..."
                            SyncStatus.SUCCESS -> "Last sync successful" // Consider using uiState.lastSyncTime here too
                            SyncStatus.ERROR -> "Last sync failed - tap to retry"
                            else -> "Manually sync your data"
                        },
                        icon = when (syncStatus) {
                            SyncStatus.SYNCING -> Icons.Filled.CloudSync
                            SyncStatus.ERROR -> Icons.Filled.CloudOff // Or Icons.Filled.SyncProblem
                            else -> Icons.Filled.Refresh
                        },
                        onClick = {
                            if (syncStatus != SyncStatus.SYNCING) {
                                coroutineScope.launch {
                                    application.authService.startUserSync(application.syncService)
                                }
                            }
                        }
                    )
                }
            }

            // Data Management Section
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)
                )
            }
            item {
                SettingsCard(
                    title = "Export Data",
                    subtitle = "Export your cattle data (CSV, JSON)",
                    icon = Icons.Filled.Download,
                    onClick = { showExportDialog = true }
                )
            }
            item {
                SettingsCard(
                    title = "Import Data",
                    subtitle = "Import cattle data from file",
                    icon = Icons.Filled.Upload, // Or FileUpload
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Import feature coming soon!")
                        }
                    }
                )
            }
            item {
                SettingsCard(
                    title = if (uiState.isSampleDataInstalled) "Remove Sample Data" else "Add Sample Data",
                    subtitle = if (uiState.isSampleDataInstalled) "Delete sample cattle and pastures" else "Add sample data for testing",
                    icon = if (uiState.isSampleDataInstalled) Icons.Filled.DeleteSweep else Icons.Filled.PlaylistAdd,
                    onClick = { showSampleDataDialog = true }
                )
            }
            item {
                SettingsCard(
                    title = "Delete Data",
                    subtitle = "Selectively delete local and server data",
                    icon = Icons.Filled.WarningAmber, // Or Icons.Outlined.DeleteForever
                    onClick = { showDeleteDataDialog = true }
                )
            }

            // App Information Section
            item {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)
                )
            }
            item {
                SettingsCard(
                    title = "Version",
                    subtitle = uiState.appVersion, // <<< Use uiState.appVersion
                    icon = Icons.Outlined.Info,
                    onClick = { /* No action needed or show app details dialog */ }
                )
            }
            item {
                SettingsCard(
                    title = "About Cattle Manager",
                    subtitle = "Learn more about the app",
                    icon = Icons.Outlined.HelpOutline, // Or Icons.Filled.Help
                    onClick = {
                         coroutineScope.launch {
                            snackbarHostState.showSnackbar("About screen coming soon!")
                        }
                    }
                )
            }
        }
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false, onClick = {}) // Block interactions
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp))
    }

    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.exportData(format)
                showExportDialog = false
                // Message/error will be shown by LaunchedEffect observing uiState
            }
        )
    }

    if (showSampleDataDialog) {
        SampleDataDialog(
            isSampleDataInstalled = uiState.isSampleDataInstalled,
            onDismiss = { showSampleDataDialog = false },
            onInstall = {
                viewModel.installSampleData()
                if (isSignedIn && currentUser?.isLocalUser == false) {
                    coroutineScope.launch { application.authService.startUserSync(application.syncService) }
                }
                showSampleDataDialog = false
                // Message/error will be shown by LaunchedEffect observing uiState
            },
            onRemove = {
                viewModel.deleteSampleData()
                if (isSignedIn && currentUser?.isLocalUser == false) {
                    coroutineScope.launch { application.authService.startUserSync(application.syncService) }
                }
                showSampleDataDialog = false
                // Message/error will be shown by LaunchedEffect observing uiState
            }
        )
    }

    if (showDeleteDataDialog) {
        DeleteDataSelectiveDialog(
            onDismiss = { showDeleteDataDialog = false },
            onConfirm = { selection ->
                coroutineScope.launch {
                    if (isSignedIn && currentUser?.isLocalUser == false) {
                        val uid = currentUser?.uid
                        if (uid != null) {
                            try {
                                application.syncService.stopRealtimeSync(uid)
                                // The ViewModel's deleteSelectedData will handle showing a message/error
                                viewModel.deleteSelectedData(selection)
                                // No longer calling viewModel.setError here
                                application.syncService.startRealtimeSync(uid) // <<< CORRECTED CALL
                            } catch (e: Exception) {
                                // ViewModel should ideally catch its own errors. If not,
                                // we can set a general error here, but it's better if VM handles it.
                                // For now, relying on VM's error handling.
                                 snackbarHostState.showSnackbar("Error during selective delete: ${e.message}")
                            }
                        } else {
                             snackbarHostState.showSnackbar("User ID not found for sync reset.")
                        }
                    } else { // Local user or not signed in
                        viewModel.deleteSelectedData(selection)
                    }
                }
                showDeleteDataDialog = false
                 // Message/error will be shown by LaunchedEffect observing uiState
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors = getCardColors(),
    content: @Composable (() -> Unit)? = null // For optional extra content
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Consistent vertical padding
        enabled = enabled,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp), // Consistent shape
        colors = colors
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp) // Standard padding within the card
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // title can serve as description
                modifier = Modifier.size(28.dp), // Slightly larger icon
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            // If additional content is provided, display it here
            content?.let {
                Spacer(modifier = Modifier.width(16.dp)) // Add some space before the custom content
                it()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit // "CSV" or "JSON"
) {
    var selectedFormat by remember { mutableStateOf("CSV") } // Default to CSV

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Download, contentDescription = "Export Data") },
        title = { Text("Export Data") },
        text = {
            Column {
                Text("Select the format for data export:")
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { selectedFormat = "CSV" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFormat == "CSV") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {Text("CSV")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedFormat = "JSON" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFormat == "JSON") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("JSON")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(selectedFormat) }) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleDataDialog(
    isSampleDataInstalled: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (isSampleDataInstalled) Icons.Filled.DeleteSweep else Icons.Filled.PlaylistAdd, contentDescription = "Sample Data") },
        title = { Text(if (isSampleDataInstalled) "Remove Sample Data" else "Add Sample Data") },
        text = {
            Text(
                if (isSampleDataInstalled) "Are you sure you want to remove the sample cattle, pastures, and activities? This action cannot be undone."
                else "Would you like to add sample cattle, pastures, and activities to explore the app's features? You can remove it later."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSampleDataInstalled) onRemove() else onInstall()
                    onDismiss() // Dismiss after action
                },
                colors = if (isSampleDataInstalled) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteDataSelectiveDialog(
    onDismiss: () -> Unit,
    onConfirm: (SettingsViewModel.DeleteSelection) -> Unit
) {
    var deleteLocal by remember { mutableStateOf(true) }
    var deleteServer by remember { mutableStateOf(false) } // Default to not deleting server data for safety

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.WarningAmber, contentDescription = "Delete Data") },
        title = { Text("Delete Data Selectively") },
        text = {
            Column {
                Text("Choose which data to delete. This action cannot be undone.")
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteLocal,
                        onCheckedChange = { deleteLocal = it }
                    )
                    Text("Delete Local Data (on this device)")
                }
                //Spacer(modifier = Modifier.height(8.dp))
                //Row(verticalAlignment = Alignment.CenterVertically) {
                //    Checkbox(
                //        checked = deleteServer,
                //        onCheckedChange = { deleteServer = it }
                //    )
                //    Text("Delete Server Data (requires re-sync if enabled)")
                //}
                Text(
                    "Note: Server data deletion is currently disabled in this dialog. " +
                            "To delete server data, please use the account management screen after ensuring local data is backed up or not needed.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(SettingsViewModel.DeleteSelection(deleteLocal, deleteServer))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = deleteLocal || deleteServer // Only enable if at least one option is selected
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
