package com.jumblemint.cows.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import com.jumblemint.cows.util.AgeRange // Import for type
import com.jumblemint.cows.util.AgeUtils // Import centralized AgeUtils

// Local AgeRangeOptions object removed

const val CHIP_DISPLAY_THRESHOLD = 6

data class AnimalFilterState(
    val classifications: List<Classification> = emptyList(),
    val genders: List<Gender> = emptyList(),
    val pastures: List<String> = emptyList(),
    val breeds: List<String> = emptyList(),
    val statuses: List<Status> = emptyList(),
    val tagColors: List<String> = emptyList(),
    val isWatched: Boolean? = null,
    val selectedAgeRanges: List<String> = emptyList() 
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimalFilterScreen(
    initialFilterState: AnimalFilterState = AnimalFilterState(),
    availablePastures: List<String>,
    availableBreeds: List<String>,
    availableTagColors: List<String>,
    onApplyFilters: (AnimalFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var currentFilterState by remember { mutableStateOf(initialFilterState) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) 
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f) 
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Filter Animals",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )

                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                    MultiSelectChipGroup(
                        title = "Gender",
                        options = Gender.entries.toList(),
                        selectedOptions = currentFilterState.genders,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(genders = it) },
                        itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
                    )

                    MultiSelectChipGroup(
                        title = "Type", 
                        options = Classification.entries.toList(),
                        selectedOptions = currentFilterState.classifications,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(classifications = it) },
                        itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
                    )

                    MultiSelectChipGroup(
                        title = "Age Range",
                        options = AgeUtils.ageRanges, // Use centralized AgeUtils.ageRanges
                        // Map selected keys back to AgeRange objects for the chip group
                        selectedOptions = currentFilterState.selectedAgeRanges.mapNotNull { key -> 
                            AgeUtils.ageRanges.find { it.key == key } 
                        },
                        onSelectionChanged = { selectedAgeRangeObjects -> 
                            currentFilterState = currentFilterState.copy(selectedAgeRanges = selectedAgeRangeObjects.map { it.key })
                        },
                        itemLabel = { it.label } // Use the label from AgeRange object
                    )
                    
                    Text("Watched Status", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top=8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
                        val watchedOptions = listOf(null, true, false)
                        val watchedLabels = listOf("Any", "Yes", "No")
                        watchedOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = watchedOptions.size),
                                onClick = { currentFilterState = currentFilterState.copy(isWatched = option) },
                                selected = currentFilterState.isWatched == option,
                                icon = { 
                                    if (currentFilterState.isWatched == option && option != null) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(18.dp))
                                    }
                                }
                            ) {
                                Text(text = watchedLabels[index])
                            }
                        }
                    }

                    ConditionalMultiSelectFilter(
                        title = "Pasture",
                        options = availablePastures,
                        selectedOptions = currentFilterState.pastures,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(pastures = it) },
                        itemLabel = { it },
                        chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
                    )

                    ConditionalMultiSelectFilter(
                        title = "Breed",
                        options = availableBreeds,
                        selectedOptions = currentFilterState.breeds,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(breeds = it) },
                        itemLabel = { it },
                        chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
                    )

                    CollapsibleFilterSection(title = "More Filters") {
                        MultiSelectChipGroup(
                            title = "Status",
                            options = Status.entries.toList(),
                            selectedOptions = currentFilterState.statuses,
                            onSelectionChanged = { currentFilterState = currentFilterState.copy(statuses = it) },
                            itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
                        )
                        ConditionalMultiSelectFilter(
                            title = "Tag Color",
                            options = availableTagColors,
                            selectedOptions = currentFilterState.tagColors,
                            onSelectionChanged = { currentFilterState = currentFilterState.copy(tagColors = it) },
                            itemLabel = { it },
                            chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
                        )
                    }
                } 

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp) 
                ) {
                    OutlinedButton(
                        onClick = { currentFilterState = AnimalFilterState() }, 
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                    Button(
                        onClick = {
                            onApplyFilters(currentFilterState)
                            onDismiss() 
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
            } 
        } 
    } 
}

@Composable
private fun CollapsibleFilterSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                content()
            }
        }
        Divider(modifier=Modifier.padding(top=8.dp))
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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (selectedOptions.isNotEmpty()) {
                 TextButton(onClick = { onSelectionChanged(emptyList()) }) {
                    Text("Clear")
                }
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
                    label = { Text(itemLabel(option)) },
                    leadingIcon = if (isSelected) { { Icon(Icons.Filled.Check, "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp)) {
        var showDialog by remember { mutableStateOf(false) }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (selectedOptions.isNotEmpty() && options.size <= chipDisplayThreshold) {
                 TextButton(onClick = { onSelectionChanged(emptyList()) }) {
                    Text("Clear")
                }
            } else if (options.size > chipDisplayThreshold) {
                 TextButton(onClick = { showDialog = true }) { 
                    Text(if (selectedOptions.isEmpty()) "Select" else "Edit")
                }
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
                        label = { Text(itemLabel(option)) },
                        leadingIcon = if (isSelected) { { Icon(Icons.Filled.Check, "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxHeight(0.5f)) {
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
            }) { Text("Clear Selected") }
        }
    )
}
