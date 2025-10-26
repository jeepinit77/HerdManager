package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.DatePickerField
import com.jumblemint.cows.ui.components.DropdownField
import com.jumblemint.cows.ui.components.ParentPicker
import com.jumblemint.cows.ui.components.ParentSelectionField
import com.jumblemint.cows.ui.components.formatParentDisplay
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.theme.getGenderColor
import com.jumblemint.cows.ui.theme.defaultOutlinedTextFieldColors
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailUiState
import com.jumblemint.cows.ui.components.UnsavedChangesDialog
import com.jumblemint.cows.util.identifierRequirementMessage
import com.jumblemint.cows.util.isIdentifierSatisfied
import com.jumblemint.cows.util.usesNames
import com.jumblemint.cows.util.usesTags
import kotlin.collections.buildMap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CowEditScreen(
    cowId: Long,
    viewModel: CowDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val database = CattleDatabase.getDatabase(context)
    val repository = remember {
        CattleRepository(
            database.cowDao(), database.pastureDao(), database.activityDao(),
            database.settingsDao(), database.noteDao(), database.userDao(),
            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
            database.activityTypeConfigDao(), database.breedDao()
        )
    }

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val uiState by viewModel.uiState.collectAsState()
    val tagColorMap: Map<String, Color> = rememberTagColorMap(repository)
    var saveAttempted by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    var showMotherPicker by remember { mutableStateOf(false) }
    var showFatherPicker by remember { mutableStateOf(false) }

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
    val motherDisplay = selectedMother?.let { formatParentDisplay(it, uiState.identifierMode) } ?: uiState.motherName.orEmpty()
    val fatherDisplay = selectedFather?.let { formatParentDisplay(it, uiState.identifierMode) } ?: uiState.fatherName.orEmpty()

    val fatherOptions = remember(uiState.availableFathers, uiState.fatherId) {
        val bulls = uiState.availableFathers.filter { it.classification == Classification.BULL }
        val current = uiState.availableFathers.find { it.id == uiState.fatherId }
        if (current != null && bulls.none { it.id == current.id }) {
            (bulls + current).distinctBy { it.id }
        } else {
            bulls
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Notify parent about unsaved changes
    LaunchedEffect(uiState.hasUnsavedChanges) {
        onUnsavedChangesChanged(uiState.hasUnsavedChanges)
    }

    // Handle back press with unsaved changes warning
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

    if (showMotherPicker) {
        ParentPicker(
            title = "Select Mother",
            animals = uiState.availableMothers,
            pastureNames = pastureNames,
            classificationOptions = listOf(Classification.COW, Classification.HEIFER),
            enablePastureFilter = true,
            allowClearSelection = true,
            identifierMode = uiState.identifierMode,
            onSelect = { cow -> viewModel.updateMother(cow.id) },
            onClearSelection = { viewModel.updateMother(null) },
            onDismiss = { showMotherPicker = false }
        )
    }

    if (showFatherPicker) {
        ParentPicker(
            title = "Select Father",
            animals = fatherOptions,
            pastureNames = pastureNames,
            enablePastureFilter = true,
            allowClearSelection = true,
            identifierMode = uiState.identifierMode,
            onSelect = { cow -> viewModel.updateFather(cow.id) },
            onClearSelection = { viewModel.updateFather(null) },
            onDismiss = { showFatherPicker = false }
        )
    }

    // Function to handle save logic
    val handleSave = {
        viewModel.saveCow()
    }

    // Handle save trigger from top bar (only when there are changes)
    LaunchedEffect(saveTriggered) {
        if (saveTriggered && uiState.hasUnsavedChanges) {
            handleSave()
            onSaveHandled()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Profile", "Pedigree", "Management")
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    // Focus/scroll helpers for error focusing
    val nameFocusRequester = remember { FocusRequester() }
    val tagFocusRequester = remember { FocusRequester() }
    val genderBringRequester = remember { BringIntoViewRequester() }
    val classificationBringRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    // React to a Save attempt: mark attempted, and navigate to the first tab with errors
    LaunchedEffect(viewModel) {
        viewModel.saveAttemptSignal.collect {
            saveAttempted = true
            // Use the uiState captured at the moment of the signal for initial error check
            val uiStateAtSignalTime = uiState
            val missingOnProfile = (!uiStateAtSignalTime.identifierMode.isIdentifierSatisfied(
                uiStateAtSignalTime.name,
                uiStateAtSignalTime.tagNumber
            )) ||
                    (uiStateAtSignalTime.gender == null) ||
                    (uiStateAtSignalTime.classification == null)

            if (missingOnProfile) {
                // Errors are on the Profile tab (index 0).
                if (selectedTabIndex != 0) {
                    // Not currently on the Profile tab. Switch to it.
                    selectedTabIndex = 0 // Update TabRow indicator
                    scope.launch {
                        pagerState.animateScrollToPage(0) // Wait for scroll to complete
                        // Now that the Profile tab (page 0) is visible, request focus based on LATEST uiState.
                        val latestUiState = viewModel.uiState.value
                        when {
                            !latestUiState.identifierMode.isIdentifierSatisfied(
                                latestUiState.name,
                                latestUiState.tagNumber
                            ) -> {
                                when {
                                    latestUiState.identifierMode.usesNames() -> {
                                        nameFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                    latestUiState.identifierMode.usesTags() -> {
                                        tagFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }
                            }
                            latestUiState.gender == null -> {
                                genderBringRequester.bringIntoView()
                            }
                            latestUiState.classification == null -> {
                                classificationBringRequester.bringIntoView()
                            }
                        }
                    }
                } else {
                    // Already on the Profile tab. Just handle focus/bringIntoView based on LATEST uiState.
                    scope.launch {
                        val latestUiState = viewModel.uiState.value
                        when {
                            !latestUiState.identifierMode.isIdentifierSatisfied(
                                latestUiState.name,
                                latestUiState.tagNumber
                            ) -> {
                                when {
                                    latestUiState.identifierMode.usesNames() -> {
                                        nameFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                    latestUiState.identifierMode.usesTags() -> {
                                        tagFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }
                            }
                            latestUiState.gender == null -> {
                                genderBringRequester.bringIntoView()
                            }
                            latestUiState.classification == null -> {
                                classificationBringRequester.bringIntoView()
                            }
                        }
                    }
                }
            }
            // Add handling for errors on other tabs here if needed.
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onSave = {
                showUnsavedChangesDialog = false
                handleSave()
            },
            onDiscard = {
                showUnsavedChangesDialog = false
                onNavigateBack()
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Build a list of validation errors to show all at once
            val validationErrors = if (saveAttempted) {
                buildList {
                    if (!uiState.identifierMode.isIdentifierSatisfied(uiState.name, uiState.tagNumber)) {
                        add(uiState.identifierMode.identifierRequirementMessage())
                    }
                    if (uiState.gender == null) add("Please select a Gender.")
                    if (uiState.classification == null) add("Please select a Classification.")
                }
            } else emptyList()
            if (validationErrors.isNotEmpty() || uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (validationErrors.isNotEmpty()) {
                            validationErrors.forEach { msg ->
                                Text(text = "• $msg", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        } else {
                            Text(
                                text = uiState.error ?: "An unknown error occurred.",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val profileHasError = saveAttempted && (
                        !uiState.identifierMode.isIdentifierSatisfied(uiState.name, uiState.tagNumber) ||
                        uiState.gender == null ||
                        uiState.classification == null
                    )
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                            saveAttempted = false
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(title)
                                if (index == 0 && profileHasError) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    when (page) {
                        0 -> ProfileTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            tagColorMap = tagColorMap,
                            saveAttempted = saveAttempted,
                            nameFocusRequester = nameFocusRequester,
                            tagFocusRequester = tagFocusRequester,
                            genderBringRequester = genderBringRequester,
                            classificationBringRequester = classificationBringRequester,

                            isDarkTheme = isDarkTheme
                        )
                        1 -> PedigreeTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            motherDisplay = motherDisplay,
                            fatherDisplay = fatherDisplay,
                            onMotherClick = { showMotherPicker = true },
                            onFatherClick = { showFatherPicker = true }
                        )
                        2 -> ManagementTabContent(viewModel, uiState)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProfileTabContent(
    viewModel: CowDetailViewModel,
    uiState: CowDetailUiState,
    tagColorMap: Map<String, Color>,
    saveAttempted: Boolean,
    nameFocusRequester: FocusRequester,
    tagFocusRequester: FocusRequester,
    genderBringRequester: BringIntoViewRequester,
    classificationBringRequester: BringIntoViewRequester,
 
    isDarkTheme: Boolean
)
{
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Identification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val usesNames = uiState.identifierMode.usesNames()
        val usesTags = uiState.identifierMode.usesTags()
        val identifierError = saveAttempted && !uiState.identifierMode.isIdentifierSatisfied(uiState.name, uiState.tagNumber)
        val requirementMessage = uiState.identifierMode.identifierRequirementMessage()
        val showNameMessage = identifierError && usesNames && uiState.name.isBlank() && (!usesTags || uiState.tagNumber.isBlank())
        val showTagMessage = identifierError && usesTags && uiState.tagNumber.isBlank() && (!usesNames || uiState.name.isBlank())
        val nameLabel = if (uiState.identifierMode == AnimalIdentifierMode.NAMES) "Name*" else "Name"
        val tagLabel = if (uiState.identifierMode == AnimalIdentifierMode.TAG_NUMBERS) "Tag Number*" else "Tag Number"

        when {
            usesNames && usesTags -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = { Text(nameLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocusRequester),
                            isError = identifierError && uiState.name.isBlank() && uiState.tagNumber.isBlank(),
                            supportingText = if (showNameMessage) { { Text(requirementMessage) } } else null,
                            colors = defaultOutlinedTextFieldColors()
                        )

                        Box {
                            OutlinedTextField(
                                value = uiState.tagNumber,
                                onValueChange = viewModel::updateTagNumber,
                                label = { Text(tagLabel) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(tagFocusRequester),
                                enabled = !uiState.isNameTagLinked,
                                isError = identifierError && uiState.name.isBlank() && uiState.tagNumber.isBlank(),
                                supportingText = if (showTagMessage && !uiState.isNameTagLinked) { { Text(requirementMessage) } } else null,
                                colors = defaultOutlinedTextFieldColors()
                            )
                            if (uiState.isNameTagLinked) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .pointerInput(Unit) {},
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Linked to name field",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .fillMaxHeight()
                            .clickable(
                                onClick = { viewModel.toggleNameTagLink() },
                                role = Role.Button,
                                onClickLabel = if (uiState.isNameTagLinked) "Unlink Name and Tag Number" else "Link Name and Tag Number"
                            )
                            .padding(horizontal = 2.dp)
                            .semantics {
                                contentDescription = if (uiState.isNameTagLinked) "Name and Tag Number are linked. Click to unlink."
                                                   else "Name and Tag Number are unlinked. Click to link."
                            },
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val linkIndicatorColor = if (uiState.isNameTagLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        Text("╗", color = linkIndicatorColor, style = MaterialTheme.typography.headlineSmall)
                        Icon(
                            imageVector = if (uiState.isNameTagLinked) Icons.Filled.Link else Icons.Filled.LinkOff,
                            contentDescription = null,
                            tint = linkIndicatorColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("╝", color = linkIndicatorColor, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            usesNames -> {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text(nameLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    isError = identifierError,
                    supportingText = if (showNameMessage) { { Text(requirementMessage) } } else null,
                    colors = defaultOutlinedTextFieldColors()
                )
            }

            usesTags -> {
                OutlinedTextField(
                    value = uiState.tagNumber,
                    onValueChange = viewModel::updateTagNumber,
                    label = { Text(tagLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(tagFocusRequester),
                    isError = identifierError,
                    supportingText = if (showTagMessage) { { Text(requirementMessage) } } else null,
                    colors = defaultOutlinedTextFieldColors()
                )
            }
        }

        DropdownField(
            value = uiState.tagColor ?: "",
            onValueChange = { 
                viewModel.updateTagColor(it)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
            label = "Tag Color", 
            options = uiState.tagColors,
            modifier = Modifier.fillMaxWidth(),
            valueBackgroundColor = { name -> resolveTagColor(name, tagColorMap) }, 
            optionBackgroundColor = { name -> resolveTagColor(name, tagColorMap) }
        )
        
        Text("Birth & Genetics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        DatePickerField(
            value = uiState.birthDate,
            onValueChange = {
                viewModel.updateBirthDate(it)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
            label = "Birth Date", // Removed asterisk
            modifier = Modifier.fillMaxWidth()
        )

        Text("Gender*", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(genderBringRequester)
        ) {
            Gender.entries.forEachIndexed { index, genderOption ->
                val currentActiveContainerColor = getGenderColor(genderOption)
                val currentActiveContentColor = when (genderOption) {
                    Gender.MALE -> if (isDarkTheme) Color.Black else Color.White
                    Gender.FEMALE -> if (isDarkTheme) Color.Black else Color.White
                    Gender.TBD -> if (isDarkTheme) Color.Black else Color.White
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Gender.entries.size),
                    onClick = { 
                        viewModel.updateGender(genderOption)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    selected = uiState.gender == genderOption,
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = currentActiveContainerColor,
                        activeContentColor = currentActiveContentColor
                    )
                ) {
                    Text(text = genderOption.name.lowercase().replaceFirstChar { it.titlecase() })
                }
            }
        }
        if (saveAttempted && uiState.gender == null) {
            Text(
                text = "Please select a Gender.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        Text("Type*", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        val availableClassifications = remember(uiState.gender) {
            when (uiState.gender) {
                Gender.FEMALE -> listOf(Classification.COW, Classification.HEIFER, Classification.CALF)
                Gender.MALE -> listOf(Classification.BULL, Classification.STEER, Classification.CALF)
                Gender.TBD -> Classification.entries.toList()
                null -> Classification.entries.toList()
            }
        }

        if (availableClassifications.isNotEmpty()) {
            val classificationActiveContainerColor = uiState.gender?.let { getGenderColor(it) } ?: MaterialTheme.colorScheme.primary
            val classificationActiveContentColor = when (uiState.gender) {
                Gender.MALE -> if (isDarkTheme) Color.Black else Color.White
                Gender.FEMALE -> if (isDarkTheme) Color.Black else Color.White
                else -> if (isDarkTheme) Color.Black else Color.White
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(classificationBringRequester)
            ) {
                availableClassifications.forEachIndexed { index, classificationOption ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = availableClassifications.size),
                        onClick = { 
                            viewModel.updateClassification(classificationOption)
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        selected = uiState.classification == classificationOption,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = classificationActiveContainerColor,
                            activeContentColor = classificationActiveContentColor
                        )
                    ) {
                        Text(text = classificationOption.name.lowercase().replaceFirstChar { it.titlecase() })
                    }
                }
            }
        } else {
            Text(
                "Select a gender to see types", 
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
            )
        }
        if (saveAttempted && uiState.classification == null) {
            Text(
                text = "Please select a Classification.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        Text("Physical Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = uiState.colorMarkings,
            onValueChange = viewModel::updateColorMarkings,
            label = { Text("Color/Markings") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3,
            colors = defaultOutlinedTextFieldColors()
        )
    }
}

@Composable
private fun PedigreeTabContent(
    viewModel: CowDetailViewModel,
    uiState: CowDetailUiState,
    motherDisplay: String,
    fatherDisplay: String,
    onMotherClick: () -> Unit,
    onFatherClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Parentage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ParentSelectionField(
            label = "Mother",
            value = motherDisplay,
            onClick = onMotherClick,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Tap to select a mother"
        )
        ParentSelectionField(
            label = "Father",
            value = fatherDisplay,
            onClick = onFatherClick,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Tap to select a father"
        )

        Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
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
            label = { Text("Registration Number") },
            modifier = Modifier.fillMaxWidth(),
            colors = defaultOutlinedTextFieldColors()
        )
    }
}

@Composable
private fun ManagementTabContent(
    viewModel: CowDetailViewModel,
    uiState: CowDetailUiState
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Management Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        DropdownField(
            value = uiState.status.name, 
            onValueChange = { statusName -> viewModel.updateStatus(Status.valueOf(statusName)) },
            label = "Status*",
            options = Status.entries.map { it.name },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownField(
            value = uiState.pastureName ?: "",
            onValueChange = { selectedPastureName -> 
                val pasture = uiState.availablePastures.find { p -> p.name == selectedPastureName }
                viewModel.updatePasture(pasture?.id)
            },
            label = "Pasture",
            options = listOf("") + uiState.availablePastures.map { it.name },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Add to Watchlist",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = uiState.isWatched,
                onCheckedChange = viewModel::updateIsWatched
            )
        }
    }
}
