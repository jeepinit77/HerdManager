package com.jumblemint.cows.ui.components

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
    onDismiss: () -> Unit,
    viewModel: com.jumblemint.cows.ui.viewmodel.CowsViewModel? = null
) {
    var currentFilterState by remember { mutableStateOf(initialFilterState) }
    val scrollState = rememberScrollState()
    val dialogContentPadding = 16.dp // Standard padding for the dialog content area
    
    // Track preview result count
    var previewCount by remember { mutableStateOf<Int?>(null) }
    
    // Calculate preview count when filters change
    LaunchedEffect(currentFilterState) {
        viewModel?.let {
            previewCount = it.getPreviewResultCount(
                previewClassifications = currentFilterState.classifications,
                previewGenders = currentFilterState.genders,
                previewPastures = currentFilterState.pastures,
                previewBreeds = currentFilterState.breeds,
                previewStatuses = currentFilterState.statuses,
                previewTagColors = currentFilterState.tagColors,
                previewIsWatched = currentFilterState.isWatched,
                previewAgeRanges = currentFilterState.selectedAgeRanges
            )
        }
    }

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
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center) 
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd) 
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Filters")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)) 

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
                        options = AgeUtils.ageRanges, 
                        selectedOptions = currentFilterState.selectedAgeRanges.mapNotNull { key -> 
                            AgeUtils.ageRanges.find { it.key == key } 
                        },
                        onSelectionChanged = { selectedAgeRangeObjects -> 
                            currentFilterState = currentFilterState.copy(selectedAgeRanges = selectedAgeRangeObjects.map { it.key })
                        },
                        itemLabel = { it.label }
                    )
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = {
                                    val nextState = when (currentFilterState.isWatched) {
                                        null -> true
                                        true -> false
                                        false -> null
                                    }
                                    currentFilterState = currentFilterState.copy(isWatched = nextState)
                                })
                                .padding(vertical = 0.dp), 
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Watched Status", 
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Checkbox(
                                checked = currentFilterState.isWatched ?: false, 
                                onCheckedChange = { 
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, bottom = 0.dp)
                        )
                        Divider(
                            modifier = Modifier
                                .padding(top = 1.dp) 
                                .fillMaxWidth() 
                        )
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

                // Result count display
                previewCount?.let { count ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$count result${if (count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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

@OptIn(ExperimentalFoundationApi::class) 
@Composable
private fun CollapsibleFilterSection(
    title: String, 
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope() 

    Column(modifier = Modifier.padding(vertical = 0.dp)) { 
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded } 
                .padding(vertical = 0.dp), 
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

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
                .padding(top = 1.dp) 
                .fillMaxWidth() 
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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) { 
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = { onSelectionChanged(emptyList()) },
                enabled = selectedOptions.isNotEmpty(),
                modifier = Modifier
                    .alpha(if (selectedOptions.isNotEmpty()) 1f else 0f)
                    .heightIn(min = 0.dp) // Allow button to shrink vertically
            ) {
                Text("Clear")
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 0.dp), 
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary, 
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary, 
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary 
                    )
                )
            }
        }
        Divider(
            modifier = Modifier
                .padding(top = 1.dp) 
                .fillMaxWidth() 
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

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp, top = 0.dp)) { 
        var showDialog by remember { mutableStateOf(false) }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val showClearButton = selectedOptions.isNotEmpty() && options.size <= chipDisplayThreshold
            val showSelectEditButton = options.size > chipDisplayThreshold

            TextButton(
                onClick = { 
                    if (showClearButton) onSelectionChanged(emptyList()) 
                    else if (showSelectEditButton) showDialog = true 
                },
                enabled = showClearButton || showSelectEditButton,
                modifier = Modifier
                    .alpha(if (showClearButton || showSelectEditButton) 1f else 0f)
                    .heightIn(min = 0.dp) // Allow button to shrink vertically
            ) {
                Text(
                    if (showSelectEditButton) {
                        if (selectedOptions.isEmpty()) "Select" else "Edit"
                    } else {
                        "Clear" 
                    }
                )
            }
        }

        if (options.size <= chipDisplayThreshold) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 0.dp), 
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary, 
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary, 
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary 
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
        Divider(
            modifier = Modifier
                .padding(top = 1.dp) 
                .fillMaxWidth() 
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
    com.jumblemint.cows.ui.components.AppAlertDialog(
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
                            onCheckedChange = null, 
                            colors = CheckboxDefaults.colors( 
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        ) 
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = itemLabel(option),
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
