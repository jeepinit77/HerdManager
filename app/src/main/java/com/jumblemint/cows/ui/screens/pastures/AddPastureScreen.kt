package com.jumblemint.cows.ui.screens.pastures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.model.Pasture

@Composable
fun PastureDetailScreen(
    onSave: (Pasture) -> Unit,
    onCancel: () -> Unit,
    editPasture: Pasture? = null,
    modifier: Modifier = Modifier,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {}
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

    // Handle save trigger from top bar
    LaunchedEffect(saveTriggered) {
        if (saveTriggered) {
            handleSave()
            onSaveHandled()
        }
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
}
