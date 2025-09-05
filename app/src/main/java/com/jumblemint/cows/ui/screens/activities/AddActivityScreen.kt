package com.jumblemint.cows.ui.screens.activities

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.viewmodel.AddActivityViewModel
import com.jumblemint.cows.ui.viewmodel.AddActivityViewModelFactory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    editId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao()
    )
    val viewModel: AddActivityViewModel = viewModel(
        factory = AddActivityViewModelFactory(context.applicationContext as Application, repository, editId)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Filter states
    var showFilters by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf<Gender?>(null) }
    var selectedClassification by remember { mutableStateOf<Classification?>(null) }
    var selectedPasture by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter the cows based on selected filters
    val filteredCows = remember(uiState.availableCows, selectedGender, selectedClassification, selectedPasture, searchQuery) {
        uiState.availableCows.filter { cow ->
            val matchesGender = selectedGender == null || cow.gender == selectedGender
            val matchesClassification = selectedClassification == null || cow.classification == selectedClassification
            val matchesPasture = selectedPasture == null || cow.pastureId == selectedPasture
            val matchesSearch = searchQuery.isBlank() || 
                cow.name?.contains(searchQuery, ignoreCase = true) == true ||
                cow.tagNumber?.contains(searchQuery, ignoreCase = true) == true
            
            matchesGender && matchesClassification && matchesPasture && matchesSearch
        }
    }
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(if (editId != null) "Edit Activity" else "Add Activity") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.saveActivity() },
                    enabled = !uiState.isLoading && uiState.selectedCows.isNotEmpty()
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
        )
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Activity Details
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Activity Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            DropdownField(
                                value = uiState.activityType?.name ?: "",
                                onValueChange = { typeName ->
                                    val type = ActivityType.values().find { it.name == typeName }
                                    viewModel.updateActivityType(type)
                                },
                                label = "Activity Type",
                                options = ActivityType.values().map { it.name },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            DatePickerField(
                                value = uiState.date,
                                onValueChange = viewModel::updateDate,
                                label = "Date",
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Show pasture selection for MOVED activity
                            if (uiState.activityType == ActivityType.MOVED) {
                                DropdownField(
                                    value = uiState.toPastureName ?: "",
                                    onValueChange = { name ->
                                        val pasture = uiState.availablePastures.find { it.name == name }
                                        viewModel.updateToPasture(pasture?.id)
                                    },
                                    label = "Move to Pasture",
                                    options = uiState.availablePastures.map { it.name },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = viewModel::updateNotes,
                                label = { Text("Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                placeholder = { 
                                    Text(
                                        if (uiState.activityType in listOf(ActivityType.WORKED, ActivityType.OTHER)) 
                                            "Notes required for this activity type" 
                                        else 
                                            "Optional notes"
                                    ) 
                                }
                            )
                        }
                    }
                }
                
                // Cow Selection
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Cows",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "${uiState.selectedCows.size} selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Search field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search cows...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { 
                                        // Select all filtered cows
                                        val filteredCowIds = filteredCows.map { it.id }.toSet()
                                        filteredCowIds.forEach { viewModel.selectCow(it) }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Select All")
                                }
                                Button(
                                    onClick = { viewModel.clearSelection() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Clear All")
                                }
                                
                                FilterChip(
                                    onClick = { showFilters = !showFilters },
                                    label = { Text("Filters") },
                                    selected = selectedGender != null || selectedClassification != null || selectedPasture != null,
                                    leadingIcon = if (showFilters) {
                                        { Icon(Icons.Default.ExpandLess, contentDescription = "Collapse") }
                                    } else {
                                        { Icon(Icons.Default.ExpandMore, contentDescription = "Expand") }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Collapsible Filters
                if (showFilters) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Filter Cows",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Gender Filter
                                Text(
                                    text = "Gender",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        onClick = { selectedGender = if (selectedGender == Gender.MALE) null else Gender.MALE },
                                        label = { Text("Male") },
                                        selected = selectedGender == Gender.MALE
                                    )
                                    FilterChip(
                                        onClick = { selectedGender = if (selectedGender == Gender.FEMALE) null else Gender.FEMALE },
                                        label = { Text("Female") },
                                        selected = selectedGender == Gender.FEMALE
                                    )
                                    FilterChip(
                                        onClick = { selectedGender = if (selectedGender == Gender.TBD) null else Gender.TBD },
                                        label = { Text("TBD") },
                                        selected = selectedGender == Gender.TBD
                                    )
                                }
                                
                                // Classification Filter
                                Text(
                                    text = "Classification",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Classification.values().forEach { classification ->
                                        FilterChip(
                                            onClick = { 
                                                selectedClassification = if (selectedClassification == classification) null else classification 
                                            },
                                            label = { Text(classification.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                            selected = selectedClassification == classification
                                        )
                                    }
                                }
                                
                                // Pasture Filter
                                if (uiState.availablePastures.isNotEmpty()) {
                                    Text(
                                        text = "Pasture",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(uiState.availablePastures) { pasture ->
                                            FilterChip(
                                                onClick = { 
                                                    selectedPasture = if (selectedPasture == pasture.id) null else pasture.id 
                                                },
                                                label = { Text(pasture.name) },
                                                selected = selectedPasture == pasture.id
                                            )
                                        }
                                    }
                                }
                                
                                // Clear all filters button
                                TextButton(
                                    onClick = {
                                        selectedGender = null
                                        selectedClassification = null
                                        selectedPasture = null
                                        searchQuery = ""
                                    }
                                ) {
                                    Text("Clear All Filters")
                                }
                            }
                        }
                    }
                }
                
                // Cow List
                items(filteredCows) { cow ->
                    CowSelectionCard(
                        cow = cow,
                        isSelected = cow.id in uiState.selectedCows,
                        onSelectionChanged = { isSelected ->
                            if (isSelected) {
                                viewModel.selectCow(cow.id)
                            } else {
                                viewModel.deselectCow(cow.id)
                            }
                        }
                    )
                }
                
                // Error message
                uiState.error?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowSelectionCard(
    cow: Cow,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelectionChanged(!isSelected) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChanged
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cow.name ?: "Unnamed Cow",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${cow.gender.name} • ${cow.classification.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                cow.tagNumber?.let { tagNumber ->
                    Text(
                        text = "Tag: $tagNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}