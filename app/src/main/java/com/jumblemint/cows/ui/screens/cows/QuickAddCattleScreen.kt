package com.jumblemint.cows.ui.screens.cows

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.ui.theme.BackgroundColorProvider
import com.jumblemint.cows.ui.theme.SmartText
import com.jumblemint.cows.ui.theme.contrastingTextColor
import com.jumblemint.cows.ui.theme.getCardBackgroundColor
import com.jumblemint.cows.ui.viewmodel.QuickAddCattleViewModel
import com.jumblemint.cows.ui.viewmodel.QuickAddCattleViewModelFactory
import com.jumblemint.cows.ui.viewmodel.QuickAddEntry
import com.jumblemint.cows.ui.viewmodel.QuickAddSection
import kotlinx.coroutines.launch

@Composable
fun QuickAddCattleScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val repository = remember(database) {
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

    val viewModel: QuickAddCattleViewModel = viewModel(
        factory = QuickAddCattleViewModelFactory(application, repository)
    )

    QuickAddCattleScreen(
        modifier = modifier,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        saveTriggered = saveTriggered,
        onSaveHandled = onSaveHandled,
        onUnsavedChangesChanged = onUnsavedChangesChanged,
        backPressed = backPressed,
        onBackHandled = onBackHandled
    )
}

@Composable
fun QuickAddCattleScreen(
    modifier: Modifier,
    viewModel: QuickAddCattleViewModel,
    onNavigateBack: () -> Unit,
    saveTriggered: Boolean,
    onSaveHandled: () -> Unit,
    onUnsavedChangesChanged: (Boolean) -> Unit,
    backPressed: Boolean,
    onBackHandled: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val unsavedCount = uiState.totalAnimals

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.hasUnsavedChanges) {
        onUnsavedChangesChanged(uiState.hasUnsavedChanges)
    }

    LaunchedEffect(saveTriggered) {
        if (saveTriggered) {
            viewModel.saveAnimals()
            onSaveHandled()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
            viewModel.acknowledgeSave()
        }
    }

    LaunchedEffect(backPressed) {
        if (backPressed) {
            if (uiState.hasUnsavedChanges) {
                showUnsavedChangesDialog = true
            } else {
                onNavigateBack()
            }
            onBackHandled()
        }
    }

    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onSave = {
                showUnsavedChangesDialog = false
                viewModel.saveAnimals()
            },
            onDiscard = {
                showUnsavedChangesDialog = false
                onUnsavedChangesChanged(false)
                onNavigateBack()
            }
        )
    }

    val listState = rememberLazyListState()
    val cardBackground = getCardBackgroundColor()
    val cardContentColor = cardBackground.contrastingTextColor()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val bannerColor = MaterialTheme.colorScheme.primaryContainer
        Surface(
            tonalElevation = 0.dp,
            color = bannerColor
        ) {
            SmartText(
                text = if (unsavedCount == 1) {
                    "1 animal not yet saved"
                } else {
                    "$unsavedCount animals not yet saved"
                },
                style = MaterialTheme.typography.bodyMedium,
                backgroundColor = bannerColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        val coroutineScope = rememberCoroutineScope()
        val toggleBackground = cardBackground
        val toggleContentColor = cardContentColor

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = toggleBackground,
                        contentColor = toggleContentColor
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    BackgroundColorProvider(backgroundColor = toggleBackground) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmartText(
                                text = "Limit Tag IDs to Numeric",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                backgroundColor = toggleBackground
                            )
                            IconButton(
                                onClick = { showHelpDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = toggleContentColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HelpOutline,
                                    contentDescription = "How to use quick add"
                                )
                            }
                            Switch(
                                checked = uiState.limitTagIdsToNumeric,
                                onCheckedChange = { viewModel.setLimitTagIdsToNumeric(it) },
                                enabled = !uiState.isSaving,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = toggleContentColor,
                                    uncheckedTrackColor = toggleContentColor.copy(alpha = 0.4f),
                                    uncheckedBorderColor = toggleContentColor.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(QuickAddSection.entries, key = { it.name }) { section ->
                QuickAddSectionCard(
                    section = section,
                    entries = uiState.sections[section].orEmpty(),
                    expanded = uiState.expandedSection == section,
                    enabled = !uiState.isSaving,
                    numericOnly = uiState.limitTagIdsToNumeric,
                    backgroundColor = cardBackground,
                    contentColor = cardContentColor,
                    onHeaderClick = { viewModel.setExpandedSection(section) },
                    onNameChanged = { entryId, value -> viewModel.updateName(section, entryId, value) },
                    onTagChanged = { entryId, value -> viewModel.updateTag(section, entryId, value) },
                    onRemoveEntry = { entryId -> viewModel.removeEntry(section, entryId) },
                    onEnsureLastEntryVisible = {
                        coroutineScope.launch {
                            listState.animateScrollBy(200f)
                        }
                    }
                )
            }
        }

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = { viewModel.saveAnimals() },
                    enabled = !uiState.isSaving && unsavedCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving…")
                    } else {
                        Text("Save Animals")
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            },
            title = { Text("Quick Add Help") },
            text = {
                Text(
                    "Expand a section to add animals of that type. Each row accepts a name, a Tag ID, or both—add at least one value before saving. New rows appear automatically after you start typing, and the delete button lets you remove an entry. Use the numeric toggle to open a number keypad for Tag IDs."
                )
            }
        )
    }
}

@Composable
private fun QuickAddSectionCard(
    section: QuickAddSection,
    entries: List<QuickAddEntry>,
    expanded: Boolean,
    enabled: Boolean,
    numericOnly: Boolean,
    backgroundColor: Color,
    contentColor: Color,
    onHeaderClick: () -> Unit,
    onNameChanged: (Long, String) -> Unit,
    onTagChanged: (Long, String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onEnsureLastEntryVisible: () -> Unit
) {
    var previousExpanded by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded && !previousExpanded) {
            onEnsureLastEntryVisible()
        }
        previousExpanded = expanded
    }

    var previousLastEntryId by remember { mutableStateOf(entries.lastOrNull()?.id) }
    LaunchedEffect(entries.lastOrNull()?.id) {
        val currentLast = entries.lastOrNull()
        val currentLastId = currentLast?.id
        if (expanded && currentLastId != null && currentLastId != previousLastEntryId && currentLast?.hasContent() == false) {
            onEnsureLastEntryVisible()
        }
        previousLastEntryId = currentLastId
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 2.dp else 0.dp)
    ) {
        BackgroundColorProvider(backgroundColor = backgroundColor) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val enteredCount = entries.count { it.hasContent() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .clickable(enabled = enabled, onClick = onHeaderClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmartText(
                        text = "${section.displayName} ($enteredCount)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        backgroundColor = backgroundColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = contentColor
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        entries.forEach { entry ->
                            QuickAddRow(
                                entry = entry,
                                enabled = enabled,
                                numericOnly = numericOnly,
                                contentColor = contentColor,
                                onNameChanged = { onNameChanged(entry.id, it) },
                                onTagChanged = { onTagChanged(entry.id, it) },
                                onRemove = { onRemoveEntry(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddRow(
    entry: QuickAddEntry,
    enabled: Boolean,
    numericOnly: Boolean,
    contentColor: Color,
    onNameChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    val fieldContainerColor = MaterialTheme.colorScheme.surface
    val fieldTextColor = fieldContainerColor.contrastingTextColor()
    val placeholderColor = fieldTextColor.copy(alpha = 0.6f)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldContainerColor,
        unfocusedContainerColor = fieldContainerColor,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = fieldTextColor.copy(alpha = 0.4f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = fieldTextColor.copy(alpha = 0.8f),
        focusedTextColor = fieldTextColor,
        unfocusedTextColor = fieldTextColor,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor
    )
    val keyboardOptions = if (numericOnly) {
        KeyboardOptions(keyboardType = KeyboardType.Number)
    } else {
        KeyboardOptions.Default
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = entry.name,
            onValueChange = onNameChanged,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            singleLine = true,
            maxLines = 1,
            enabled = enabled,
            label = { Text("Name") },
            placeholder = { Text("Name", color = placeholderColor, maxLines = 1) },
            colors = textFieldColors
        )
        OutlinedTextField(
            value = entry.tagNumber,
            onValueChange = onTagChanged,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            singleLine = true,
            maxLines = 1,
            enabled = enabled,
            label = { Text("Tag ID") },
            placeholder = { Text("Tag ID", color = placeholderColor, maxLines = 1) },
            colors = textFieldColors,
            keyboardOptions = keyboardOptions
        )
        if (entry.hasContent()) {
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = contentColor
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove animal")
            }
        }
    }
}
