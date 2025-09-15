package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowEditScreen(
    cowId: Long,
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
            database.activityTypeConfigDao(),
            database.breedDao()
        )
    }
    val viewModel: CowDetailViewModel = viewModel(
        factory = CowDetailViewModelFactory(application, repository, cowId)
    )

    val uiState by viewModel.uiState.collectAsState()
    val tagColorMap = rememberTagColorMap(repository)
    val scrollState = rememberScrollState()
    var saveAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            saveAttempted = true
            scrollState.animateScrollTo(0)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    if (uiState.isLoading && cowId != 0L) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            uiState.error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Card 1: Identification
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Identification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val fieldsBlankError = saveAttempted && uiState.name.isBlank() && uiState.tagNumber.isBlank()

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = fieldsBlankError,
                        supportingText = if (fieldsBlankError) {
                            { Text("Either Name or Tag Number must be provided.") }
                        } else null,
                        colors = if (fieldsBlankError) OutlinedTextFieldDefaults.colors(
                            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ) else OutlinedTextFieldDefaults.colors()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (uiState.isNameTagLinked) 0.dp else 12.dp)
                            .zIndex(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { viewModel.toggleNameTagLink() }) {
                            Icon(
                                imageVector = if (uiState.isNameTagLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = if (uiState.isNameTagLinked) "Unlink Name and Tag Number" else "Link Name and Tag Number",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.tagNumber,
                        onValueChange = viewModel::updateTagNumber,
                        label = { Text("Tag Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isNameTagLinked,
                        isError = fieldsBlankError,
                        supportingText = if (fieldsBlankError) {
                            { Text("Either Name or Tag Number must be provided.") }
                        } else if (uiState.isNameTagLinked) {
                            { Text("Linked to Name field", color = MaterialTheme.colorScheme.primary) }
                        } else null,
                        colors = if (fieldsBlankError) {
                            OutlinedTextFieldDefaults.colors(
                                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            )
                        } else if (!uiState.isNameTagLinked) {
                            OutlinedTextFieldDefaults.colors()
                        } else {
                            OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    DropdownField(
                        value = uiState.tagColor ?: "",
                        onValueChange = viewModel::updateTagColor,
                        label = "Tag Color",
                        options = uiState.tagColors,
                        modifier = Modifier.fillMaxWidth(),
                        valueBackgroundColor = { name -> resolveTagColor(name, tagColorMap) },
                        optionBackgroundColor = { name -> resolveTagColor(name, tagColorMap) }
                    )
                }
            }

            // Card 2: Birth & Genetics
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Birth & Genetics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DatePickerField(
                        value = uiState.birthDate,
                        onValueChange = viewModel::updateBirthDate,
                        label = "Birth Date",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.entries.forEach { genderOption ->
                            val isSelected = uiState.gender == genderOption
                            OutlinedButton(
                                onClick = { viewModel.updateGender(genderOption) },
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

                    Text(
                        text = "Classification",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    val availableClassifications = remember(uiState.gender) {
                        when (uiState.gender) {
                            Gender.FEMALE -> listOf(Classification.COW, Classification.HEIFER, Classification.CALF)
                            Gender.MALE -> listOf(Classification.BULL, Classification.STEER, Classification.CALF)
                            Gender.TBD -> Classification.entries.toList()
                            null -> Classification.entries.toList()
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (availableClassifications.isNotEmpty()) {
                            val firstRowClassifications: List<Classification>
                            val secondRowClassifications: List<Classification>

                            when (availableClassifications.size) {
                                in 0..3 -> { 
                                    firstRowClassifications = availableClassifications
                                    secondRowClassifications = emptyList()
                                }
                                4 -> {
                                    firstRowClassifications = availableClassifications.take(2)
                                    secondRowClassifications = availableClassifications.drop(2)
                                }
                                else -> { // 5 or more
                                    firstRowClassifications = availableClassifications.take(3)
                                    secondRowClassifications = availableClassifications.drop(3)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                firstRowClassifications.forEach { classificationOption ->
                                    val isSelected = uiState.classification == classificationOption
                                    OutlinedButton(
                                        onClick = { viewModel.updateClassification(classificationOption) },
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
                                        Text(text = classificationOption.name.lowercase().replaceFirstChar { it.titlecase() })
                                    }
                                }
                            }

                            if (secondRowClassifications.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    secondRowClassifications.forEach { classificationOption ->
                                        val isSelected = uiState.classification == classificationOption
                                        OutlinedButton(
                                            onClick = { viewModel.updateClassification(classificationOption) },
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
                                            Text(text = classificationOption.name.lowercase().replaceFirstChar { it.titlecase() })
                                        }
                                    }
                                    val firstRowMaxItems = if (availableClassifications.size >= 5 || availableClassifications.size == 3) 3 else 2
                                    if (firstRowClassifications.size == firstRowMaxItems && secondRowClassifications.size < firstRowMaxItems) {
                                        val spacersToBalance = firstRowMaxItems - secondRowClassifications.size
                                        repeat(spacersToBalance) {
                                            Spacer(Modifier.weight(1f))
                                            if (it < spacersToBalance - 1) { 
                                                Spacer(Modifier.width(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Select a gender to see classifications", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    DropdownField(
                        value = uiState.breed ?: "",
                        onValueChange = viewModel::updateBreed,
                        label = "Breed",
                        options = listOf("") + uiState.breeds,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.registrationNumber,
                        onValueChange = viewModel::updateRegistrationNumber,
                        label = { Text("Registration Number (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Card 3: Physical Appearance
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Physical Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = uiState.colorMarkings,
                        onValueChange = viewModel::updateColorMarkings,
                        label = { Text("Color/Markings (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            // Card 4: Parentage
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Parentage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DropdownField(
                        value = uiState.motherName ?: "",
                        onValueChange = { motherName ->
                            val mother = uiState.availableMothers.find { it.name == motherName || it.tagNumber == motherName }
                            viewModel.updateMother(mother?.id)
                        },
                        label = "Mother",
                        options = listOf("") + uiState.availableMothers.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } ?: it.tagNumber },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownField(
                        value = uiState.fatherName ?: "",
                        onValueChange = { fatherName ->
                            val father = uiState.availableFathers.find { it.name == fatherName || it.tagNumber == fatherName }
                            viewModel.updateFather(father?.id)
                        },
                        label = "Father",
                        options = listOf("") + uiState.availableFathers.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } ?: it.tagNumber },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Card 5: Management
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DropdownField(
                        value = uiState.status.name,
                        onValueChange = { viewModel.updateStatus(Status.valueOf(it)) },
                        label = "Status",
                        options = Status.entries.map { it.name },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownField(
                        value = uiState.pastureName ?: "",
                        onValueChange = { pastureName ->
                            val pasture = uiState.availablePastures.find { it.name == pastureName }
                            viewModel.updatePasture(pasture?.id)
                        },
                        label = "Pasture",
                        options = listOf("") + uiState.availablePastures.map { it.name },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isWatched,
                            onCheckedChange = viewModel::updateIsWatched
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch this cow",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (uiState.isWatched) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (uiState.isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    saveAttempted = true
                    viewModel.saveCow()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading && cowId != 0L) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (cowId == 0L) "Add Animal" else "Save Changes")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
