package com.jumblemint.cows.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender

data class AnimalFilterState(
    val searchTerm: String = "",
    val classifications: List<Classification> = emptyList(),
    val gender: Gender? = null,
    val pasture: String? = null,
    // TODO: Add other filter properties here: status, breed, isWatched
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalFilterScreen(
    initialFilterState: AnimalFilterState = AnimalFilterState(),
    availablePastures: List<String>,
    // availableClassifications will use Classification.entries directly for now
    onApplyFilters: (AnimalFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentFilterState by remember { mutableStateOf(initialFilterState) }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Filter Animals", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = currentFilterState.searchTerm,
                onValueChange = { currentFilterState = currentFilterState.copy(searchTerm = it) },
                label = { Text("Search (Name, Tag, etc.)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Gender", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Gender.entries.forEachIndexed { index, genderOption ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Gender.entries.size),
                        onClick = { currentFilterState = currentFilterState.copy(gender = genderOption) },
                        selected = currentFilterState.gender == genderOption
                    ) {
                        Text(text = genderOption.name.lowercase().replaceFirstChar { it.titlecase() })
                    }
                }
            }
            // Optional: Add a clear button for gender
            TextButton(onClick = { currentFilterState = currentFilterState.copy(gender = null) }, modifier = Modifier.align(Alignment.End)) {
                Text("Any Gender")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Type (Classification)", style = MaterialTheme.typography.titleMedium)
            // Using Checkboxes for multi-select Classification
            Classification.entries.forEach { classification ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = currentFilterState.classifications.contains(classification),
                        onCheckedChange = {
                            val newClassifications = currentFilterState.classifications.toMutableList()
                            if (it) {
                                newClassifications.add(classification)
                            } else {
                                newClassifications.remove(classification)
                            }
                            currentFilterState = currentFilterState.copy(classifications = newClassifications)
                        }
                    )
                    Text(text = classification.name.lowercase().replaceFirstChar { it.titlecase() }, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (currentFilterState.classifications.isNotEmpty()){
                 TextButton(onClick = { currentFilterState = currentFilterState.copy(classifications = emptyList()) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Any Type")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pasture Filter (Dropdown - basic implementation)
            Text("Pasture", style = MaterialTheme.typography.titleMedium)
            var pastureExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = pastureExpanded,
                onExpandedChange = { pastureExpanded = !pastureExpanded },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = currentFilterState.pasture ?: "Any Pasture",
                    onValueChange = {}, // Read-only, selection happens in dropdown
                    readOnly = true,
                    label = { Text("Select Pasture") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pastureExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = pastureExpanded,
                    onDismissRequest = { pastureExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Any Pasture") },
                        onClick = {
                            currentFilterState = currentFilterState.copy(pasture = null)
                            pastureExpanded = false
                        }
                    )
                    availablePastures.forEach { pastureName ->
                        DropdownMenuItem(
                            text = { Text(pastureName) },
                            onClick = {
                                currentFilterState = currentFilterState.copy(pasture = pastureName)
                                pastureExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApplyFilters(currentFilterState)
                    onDismiss() // Dismiss after applying
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
