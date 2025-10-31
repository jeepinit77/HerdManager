package com.jumblemint.cows.ui.screens.cows

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.ui.theme.contrastingTextColor
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.ui.viewmodel.QuickAddCattleViewModel
import com.jumblemint.cows.ui.viewmodel.QuickAddCattleViewModelFactory
import com.jumblemint.cows.ui.viewmodel.QuickAddEntry
import com.jumblemint.cows.ui.viewmodel.QuickAddSection
import kotlinx.coroutines.delay

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

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            tonalElevation = 0.dp,
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Text(
                text = if (unsavedCount == 1) {
                    "1 animal not yet saved"
                } else {
                    "$unsavedCount animals not yet saved"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiaryContainer.contrastingTextColor(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(QuickAddSection.entries, key = { it.name }) { section ->
                    QuickAddSectionCard(
                        section = section,
                        entries = uiState.sections[section].orEmpty(),
                        expanded = uiState.expandedSection == section,
                        enabled = !uiState.isSaving,
                        onHeaderClick = { viewModel.setExpandedSection(section) },
                        onNameChanged = { entryId, value -> viewModel.updateName(section, entryId, value) },
                        onTagChanged = { entryId, value -> viewModel.updateTag(section, entryId, value) },
                        onRemoveEntry = { entryId -> viewModel.removeEntry(section, entryId) }
                    )
                }
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
}

@Composable
private fun QuickAddSectionCard(
    section: QuickAddSection,
    entries: List<QuickAddEntry>,
    expanded: Boolean,
    enabled: Boolean,
    onHeaderClick: () -> Unit,
    onNameChanged: (Long, String) -> Unit,
    onTagChanged: (Long, String) -> Unit,
    onRemoveEntry: (Long) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(entries.size, expanded) {
        if (expanded) {
            delay(120)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Surface(
        tonalElevation = if (expanded) 2.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clickable(enabled = enabled, onClick = onHeaderClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
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
                    entries.forEachIndexed { index, entry ->
                        QuickAddRow(
                            entry = entry,
                            enabled = enabled,
                            onNameChanged = { onNameChanged(entry.id, it) },
                            onTagChanged = { onTagChanged(entry.id, it) },
                            onRemove = { onRemoveEntry(entry.id) },
                            modifier = if (index == entries.lastIndex) {
                                Modifier.bringIntoViewRequester(bringIntoViewRequester)
                            } else {
                                Modifier
                            }
                        )
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
    onNameChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
            enabled = enabled,
            label = { Text("Name") },
            colors = defaultOutlinedTextFieldColors()
        )
        OutlinedTextField(
            value = entry.tagNumber,
            onValueChange = onTagChanged,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            singleLine = true,
            enabled = enabled,
            label = { Text("Tag number") },
            colors = defaultOutlinedTextFieldColors()
        )
        if (entry.hasContent()) {
            IconButton(
                onClick = onRemove,
                enabled = enabled
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove animal")
            }
        }
    }
}
