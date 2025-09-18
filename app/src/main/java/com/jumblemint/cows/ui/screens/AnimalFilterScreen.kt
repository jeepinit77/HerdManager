package com.jumblemint.cows.ui.screens

import androidx.compose.foundation.clickable
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
import com.jumblemint.cows.data.model.Status

const val CHIP_DISPLAY_THRESHOLD = 6

data class AnimalFilterState(
    val searchTerm: String = "",
    val classifications: List<Classification> = emptyList(),
    val genders: List<Gender> = emptyList(),
    val pastures: List<String> = emptyList(),
    val breeds: List<String> = emptyList(),
    val statuses: List<Status> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimalFilterScreen(
    initialFilterState: AnimalFilterState = AnimalFilterState(),
    availablePastures: List<String>,
    availableBreeds: List<String>,
    onApplyFilters: (AnimalFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentFilterState by remember { mutableStateOf(initialFilterState) }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f) 
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                "Filter Animals",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
            )

            OutlinedTextField(
                value = currentFilterState.searchTerm,
                onValueChange = { currentFilterState = currentFilterState.copy(searchTerm = it) },
                label = { Text("Search (Name, Tag, etc.)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            MultiSelectChipGroup(
                title = "Status",
                options = Status.entries.toList(),
                selectedOptions = currentFilterState.statuses,
                onSelectionChanged = { newStatuses -> currentFilterState = currentFilterState.copy(statuses = newStatuses) },
                itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
            )

            MultiSelectChipGroup(
                title = "Gender",
                options = Gender.entries.toList(),
                selectedOptions = currentFilterState.genders,
                onSelectionChanged = { newGenders -> currentFilterState = currentFilterState.copy(genders = newGenders) },
                itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
            )

            MultiSelectChipGroup(
                title = "Type (Classification)",
                options = Classification.entries.toList(),
                selectedOptions = currentFilterState.classifications,
                onSelectionChanged = { newClassifications -> currentFilterState = currentFilterState.copy(classifications = newClassifications) },
                itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
            )

            ConditionalMultiSelectFilter(
                title = "Pasture",
                options = availablePastures,
                selectedOptions = currentFilterState.pastures,
                onSelectionChanged = { newPastures -> currentFilterState = currentFilterState.copy(pastures = newPastures) },
                itemLabel = { it },
                chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
            )

            ConditionalMultiSelectFilter(
                title = "Breed",
                options = availableBreeds,
                selectedOptions = currentFilterState.breeds,
                onSelectionChanged = { newBreeds -> currentFilterState = currentFilterState.copy(breeds = newBreeds) },
                itemLabel = { it },
                chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { currentFilterState = AnimalFilterState() }, // Reset state
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear All")
                }
                Button(
                    onClick = {
                        onApplyFilters(currentFilterState)
                        // onDismiss() // Intentionally not dismissing here; caller handles dismiss or apply also dismisses.
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filters")
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // For bottom padding after buttons
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiSelectChipGroup(
    title: String,
    options: List<T>,
    selectedOptions: List<T>,
    onSelectionChanged: (List<T>) -> Unit,
    itemLabel: (T) -> String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { onSelectionChanged(emptyList()) }) {
                 Text(if (selectedOptions.isEmpty()) "Any" else "Clear")
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = selectedOptions.toMutableList()
                        if (isSelected) {
                            newSelection.remove(option)
                        } else {
                            newSelection.add(option)
                        }
                        onSelectionChanged(newSelection)
                    },
                    label = { Text(itemLabel(option)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> ConditionalMultiSelectFilter(
    title: String,
    options: List<T>,
    selectedOptions: List<T>,
    onSelectionChanged: (List<T>) -> Unit,
    itemLabel: (T) -> String,
    chipDisplayThreshold: Int
) {
    if (options.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        var showDialog by remember { mutableStateOf(false) }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { 
                if (options.size > chipDisplayThreshold && options.isNotEmpty()) {
                    showDialog = true 
                } else {
                    onSelectionChanged(emptyList()) 
                }
            }) {
                Text(if (selectedOptions.isEmpty() || (options.size > chipDisplayThreshold && !showDialog) ) "Any" else if (options.size > chipDisplayThreshold && showDialog) "Select" else "Clear")
            }
        }

        if (options.size <= chipDisplayThreshold) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSelection = selectedOptions.toMutableList()
                            if (isSelected) {
                                newSelection.remove(option)
                            } else {
                                newSelection.add(option)
                            }
                            onSelectionChanged(newSelection)
                        },
                        label = { Text(itemLabel(option)) }
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(if (selectedOptions.isEmpty()) "Select $title..." else "${selectedOptions.size} selected - Edit")
            }

            if (showDialog) {
                MultiSelectDropdownDialog(
                    title = "Select $title",
                    options = options,
                    selectedOptions = selectedOptions,
                    onSelectionChanged = onSelectionChanged,
                    onDismiss = { showDialog = false },
                    itemLabel = itemLabel
                )
            }
        }
    }
}

@Composable
private fun <T> MultiSelectDropdownDialog(
    title: String,
    options: List<T>,
    selectedOptions: List<T>,
    onSelectionChanged: (List<T>) -> Unit,
    onDismiss: () -> Unit,
    itemLabel: (T) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxHeight(0.5f)) { // Allow dialog content to scroll and limit height
                options.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { 
                                val newSelection = selectedOptions.toMutableList()
                                if (isSelected) newSelection.remove(option) else newSelection.add(option)
                                onSelectionChanged(newSelection)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null) 
                        Spacer(Modifier.width(8.dp))
                        Text(itemLabel(option))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { 
                onSelectionChanged(emptyList())
                // onDismiss() // Let user click Done to dismiss after clearing
            }) { Text("Clear Selected") }
        }
    )
}
