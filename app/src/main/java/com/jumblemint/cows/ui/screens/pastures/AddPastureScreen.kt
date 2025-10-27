package com.jumblemint.cows.ui.screens.pastures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.ui.components.FocusAwareLiveSync

@Composable
fun PastureDetailScreen(
    onSave: (Pasture) -> Unit,
    onCancel: () -> Unit,
    editPasture: Pasture? = null,
    modifier: Modifier = Modifier,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as CattleApplication
    var name by remember { mutableStateOf("") }
    var sizeAcres by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    FocusAwareLiveSync(
        orchestrator = application.syncOrchestrator,
        screenKey = "AddPasture",
        intervalMs = 20_000L,
        leadingRun = true
    )

    // Track initial values to detect changes
    val initialName = remember(editPasture) { editPasture?.name ?: "" }
    val initialSizeAcres = remember(editPasture) { editPasture?.sizeAcres?.toString() ?: "" }
    val initialDescription = remember(editPasture) { editPasture?.description ?: "" }

    LaunchedEffect(editPasture) {
        editPasture?.let { pasture ->
            name = pasture.name
            sizeAcres = pasture.sizeAcres?.toString() ?: ""
            description = pasture.description ?: ""
        }
    }

    // Calculate if there are unsaved changes
    val currentHasUnsavedChanges = name.trim() != initialName.trim() ||
            sizeAcres.trim() != initialSizeAcres.trim() ||
            description.trim() != initialDescription.trim()

    // Notify parent about unsaved changes
    LaunchedEffect(currentHasUnsavedChanges) {
        onUnsavedChangesChanged(currentHasUnsavedChanges)
    }

    // Handle back press with unsaved changes warning
    LaunchedEffect(backPressed) {
        if (backPressed) {
            if (currentHasUnsavedChanges) {
                showUnsavedChangesDialog = true
            } else {
                onCancel()
            }
            onBackHandled()
        }
    }

    // Function to handle save logic
    val handleSave = {
        val trimmedName = name.trim()
        if (trimmedName.isNotBlank()) {
            val acres = sizeAcres.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val trimmedDescription = description.trim().takeIf { it.isNotBlank() }

            try {
                onSave(
                    if (editPasture != null) {
                        editPasture.copy(
                            name = trimmedName,
                            sizeAcres = acres,
                            description = trimmedDescription
                        )
                    } else {
                        Pasture(
                            id = "",
                            name = trimmedName,
                            sizeAcres = acres,
                            description = trimmedDescription
                        )
                    }
                )
            } catch (e: Exception) {
                println("PastureDetailScreen: Error creating pasture: ${e.message}")
                e.printStackTrace()
            }
        } else {
            nameError = "Name cannot be empty"
        }
    }

    // Handle save trigger from top bar (only when there are changes)
    LaunchedEffect(saveTriggered) {
        if (saveTriggered && currentHasUnsavedChanges) {
            handleSave()
            onSaveHandled()
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onSave = {
                showUnsavedChangesDialog = false
                handleSave()
            },
            onDiscard = {
                showUnsavedChangesDialog = false
                onCancel()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = if (it.isBlank()) "Name cannot be empty" else null
            },
            label = { Text("Pasture Name*") },
            isError = nameError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = defaultOutlinedTextFieldColors()
        )
        if (nameError != null) {
            Text(
                nameError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp) // Indent error slightly
            )
        }

        OutlinedTextField(
            value = sizeAcres,
            onValueChange = { value ->
                // Allow decimal but ensure it's a valid structure
                val filtered = value.filter { char -> char.isDigit() || char == '.' }
                if (filtered.count { it == '.' } <= 1) { // Allow only one decimal point
                    sizeAcres = filtered
                }
            },
            label = { Text("Size in Acres (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = defaultOutlinedTextFieldColors()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            colors = defaultOutlinedTextFieldColors()
        )
    }
}
