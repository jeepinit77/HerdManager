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
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    // Remember repository to avoid recreation on recomposition
    val repository = remember {
        CattleRepository(
            database.cowDao(), database.pastureDao(), database.activityDao(),
            database.settingsDao(), database.noteDao(), database.userDao(),
            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
            database.activityTypeConfigDao()
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastDeleted by remember { mutableStateOf<TagColor?>(null) }

    Scaffold(
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Tag Colors") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editingColor = null; showAddDialog = true }) { // Clear editingColor for add
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add New Color") // Changed Icon
                    }
                    var showResetConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset to Default Colors") // Changed Icon
                    }
                    if (showResetConfirm) {
                        AlertDialog(
                            onDismissRequest = { showResetConfirm = false },
                            icon = { Icon(Icons.Default.WarningAmber, contentDescription = "Warning") }, // Changed Icon
                            title = { Text("Reset Tag Colors?") },
                            text = { Text("This will remove all custom tag colors and reinstall the default set. This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showResetConfirm = false
                                        viewModel.resetToDefaults()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Tag colors reset to defaults.")
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
            )
        },
        contentWindowInsets = WindowInsets(0,0,0,0) // Kept as is, can be reviewed
    ) { paddingValues ->
        if (tagColors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Palette, contentDescription = "No colors", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No custom tag colors found.", style = MaterialTheme.typography.headlineSmall)
                    Text("Add colors using the '+' button above.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp), // Only horizontal for list items
                contentPadding = PaddingValues(vertical = 16.dp), // Vertical padding for overall list
                verticalArrangement = Arrangement.spacedBy(10.dp) // Spacing between items
            ) {
                items(tagColors, key = { it.id }) { tagColor -> // Added key for better performance
                    TagColorItem(
                        tagColor = tagColor,
                        onEdit = { editingColor = it },
                        onDelete = { color ->
                            lastDeleted = color // Store for potential undo
                            viewModel.deleteTagColor(color)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Color '${color.name}' deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    lastDeleted?.let { viewModel.restoreTagColor(it) }
                                }
                                lastDeleted = null // Clear after use
                            }
                        }
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(12.dp) // Softer corners
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Adjusted padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp) // Slightly smaller preview
                    .clip(CircleShape)
                    .background(tagColor.toColor())
                    .border(
                        width = 1.dp, // Thinner border
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), // Softer border
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = tagColor.name,
                style = MaterialTheme.typography.bodyLarge, // Adjusted style
                modifier = Modifier.weight(1f)
            )
            // Actions
            IconButton(onClick = { onEdit(tagColor) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${tagColor.name}", tint = MaterialTheme.colorScheme.primary) // <<< CHANGED ICON
            }
            IconButton(onClick = { onDelete(tagColor) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${tagColor.name}", tint = MaterialTheme.colorScheme.error) // Changed Icon
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // For AlertDialog
@Composable
fun TagColorDialog(
    tagColorToEdit: TagColor? = null, // Renamed for clarity
    onDismiss: () -> Unit,
    onSave: (name: String, colorInt: Int, id: String?) -> Unit // Pass ID for updates
) {
    var name by remember(tagColorToEdit) { mutableStateOf(tagColorToEdit?.name ?: "") }
    var selectedColor by remember(tagColorToEdit) { mutableStateOf(tagColorToEdit?.toColor() ?: Color(0xFFE91E63)) } // Default to a vibrant pink
    var nameError by remember { mutableStateOf<String?>(null) }

    val colorPickerController = rememberColorPickerController() // No explicit type

    LaunchedEffect(tagColorToEdit) {
        // The method to programmatically set the controller's initial color
        // is currently unresolved. This will lead to the picker defaulting to white.
        // Developer needs to investigate the library API for version 1.0.0.
    }

    AlertDialog(
        onDismissRequest = onDismiss,
//        icon = { Icon(if (tagColorToEdit != null) Icons.Filled.Edit else Icons.Filled.Add, contentDescription = null) },
        title = { Text(if (tagColorToEdit != null) "Edit Tag Color" else "Add New Tag Color") },
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
                    isError = nameError != null
                )
                if (nameError != null) {
                    Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selected Color:", style = MaterialTheme.typography.bodyLarge)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Increased height for better usability
                        .padding(vertical = 8.dp),
                    controller = colorPickerController,
                    initialColor = selectedColor, // Initialize picker with current color
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        selectedColor = colorEnvelope.color
                    }
                )
                // BrightnessSlider for more control, if desired
                 BrightnessSlider(
                     modifier = Modifier.fillMaxWidth().height(35.dp),
                     controller = colorPickerController,
                     borderRadius = 8.dp,
                     wheelRadius = 10.dp,
                     wheelColor = Color.White,
                     wheelPaint = remember { androidx.compose.ui.graphics.Paint().apply { color = Color.Black } },
                     initialColor = selectedColor // Initialize slider with current color
                 )
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
