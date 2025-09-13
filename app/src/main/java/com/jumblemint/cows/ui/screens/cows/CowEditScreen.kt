package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
            database.activityTypeConfigDao()
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

    Box(modifier = modifier) {
        if (uiState.isLoading && cowId != 0L) {
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
                            label = { Text("Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = fieldsBlankError,
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
                            label = { Text("Tag Number (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = fieldsBlankError,
                             supportingText = if (fieldsBlankError) {
                                { Text("Either Name or Tag Number must be provided.") }
                            } else null,
                            colors = if (fieldsBlankError) OutlinedTextFieldDefaults.colors(
                                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            ) else OutlinedTextFieldDefaults.colors()
                        )
                        DropdownField(
                            value = uiState.tagColor ?: "",
                            onValueChange = viewModel::updateTagColor,
                            label = "Tag Color",
                            options = uiState.tagColors,
                            modifier = Modifier.fillMaxWidth(),
                            valueBackgroundColor = { name ->
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
                            value = uiState.gender.name,
                            onValueChange = { viewModel.updateGender(Gender.valueOf(it)) },
                            label = "Gender",
                            options = Gender.entries.map { it.name },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            value = uiState.classification.name,
                            onValueChange = { viewModel.updateClassification(Classification.valueOf(it)) },
                            label = "Classification",
                            options = Classification.entries.map { it.name },
                            modifier = Modifier.fillMaxWidth()
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
                    }
                }
            }
        }
    }
}