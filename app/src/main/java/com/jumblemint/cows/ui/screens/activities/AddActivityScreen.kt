package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
        factory = AddActivityViewModelFactory(repository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Add Activity") },
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
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.selectAllCows() },
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
                            }
                        }
                    }
                }
                
                // Cow List
                items(uiState.availableCows) { cow ->
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