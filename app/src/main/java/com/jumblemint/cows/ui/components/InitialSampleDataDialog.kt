package com.jumblemint.cows.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jumblemint.cows.ui.components.SecondaryButton
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
        description = "Choose the activity types you track. Add custom ones now or later in Settings."
    ),
    PASTURES(
        title = "Add Pastures",
        description = "Add pasture names now. You can add acres and descriptions later in Settings."
    )
}

private data class CustomBreedField(
    val id: String = UUID.randomUUID().toString(),
    val value: String = ""
)

private data class CustomActivityField(
    val id: String = UUID.randomUUID().toString(),
    val value: String = ""
)

private data class CustomPastureField(
    val id: String = UUID.randomUUID().toString(),
    val value: String = ""
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
            var customActivityFields by remember { mutableStateOf(listOf(CustomActivityField())) }

            var customPastureFields by remember { mutableStateOf(listOf(CustomPastureField())) }

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
                                customFields = customActivityFields,
                                onToggleDefault = { activityId ->
                                    selectedDefaultActivityIds = selectedDefaultActivityIds.toMutableSet().apply {
                                        if (contains(activityId)) remove(activityId) else add(activityId)
                                    }
                                },
                                onCustomValueChanged = { id, value ->
                                    customActivityFields = customActivityFields.map { field ->
                                        if (field.id == id) field.copy(value = value) else field
                                    }
                                    if (customActivityFields.lastOrNull()?.id == id && value.trim().isNotEmpty()) {
                                        customActivityFields = customActivityFields + CustomActivityField()
                                    }
                                },
                                onRemoveCustomField = { id ->
                                    customActivityFields = customActivityFields
                                        .filterNot { it.id == id }
                                        .ifEmpty { listOf(CustomActivityField()) }
                                }
                            )
                        }

                        SetupWizardStep.PASTURES -> {
                            PastureSetupStep(
                                customFields = customPastureFields,
                                onCustomValueChanged = { id, value ->
                                    customPastureFields = customPastureFields.map { field ->
                                        if (field.id == id) field.copy(value = value) else field
                                    }
                                    if (customPastureFields.lastOrNull()?.id == id && value.trim().isNotEmpty()) {
                                        customPastureFields = customPastureFields + CustomPastureField()
                                    }
                                },
                                onRemoveField = { id ->
                                    customPastureFields = customPastureFields
                                        .filterNot { it.id == id }
                                        .ifEmpty { listOf(CustomPastureField()) }
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
                        if (currentStepIndex > 0) {
                            SecondaryButton(
                                onClick = {
                                    if (!isSaving) {
                                        errorMessage = null
                                        currentStepIndex--
                                    }
                                },
                                enabled = !isSaving
                            ) {
                                Text("Back")
                            }
                        }

                        SecondaryButton(onClick = { if (!isSaving) handleSkip() }, enabled = !isSaving) {
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
                                        val trimmedCustomActivities = customActivityFields
                                            .map { it.value.trim() }
                                            .filter { it.isNotEmpty() }
                                            .distinctBy { it.lowercase(Locale.US) }
                                        val nameRegistry = mutableSetOf<String>().apply {
                                            addAll(selectedDefaults.map { it.name.uppercase(Locale.US) })
                                        }
                                        val customActivities = trimmedCustomActivities.map { displayName ->
                                            val generatedName = nameRegistry.generateActivityTypeName(displayName)
                                            ActivityTypeConfig(
                                                name = generatedName,
                                                displayName = displayName,
                                                description = null
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
                                        val pasturesToSave = customPastureFields.mapNotNull { field ->
                                            val trimmedName = field.value.trim()
                                            if (trimmedName.isEmpty()) {
                                                null
                                            } else {
                                                Pasture(
                                                    id = field.id,
                                                    name = trimmedName
                                                )
                                            }
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

@Composable
private fun IdentifierPreferenceStep(
    selected: AnimalIdentifierMode?,
    onSelect: (AnimalIdentifierMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val options = listOf(
            AnimalIdentifierMode.NAMES to "Animal Names",
            AnimalIdentifierMode.TAG_NUMBERS to "Tag Numbers",
            AnimalIdentifierMode.BOTH to "Both"
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(label)
                }
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
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove custom breed"
                                )
                            }
                        }
                    },
                    colors = defaultOutlinedTextFieldColors()
                )
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

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
                    androidx.compose.material3.Checkbox(
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
    customFields: List<CustomActivityField>,
    onToggleDefault: (String) -> Unit,
    onCustomValueChanged: (String, String) -> Unit,
    onRemoveCustomField: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Custom Activity Types", style = MaterialTheme.typography.titleSmall)
            customFields.forEach { field ->
                OutlinedTextField(
                    value = field.value,
                    onValueChange = { onCustomValueChanged(field.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Add a custom activity type") },
                    singleLine = true,
                    trailingIcon = {
                        if (field.value.isNotBlank()) {
                            IconButton(onClick = { onRemoveCustomField(field.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove custom activity"
                                )
                            }
                        }
                    },
                    colors = defaultOutlinedTextFieldColors()
                )
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

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
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(activity.displayName, style = MaterialTheme.typography.bodyLarge)
                        activity.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastureSetupStep(
    customFields: List<CustomPastureField>,
    onCustomValueChanged: (String, String) -> Unit,
    onRemoveField: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pasture Names", style = MaterialTheme.typography.titleSmall)
            customFields.forEach { field ->
                OutlinedTextField(
                    value = field.value,
                    onValueChange = { onCustomValueChanged(field.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Add a pasture name") },
                    singleLine = true,
                    trailingIcon = {
                        if (field.value.isNotBlank()) {
                            IconButton(onClick = { onRemoveField(field.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove pasture")
                            }
                        }
                    },
                    colors = defaultOutlinedTextFieldColors()
                )
            }
        }
    }
}
