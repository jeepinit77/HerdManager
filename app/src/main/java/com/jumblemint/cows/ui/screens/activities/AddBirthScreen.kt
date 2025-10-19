package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Icons for TopAppBar were here, will be handled by parent
import com.jumblemint.cows.data.preferences.TipsManager 
import com.jumblemint.cows.ui.components.TipOverlay 
import com.jumblemint.cows.ui.components.WobblingLightbulbIcon 
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModel
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModelFactory
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors

// Removed @OptIn(ExperimentalMaterial3Api::class) if TopAppBar is removed, 
// but keeping it if other Material3 components are used directly.
@OptIn(ExperimentalMaterial3Api::class) // Keep for Card, OutlinedTextField etc.
@Composable
fun AddBirthScreen(
    onNavigateBack: () -> Unit, // This will be used by parent's navigation icon
    viewModel: AddBirthViewModel, // Hoist or provide viewModel for parent's save action
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // Still needed for TipsManager, etc.
    // application, database, repository, and local viewModel instantiation removed if 
    // viewModel is hoisted. If not, they would remain for local instantiation.
    // Assuming viewModel is now passed as a parameter for parent to access recordBirth().

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack() // This remains, as it's a side effect of saving
        }
    }

    val tipsManager = remember { TipsManager(context) }
    val tipId = "add_birth_screen_info_tip" 
    val tipIconVisible by tipsManager.isTipVisible(tipId).collectAsState(initial = true)
    var showTipOverlay by remember { mutableStateOf(false) }

    // Scaffold and TopAppBar are removed.
    // The main content Column is now the top-level composable.
    // It takes the modifier passed to AddBirthScreen, which should include padding from the parent Scaffold.

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(), // Apply the main modifier here
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier // Apply the main modifier here, expecting it to have padding
                .fillMaxSize() // Column itself fills the size given by modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp), // This is additional content padding INSIDE the screen area
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Birth Details Card with Coaching Tip Icon
            Box(modifier = Modifier.fillMaxWidth()) { 
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
                if (tipIconVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd) 
                            .offset(x = 18.dp, y = (-18).dp)
                    ) {
                        IconButton(
                            onClick = { showTipOverlay = true }
                        ) {
                            WobblingLightbulbIcon()
                        }
                    }
                }
            }

            if (showTipOverlay) {
                TipOverlay(
                    tipId = tipId,
                    tipText = "• Calves are auto assigned to the same pasture as the mother.\n" +
                              "• After selecting mother, if the name field is blank, a default calf name is calculated and prefilled (Mother Name/Tag plus birth year).",
                    onClosed = { showTipOverlay = false },
                    tipsManager = tipsManager
                )
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
                        singleLine = true,
                        colors = defaultOutlinedTextFieldColors()
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
                        singleLine = true,
                        colors = defaultOutlinedTextFieldColors()
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
                        maxLines = 4,
                        colors = defaultOutlinedTextFieldColors()
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
            Spacer(modifier = Modifier.height(16.dp)) // This spacer might be adjusted by parent padding
        }
    }
}
