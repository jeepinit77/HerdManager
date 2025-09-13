package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.components.SimpleTopAppBar
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.theme.getGenderColor
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory
import java.time.LocalDate

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
    val maleColor = getGenderColor(Gender.MALE)
    val femaleColor = getGenderColor(Gender.FEMALE)
    val tbdColor = getGenderColor(Gender.TBD)
    val genderColorMap = remember(maleColor, femaleColor, tbdColor) {
        mapOf(
            Gender.MALE.name to maleColor,
            Gender.FEMALE.name to femaleColor,
            Gender.TBD.name to tbdColor
        )
    }
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
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
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
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Basic Information
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val fieldsBlankError = saveAttempted && uiState.name.isBlank() && uiState.tagNumber.isBlank()
                        
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = fieldsBlankError,
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.toggleNameTagLink() }
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isNameTagLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                        contentDescription = if (uiState.isNameTagLinked) "Unlink from tag number" else "Link to tag number",
                                        tint = if (uiState.isNameTagLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            supportingText = if (fieldsBlankError) {
                                { Text("Either Name or Tag Number must be provided.") }
                            } else null,
                            colors = if (fieldsBlankError) OutlinedTextFieldDefaults.colors(
                                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            ) else OutlinedTextFieldDefaults.colors()
                        )
                        
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
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        )
                        DropdownField(
                            value = uiState.tagColor ?: "",
                            onValueChange = viewModel::updateTagColor,
                            label = "Tag Color",
                            options = uiState.tagColors,
                            modifier = Modifier.fillMaxWidth(),
                            valueBackgroundColor = { name ->
                                resolveTagColor(name, tagColorMap)
                            },
                            optionBackgroundColor = { name ->
                                resolveTagColor(name, tagColorMap)
                            }
                        )
                        DatePickerField(
                            value = uiState.birthDate,
                            onValueChange = viewModel::updateBirthDate,
                            label = "Birth Date",
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            value = uiState.gender?.name ?: "",
                            onValueChange = { genderName ->
                                if (genderName.isNotEmpty()) {
                                    viewModel.updateGender(Gender.valueOf(genderName))
                                } else {
                                    viewModel.updateGender(null)
                                }
                            },
                            label = "Gender",
                            options = Gender.entries.map { it.name },
                            modifier = Modifier.fillMaxWidth(),
                            valueBackgroundColor = { genderName ->
                                genderColorMap[genderName]
                            },
                            optionBackgroundColor = { genderName ->
                                genderColorMap[genderName]
                            }
                        )
                        val availableClassifications = remember(uiState.gender) {
                            when (uiState.gender) {
                                Gender.FEMALE -> listOf(Classification.COW, Classification.HEIFER, Classification.CALF)
                                Gender.MALE -> listOf(Classification.BULL, Classification.STEER, Classification.CALF)
                                Gender.TBD -> Classification.entries.toList()
                                null -> Classification.entries.toList()
                            }
                        }
                        val classificationColorMap = remember(maleColor, femaleColor, tbdColor) {
                            mapOf(
                                Classification.COW.name to femaleColor,
                                Classification.HEIFER.name to femaleColor,
                                Classification.BULL.name to maleColor,
                                Classification.STEER.name to maleColor,
                                Classification.CALF.name to tbdColor
                            )
                        }
                        DropdownField(
                            value = uiState.classification?.name ?: "",
                            onValueChange = { classificationName ->
                                if (classificationName.isNotEmpty()) {
                                    viewModel.updateClassification(Classification.valueOf(classificationName))
                                } else {
                                    viewModel.updateClassification(null)
                                }
                            },
                            label = "Classification",
                            options = availableClassifications.map { it.name },
                            modifier = Modifier.fillMaxWidth(),
                            valueBackgroundColor = { classificationName ->
                                classificationColorMap[classificationName]
                            },
                            optionBackgroundColor = { classificationName ->
                                classificationColorMap[classificationName]
                            }
                        )
                    }
                }

                // Physical Description
                Card(modifier = Modifier.fillMaxWidth()) {
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
                            label = { Text("Color/Markings (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        OutlinedTextField(
                            value = uiState.registrationNumber,
                            onValueChange = viewModel::updateRegistrationNumber,
                            label = { Text("Registration Number (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            value = uiState.breed ?: "",
                            onValueChange = viewModel::updateBreed,
                            label = "Breed",
                            options = listOf("") + uiState.breeds,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Parentage
                Card(modifier = Modifier.fillMaxWidth()) {
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
                            onValueChange = { motherName ->
                                val mother = uiState.availableMothers.find { it.name == motherName }
                                viewModel.updateMother(mother?.id)
                            },
                            label = "Mother",
                            options = listOf("") + uiState.availableMothers.map { it.name ?: "Unnamed" },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            value = uiState.fatherName ?: "",
                            onValueChange = { fatherName ->
                                val father = uiState.availableFathers.find { it.name == fatherName }
                                viewModel.updateFather(father?.id)
                            },
                            label = "Father",
                            options = listOf("") + uiState.availableFathers.map { it.name ?: "Unnamed" },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Status & Location
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Status & Location",
                            style = MaterialTheme.typography.titleMedium
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
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (cowId == 0L) "Add Animal" else "Save Changes")
                }
            }
    }
}