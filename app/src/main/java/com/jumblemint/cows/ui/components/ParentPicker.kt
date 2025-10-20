package com.jumblemint.cows.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors

private const val ALL_PASTURES_KEY = "__ALL__"
private const val UNASSIGNED_PASTURE_KEY = "__UNASSIGNED__"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentPicker(
    title: String,
    animals: List<Cow>,
    pastureNames: Map<String?, String>,
    classificationOptions: List<Classification> = emptyList(),
    enablePastureFilter: Boolean = true,
    allowClearSelection: Boolean = false,
    quickPicks: List<Cow> = emptyList(),
    onSelect: (Cow) -> Unit,
    onClearSelection: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close parent picker",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                var searchQuery by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name or tag") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                searchQuery = ""
                                focusRequester.requestFocus()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = defaultOutlinedTextFieldColors()
                )

                val quickPickItems = remember(quickPicks, animals) {
                    quickPicks.distinctBy { it.id }.filter { pick -> animals.any { it.id == pick.id } }
                }

                if (quickPickItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Recent picks",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickPickItems.forEach { cow ->
                                AssistChip(
                                    onClick = {
                                        focusManager.clearFocus()
                                        onSelect(cow)
                                        onDismiss()
                                    },
                                    label = {
                                        Text(
                                            text = formatPrimaryLabel(cow),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                var selectedClassifications by remember { mutableStateOf(setOf<Classification>()) }
                if (classificationOptions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                            Text(
                                "Filter by classification",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            classificationOptions.forEach { option ->
                                val isSelected = option in selectedClassifications
                                FilterChip(
                                    onClick = {
                                        selectedClassifications = if (isSelected) {
                                            selectedClassifications - option
                                        } else {
                                            selectedClassifications + option
                                        }
                                    },
                                    label = {
                                        Text(option.name.lowercase().replaceFirstChar { it.titlecase() })
                                    },
                                    selected = isSelected
                                )
                            }
                        }
                    }
                }

                var selectedPastureKey by remember { mutableStateOf(ALL_PASTURES_KEY) }
                val pastureOptions = remember(pastureNames) {
                    val sortedPastures = pastureNames.entries
                        .filter { it.key != null }
                        .sortedBy { it.value }
                    buildList {
                        add(ALL_PASTURES_KEY to "All Pastures")
                        pastureNames[null]?.let { name ->
                            add(UNASSIGNED_PASTURE_KEY to name)
                        }
                        sortedPastures.forEach { (id, name) ->
                            add((id ?: UNASSIGNED_PASTURE_KEY) to name)
                        }
                    }
                }
                if (enablePastureFilter) {
                    val selectedLabel = pastureOptions.find { it.first == selectedPastureKey }?.second ?: "All Pastures"
                    DropdownField(
                        value = selectedLabel,
                        onValueChange = { label ->
                            val option = pastureOptions.find { it.second == label }
                            selectedPastureKey = option?.first ?: ALL_PASTURES_KEY
                        },
                        label = "Filter by pasture",
                        options = pastureOptions.map { it.second },
                        modifier = Modifier.fillMaxWidth(),
                        showNoneOption = false
                    )
                }

                val filteredAnimals = remember(animals, searchQuery, selectedClassifications, selectedPastureKey) {
                    animals.filter { cow ->
                        val matchesSearch = searchQuery.isBlank() ||
                                cow.name?.contains(searchQuery, ignoreCase = true) == true ||
                                cow.tagNumber?.contains(searchQuery, ignoreCase = true) == true
                        val matchesClassification = selectedClassifications.isEmpty() ||
                                cow.classification in selectedClassifications
                        val matchesPasture = when (selectedPastureKey) {
                            ALL_PASTURES_KEY -> true
                            UNASSIGNED_PASTURE_KEY -> cow.pastureId == null
                            else -> cow.pastureId == selectedPastureKey
                        }
                        matchesSearch && matchesClassification && matchesPasture
                    }
                }

                if (allowClearSelection && onClearSelection != null) {
                    OutlinedButton(
                        onClick = {
                            onClearSelection()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Clear selection")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredAnimals.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No animals match your filters.",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || selectedClassifications.isNotEmpty() || selectedPastureKey != ALL_PASTURES_KEY) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try adjusting your search or filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredAnimals, key = { it.id }) { cow ->
                                ParentPickerItem(
                                    cow = cow,
                                    pastureNames = pastureNames,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onSelect(cow)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentPickerItem(
    cow: Cow,
    pastureNames: Map<String?, String>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val name = cow.name?.takeIf { it.isNotBlank() }
    val tag = cow.tagNumber?.takeIf { it.isNotBlank() }
    val pastureLabel = pastureNames[cow.pastureId] ?: pastureNames[null]
    val secondaryDetails = buildList {
        pastureLabel?.let { add(it) }
        add(cow.classification.name.lowercase().replaceFirstChar { it.titlecase() })
    }.joinToString(" • ")

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name ?: tag ?: "Unnamed Animal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tag != null && name != null) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            if (name == null && tag != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tag $tag",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (secondaryDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryDetails,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Divider()
    }
}

private fun formatPrimaryLabel(cow: Cow): String {
    val name = cow.name?.takeIf { it.isNotBlank() }
    val tag = cow.tagNumber?.takeIf { it.isNotBlank() }
    return when {
        name != null && tag != null -> "$name • $tag"
        name != null -> name
        tag != null -> tag
        else -> "Unnamed Animal"
    }
}
