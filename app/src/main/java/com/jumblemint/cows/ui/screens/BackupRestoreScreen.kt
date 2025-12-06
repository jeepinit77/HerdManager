package com.jumblemint.cows.ui.screens

import android.app.Activity
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.viewmodel.BackupViewModel
import com.jumblemint.cows.ui.viewmodel.BackupViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(
        factory = BackupViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState(initial = false)
    val backupFrequency by viewModel.backupFrequency.collectAsState(initial = "DAILY")
    val customIntervalHours by viewModel.customIntervalHours.collectAsState(initial = 24L)
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState(initial = 0L)
    val googleDriveBackupEnabled by viewModel.googleDriveBackupEnabled.collectAsState(initial = false)
    val backupOnEventEnabled by viewModel.backupOnEventEnabled.collectAsState(initial = false)
    val driveBackups by viewModel.driveBackups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    
    val localBackupEnabled by viewModel.localBackupEnabled.collectAsState(initial = true)
    val localBackupUri by viewModel.localBackupUri.collectAsState(initial = null)
    val customIntervalValue by viewModel.customIntervalValue.collectAsState(initial = 1)
    val customIntervalUnit by viewModel.customIntervalUnit.collectAsState(initial = "DAYS")
    
    val backupHour by viewModel.backupHour.collectAsState(initial = 2)
    val backupMinute by viewModel.backupMinute.collectAsState(initial = 0)
    val backupDayOfWeek by viewModel.backupDayOfWeek.collectAsState(initial = 1)
    
    // Google Sign-In
    var googleAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
    }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                googleAccount = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                viewModel.fetchDriveBackups(googleAccount!!)
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Sign-in failed: ${e.message}") }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            googleAccount = account
            viewModel.fetchDriveBackups(account)
        }
    }
    
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Backup") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Restore") }
                )
            }
            
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTabIndex) {
                    0 -> BackupSettingsTab(
                        autoBackupEnabled = autoBackupEnabled,
                        onAutoBackupChanged = viewModel::setAutoBackupEnabled,
                        backupFrequency = backupFrequency,
                        onFrequencyChanged = viewModel::setBackupFrequency,
                        customIntervalValue = customIntervalValue,
                        customIntervalUnit = customIntervalUnit,
                        onCustomIntervalChanged = viewModel::setCustomInterval,
                        localBackupEnabled = localBackupEnabled,
                        onLocalBackupEnabledChanged = viewModel::setLocalBackupEnabled,
                        localBackupUri = localBackupUri,
                        onLocalBackupUriChanged = viewModel::setLocalBackupUri,
                        googleDriveBackupEnabled = googleDriveBackupEnabled,
                        onGoogleDriveBackupChanged = { enabled ->
                             viewModel.setGoogleDriveBackupEnabled(enabled)
                        },
                        backupOnEventEnabled = backupOnEventEnabled,
                        onBackupOnEventChanged = viewModel::setBackupOnEventEnabled,
                        lastBackupTimestamp = lastBackupTimestamp,
                        onBackupNow = {
                             viewModel.performManualBackup(googleAccount)
                        },
                        googleAccount = googleAccount,
                        onSignIn = {
                            val signInIntent = GoogleSignIn.getClient(context, gso).signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        onSignOut = {
                            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                                googleAccount = null
                                viewModel.setGoogleDriveBackupEnabled(false)
                            }
                        },
                        backupHour = backupHour,
                        backupMinute = backupMinute,
                        backupDayOfWeek = backupDayOfWeek,
                        onTimeChanged = viewModel::setBackupTime,
                        onDayOfWeekChanged = viewModel::setBackupDayOfWeek
                    )
                    1 -> RestoreTab(
                        onRestoreFromLocal = { uri -> viewModel.restoreFromLocal(uri) },
                        onRestoreFromDrive = { fileId -> 
                             if (googleAccount != null) {
                                 viewModel.restoreFromDrive(fileId, googleAccount!!)
                             }
                        },
                        driveBackups = driveBackups,
                        onConnectDrive = {
                            val signInIntent = GoogleSignIn.getClient(context, gso).signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        isDriveConnected = googleAccount != null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsTab(
    autoBackupEnabled: Boolean,
    onAutoBackupChanged: (Boolean) -> Unit,
    backupFrequency: String,
    onFrequencyChanged: (String) -> Unit,
    customIntervalValue: Int,
    customIntervalUnit: String,
    onCustomIntervalChanged: (Int, String) -> Unit,
    localBackupEnabled: Boolean,
    onLocalBackupEnabledChanged: (Boolean) -> Unit,
    localBackupUri: String?,
    onLocalBackupUriChanged: (Uri?) -> Unit,
    googleDriveBackupEnabled: Boolean,
    onGoogleDriveBackupChanged: (Boolean) -> Unit,
    backupOnEventEnabled: Boolean,
    onBackupOnEventChanged: (Boolean) -> Unit,
    lastBackupTimestamp: Long,
    onBackupNow: () -> Unit,
    googleAccount: GoogleSignInAccount?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    backupHour: Int,
    backupMinute: Int,
    backupDayOfWeek: Int,
    onTimeChanged: (Int, Int) -> Unit,
    onDayOfWeekChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        onLocalBackupUriChanged(uri)
    }
    
    val localFolderDisplay = remember(localBackupUri) {
        if (localBackupUri != null) {
            try {
                val uri = Uri.parse(localBackupUri)
                val docFile = DocumentFile.fromTreeUri(context, uri)
                docFile?.name ?: localBackupUri
            } catch (e: Exception) {
                "Unknown location"
            }
        } else {
            "Default Internal Storage"
        }
    }
    
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeChanged(hour, minute) },
            backupHour,
            backupMinute,
            false // 12 hour format
        )
    }
    
    val formattedTime = remember(backupHour, backupMinute) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, backupHour)
            set(java.util.Calendar.MINUTE, backupMinute)
        }
        val format = android.text.format.DateFormat.getTimeFormat(context)
        format.format(calendar.time)
    }
    
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        item {
            // CARD 1: BACKUP LOCATION
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = getCardColors()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backup Location", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Local Storage
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = localBackupEnabled, onCheckedChange = onLocalBackupEnabledChanged)
                        Text("Local Storage", style = MaterialTheme.typography.titleSmall)
                    }
                    
                    if (localBackupEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(localFolderDisplay, style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                                Text("Select Folder")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Google Drive
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = googleDriveBackupEnabled,
                            onCheckedChange = { 
                                if (it && googleAccount == null) {
                                    onSignIn()
                                } else {
                                    onGoogleDriveBackupChanged(it) 
                                }
                            }
                        )
                        Text("Google Drive", style = MaterialTheme.typography.titleSmall)
                    }
                    
                    if (googleDriveBackupEnabled || googleAccount != null) {
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            if (googleAccount == null) {
                                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text("Sign In to Google Drive")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Signed in as:", style = MaterialTheme.typography.labelSmall)
                                        Text(googleAccount.email ?: "User", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    TextButton(onClick = onSignOut) {
                                        Text("Disconnect")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // CARD 2: SCHEDULE
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = getCardColors()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Schedule", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoBackupEnabled, onCheckedChange = onAutoBackupChanged)
                        Text("Enable Scheduled Backups")
                    }
                    
                    if (autoBackupEnabled) {
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            // Daily
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = backupFrequency == "DAILY", 
                                    onClick = { onFrequencyChanged("DAILY") },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text("Daily")
                            }
                            
                            if (backupFrequency == "DAILY") {
                                Button(
                                    onClick = { timePickerDialog.show() },
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                ) {
                                    Text("At $formattedTime")
                                }
                            }

                            // Weekly
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = backupFrequency == "WEEKLY", 
                                    onClick = { onFrequencyChanged("WEEKLY") },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text("Weekly")
                            }
                            
                            if (backupFrequency == "WEEKLY") {
                                Column(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
                                    var expanded by remember { mutableStateOf(false) }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Every ")
                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                value = daysOfWeek.getOrElse(backupDayOfWeek - 1) { "Monday" },
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier.menuAnchor().width(150.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                daysOfWeek.forEachIndexed { index, day ->
                                                    DropdownMenuItem(
                                                        text = { Text(day) },
                                                        onClick = {
                                                            onDayOfWeekChanged(index + 1)
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { timePickerDialog.show() }) {
                                        Text("At $formattedTime")
                                    }
                                }
                            }

                            // Custom
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = backupFrequency == "CUSTOM", 
                                    onClick = { onFrequencyChanged("CUSTOM") },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text("Custom Interval")
                            }
                            
                            if (backupFrequency == "CUSTOM") {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Every", style = MaterialTheme.typography.bodyLarge)
                                        
                                        OutlinedTextField(
                                            value = customIntervalValue.toString(),
                                            onValueChange = { 
                                                if (it.isEmpty()) {
                                                    onCustomIntervalChanged(0, customIntervalUnit)
                                                } else {
                                                    it.toIntOrNull()?.let { value ->
                                                        onCustomIntervalChanged(value, customIntervalUnit)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.width(100.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val units = listOf("HOURS", "DAYS", "WEEKS", "MONTHS")
                                    SingleChoiceSegmentedButtonRow(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        units.forEachIndexed { index, unit ->
                                            SegmentedButton(
                                                selected = customIntervalUnit == unit,
                                                onClick = { onCustomIntervalChanged(customIntervalValue, unit) },
                                                shape = SegmentedButtonDefaults.itemShape(index, units.size)
                                            ) {
                                                Text(unit.lowercase().replaceFirstChar { it.uppercase() })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // CARD 3: TRIGGERS
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = getCardColors()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Triggers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupOnEventEnabled, onCheckedChange = onBackupOnEventChanged)
                        Text("Backup on significant events (e.g. Activity added)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // CARD 4: STATUS & MANUAL ACTION
            Card(modifier = Modifier.fillMaxWidth(), colors = getCardColors()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last Backup: ${if (lastBackupTimestamp > 0) SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp)) else "Never"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBackupNow, modifier = Modifier.fillMaxWidth()) {
                        Text("Backup Now")
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreTab(
    onRestoreFromLocal: (Uri) -> Unit,
    onRestoreFromDrive: (String) -> Unit,
    driveBackups: List<com.google.api.services.drive.model.File>,
    onConnectDrive: () -> Unit,
    isDriveConnected: Boolean
) {
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onRestoreFromLocal(it) }
    }

    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { launcher.launch(arrayOf("application/octet-stream", "application/x-sqlite3")) }, colors = getCardColors()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Restore from Local File")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Google Drive Backups", style = MaterialTheme.typography.titleMedium)
        }
        
        if (!isDriveConnected) {
            item {
                Button(onClick = onConnectDrive, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect Google Drive")
                }
            }
        } else {
            if (driveBackups.isEmpty()) {
                item {
                    Text("No backups found on Drive.", modifier = Modifier.padding(16.dp))
                }
            } else {
                items(driveBackups) { file ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onRestoreFromDrive(file.id) }, colors = getCardColors()) {
                         Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.CloudDownload, contentDescription = null)
                             Spacer(modifier = Modifier.width(16.dp))
                             Column {
                                 Text(file.name)
                                 file.createdTime?.let {
                                     Text(it.toString(), style = MaterialTheme.typography.bodySmall)
                                 }
                             }
                         }
                    }
                }
            }
        }
    }
}
