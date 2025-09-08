package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // Specific import for ArrowBack
import androidx.compose.material.icons.filled.Save // Specific import for Save
import androidx.compose.material.icons.outlined.Info // <<< IMPORT ADDED FOR OUTLINED INFO ICON
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModel
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModelFactory
// import java.time.LocalDate // <<< REMOVED UNUSED IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)

    val repository = remember {
        CattleRepository(
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
    }

    val viewModel: AddBirthViewModel = viewModel(
        factory = AddBirthViewModelFactory(
            repository,
            application.authService,
            application.syncService
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Record Birth") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back") // Changed to Filled
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.recordBirth() },
                        enabled = !uiState.isLoading && uiState.motherId != null
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Birth Record") // Changed to Filled
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Birth Details Card
                Card(modifier = Modifier.fillMaxWidth()) {
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
                                val mother = uiState.availableMothers.find { it.name == name || it.tagNumber == name }
                                viewModel.updateMother(mother?.id)
                            },
                            label = "Mother*",
                            options = uiState.availableMothers.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } ?: it.tagNumber },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.error?.contains("Mother") == true
                        )
                        DropdownField(
                            value = uiState.fatherName ?: "",
                            onValueChange = { name ->
                                val father = uiState.availableFathers.find { it.name == name || it.tagNumber == name }
                                viewModel.updateFather(father?.id)
                            },
                            label = "Father (Optional)",
                            options = listOf("") + uiState.availableFathers.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } ?: it.tagNumber },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Calf Details Card
                Card(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownField(
                            value = uiState.calfGender.name,
                            onValueChange = { viewModel.updateCalfGender(Gender.valueOf(it)) },
                            label = "Calf Gender",
                            options = Gender.entries.map { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.calfTagNumber,
                            onValueChange = viewModel::updateCalfTagNumber,
                            label = { Text("Tag Number (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownField(
                            value = uiState.calfTagColor,
                            onValueChange = viewModel::updateCalfTagColor,
                            label = "Tag Color (Optional)",
                            options = listOf("") + uiState.tagColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.calfColorMarkings,
                            onValueChange = viewModel::updateCalfColorMarkings,
                            label = { Text("Color/Markings (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                // Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Info, // <<< CORRECTED ICON REFERENCE
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Important Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = "• The calf will be automatically assigned to the 'Calf Pasture' (if it exists, otherwise unassigned).\n" +
                                   "• Birth activities will be recorded for both the mother and the calf.\n" +
                                   "• The calf's classification will be set to 'CALF'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Error message display
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
