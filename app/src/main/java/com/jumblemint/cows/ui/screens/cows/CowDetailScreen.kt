package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack // Updated import
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility // New import
import androidx.compose.material.icons.filled.VisibilityOff // New import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowDetailScreen(
    cowId: Long,
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
    val viewModel: CowDetailViewModel = viewModel(
        factory = CowDetailViewModelFactory(repository, cowId)
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
        // Top App Bar
        TopAppBar(
            title = { Text(if (cowId == 0L) "Add Cow" else "Edit Cow") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
//                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // Updated icon
                }
            },
            actions = {
                if (cowId != 0L) { // Only show watch toggle for existing cows
                    IconToggleButton(
                        checked = uiState.isWatched,
                        onCheckedChange = { viewModel.updateIsWatched(it) }
                    ) {
                        Icon(
                            if (uiState.isWatched) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (uiState.isWatched) "Stop Watching" else "Watch"
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.saveCow() },
                    enabled = !uiState.isLoading
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic Information
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = { Text("Name (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = uiState.tagNumber,
                            onValueChange = viewModel::updateTagNumber,
                            label = { Text("Tag Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.tagColor ?: "", // THIS IS LINE 126 - MODIFIED
                            onValueChange = viewModel::updateTagColor,
                            label = "Tag Color",
                            options = uiState.tagColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DatePickerField(
                            value = uiState.birthDate,
                            onValueChange = viewModel::updateBirthDate,
                            label = "Birth Date",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.gender.name,
                            onValueChange = { viewModel.updateGender(Gender.valueOf(it)) },
                            label = "Gender",
                            options = Gender.values().map { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.classification.name,
                            onValueChange = { viewModel.updateClassification(Classification.valueOf(it)) },
                            label = "Classification",
                            options = Classification.values().map { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Physical Description
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Physical Description",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = uiState.colorMarkings,
                            onValueChange = viewModel::updateColorMarkings,
                            label = { Text("Color/Markings") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
                
                // Parentage
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Parentage",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        DropdownField(
                            value = uiState.motherName ?: "",
                            onValueChange = { name ->
                                val mother = uiState.availableMothers.find { it.name == name }
                                viewModel.updateMother(mother?.id)
                            },
                            label = "Mother",
                            options = uiState.availableMothers.mapNotNull { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.fatherName ?: "",
                            onValueChange = { name ->
                                val father = uiState.availableFathers.find { it.name == name }
                                viewModel.updateFather(father?.id)
                            },
                            label = "Father",
                            options = uiState.availableFathers.mapNotNull { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Location & Status
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Location & Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        DropdownField(
                            value = uiState.pastureName ?: "",
                            onValueChange = { name ->
                                val pasture = uiState.availablePastures.find { it.name == name }
                                viewModel.updatePasture(pasture?.id) // pasture.id is String?
                            },
                            label = "Pasture",
                            options = uiState.availablePastures.map { it.name }, // Assuming pasture names are non-null
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.status.name,
                            onValueChange = { viewModel.updateStatus(Status.valueOf(it)) },
                            label = "Status",
                            options = Status.values().map { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Error message
                uiState.error?.let { error ->
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
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
