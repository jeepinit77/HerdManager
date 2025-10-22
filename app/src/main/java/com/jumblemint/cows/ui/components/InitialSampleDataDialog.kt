package com.jumblemint.cows.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jumblemint.cows.data.model.ActivityTypeConfig
import com.jumblemint.cows.data.model.AnimalIdentifierMode
import com.jumblemint.cows.data.model.Breed
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.model.TagColor
import com.jumblemint.cows.ui.screens.settings.TagColorDialog
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private val SETUP_WIZARD_STEP_COUNT = SetupWizardStep.entries.size

enum class SetupWizardStep(val title: String, val description: String) {
    IDENTIFIERS(
        title = "Animal Identifiers",
        description = "Do you use animal names, tag numbers, or both?"
    ),
    BREEDS(
        title = "Select Breeds",
        description = "Choose the breeds you manage. Add any custom breeds you need."
    ),
    TAG_COLORS(
        title = "Tag Colors",
        description = "Pick the tag colors you use and add any custom colors."
    ),
    ACTIVITIES(
        title = "Activity Types",
        description = "Choose the activity types you track and add any custom ones."
    ),
    PASTURES(
        title = "Add Pastures",
        description = "Create your pastures so you can start assigning animals right away."
    )
}

data class SetupWizardPastureDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val details: String
)

private data class CustomBreedField(
    val id: String = UUID.randomUUID().toString(),
    val value: String = ""
)

private data class SetupWizardActivityDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String
)

private fun MutableSet<String>.generateActivityTypeName(displayName: String): String {
    val sanitizedBase = displayName.trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
        .ifEmpty { "CUSTOM_ACTIVITY" }

    var candidate = sanitizedBase
    var suffix = 1
    while (contains(candidate)) {
        candidate = "${sanitizedBase}_${suffix++}"
    }
    add(candidate)
    return candidate
}

@Composable
fun InitialSetupLandingDialog(
    onInstallSampleData: () -> Unit,
    onStartEmpty: () -> Unit,
    onStartWizard: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome to Herd Manager",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pick the best way to get started. You can change any of these choices later in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close initial setup")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InitialSetupOptionCard(
                        title = "Install Sample Data to Explore App",
                        subtitle = "You can remove just the sample data later.",
                        onClick = onInstallSampleData,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    InitialSetupOptionCard(
                        title = "Start with empty app",
                        subtitle = "You can add sample data later from Settings.",
                        onClick = onStartEmpty,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    InitialSetupOptionCard(
                        title = "Setup Wizard - $SETUP_WIZARD_STEP_COUNT steps",
                        subtitle = "Set up pasture names, choose your tag colors, breeds, and activity types.",
                        onClick = onStartWizard,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialSetupOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SetupWizardDialog(
    defaultBreeds: List<Breed>,
    defaultTagColors: List<TagColor>,
    defaultActivityTypes: List<ActivityTypeConfig>,
    onExit: () -> Unit,
    onFinished: () -> Unit,
    onSaveIdentifierMode: suspend (AnimalIdentifierMode) -> Unit,
    onSaveBreeds: suspend (List<Breed>) -> Unit,
    onSaveTagColors: suspend (List<TagColor>) -> Unit,
    onSaveActivities: suspend (List<ActivityTypeConfig>) -> Unit,
    onSavePastures: suspend (List<Pasture>) -> Unit
) {
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            val steps = remember { SetupWizardStep.entries.toList() }
            var currentStepIndex by remember { mutableStateOf(0) }
            var selectedIdentifierMode by remember { mutableStateOf<AnimalIdentifierMode?>(null) }
            var selectedDefaultBreedIds by remember {
                mutableStateOf(emptySet<String>())
            }
            var customBreedFields by remember { mutableStateOf(listOf(CustomBreedField())) }

            var colorOptions by remember {
                mutableStateOf(defaultTagColors)
            }
            var selectedColorIds by remember {
                mutableStateOf(emptySet<String>())
            }

            var selectedDefaultActivityIds by remember {
                mutableStateOf(emptySet<String>())
            }
            var activityDrafts by remember { mutableStateOf(emptyList<SetupWizardActivityDraft>()) }
            var activityName by remember { mutableStateOf("") }
            var activityDescription by remember { mutableStateOf("") }

            var pastureDrafts by remember { mutableStateOf(emptyList<SetupWizardPastureDraft>()) }
            var pastureName by remember { mutableStateOf("") }
            var pastureSize by remember { mutableStateOf("") }
            var pastureDetails by remember { mutableStateOf("") }

            var isSaving by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val coroutineScope = rememberCoroutineScope()

            fun moveToNextStep() {
                errorMessage = null
                if (currentStepIndex < steps.lastIndex) {
                    currentStepIndex++
                } else {
                    onFinished()
                }
            }

            fun handleSkip() {
                if (currentStepIndex == steps.lastIndex) {
                    onFinished()
                } else {
                    moveToNextStep()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = steps[currentStepIndex].title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step ${currentStepIndex + 1} of ${steps.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onExit, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "Exit wizard")
                    }
                }

                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1f) / steps.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )

                Text(
                    text = steps[currentStepIndex].description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (steps[currentStepIndex]) {
                        SetupWizardStep.IDENTIFIERS -> {
                            IdentifierPreferenceStep(
                                selected = selectedIdentifierMode,
                                onSelect = { selectedIdentifierMode = it }
                            )
                        }

                        SetupWizardStep.BREEDS -> {
                            BreedSelectionStep(
                                defaultBreeds = defaultBreeds,
                                selectedDefaultIds = selectedDefaultBreedIds,
                                customFields = customBreedFields,
                                onToggleDefault = { breedId ->
                                    selectedDefaultBreedIds = selectedDefaultBreedIds.toMutableSet().apply {
                                        if (contains(breedId)) remove(breedId) else add(breedId)
                                    }
                                },
                                onCustomValueChanged = { id, value ->
                                    customBreedFields = customBreedFields.map { field ->
                                        if (field.id == id) field.copy(value = value) else field
                                    }
                                    val trimmed = value.trim()
                                    if (customBreedFields.lastOrNull()?.id == id && trimmed.isNotEmpty()) {
                                        customBreedFields = customBreedFields + CustomBreedField()
                                    }
                                },
                                onRemoveCustomField = { id ->
                                    customBreedFields = customBreedFields
                                        .filterNot { it.id == id }
                                        .ifEmpty { listOf(CustomBreedField()) }
                                }
                            )
                        }

                        SetupWizardStep.TAG_COLORS -> {
                            var showColorDialog by remember { mutableStateOf(false) }
                            if (showColorDialog) {
                                TagColorDialog(
                                    onDismiss = { showColorDialog = false },
                                    onSave = { name, colorInt, existingId ->
                                        showColorDialog = false
                                        val trimmedName = name.trim()
                                        if (trimmedName.isEmpty()) return@TagColorDialog
                                        val existing = colorOptions.firstOrNull { it.id == existingId }
                                        if (existing != null) {
                                            colorOptions = colorOptions.map {
                                                if (it.id == existing.id) it.copy(name = trimmedName, colorValue = colorInt, updatedAt = System.currentTimeMillis())
                                                else it
                                            }
                                            selectedColorIds = selectedColorIds + existing.id
                                        } else {
                                            val duplicate = colorOptions.firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
                                            if (duplicate != null) {
                                                colorOptions = colorOptions.map {
                                                    if (it.id == duplicate.id) it.copy(name = trimmedName, colorValue = colorInt, updatedAt = System.currentTimeMillis(), isDefault = false)
                                                    else it
                                                }
                                                selectedColorIds = selectedColorIds + duplicate.id
                                            } else {
                                                val newColor = TagColor(name = trimmedName, colorValue = colorInt, isDefault = false)
                                                colorOptions = colorOptions + newColor
                                                selectedColorIds = selectedColorIds + newColor.id
                                            }
                                        }
                                    }
                                )
                            }

                            TagColorSelectionStep(
                                tagColors = colorOptions,
                                selectedIds = selectedColorIds,
                                onToggle = { colorId ->
                                    selectedColorIds = selectedColorIds.toMutableSet().apply {
                                        if (contains(colorId)) remove(colorId) else add(colorId)
                                    }
                                },
                                onRemoveCustom = { colorId ->
                                    val toRemove = colorOptions.firstOrNull { it.id == colorId && !it.isDefault }
                                    if (toRemove != null) {
                                        colorOptions = colorOptions.filterNot { it.id == colorId }
                                        selectedColorIds = selectedColorIds - colorId
                                    }
                                },
                                onAddCustom = { showColorDialog = true }
                            )
                        }

                        SetupWizardStep.ACTIVITIES -> {
                            ActivitySelectionStep(
                                defaultActivities = defaultActivityTypes,
                                selectedDefaultIds = selectedDefaultActivityIds,
                                activityName = activityName,
                                activityDescription = activityDescription,
                                customActivities = activityDrafts,
                                onToggleDefault = { activityId ->
                                    selectedDefaultActivityIds = selectedDefaultActivityIds.toMutableSet().apply {
                                        if (contains(activityId)) remove(activityId) else add(activityId)
                                    }
                                },
                                onActivityNameChanged = { activityName = it },
                                onActivityDescriptionChanged = { activityDescription = it },
                                onAddCustomActivity = addCustomActivity@{
                                    val trimmedName = activityName.trim()
                                    if (trimmedName.isEmpty()) return@addCustomActivity
                                    val draft = SetupWizardActivityDraft(
                                        name = trimmedName,
                                        description = activityDescription.trim()
                                    )
                                    activityDrafts = activityDrafts + draft
                                    activityName = ""
                                    activityDescription = ""
                                },
                                onRemoveCustomActivity = { id ->
                                    activityDrafts = activityDrafts.filterNot { it.id == id }
                                }
                            )
                        }

                        SetupWizardStep.PASTURES -> {
                            PastureSetupStep(
                                pastureName = pastureName,
                                pastureSize = pastureSize,
                                pastureDetails = pastureDetails,
                                pastureDrafts = pastureDrafts,
                                onNameChanged = { pastureName = it },
                                onSizeChanged = { pastureSize = it },
                                onDetailsChanged = { pastureDetails = it },
                                onAddPasture = {
                                    val draft = SetupWizardPastureDraft(
                                        name = pastureName.trim(),
                                        size = pastureSize.trim(),
                                        details = pastureDetails.trim()
                                    )
                                    pastureDrafts = pastureDrafts + draft
                                    pastureName = ""
                                    pastureSize = ""
                                    pastureDetails = ""
                                },
                                onRemoveDraft = { id ->
                                    pastureDrafts = pastureDrafts.filterNot { it.id == id }
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exit Wizard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .clickable(enabled = !isSaving, onClick = onExit)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { if (!isSaving) handleSkip() }, enabled = !isSaving) {
                            Text("Skip")
                        }

                        val saveButtonLabel = if (currentStepIndex == steps.lastIndex) "Save & Finish" else "Save & Next"
                        val saveEnabled = when (steps[currentStepIndex]) {
                            SetupWizardStep.IDENTIFIERS -> selectedIdentifierMode != null && !isSaving
                            SetupWizardStep.PASTURES -> !isSaving
                            else -> !isSaving
                        }

                        Button(
                            onClick = {
                                if (isSaving) return@Button
                                when (steps[currentStepIndex]) {
                                    SetupWizardStep.IDENTIFIERS -> {
                                        val mode = selectedIdentifierMode ?: return@Button
                                        coroutineScope.launch {
                                            isSaving = true
                                            errorMessage = null
                                            try {
                                                onSaveIdentifierMode(mode)
                                                moveToNextStep()
                                            } catch (t: Throwable) {
                                                errorMessage = t.localizedMessage ?: "Unable to save preference"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }

                                    SetupWizardStep.BREEDS -> {
                                        val trimmedCustomBreeds = customBreedFields
                                            .map { it.value.trim() }
                                            .filter { it.isNotEmpty() }
                                        val selectedDefaults = defaultBreeds.filter { selectedDefaultBreedIds.contains(it.id) }
                                        val breedsToSave = buildList {
                                            addAll(selectedDefaults)
                                            trimmedCustomBreeds.forEach { customName ->
                                                add(Breed(name = customName))
                                            }
                                        }

                                        coroutineScope.launch {
                                            isSaving = true
                                            errorMessage = null
                                            try {
                                                onSaveBreeds(breedsToSave)
                                                moveToNextStep()
                                            } catch (t: Throwable) {
                                                errorMessage = t.localizedMessage ?: "Unable to save breeds"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }

                                    SetupWizardStep.TAG_COLORS -> {
                                        val colorsToSave = colorOptions.filter { selectedColorIds.contains(it.id) }
                                        coroutineScope.launch {
                                            isSaving = true
                                            errorMessage = null
                                            try {
                                                onSaveTagColors(colorsToSave)
                                                moveToNextStep()
                                            } catch (t: Throwable) {
                                                errorMessage = t.localizedMessage ?: "Unable to save tag colors"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }

                                    SetupWizardStep.ACTIVITIES -> {
                                        val selectedDefaults = defaultActivityTypes.filter {
                                            selectedDefaultActivityIds.contains(it.id)
                                        }
                                        val nameRegistry = mutableSetOf<String>().apply {
                                            addAll(selectedDefaults.map { it.name.uppercase(Locale.US) })
                                        }
                                        val customActivities = activityDrafts.map { draft ->
                                            val displayName = draft.name.trim()
                                            val generatedName = nameRegistry.generateActivityTypeName(displayName)
                                            ActivityTypeConfig(
                                                name = generatedName,
                                                displayName = displayName,
                                                description = draft.description.takeIf { it.isNotBlank() }
                                            )
                                        }
                                        val activitiesToSave = selectedDefaults + customActivities

                                        coroutineScope.launch {
                                            isSaving = true
                                            errorMessage = null
                                            try {
                                                onSaveActivities(activitiesToSave)
                                                moveToNextStep()
                                            } catch (t: Throwable) {
                                                errorMessage = t.localizedMessage ?: "Unable to save activity types"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }

                                    SetupWizardStep.PASTURES -> {
                                        val pasturesToSave = pastureDrafts.map { draft ->
                                            Pasture(
                                                id = draft.id,
                                                name = draft.name,
                                                description = draft.details.takeIf { it.isNotBlank() },
                                                sizeAcres = draft.size.toDoubleOrNull()
                                            )
                                        }

                                        coroutineScope.launch {
                                            isSaving = true
                                            errorMessage = null
                                            try {
                                                onSavePastures(pasturesToSave)
                                                moveToNextStep()
                                            } catch (t: Throwable) {
                                                errorMessage = t.localizedMessage ?: "Unable to save pastures"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = saveEnabled
                        ) {
                            Text(saveButtonLabel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentifierPreferenceStep(
    selected: AnimalIdentifierMode?,
    onSelect: (AnimalIdentifierMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Choose how you'd like to identify animals in the app.",
            style = MaterialTheme.typography.bodyMedium
        )
        val options = listOf(
            AnimalIdentifierMode.NAMES to "Animal Names",
            AnimalIdentifierMode.TAG_NUMBERS to "Tag Numbers",
            AnimalIdentifierMode.BOTH to "Both"
        )

        // SegmentedButton is not available on older M3 versions.
        // Use stable FilterChips to create a single-choice, segmented-like row.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (mode, label) ->
                val isSelected = selected == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(mode) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }

        Text(
            text = "You can update this preference anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BreedSelectionStep(
    defaultBreeds: List<Breed>,
    selectedDefaultIds: Set<String>,
    customFields: List<CustomBreedField>,
    onToggleDefault: (String) -> Unit,
    onCustomValueChanged: (String, String) -> Unit,
    onRemoveCustomField: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Select the breeds you manage. Add any custom breeds as needed.",
            style = MaterialTheme.typography.bodyMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Custom Breeds", style = MaterialTheme.typography.titleSmall)
            customFields.forEach { field ->
                OutlinedTextField(
                    value = field.value,
                    onValueChange = { onCustomValueChanged(field.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Add a custom breed") },
                    singleLine = true,
                    trailingIcon = {
                        if (field.value.isNotBlank()) {
                            IconButton(onClick = { onRemoveCustomField(field.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove custom breed")
                            }
                        }
                    },
                    colors = defaultOutlinedTextFieldColors()
                )
            }
        }

        Divider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Default Breeds", style = MaterialTheme.typography.titleSmall)
            defaultBreeds.sortedBy { it.name }.forEach { breed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleDefault(breed.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = selectedDefaultIds.contains(breed.id),
                        onCheckedChange = { onToggleDefault(breed.id) }
                    )
                    Text(
                        text = breed.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagColorSelectionStep(
    tagColors: List<TagColor>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onRemoveCustom: (String) -> Unit,
    onAddCustom: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Select the tag colors you use. Add custom colors for your operation.",
            style = MaterialTheme.typography.bodyMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tagColors.forEach { tagColor ->
                val color = Color(tagColor.colorValue)
                val textColor = if (color.luminance() > 0.5f) Color.Black else Color.White
                val isSelected = selectedIds.contains(tagColor.id)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onToggle(tagColor.id) },
                    color = color,
                    tonalElevation = if (isSelected) 6.dp else 2.dp,
                    shadowElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = textColor)
                        }
                        Text(
                            text = tagColor.name,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!tagColor.isDefault) {
                            IconButton(
                                onClick = { onRemoveCustom(tagColor.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove color",
                                    tint = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onAddCustom, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Custom color")
        }
    }
}

@Composable
private fun ActivitySelectionStep(
    defaultActivities: List<ActivityTypeConfig>,
    selectedDefaultIds: Set<String>,
    activityName: String,
    activityDescription: String,
    customActivities: List<SetupWizardActivityDraft>,
    onToggleDefault: (String) -> Unit,
    onActivityNameChanged: (String) -> Unit,
    onActivityDescriptionChanged: (String) -> Unit,
    onAddCustomActivity: () -> Unit,
    onRemoveCustomActivity: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Choose the activity types you want to track. You can add more later in Settings.",
            style = MaterialTheme.typography.bodyMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Default Activity Types", style = MaterialTheme.typography.titleSmall)
            defaultActivities.sortedBy { it.displayName.uppercase(Locale.US) }.forEach { activity ->
                val isSelected = selectedDefaultIds.contains(activity.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleDefault(activity.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleDefault(activity.id) }
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(activity.displayName, style = MaterialTheme.typography.bodyLarge)
                        activity.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Divider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Custom Activity Type", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = activityName,
                onValueChange = onActivityNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Activity name*") },
                singleLine = true,
                colors = defaultOutlinedTextFieldColors()
            )

            OutlinedTextField(
                value = activityDescription,
                onValueChange = onActivityDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 2,
                colors = defaultOutlinedTextFieldColors()
            )

            Button(
                onClick = onAddCustomActivity,
                enabled = activityName.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Add Activity Type")
            }

            if (customActivities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Custom activity types", style = MaterialTheme.typography.titleSmall)
                    customActivities.forEach { draft ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(draft.name, style = MaterialTheme.typography.titleMedium)
                                    if (draft.description.isNotBlank()) {
                                        Text(draft.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                IconButton(onClick = { onRemoveCustomActivity(draft.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove activity type")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastureSetupStep(
    pastureName: String,
    pastureSize: String,
    pastureDetails: String,
    pastureDrafts: List<SetupWizardPastureDraft>,
    onNameChanged: (String) -> Unit,
    onSizeChanged: (String) -> Unit,
    onDetailsChanged: (String) -> Unit,
    onAddPasture: () -> Unit,
    onRemoveDraft: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Add the pastures you want to manage right away. You can edit these later in Settings.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = pastureName,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Pasture name*") },
            singleLine = true,
            colors = defaultOutlinedTextFieldColors()
        )

        OutlinedTextField(
            value = pastureSize,
            onValueChange = onSizeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Size (acres)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = defaultOutlinedTextFieldColors()
        )

        OutlinedTextField(
            value = pastureDetails,
            onValueChange = onDetailsChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Details") },
            minLines = 2,
            colors = defaultOutlinedTextFieldColors()
        )

        Button(
            onClick = onAddPasture,
            enabled = pastureName.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Pasture")
        }

        if (pastureDrafts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pastures to create", style = MaterialTheme.typography.titleSmall)
                pastureDrafts.forEach { draft ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(draft.name, style = MaterialTheme.typography.titleMedium)
                                if (draft.size.isNotBlank()) {
                                    Text(
                                        text = "Size: ${draft.size} acres",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (draft.details.isNotBlank()) {
                                    Text(
                                        text = draft.details,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveDraft(draft.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove pasture")
                            }
                        }
                    }
                }
            }
        }
    }
}
