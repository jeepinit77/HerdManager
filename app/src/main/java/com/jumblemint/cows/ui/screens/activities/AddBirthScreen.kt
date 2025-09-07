package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.jumblemint.cows.CattleApplication // Added import
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModel
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModelFactory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    // Get application instance to access services
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)

    // Instantiate services
    val authService = application.authService
    val syncService = application.syncService

    val repository = CattleRepository(
        database.cowDao(),
        database.pastureDao(),
        database.activityDao(),
        database.settingsDao(),
        database.noteDao(),
        database.userDao(),
        database.herdDao(),
        database.herdMemberDao(),
        database.tagColorDao(),
        database.activityTypeConfigDao()
    )
    
    val viewModel: AddBirthViewModel = viewModel(
        factory = AddBirthViewModelFactory(repository, authService, syncService) // Updated factory instantiation
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
            title = { Text("Record Birth") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.recordBirth() },
                    enabled = !uiState.isLoading && uiState.motherId != null
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
                // Birth Details
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Birth Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        DatePickerField(
                            value = uiState.birthDate,
                            onValueChange = viewModel::updateBirthDate,
                            label = "Birth Date",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.motherName ?: "",
                            onValueChange = { name ->
                                val mother = uiState.availableMothers.find { it.name == name }
                                viewModel.updateMother(mother?.id)
                            },
                            label = "Mother *",
                            options = uiState.availableMothers.mapNotNull { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.fatherName ?: "",
                            onValueChange = { name ->
                                val father = uiState.availableFathers.find { it.name == name }
                                viewModel.updateFather(father?.id)
                            },
                            label = "Father (Optional)",
                            options = uiState.availableFathers.mapNotNull { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Calf Details
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Calf Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = uiState.calfName,
                            onValueChange = viewModel::updateCalfName,
                            label = { Text("Calf Name (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.calfGender.name,
                            onValueChange = { viewModel.updateCalfGender(Gender.valueOf(it)) },
                            label = "Calf Gender",
                            options = Gender.values().map { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = uiState.calfTagNumber,
                            onValueChange = viewModel::updateCalfTagNumber,
                            label = { Text("Tag Number (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownField(
                            value = uiState.calfTagColor,
                            onValueChange = viewModel::updateCalfTagColor,
                            label = "Tag Color (Optional)",
                            options = uiState.tagColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = uiState.calfColorMarkings,
                            onValueChange = viewModel::updateCalfColorMarkings,
                            label = { Text("Color/Markings (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
                
                // Information Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Birth Event Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "• The calf will be automatically assigned to the Calf Pasture\n" +
                                    "• Birth activities will be recorded for both mother and calf\n" +
                                    "• The calf's classification will be set to 'CALF'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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