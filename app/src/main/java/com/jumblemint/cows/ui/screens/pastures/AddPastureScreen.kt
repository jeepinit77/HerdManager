package com.jumblemint.cows.ui.screens.pastures

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.model.Pasture
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastureScreen(
    onAddPasture: (Pasture) -> Unit,
    onCancel: () -> Unit,
    editPasture: Pasture? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var sizeAcres by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(editPasture) {
        editPasture?.let { pasture ->
            name = pasture.name
            sizeAcres = pasture.sizeAcres?.toString() ?: ""
            description = pasture.description ?: ""
        }
    }

    // Wrap AlertDialog in a Box that takes the modifier
    Box(
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        contentAlignment = Alignment.Center // Center the AlertDialog
    ) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(if (editPasture != null) "Edit Pasture" else "Add New Pasture") },
            text = {
                Column(
                    // Removed padding from here, can be added if needed around text fields specifically
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val acres = sizeAcres.toDoubleOrNull()
                            onAddPasture(
                                if (editPasture != null) {
                                    editPasture.copy(
                                        name = name.trim(),
                                        sizeAcres = acres,
                                        description = description.trim().takeIf { it.isNotBlank() }
                                    )
                                } else {
                                    Pasture(
                                        id = UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        sizeAcres = acres,
                                        description = description.trim().takeIf { it.isNotBlank() }
                                    )
                                }
                            )
                        } else {
                            nameError = "Name cannot be empty"
                        }
                    }
                ) {
                    Text(if (editPasture != null) "Save Changes" else "Add Pasture")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        )
    }
}
