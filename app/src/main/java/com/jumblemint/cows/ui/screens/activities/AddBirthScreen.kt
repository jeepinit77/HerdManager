package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
// import androidx.compose.material.icons.outlined.Lightbulb // Removed, using WobblingLightbulbIcon
import com.jumblemint.cows.data.preferences.TipsManager // Added for coaching tips
import com.jumblemint.cows.ui.components.TipOverlay // Added for coaching tips
import com.jumblemint.cows.ui.components.WobblingLightbulbIcon // Added for coaching tips
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp // Removed if not used by other text elements
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModel
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModelFactory

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.recordBirth() },
                        enabled = !uiState.isLoading && uiState.motherId != null
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Birth Record")
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
                // Coaching Tip System Integration
                val tipsManager = remember { TipsManager(context) }
                val tipId = "add_birth_screen_info_tip" // Unique ID for this tip
                val tipIconVisible by tipsManager.isTipVisible(tipId).collectAsState(initial = true)
                var showTipOverlay by remember { mutableStateOf(false) }

                if (tipIconVisible) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // Added padding
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showTipOverlay = true }) {
                            WobblingLightbulbIcon()
                        }
                    }
                }

                if (showTipOverlay) {
                    TipOverlay(
                        tipId = tipId,
                        tipText = "• Calves are auto assigned to the same pasture as the mother.\n" +
                                  "• Birth activity will be recorded for calf.\n" +
                                  "• Calved activity will be recorded for cow.",
                        onClosed = { showTipOverlay = false },
                        tipsManager = tipsManager
                    )
                }
                // End of Coaching Tip System Integration

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

                        var calfNameTfv by remember { mutableStateOf(TextFieldValue(uiState.calfName)) }

                        LaunchedEffect(uiState.calfName) {
                            if (calfNameTfv.text != uiState.calfName) {
                                calfNameTfv = TextFieldValue(
                                    text = uiState.calfName,
                                    selection = TextRange(uiState.calfName.length)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = calfNameTfv,
                            onValueChange = { newValue ->
                                calfNameTfv = newValue
                                viewModel.updateCalfName(newValue.text)
                            },
                            label = { Text("Calf Name (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        calfNameTfv = calfNameTfv.copy(
                                            selection = TextRange(0, calfNameTfv.text.length)
                                        )
                                    }
                                },
                            singleLine = true
                        )
                        
                        Text(
                            text = "Calf Gender", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Gender.entries.forEach { genderOption ->
                                val isSelected = uiState.calfGender == genderOption
                                OutlinedButton(
                                    onClick = { viewModel.updateCalfGender(genderOption) },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = if (isSelected) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors()
                                    },
                                    border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Text(text = genderOption.name.lowercase().replaceFirstChar { it.titlecase() })
                                }
                            }
                        }
                        
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
