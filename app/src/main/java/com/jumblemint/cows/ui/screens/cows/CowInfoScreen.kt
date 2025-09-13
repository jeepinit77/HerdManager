package com.jumblemint.cows.ui.screens.cows

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication // Assuming this is your Application class
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CattleTagBadge
import com.jumblemint.cows.ui.components.SimpleTopAppBar
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.CowInfoViewModel
import com.jumblemint.cows.ui.viewmodel.CowInfoViewModelFactory // Assuming factory is in this package
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowInfoScreen(
    cowId: Long,
    onNavigateBack: () -> Unit,
    onEditCow: () -> Unit,
    onNavigateToCow: (Long) -> Unit,
    onCloseFlow: () -> Unit, // Changed from onNavigateToDashboard
    modifier: Modifier = Modifier // This modifier is for the Navigation component to use
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember { // Create repository once
        CattleRepository(
            cowDao = database.cowDao(),
            pastureDao = database.pastureDao(),
            activityDao = database.activityDao(),
            settingsDao = database.settingsDao(),
            noteDao = database.noteDao(),
            userDao = database.userDao(),
            herdDao = database.herdDao(),
            herdMemberDao = database.herdMemberDao(),
            tagColorDao = database.tagColorDao(),
            activityTypeConfigDao = database.activityTypeConfigDao()
        )
    }
    val viewModel: CowInfoViewModel = viewModel(
        factory = CowInfoViewModelFactory(repository, cowId)
    )

    val uiState by viewModel.uiState.collectAsState()
    val tagColorMap = rememberTagColorMap(repository)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SimpleTopAppBar(
                title = uiState.cow?.name ?: "Cow Information",
                onBack = onNavigateBack,
                onEdit = onEditCow,
                onClose = onCloseFlow
            )
        }
    ) { paddingValues -> 
        Column(
            modifier = Modifier 
                .fillMaxSize()
                .padding(paddingValues) 
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.cow?.let { cow ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cow.tagNumber != null || cow.tagColor != null) {
                                    CattleTagBadge(
                                        tagNumber = cow.tagNumber,
                                        tagColor = cow.tagColor,
                                        modifier = Modifier.height(80.dp).widthIn(min = 60.dp),
                                        backgroundColor = resolveTagColor(cow.tagColor, tagColorMap)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cow.name ?: "Unnamed Cow",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${cow.classification.name} • ${cow.gender.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    cow.birthDate?.let {
                                        Text(
                                            text = "Age: ${calculateAgeString(it)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Basic Information Card
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SectionTitle("Basic Information")
                                cow.birthDate?.let {
                                    InfoRow("Birth Date", it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                                }
                                InfoRow("Status", cow.status.name.lowercase().replaceFirstChar { it.uppercase() })
                                uiState.pastureName?.let { InfoRow("Pasture", it) }
                                cow.colorMarkings?.takeIf { it.isNotBlank() }?.let { InfoRow("Color/Markings", it) }
                            }
                        }

                        // Parentage Card
                        if (uiState.mother != null || uiState.father != null) {
                            Card {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectionTitle("Parentage")
                                    uiState.mother?.let { mother ->
                                        RelatedCowRow(label = "Mother", cow = mother, onNavigateToCow = onNavigateToCow)
                                    }
                                    uiState.father?.let { father ->
                                        RelatedCowRow(label = "Father", cow = father, onNavigateToCow = onNavigateToCow)
                                    }
                                }
                            }
                        }

                        // Children Card
                        if (uiState.children.isNotEmpty()) {
                            CollapsibleLazyColumnCard(
                                title = "Children (${uiState.children.size})",
                                items = uiState.children,
                                onNavigateToCow = onNavigateToCow,
                                initiallyExpanded = uiState.children.size <= 3
                            )
                        }

                        // Maternal Siblings Card
                        if (uiState.maternalSiblings.isNotEmpty()) {
                            Card {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectionTitle("Maternal Siblings (${uiState.maternalSiblings.size})")
                                    uiState.maternalSiblings.forEach { sibling ->
                                        RelatedCowRow(cow = sibling, onNavigateToCow = onNavigateToCow)
                                    }
                                }
                            }
                        }

                        // Paternal Siblings Card
                        if (uiState.paternalSiblings.isNotEmpty()) {
                            CollapsibleLazyColumnCard(
                                title = "Paternal Siblings (${uiState.paternalSiblings.size})",
                                items = uiState.paternalSiblings,
                                onNavigateToCow = onNavigateToCow,
                                initiallyExpanded = false
                            )
                        }
                        // Activities Card
                        if (uiState.activities.isNotEmpty()) {
                            Card {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectionTitle("Recent Activities (${uiState.activities.size})")
                                    uiState.activities.take(5).forEach { activity ->
                                        val dateStr = activity.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                        InfoRow(activity.activityType.name, "$dateStr - ${activity.notes ?: "No notes"}")
                                    }
                                    if (uiState.activities.size > 5) {
                                        Text(
                                            text = "... and ${uiState.activities.size - 5} more",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Error Message Card
                        uiState.error?.let { error ->
                            Card(
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
                        Spacer(modifier = Modifier.height(16.dp)) // Bottom spacer
                    }
                } ?: run {
                    if (!uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Cow not found or an error occurred.", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f).padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun RelatedCowRow(
    label: String? = null,
    cow: Cow,
    onNavigateToCow: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToCow(cow.id) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label?.let {
                Text(
                    text = "$it: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = cow.name ?: "Unnamed",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            cow.tagNumber?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = " ($it)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            cow.birthDate?.let {
                Text(
                    text = "- ${calculateAgeString(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), thickness = 1.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleLazyColumnCard(
    title: String,
    items: List<Cow>,
    onNavigateToCow: (Long) -> Unit,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle(text = title)
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = expanded) {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items, key = { it.id }) { relatedCow ->
                        RelatedCowRow(cow = relatedCow, onNavigateToCow = onNavigateToCow)
                    }
                }
            }
        }
    }
}

private fun calculateAgeString(birthDate: LocalDate, currentDate: LocalDate = LocalDate.now()): String {
    val period = Period.between(birthDate, currentDate)
    return when {
        period.years > 0 -> "${period.years}y ${period.months}m"
        period.months > 0 -> "${period.months}m ${period.days}d"
        else -> "${period.days}d"
    }
}
