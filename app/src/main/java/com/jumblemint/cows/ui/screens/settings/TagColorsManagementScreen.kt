package com.jumblemint.cows.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Keep this import
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit // <<< ADDED IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
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


    val scope = rememberCoroutineScope()
    var lastDeleted by remember { mutableStateOf<TagColor?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    
    LaunchedEffect(resetTriggered) {
        if (resetTriggered) {
            showResetConfirm = true
            onResetHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        
        if (tagColors.isEmpty()) {
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
                        Icons.Filled.Palette, 
                        contentDescription = "No colors", 
                        modifier = Modifier.size(48.dp), 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No tag colors found.", 
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Add colors using the + button.", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tagColors, key = { it.id }) { tagColor ->
                    TagColorItem(
                        tagColor = tagColor,
                        onEdit = { editingColor = it; showAddDialog = true },
                        onDelete = { color ->
                            lastDeleted = color
                            viewModel.deleteTagColor(color)
                            // Note: Undo functionality removed due to no snackbar in non-nested layout
                        }
                    )
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "Add Tag Color",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = { 
                Icon(
                    Icons.Default.WarningAmber, 
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                ) 
            },
            title = { 
                Text(
                    "Reset Tag Colors?",
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = { 
                Text(
                    "This will remove all custom tag colors and restore the default set. This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        viewModel.resetToDefaults()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { 
                    Text("Reset") 
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { 
                    Text("Cancel") 
                }
            }
        )
    }

    if (showAddDialog || editingColor != null) {
        TagColorDialog(
            tagColorToEdit = editingColor, // Pass the color to be edited
            onDismiss = {
                showAddDialog = false
                editingColor = null
            },
            onSave = { name, colorInt, id ->
                if (id != null && editingColor != null) { // Editing existing color
                    viewModel.updateTagColor(
                        editingColor!!.copy(
                            name = name,
                            colorValue = colorInt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else { // Adding new color
                    viewModel.addTagColor(name, colorInt)
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
        modifier = Modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tagColor.toColor())
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = tagColor.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onEdit(tagColor) }) {
                Icon(
                    Icons.Outlined.Edit, 
                    contentDescription = "Edit ${tagColor.name}", 
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { onDelete(tagColor) }) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    contentDescription = "Delete ${tagColor.name}", 
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagColorDialog(
    tagColorToEdit: TagColor? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorInt: Int, id: String?) -> Unit
) {
    var name by remember(tagColorToEdit) { mutableStateOf(tagColorToEdit?.name ?: "") }
    var selectedColor by remember(tagColorToEdit) { mutableStateOf(tagColorToEdit?.toColor() ?: Color(0xFFE91E63)) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val colorPickerController = rememberColorPickerController()
    
    // Theme-aware colors
    val wheelColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.outline
    val backgroundColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        title = { 
            Text(
                if (tagColorToEdit != null) "Edit Tag Color" else "Add New Tag Color",
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
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
                    label = { Text("Color Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (nameError != null) {
                    Text(
                        nameError!!, 
                        color = MaterialTheme.colorScheme.error, 
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Selected Color:", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.dp, borderColor, CircleShape)
                    )
                }
                
                // Color picker with theme-aware styling
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HsvColorPicker(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            controller = colorPickerController,
                            initialColor = selectedColor,
                            onColorChanged = { colorEnvelope: ColorEnvelope ->
                                selectedColor = colorEnvelope.color
                            }
                        )
                        BrightnessSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(35.dp),
                            controller = colorPickerController,
                            borderRadius = 8.dp,
                            wheelRadius = 8.dp,
                            wheelColor = wheelColor,
                            wheelPaint = remember { 
                                androidx.compose.ui.graphics.Paint().apply { 
                                    color = wheelColor.copy(alpha = 0.8f)
                                } 
                            },
                            initialColor = selectedColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedColor.toArgb(), tagColorToEdit?.id)
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

// Helper extension function
fun TagColor.toColor(): Color = Color(this.colorValue)
