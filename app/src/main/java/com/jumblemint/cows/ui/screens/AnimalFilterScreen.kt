package com.jumblemint.cows.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
// import androidx.compose.ui.unit.Dp // Not strictly needed as a param type if using dp directly
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jumblemint.cows.data.model.Classification // Ensure this import and class exists
import com.jumblemint.cows.data.model.Gender // Ensure this import and class exists
import com.jumblemint.cows.data.model.Status // Ensure this import and class exists
import com.jumblemint.cows.util.AgeRange // Ensure this import and class exists
import com.jumblemint.cows.util.AgeUtils // Ensure this import and class exists
import kotlinx.coroutines.launch

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
    val dialogContentPadding = 16.dp // Standard padding for the dialog content area

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
            Column(modifier = Modifier.padding(dialogContentPadding)) { // Main content area padding
                Box(modifier = Modifier.fillMaxWidth()) { 
                    Text(
                        "Filter Animals",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center) 
                    )
                    // Close button as per commit message
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd) 
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Filters")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)) 

                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                    // Gender Filter
                    MultiSelectChipGroup(
                        title = "Gender",
                        options = Gender.entries.toList(), // Assumes Gender enum exists
                        selectedOptions = currentFilterState.genders,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(genders = it) },
                        itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
                    )

                    // Type (Classification) Filter
                    MultiSelectChipGroup(
                        title = "Type", 
                        options = Classification.entries.toList(), // Assumes Classification enum exists
                        selectedOptions = currentFilterState.classifications,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(classifications = it) },
                        itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
                    )

                    // Age Range Filter
                    MultiSelectChipGroup(
                        title = "Age Range",
                        options = AgeUtils.ageRanges, // Assumes AgeUtils.ageRanges is defined
                        selectedOptions = currentFilterState.selectedAgeRanges.mapNotNull { key -> 
                            AgeUtils.ageRanges.find { it.key == key } 
                        },
                        onSelectionChanged = { selectedAgeRangeObjects -> 
                            currentFilterState = currentFilterState.copy(selectedAgeRanges = selectedAgeRangeObjects.map { it.key })
                        },
                        itemLabel = { it.label }
                    )
                    
                    // Watched Status Filter (Checkbox as per commit message)
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = {
                                    // Cycle through: null (Any) -> true (Yes) -> false (No) -> null (Any)
                                    val nextState = when (currentFilterState.isWatched) {
                                        null -> true
                                        true -> false
                                        false -> null
                                    }
                                    currentFilterState = currentFilterState.copy(isWatched = nextState)
                                })
                                .padding(vertical = 2.dp), // Compact title row padding
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Watched Status", style = MaterialTheme.typography.titleMedium)
                            Checkbox(
                                checked = currentFilterState.isWatched ?: false, 
                                onCheckedChange = { // Click is handled by Row, but provide for accessibility if needed
                                     val nextState = when (currentFilterState.isWatched) {
                                        null -> true
                                        true -> false
                                        false -> null
                                    }
                                    currentFilterState = currentFilterState.copy(isWatched = nextState)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                )
                            )
                        }
                         Text(
                            text = when (currentFilterState.isWatched) {
                                true -> "Only showing watched"
                                false -> "Only showing not watched"
                                null -> "Showing all (watched & not watched)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, bottom = 0.dp)
                        )
                        Divider(
                            modifier = Modifier
                                .padding(top = 2.dp) 
                                .fillMaxWidth() // Divider fills width within parent's padding
                        )
                    }

                    // Pasture Filter
                    ConditionalMultiSelectFilter(
                        title = "Pasture",
                        options = availablePastures,
                        selectedOptions = currentFilterState.pastures,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(pastures = it) },
                        itemLabel = { it },
                        chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
                    )

                    // Breed Filter
                    ConditionalMultiSelectFilter(
                        title = "Breed",
                        options = availableBreeds,
                        selectedOptions = currentFilterState.breeds,
                        onSelectionChanged = { currentFilterState = currentFilterState.copy(breeds = it) },
                        itemLabel = { it },
                        chipDisplayThreshold = CHIP_DISPLAY_THRESHOLD
                    )

                    // Collapsible Section for More Filters (as per commit message)
                    CollapsibleFilterSection(title = "More Filters") {
                        // Status Filter
                        MultiSelectChipGroup(
                            title = "Status",
                            options = Status.entries.toList(), // Assumes Status enum exists
                            selectedOptions = currentFilterState.statuses,
                            onSelectionChanged = { currentFilterState = currentFilterState.copy(statuses = it) },
                            itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } }
                        )
                        // Tag Color Filter
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
                        onClick = { currentFilterState = AnimalFilterState() }, // Clear All
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

@OptIn(ExperimentalFoundationApi::class) // For BringIntoViewRequester
@Composable
private fun CollapsibleFilterSection(
    title: String, 
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope() // For launching bringIntoView

    Column(modifier = Modifier.padding(vertical = 0.dp)) { // Compact layout for the section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded } 
                .padding(vertical = 2.dp), // Compact title row
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

        // BringIntoViewRequester logic as per commit message
        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                scope.launch { 
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
                content()
            }
        }
        Divider(
            modifier = Modifier
                .padding(top = 2.dp)
                .fillMaxWidth() // Divider fills width within parent's padding
        )
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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) { // Compact section layout
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp), // Compact title row
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            // Clear button visibility logic as per commit message
            TextButton(
                onClick = { onSelectionChanged(emptyList()) },
                enabled = selectedOptions.isNotEmpty(),
                modifier = Modifier.alpha(if (selectedOptions.isNotEmpty()) 1f else 0f) 
            ) {
                Text("Clear")
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 0.dp), // Compact chip row
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
                    // Chip colors using primary theme as per commit message
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary, 
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary, 
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary 
                    )
                )
            }
        }
        Divider(
            modifier = Modifier
                .padding(top = 2.dp)
                .fillMaxWidth() // Divider fills width within parent's padding
        )
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

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) { // Compact section layout
        var showDialog by remember { mutableStateOf(false) }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp), // Compact title row
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            // Select/Edit/Clear button logic as per commit message
            val showClearButton = selectedOptions.isNotEmpty() && options.size <= chipDisplayThreshold
            val showSelectEditButton = options.size > chipDisplayThreshold

            TextButton(
                onClick = { 
                    if (showClearButton) onSelectionChanged(emptyList()) 
                    else if (showSelectEditButton) showDialog = true 
                },
                enabled = showClearButton || showSelectEditButton,
                modifier = Modifier.alpha(if (showClearButton || showSelectEditButton) 1f else 0f) 
            ) {
                Text(
                    if (showSelectEditButton) {
                        if (selectedOptions.isEmpty()) "Select" else "Edit"
                    } else {
                        "Clear" // This case is for when options.size <= chipDisplayThreshold
                    }
                )
            }
        }

        if (options.size <= chipDisplayThreshold) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 0.dp), // Compact chip row
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
                            selectedContainerColor = MaterialTheme.colorScheme.primary, 
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary, 
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary 
                        )
                    )
                }
            }
        } else { // options.size > chipDisplayThreshold, show button to open dialog
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp) // Give button some space
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
        Divider(
            modifier = Modifier
                .padding(top = 2.dp)
                .fillMaxWidth() // Divider fills width within parent's padding
        )
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
                        Checkbox(
                            checked = isSelected, 
                            onCheckedChange = null, // Click handled by Row
                            // Checkbox color in dialog as per commit message
                            colors = CheckboxDefaults.colors( 
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        ) 
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
                onSelectionChanged(emptyList()) // Clear selection in dialog
            }) { Text("Clear Selected") }
        }
    )
}
