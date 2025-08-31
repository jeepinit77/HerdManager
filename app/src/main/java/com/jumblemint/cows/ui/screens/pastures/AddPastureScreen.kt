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
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sizeAcres by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") } // Input for description
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add New Pasture") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
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
                    Text(nameError!!, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = sizeAcres,
                    onValueChange = { sizeAcres = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Size in Acres (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField( // Field for description
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val acres = sizeAcres.toDoubleOrNull()
                        // Construct Pasture with description, no notes/lastMovedTo
                        onAddPasture(
                            Pasture(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                sizeAcres = acres,
                                description = description.takeIf { it.isNotBlank() }
                            )
                        )
                    } else {
                        nameError = "Name cannot be empty"
                    }
                }
            ) {
                Text("Add Pasture")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
