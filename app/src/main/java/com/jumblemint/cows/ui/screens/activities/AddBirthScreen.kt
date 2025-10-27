package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.model.Classification
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.preferences.TipsManager
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.components.ParentPicker
import com.jumblemint.cows.ui.components.ParentSelectionField
import com.jumblemint.cows.ui.components.formatParentDisplay
import com.jumblemint.cows.ui.components.SectionTitle
import com.jumblemint.cows.ui.components.TipOverlay
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.ui.components.WobblingLightbulbIcon
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.ui.components.FocusAwareLiveSync
import kotlin.collections.buildMap

// Removed @OptIn(ExperimentalMaterial3Api::class) if TopAppBar is removed, 
// but keeping it if other Material3 components are used directly.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddBirthViewModel,
    modifier: Modifier = Modifier,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {}
) {
    val context = LocalContext.current // Still needed for TipsManager, etc.
    val application = context.applicationContext as CattleApplication
    // application, database, repository, and local viewModel instantiation removed if
    // viewModel is hoisted. If not, they would remain for local instantiation.
    // Assuming viewModel is now passed as a parameter for parent to access recordBirth().

    val uiState by viewModel.uiState.collectAsState()

    FocusAwareLiveSync(
        orchestrator = application.syncOrchestrator,
        screenKey = "AddBirth",
        intervalMs = 20_000L,
        leadingRun = true
    )

    var showMotherPicker by remember { mutableStateOf(false) }
    var showFatherPicker by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val pastureNames = remember(uiState.availablePastures) {
        buildMap<String?, String> {
            put(null, "Unassigned")
            uiState.availablePastures.sortedBy { it.name }.forEach { pasture ->
                put(pasture.id, pasture.name)
            }
        }
    }

    val selectedMother = remember(uiState.motherId, uiState.availableMothers) {
        uiState.availableMothers.find { it.id == uiState.motherId }
    }
    val selectedFather = remember(uiState.fatherId, uiState.availableFathers) {
        uiState.availableFathers.find { it.id == uiState.fatherId }
    }
    val motherDisplay = selectedMother?.let { formatParentDisplay(it, uiState.identifierMode) } ?: ""
    val fatherDisplay = selectedFather?.let { formatParentDisplay(it, uiState.identifierMode) } ?: ""
    val motherError = uiState.error?.contains("Mother") == true

    val pastureEntries = remember(pastureNames) {
        pastureNames.entries.map { it.key to it.value }
    }
    val selectedPastureLabel = pastureNames[uiState.calfPastureId] ?: pastureNames[null] ?: "Unassigned"

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showUnsavedChangesDialog = false
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.hasUnsavedChanges) {
        onUnsavedChangesChanged(uiState.hasUnsavedChanges)
    }

    LaunchedEffect(saveTriggered) {
        if (saveTriggered) {
            viewModel.recordBirth()
            onSaveHandled()
        }
    }

    LaunchedEffect(backPressed) {
        if (backPressed) {
            if (uiState.hasUnsavedChanges) {
                showUnsavedChangesDialog = true
            } else {
                onNavigateBack()
            }
            onBackHandled()
        }
    }

    val tipsManager = remember { TipsManager(context) }
    val tipId = "add_birth_screen_info_tip"
    val tipIconVisible by tipsManager.isTipVisible(tipId).collectAsState(initial = true)
    var showTipOverlay by remember { mutableStateOf(false) }

    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onSave = {
                showUnsavedChangesDialog = false
                viewModel.recordBirth()
            },
            onDiscard = {
                showUnsavedChangesDialog = false
                onNavigateBack()
            }
        )
    }

    if (showMotherPicker) {
        ParentPicker(
            title = "Select Mother",
            animals = uiState.availableMothers,
            pastureNames = pastureNames,
            classificationOptions = listOf(Classification.COW, Classification.HEIFER),
            enablePastureFilter = true,
            identifierMode = uiState.identifierMode,
            onSelect = { cow -> viewModel.updateMother(cow.id) },
            onDismiss = { showMotherPicker = false }
        )
    }

    if (showFatherPicker) {
        ParentPicker(
            title = "Select Father",
            animals = uiState.availableFathers,
            pastureNames = pastureNames,
            enablePastureFilter = true,
            allowClearSelection = true,
            quickPicks = uiState.recentSires,
            identifierMode = uiState.identifierMode,
            onSelect = { cow -> viewModel.updateFather(cow.id) },
            onClearSelection = { viewModel.updateFather(null) },
            onDismiss = { showFatherPicker = false }
        )
    }

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
                Card(modifier = Modifier.fillMaxWidth(), colors = getCardColors()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionTitle("Birth Details")
                        DatePickerField(
                            value = uiState.birthDate,
                            onValueChange = viewModel::updateBirthDate,
                            label = "Birth Date",
                            modifier = Modifier.fillMaxWidth()
                        )
                        ParentSelectionField(
                            label = "Mother*",
                            value = motherDisplay,
                            onClick = { showMotherPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            isError = motherError,
                            placeholder = "Tap to select a mother"
                        )
                        if (motherError) {
                            Text(
                                text = "Mother selection is required.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ParentSelectionField(
                            label = "Father (Optional)",
                            value = fatherDisplay,
                            onClick = { showFatherPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Tap to choose a bull"
                        )
                        DropdownField(
                            value = selectedPastureLabel,
                            onValueChange = { label ->
                                val entry = pastureEntries.firstOrNull { it.second == label }
                                viewModel.updateCalfPasture(entry?.first)
                            },
                            label = "Calf Pasture",
                            options = pastureEntries.map { it.second },
                            modifier = Modifier.fillMaxWidth(),
                            showNoneOption = false
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
            Card(modifier = Modifier.fillMaxWidth(), colors = getCardColors()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitle("Calf Details")

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

                    OutlinedTextField(
                        value = uiState.calfBirthWeight,
                        onValueChange = viewModel::updateCalfBirthWeight,
                        label = { Text("Birth Weight (lbs)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
// Suggested enhancements:
// - Surface dam health alerts alongside selection to catch repeat calving risks.
// - Add quick links to log initial vaccinations immediately after recording a birth.
