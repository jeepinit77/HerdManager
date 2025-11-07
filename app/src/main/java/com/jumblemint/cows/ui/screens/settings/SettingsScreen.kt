package com.jumblemint.cows.ui.screens.settings

// import androidx.compose.animation.core.FastOutSlowInEasing // Removed
// import androidx.compose.animation.core.RepeatMode // Removed
// import androidx.compose.animation.core.animateFloat // Removed
// import androidx.compose.animation.core.infiniteRepeatable // Removed
// import androidx.compose.animation.core.rememberInfiniteTransition // Removed
// import androidx.compose.animation.core.tween // Removed
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.jumblemint.cows.data.import.ConflictResolution
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jumblemint.cows.ui.components.AppAlertDialog
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.theme.SmartText
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import androidx.compose.ui.graphics.vector.ImageVector
import com.jumblemint.cows.ui.components.WobblingLightbulbIcon // Added/Ensured import
import com.jumblemint.cows.data.model.ActivityTypeConfig
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.data.model.Breed
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.model.TagColor
import com.jumblemint.cows.ui.components.SetupWizardDialog
import android.text.format.DateUtils
import com.jumblemint.cows.ui.components.FocusAwareLiveSync

// Helper data class for quadruple values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Removed local PulsingLightbulbIcon definition

data class SettingsRowModel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector? = null,
    val customIconContent: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

data class SettingsSectionModel(
    val title: String,
    val initiallyExpanded: Boolean,
    val rows: List<SettingsRowModel>
)

private fun formatRelativeSyncTime(timestamp: Long?): String? {
    if (timestamp == null || timestamp <= 0L) return null
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSignIn: (() -> Unit)? = null,
    onNavigateToHerds: (() -> Unit)? = null,
    onNavigateToAccountManagement: (() -> Unit)? = null,
    onNavigateToTagColors: (() -> Unit)? = null,
    onNavigateToActivityTypes: (() -> Unit)? = null,
    onNavigateToBreeds: (() -> Unit)? = null,
    onNavigateToThemeSettings: (() -> Unit)? = null,
    onNavigateToBulkAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
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
            database.activityTypeConfigDao(),
            database.breedDao()
        )
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application, repository)
    )

    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val isSignedIn by application.authService.isSignedIn.collectAsState(initial = false)
    val syncStatus by application.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    val lastSyncTimestamp by application.syncService.lastSyncTime.collectAsState(initial = null)

    val uiState by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSampleDataDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showSetupWizardConfirmation by remember { mutableStateOf(false) }
    var showSetupWizard by remember { mutableStateOf(false) }
    var showIdentifierModeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val defaultBreeds = remember { Breed.getDefaultBreeds() }
    val wizardTagColors = remember { TagColor.getWizardColorOptions() }
    val defaultActivityTypes = remember { ActivityTypeConfig.getDefaultActivityTypes() }
    val tipsManager = remember { com.jumblemint.cows.data.preferences.TipsManager(context) }

    FocusAwareLiveSync(
        orchestrator = application.syncOrchestrator,
        screenKey = "Settings",
        intervalMs = 20_000L,
        leadingRun = true
    )
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            val cursor = context.contentResolver.query(fileUri, null, null, null, null)
            val fileName = cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else null
                } else null
            }
            
            val format = when {
                fileName?.endsWith(".csv", ignoreCase = true) == true -> "CSV"
                fileName?.endsWith(".json", ignoreCase = true) == true -> "JSON"
                else -> "JSON" // Default to JSON
            }
            viewModel.importData(fileUri, format)
        }
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                try {
                    val format = uiState.pendingExportFormat ?: "JSON"
                    val (sourcePath, _) = viewModel.prepareExportData(format)
                    val sourceFile = java.io.File(sourcePath)
                    
                    context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    snackbarHostState.showSnackbar("Export completed successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMsg,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError()
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
                viewModel.clearMessage()
            }
        }
    }


    Column(
        modifier = modifier
    ) {
        var searchQuery by rememberSaveable { mutableStateOf("") }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            label = { Text("Search settings") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            colors = defaultOutlinedTextFieldColors()
        )

        val identifierSummary = when (uiState.identifierMode) {
            AnimalIdentifierMode.NAMES -> "Use animal names"
            AnimalIdentifierMode.TAG_NUMBERS -> "Use tag numbers"
            AnimalIdentifierMode.BOTH -> "Use both names and tags"
        }
        val isNamesOnly = uiState.identifierMode == AnimalIdentifierMode.NAMES

        val sections = listOf(
            SettingsSectionModel(
                title = "App Preferences",
                initiallyExpanded = true,
                rows = listOf(
                    SettingsRowModel(
                        title = "Theme Settings",
                        subtitle = "Customize app colors and appearance",
                        icon = Icons.Filled.Palette,
                        onClick = { onNavigateToThemeSettings?.invoke() }
                    ),
                    SettingsRowModel(
                        title = "Reset Tips",
                        subtitle = "Show all coach marks and tips again",
                        customIconContent = { WobblingLightbulbIcon() },
                        onClick = {
                            coroutineScope.launch {
                                tipsManager.enableAllTips()
                                snackbarHostState.showSnackbar("Tips have been reset")
                            }
                        }
                    )
                )
            ),
            SettingsSectionModel(
                title = "Herd Setup & Tools",
                initiallyExpanded = true,
                rows = listOf(
                    SettingsRowModel(
                        title = "Animal Identification",
                        subtitle = identifierSummary,
                        icon = Icons.Filled.Label,
                        onClick = { showIdentifierModeDialog = true }
                    ),
                    SettingsRowModel(
                        title = "Run Setup Wizard",
                        subtitle = "Reset breeds, pastures, tag colors, and activities",
                        icon = Icons.Filled.AutoAwesome,
                        onClick = { showSetupWizardConfirmation = true }
                    ),
                    SettingsRowModel(
                        title = "Bulk Add Animals",
                        subtitle = "Quickly enter multiple animals",
                        icon = Icons.Filled.LibraryAdd,
                        onClick = { onNavigateToBulkAdd?.invoke() }
                    ),
                    SettingsRowModel(
                        title = if (isNamesOnly) "Tagging Colors" else "Tag Colors",
                        subtitle = if (isNamesOnly) "Edit colors you use" else "Manage available tag colors",
                        icon = Icons.Filled.ColorLens,
                        onClick = { onNavigateToTagColors?.invoke() }
                    ),
                    SettingsRowModel(
                        title = "Activity Types",
                        subtitle = "Manage activity types and fields",
                        icon = Icons.Filled.Assignment,
                        onClick = { onNavigateToActivityTypes?.invoke() }
                    ),
                    SettingsRowModel(
                        title = "Breeds",
                        subtitle = "Manage available cattle breeds",
                        icon = Icons.Filled.Pets,
                        onClick = { onNavigateToBreeds?.invoke() }
                    )
                )
            ),
            run {
                val (title, subtitle, icon, onClickAction) = when {
                    currentUser?.isLocalUser == false -> {
                        val lastSyncDisplay = remember(lastSyncTimestamp, uiState.lastSyncTime) {
                            formatRelativeSyncTime(lastSyncTimestamp) ?: uiState.lastSyncTime
                        }
                        val syncStatusText = when (syncStatus) {
                            SyncStatus.SYNCING -> "Syncing..."
                            SyncStatus.SUCCESS -> "Last synced: ${lastSyncDisplay ?: "Just now"}"
                            SyncStatus.ERROR -> "Sync error occurred"
                            else -> lastSyncDisplay?.let { "Last synced: $it" } ?: "Sync enabled"
                        }
                        Quadruple(
                            "Account: ${currentUser?.displayName ?: currentUser?.email ?: "Signed In"}",
                            "$syncStatusText • Tap to manage",
                            Icons.Filled.CloudSync as ImageVector?,
                            { onNavigateToAccountManagement?.invoke() }
                        )
                    }
                    currentUser?.isLocalUser == true -> Quadruple(
                        "Sign In & Sync",
                        "Currently using local storage only",
                        Icons.Filled.CloudOff as ImageVector?,
                        { onNavigateToSignIn?.invoke() }
                    )
                    else -> Quadruple(
                        "Sign In & Sync",
                        "Sync data across devices and collaborate",
                        Icons.Filled.CloudUpload as ImageVector?,
                        { onNavigateToSignIn?.invoke() }
                    )
                }
                val showSyncNow = isSignedIn && currentUser?.isLocalUser == false
                val accountRows = buildList {
                    add(
                        SettingsRowModel(
                            title = title,
                            subtitle = subtitle,
                            icon = icon,
                            onClick = { onClickAction?.invoke() }
                        )
                    )
                    if (showSyncNow) {
                        add(
                            SettingsRowModel(
                                title = "Sync Now",
                                subtitle = when (syncStatus) {
                                    SyncStatus.SYNCING -> "Syncing in progress..."
                                    SyncStatus.SUCCESS -> "Last sync successful"
                                    SyncStatus.ERROR -> "Last sync failed - tap to retry"
                                    else -> "Manually sync your data"
                                },
                                icon = when (syncStatus) {
                                    SyncStatus.SYNCING -> Icons.Filled.CloudSync
                                    SyncStatus.ERROR -> Icons.Filled.CloudOff
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
                        )
                    }
                }
                SettingsSectionModel(
                    title = "Account & Sync",
                    initiallyExpanded = false,
                    rows = accountRows
                )
            },
            SettingsSectionModel(
                title = "Data Management",
                initiallyExpanded = false,
                rows = listOf(
                    SettingsRowModel(
                        title = "Export Data",
                        subtitle = "Export your cattle data (CSV, JSON)",
                        icon = Icons.Filled.Download,
                        onClick = { showExportDialog = true }
                    ),
                    SettingsRowModel(
                        title = "Import Data",
                        subtitle = "Import cattle data from file",
                        icon = Icons.Filled.Upload,
                        onClick = { showImportDialog = true }
                    ),
                    SettingsRowModel(
                        title = if (uiState.isSampleDataInstalled) "Remove Sample Data" else "Add Sample Data",
                        subtitle = if (uiState.isSampleDataInstalled) "Delete sample cattle and pastures" else "Add sample data for testing",
                        icon = if (uiState.isSampleDataInstalled) Icons.Filled.DeleteSweep else Icons.Filled.PlaylistAdd,
                        onClick = { showSampleDataDialog = true }
                    ),
                    SettingsRowModel(
                        title = "Delete Data",
                        subtitle = "Selectively delete local and server data",
                        icon = Icons.Filled.WarningAmber,
                        onClick = { showDeleteDataDialog = true }
                    )
                )
            ),
            SettingsSectionModel(
                title = "Support & Info",
                initiallyExpanded = false,
                rows = listOf(
                    SettingsRowModel(
                        title = "About Cattle Manager",
                        subtitle = "Learn more about the app",
                        icon = Icons.Outlined.HelpOutline,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("About screen coming soon!")
                            }
                        }
                    ),
                    SettingsRowModel(
                        title = "Version",
                        subtitle = uiState.appVersion,
                        icon = Icons.Outlined.Info,
                        onClick = { }
                    )
                )
            )
        )

        val trimmedQuery = searchQuery.trim()
        val displayedSections = if (trimmedQuery.isBlank()) {
            sections
        } else {
            val lowerQuery = trimmedQuery.lowercase()
            sections.mapNotNull { section ->
                val filteredRows = section.rows.filter { row ->
                    row.title.contains(lowerQuery, ignoreCase = true) ||
                        row.subtitle.contains(lowerQuery, ignoreCase = true)
                }
                if (filteredRows.isNotEmpty()) {
                    section.copy(rows = filteredRows)
                } else {
                    null
                }
            }
        }

        val sectionExpansionStates by viewModel.sectionExpansionStates.collectAsState()

        LaunchedEffect(sections) {
            viewModel.syncSectionExpansionStates(
                sections.associate { section -> section.title to section.initiallyExpanded }
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (displayedSections.isEmpty() && trimmedQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedSections) { section ->
                    val isExpanded = sectionExpansionStates[section.title] ?: section.initiallyExpanded
                    ExpandableSettingsSection(
                        title = section.title,
                        expanded = isExpanded,
                        onExpandedChange = { expanded ->
                            viewModel.setSectionExpanded(section.title, expanded)
                        },
                        forceExpand = trimmedQuery.isNotBlank()
                    ) {
                        section.rows.forEachIndexed { index, row ->
                            SettingsRow(
                                title = row.title,
                                subtitle = row.subtitle,
                                icon = row.icon,
                                customIconContent = row.customIconContent,
                                onClick = row.onClick,
                                enabled = row.enabled,
                                isLast = index == section.rows.lastIndex
                            )
                        }
                    }
                }
            }
        }
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false, onClick = {}) 
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.fillMaxWidth())
    }

    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                coroutineScope.launch {
                    try {
                        val (_, fileName) = viewModel.prepareExportData(format)
                        viewModel.setPendingExportFormat(format)
                        exportLauncher.launch(fileName)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Export preparation failed: ${e.message}")
                    }
                }
                showExportDialog = false
            }
        )
    }
    
    if (showImportDialog) {
        ImportDataDialog(
            onDismiss = { showImportDialog = false },
            onImport = { 
                importLauncher.launch("*/*")
                showImportDialog = false
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
            },
            onRemove = {
                viewModel.deleteSampleData()
                if (isSignedIn && currentUser?.isLocalUser == false) {
                    coroutineScope.launch { application.authService.startUserSync(application.syncService) }
                }
                showSampleDataDialog = false
            }
        )
    }

    if (showSetupWizardConfirmation) {
        AppAlertDialog(
            onDismissRequest = { showSetupWizardConfirmation = false },
            title = { Text("Replace setup with new choices?") },
            text = {
                Text(
                    "We'll immediately delete your current breeds, pastures, tag colors, and activity types so you can pick new ones. " +
                        "Animals, recorded activities, and notes stay untouched."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSetupWizardConfirmation = false
                    showSetupWizard = true
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = { showSetupWizardConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSetupWizard) {
        SetupWizardDialog(
            defaultBreeds = defaultBreeds,
            availableTagColors = wizardTagColors,
            defaultActivityTypes = defaultActivityTypes,
            initialIdentifierMode = null,
            onExit = { showSetupWizard = false },
            onFinished = {
                showSetupWizard = false
                coroutineScope.launch {
                    try {
                        repository.markInitialSetupComplete()
                        snackbarHostState.showSnackbar("Setup wizard completed.")
                    } catch (t: Throwable) {
                        snackbarHostState.showSnackbar(
                            "Setup saved but status update failed: ${t.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            },
            onSaveIdentifierMode = { mode ->
                repository.setAnimalIdentifierMode(mode)
            },
            onSaveBreeds = { breeds ->
                repository.deleteAllBreeds()
                if (breeds.isNotEmpty()) {
                    repository.insertBreeds(breeds)
                }
            },
            onSaveTagColors = { colors ->
                repository.deleteAllTagColors()
                if (colors.isNotEmpty()) {
                    repository.insertTagColors(colors)
                    repository.insertOrUpdateSetting(
                        Settings(
                            SettingsKeys.TAG_COLORS,
                            colors.joinToString(separator = ",") { it.name }
                        )
                    )
                } else {
                    repository.insertOrUpdateSetting(Settings(SettingsKeys.TAG_COLORS, ""))
                }
            },
            onSaveActivities = { activityTypes ->
                repository.deleteAllActivityTypeConfigs()
                if (activityTypes.isNotEmpty()) {
                    repository.insertActivityTypes(activityTypes)
                }
            },
            onSavePastures = { pastures ->
                repository.deleteAllPastures()
                if (pastures.isNotEmpty()) {
                    pastures.forEach { repository.insertPasture(it) }
                }
            },
            onLaunchBulkAdd = {
                showSetupWizard = false
                onNavigateToBulkAdd?.invoke()
            }
        )
    }

    if (showDeleteDataDialog) {
        DeleteDataCategoryDialog(
            onDismiss = { showDeleteDataDialog = false },
            onConfirm = { selection ->
                coroutineScope.launch {
                    if (isSignedIn && currentUser?.isLocalUser == false) {
                        val uid = currentUser?.uid
                        if (uid != null) {
                            val remoteCollections = selection.toRemoteCollections()
                            val hasSelection = selection.hasAnySelection()
                            try {
                                application.syncService.stopRealtimeSync(uid)
                                viewModel.deleteSelectedData(selection)
                                if (remoteCollections.isNotEmpty()) {
                                    application.syncService.clearServerCollections(uid, remoteCollections)
                                }
                                if (hasSelection) {
                                    application.syncService.syncUserData(uid)
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error during selective delete: ${e.message}")
                            } finally {
                                application.syncService.startRealtimeSync(uid)
                            }
                        } else {
                            snackbarHostState.showSnackbar("User ID not found for sync reset.")
                        }
                    } else {
                        viewModel.deleteSelectedData(selection)
                    }
                }
                showDeleteDataDialog = false
            }
        )
    }
    
    uiState.conflictInfo?.let { conflictInfo ->
        ConflictResolutionDialog(
            conflictCount = conflictInfo.conflictCount,
            totalRecords = conflictInfo.totalRecords,
            onDismiss = { viewModel.cancelConflictResolution() },
            onResolve = { resolution -> viewModel.resolveConflict(resolution) }
        )
    }

    if (showIdentifierModeDialog) {
        var pendingMode by remember(uiState.identifierMode) { mutableStateOf(uiState.identifierMode) }
        AppAlertDialog(
            onDismissRequest = { showIdentifierModeDialog = false },
            title = { Text("Animal Identification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose how you identify animals throughout the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val options = listOf(
                        AnimalIdentifierMode.NAMES to "Names",
                        AnimalIdentifierMode.TAG_NUMBERS to "Tag Numbers",
                        AnimalIdentifierMode.BOTH to "Both"
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                    ) {
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = pendingMode == mode,
                                onClick = { pendingMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAnimalIdentifierMode(pendingMode)
                        showIdentifierModeDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { showIdentifierModeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ExpandableSettingsSection(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    forceExpand: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val actualExpanded = if (forceExpand) true else expanded

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = getCardColors(),
        border = null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExpandedChange(!expanded)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (actualExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (actualExpanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = actualExpanded) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    customIconContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (customIconContent != null) {
                customIconContent()
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SmartText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SmartText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null, 
    customIconContent: @Composable (() -> Unit)? = null, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors = getCardColors(),
    content: @Composable (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        enabled = enabled,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        border = null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (customIconContent != null) {
                customIconContent()
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SmartText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SmartText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content?.let {
                Spacer(modifier = Modifier.width(16.dp))
                it()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Filled.Download, 
                contentDescription = "Export Data",
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        title = { 
            Text(
                "Export Data",
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column {
                Text(
                    "Select the format for data export:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(
                        onClick = { selectedFormat = "CSV" },
                        label = { Text("CSV") },
                        selected = selectedFormat == "CSV"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        onClick = { selectedFormat = "JSON" },
                        label = { Text("JSON") },
                        selected = selectedFormat == "JSON"
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(selectedFormat) }) {
                Text("Export")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
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
    AppAlertDialog(
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
                    onDismiss()
                },
                colors = if (isSampleDataInstalled) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(if (isSampleDataInstalled) "Remove" else "Install")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteDataCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (SettingsViewModel.DeleteSelection) -> Unit
) {
    var deleteCows by remember { mutableStateOf(false) }
    var deletePastures by remember { mutableStateOf(false) }
    var deleteActivities by remember { mutableStateOf(false) }
    var deleteNotes by remember { mutableStateOf(false) }
    var deleteTagColors by remember { mutableStateOf(false) }
    var deleteActivityTypes by remember { mutableStateOf(false) }
    var deleteBreeds by remember { mutableStateOf(false) }
    var deleteSettings by remember { mutableStateOf(false) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.WarningAmber, contentDescription = "Delete Data") },
        title = { Text("Delete Data by Category") },
        text = {
            Column {
                Text(
                    "Select which data categories to delete. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Data Categories
                Text(
                    "Data Categories:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                CategoryCheckbox("Cattle Records", deleteCows) { deleteCows = it }
                CategoryCheckbox("Pastures", deletePastures) { deletePastures = it }
                CategoryCheckbox("Activities", deleteActivities) { deleteActivities = it }
                CategoryCheckbox("Notes", deleteNotes) { deleteNotes = it }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Configuration:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                CategoryCheckbox("Tagging Colors (resets to defaults)", deleteTagColors) { deleteTagColors = it }
                CategoryCheckbox("Activity Types (resets to defaults)", deleteActivityTypes) { deleteActivityTypes = it }
                CategoryCheckbox("Breeds (resets to defaults)", deleteBreeds) { deleteBreeds = it }
                CategoryCheckbox("App Settings", deleteSettings) { deleteSettings = it }
            }
        },
        confirmButton = {
            val hasSelection = deleteCows || deletePastures || deleteActivities || deleteNotes || 
                             deleteTagColors || deleteActivityTypes || deleteBreeds || deleteSettings
            Button(
                onClick = {
                    onConfirm(
                        SettingsViewModel.DeleteSelection(
                            cows = deleteCows,
                            pastures = deletePastures,
                            activities = deleteActivities,
                            notes = deleteNotes,
                            tagColors = deleteTagColors,
                            activityTypes = deleteActivityTypes,
                            breeds = deleteBreeds,
                            settings = deleteSettings
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                enabled = hasSelection
            ) {
                Text("Delete Selected")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CategoryCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDataDialog(
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Filled.Upload, 
                contentDescription = "Import Data",
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        title = { 
            Text(
                "Import Data",
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column {
                Text(
                    "Select a file to import cattle data from:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Supported formats: JSON (.json), CSV (.csv)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Warning: This will add data to your existing records. Make sure to backup your current data first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = onImport) {
                Text("Select File")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolutionDialog(
    conflictCount: Int,
    totalRecords: Int,
    onDismiss: () -> Unit,
    onResolve: (ConflictResolution) -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Filled.Warning, 
                contentDescription = "Conflicts Detected",
                tint = MaterialTheme.colorScheme.error
            ) 
        },
        title = { 
            Text(
                "Duplicate Records Found",
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column {
                Text(
                    "Found $conflictCount duplicate records out of $totalRecords total records.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "How would you like to handle the duplicates?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column {
                Button(
                    onClick = { onResolve(ConflictResolution.MERGE_NEW) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Existing Records")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { onResolve(ConflictResolution.KEEP_EXISTING) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keep Existing Records")
                }
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
