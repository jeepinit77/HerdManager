package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.SettingsViewModel
import com.jumblemint.cows.ui.viewmodel.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao()
    )
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    var showTagColorsDialog by remember { mutableStateOf(false) }
    var showActivityTypesDialog by remember { mutableStateOf(false) }
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
                    onClick = { showTagColorsDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = "Activity Types",
                    subtitle = "Manage activity types",
                    icon = Icons.Default.Assignment,
                    onClick = { showActivityTypesDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = "Default Calf Pasture",
                    subtitle = uiState.defaultCalfPasture ?: "Not set",
                    icon = Icons.Default.Landscape,
                    onClick = { /* TODO: Implement pasture selection */ }
                )
            }
            
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
                    title = "Delete All Data",
                    subtitle = "Remove all cattle, pastures, and activities",
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
    if (showTagColorsDialog) {
        TagColorsDialog(
            currentColors = uiState.tagColors,
            onDismiss = { showTagColorsDialog = false },
            onSave = { colors ->
                viewModel.updateTagColors(colors)
                showTagColorsDialog = false
            }
        )
    }
    
    if (showActivityTypesDialog) {
        ActivityTypesDialog(
            currentTypes = uiState.activityTypes,
            onDismiss = { showActivityTypesDialog = false },
            onSave = { types ->
                viewModel.updateActivityTypes(types)
                showActivityTypesDialog = false
            }
        )
    }
    
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
                showSampleDataDialog = false
            },
            onRemove = {
                viewModel.deleteSampleData()
                showSampleDataDialog = false
            }
        )
    }
    
    if (showDeleteDataDialog) {
        DeleteAllDataDialog(
            onDismiss = { showDeleteDataDialog = false },
            onConfirm = {
                viewModel.deleteAllData()
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
fun TagColorsDialog(
    currentColors: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var colors by remember { mutableStateOf(currentColors.joinToString(", ")) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag Colors") },
        text = {
            Column {
                Text(
                    text = "Enter tag colors separated by commas:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = colors,
                    onValueChange = { colors = it },
                    label = { Text("Colors") },
                    placeholder = { Text("Red, Blue, Green, Yellow...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val colorList = colors.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(colorList)
                }
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

@Composable
fun ActivityTypesDialog(
    currentTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var types by remember { mutableStateOf(currentTypes.joinToString(", ")) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activity Types") },
        text = {
            Column {
                Text(
                    text = "Enter activity types separated by commas:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = types,
                    onValueChange = { types = it },
                    label = { Text("Activity Types") },
                    placeholder = { Text("MOVED, WEANED, SOLD, DECEASED...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val typeList = types.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
                    onSave(typeList)
                }
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
                        text = "• 10 sample cattle with various ages and genders\n" +
                               "• 3 pastures with different acreages\n" +
                               "• Sample activities like births, moves, and sales",
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
fun DeleteAllDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Delete All Data",
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = {
            Column {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This will permanently delete ALL data from your app:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• All cattle records\n" +
                           "• All pasture information\n" +
                           "• All activity history\n" +
                           "• All custom settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This action cannot be undone!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}