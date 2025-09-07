package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState // Added explicit import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.TagColor
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.viewmodel.TagColorsViewModel
import com.jumblemint.cows.ui.viewmodel.TagColorsViewModelFactory
import com.github.skydoves.colorpicker.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagColorsManagementScreen(
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
    
    val viewModel: TagColorsViewModel = viewModel(
        factory = TagColorsViewModelFactory(
            repository = repository,
            syncService = application.syncService,
            getUserId = { currentUser?.uid ?: "" }
        )
    )
    
    val tagColors by viewModel.tagColors.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingColor by remember { mutableStateOf<TagColor?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastDeleted by remember { mutableStateOf<TagColor?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Tag Colors") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Color")
                    }
                    var showResetConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        showResetConfirm = true
                    }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset to defaults")
                    }
                    if (showResetConfirm) {
                        AlertDialog(
                            onDismissRequest = { showResetConfirm = false },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                            title = { Text("Reset tag colors?") },
                            text = { Text("This will remove all custom colors and reinstall the default colors. This action cannot be undone.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showResetConfirm = false
                                    viewModel.resetToDefaults()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Reset to default colors")
                                    }
                                }) { Text("Reset") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tagColors) { tagColor ->
                TagColorItem(
                    tagColor = tagColor,
                    onEdit = { editingColor = it },
                    onDelete = { color ->
                        lastDeleted = color
                        viewModel.deleteTagColor(color)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Color deleted",
                                actionLabel = "Undo"
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                lastDeleted?.let { viewModel.restoreTagColor(it) }
                                lastDeleted = null
                            }
                        }
                    }
                )
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog || editingColor != null) {
        TagColorDialog(
            tagColor = editingColor,
            onDismiss = {
                showAddDialog = false
                editingColor = null
            },
            onSave = { name, color ->
                if (editingColor != null) {
                    viewModel.updateTagColor(editingColor!!.copy(
                        name = name,
                        colorValue = color.toArgb(),
                        updatedAt = System.currentTimeMillis()
                    ))
                } else {
                    viewModel.addTagColor(name, color.toArgb())
                }
                showAddDialog = false
                editingColor = null
            }
        )
    }
}

@Composable
fun TagColorItem(
    tagColor: TagColor,
    onEdit: (TagColor) -> Unit,
    onDelete: (TagColor) -> Unit
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
            // Color preview
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tagColor.toColor())
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Color name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tagColor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Actions
            Row {
                IconButton(onClick = { onEdit(tagColor) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete(tagColor) }) {
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

@Composable
fun TagColorDialog(
    tagColor: TagColor? = null,
    onDismiss: () -> Unit,
    onSave: (String, Color) -> Unit
) {
    var name by remember { mutableStateOf(tagColor?.name ?: "") }
    var selectedColor by remember { mutableStateOf(tagColor?.toColor() ?: Color.Red) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (tagColor != null) "Edit Tag Color" else "Add Tag Color") 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Color Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Color:")
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { showColorPicker = true }
                    )
                    
                    TextButton(onClick = { showColorPicker = true }) {
                        Text("Choose Color")
                    }
                }
                
                if (showColorPicker) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            HsvColorPicker(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                controller = rememberColorPickerController(),
                                onColorChanged = { colorEnvelope: ColorEnvelope ->
                                    selectedColor = colorEnvelope.color
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(
                                onClick = { showColorPicker = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedColor)
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