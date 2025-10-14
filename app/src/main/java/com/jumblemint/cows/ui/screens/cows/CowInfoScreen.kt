package com.jumblemint.cows.ui.screens.cows

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.jumblemint.cows.ui.components.*
import com.jumblemint.cows.ui.theme.getCardColors
import com.jumblemint.cows.ui.theme.contrastingTextColor
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
    onCloseFlow: () -> Unit,
    modifier: Modifier = Modifier
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

    Column(
        modifier = modifier
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
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top, 
                                horizontalArrangement = Arrangement.spacedBy(12.dp) 
                            ) {
                                if (cow.tagNumber != null || cow.tagColor != null) {
                                    CattleTagBadge(
                                        tagNumber = cow.tagNumber,
                                        tagColor = cow.tagColor,
                                        modifier = Modifier.size(width = 72.dp, height = 96.dp), 
                                        backgroundColor = resolveTagColor(cow.tagColor, tagColorMap)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) { 
                                    Text(
                                        text = cow.name ?: "Unnamed Animal",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "${cow.classification.name} • ${cow.gender.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    cow.birthDate?.let {
                                        Text(
                                            text = "Age: ${calculateAgeString(it)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Physical Details Card
                        Card(colors = getCardColors()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SectionTitle("Physical Details")
                                cow.birthDate?.let {
                                    InfoRow("Birth Date", it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                                }
                                cow.colorMarkings?.takeIf { it.isNotBlank() }?.let { InfoRow("Color/Markings", it) }
                                cow.breed?.takeIf { it.isNotBlank() }?.let { InfoRow("Breed", it) }
                                cow.registrationNumber?.takeIf { it.isNotBlank() }?.let { InfoRow("Registration #", it) }
                            }
                        }
                        
                        // Management Details Card
                        Card(colors = getCardColors()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SectionTitle("Management Details")
                                InfoRow("Status", cow.status.name.lowercase().replaceFirstChar { it.uppercase() })
                                uiState.pastureName?.let { InfoRow("Pasture", it) }
                                cow.herdId?.takeIf { it.isNotBlank() }?.let { InfoRow("Herd ID", it) }
                                InfoRow("Watched", if (cow.isWatched) "⭐ Yes" else "No")
                            }
                        }

                        // Parentage Card
                        if (uiState.mother != null || uiState.father != null) {
                            Card(colors = getCardColors()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectionTitle("Parentage")
                                    uiState.mother?.let { mother ->
                                        RelatedCowRow(label = "Mother", cow = mother, onNavigateToCow = onNavigateToCow, showDivider = uiState.father != null)
                                    }
                                    uiState.father?.let { father ->
                                        RelatedCowRow(label = "Father", cow = father, onNavigateToCow = onNavigateToCow, showDivider = false)
                                    }
                                }
                            }
                        }

                        // Progeny Card (Children)
                        if (uiState.children.isNotEmpty()) {
                            CollapsibleLazyColumnCard(
                                title = "Progeny (${uiState.children.size})",
                                items = uiState.children,
                                onNavigateToCow = onNavigateToCow,
                                initiallyExpanded = uiState.children.size <= 3
                            )
                        }

                        // Maternal Siblings Card
                        if (uiState.maternalSiblings.isNotEmpty()) {
                            CollapsibleDetailCard(
                                title = "Maternal Siblings (${uiState.maternalSiblings.size})",
                                initiallyExpanded = uiState.maternalSiblings.size <= 3
                            ) {
                                uiState.maternalSiblings.forEachIndexed { index, sibling ->
                                    RelatedCowRow(cow = sibling, onNavigateToCow = onNavigateToCow, showDivider = index < uiState.maternalSiblings.size - 1)
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

                        // Recent Activities Card
                        if (uiState.activities.isNotEmpty()) {
                            CollapsibleDetailCard(
                                title = "Recent Activities (${uiState.activities.size})",
                                initiallyExpanded = true 
                            ) {
                                uiState.activities.take(5).forEach { activity ->
                                    val dateStr = activity.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                    InfoRow(activity.activityType.name.lowercase().replaceFirstChar { it.uppercase() }, "$dateStr - ${activity.notes ?: "No notes"}")
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

                        uiState.error?.let {
                            Card(
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
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } ?: run {
                    if (!uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Animal not found or an error occurred.", style = MaterialTheme.typography.titleMedium)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun RelatedCowRow(
    label: String? = null,
    cow: Cow,
    onNavigateToCow: (Long) -> Unit,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth()
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.contrastingTextColor().copy(alpha = 0.2f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Keep for Card and Icon usage
@Composable
private fun CollapsibleLazyColumnCard(
    title: String,
    items: List<Cow>,
    onNavigateToCow: (Long) -> Unit,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(colors = getCardColors()) {
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
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Spacing between RelatedCowRow items
                ) {
                    items(items.size) { index ->
                        RelatedCowRow(cow = items[index], onNavigateToCow = onNavigateToCow, showDivider = index < items.size - 1)
                    }
                }
            }
        }
    }
}

// New CollapsibleDetailCard Composable
@Composable
private fun CollapsibleDetailCard(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(colors = getCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (expanded) 8.dp else 0.dp)) { // Adjust bottom padding when expanded
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp), // Consistent padding for clickable area
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle(text = title)
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp) // Padding for the content itself
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing for InfoRows inside
                ) {
                    content()
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
