package com.jumblemint.cows.ui.screens.cows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.model.*
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.CattleTagBadge
import com.jumblemint.cows.ui.components.rememberTagColorMap
import com.jumblemint.cows.ui.components.resolveTagColor
import com.jumblemint.cows.ui.viewmodel.CowInfoViewModel
import com.jumblemint.cows.ui.viewmodel.CowInfoViewModelFactory
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowInfoScreen(
    cowId: Long,
    onNavigateBack: () -> Unit,
    onEditCow: () -> Unit
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = CattleRepository(
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
    val viewModel: CowInfoViewModel = viewModel(
        factory = CowInfoViewModelFactory(repository, cowId)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Get tag color map for resolving tag colors
    val tagColorMap = rememberTagColorMap(repository)
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(uiState.cow?.name ?: "Cow Info") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconToggleButton(
                    checked = uiState.cow?.isWatched == true,
                    onCheckedChange = { viewModel.toggleWatch() }
                ) {
                    Icon(
                        if (uiState.cow?.isWatched == true) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (uiState.cow?.isWatched == true) "Stop Watching" else "Watch",
                        tint = if (uiState.cow?.isWatched == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditCow) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        )
        
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
                    // Header with tag and basic info
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Tag
                            if (cow.tagNumber != null || cow.tagColor != null) {
                                CattleTagBadge(
                                    tagNumber = cow.tagNumber,
                                    tagColor = cow.tagColor,
                                    modifier = Modifier.height(80.dp),
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
                                cow.birthDate?.let { birthDate ->
                                    val age = Period.between(birthDate, LocalDate.now())
                                    val ageText = when {
                                        age.years > 0 -> "${age.years}y ${age.months}m"
                                        age.months > 0 -> "${age.months}m ${age.days}d"
                                        else -> "${age.days}d"
                                    }
                                    Text(
                                        text = "Age: $ageText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Basic Information
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Basic Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            cow.birthDate?.let {
                                InfoRow("Birth Date", it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                            }
                            InfoRow("Status", cow.status.name.lowercase().replaceFirstChar { it.uppercase() })
                            uiState.pastureName?.let {
                                InfoRow("Pasture", it)
                            }
                            cow.colorMarkings?.takeIf { it.isNotBlank() }?.let {
                                InfoRow("Color/Markings", it)
                            }
                        }
                    }
                    
                    // Parentage
                    if (uiState.mother != null || uiState.father != null) {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Parentage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                uiState.mother?.let { mother ->
                                    InfoRow("Mother", mother.name ?: "Unnamed (${mother.tagNumber ?: "No tag"})")
                                }
                                uiState.father?.let { father ->
                                    InfoRow("Father", father.name ?: "Unnamed (${father.tagNumber ?: "No tag"})")
                                }
                            }
                        }
                    }
                    
                    // Children
                    if (uiState.children.isNotEmpty()) {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Children (${uiState.children.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                uiState.children.forEach { child ->
                                    val childName = child.name ?: "Unnamed"
                                    val childTag = child.tagNumber?.let { " ($it)" } ?: ""
                                    val childAge = child.birthDate?.let { birthDate ->
                                        val age = Period.between(birthDate, LocalDate.now())
                                        when {
                                            age.years > 0 -> " - ${age.years}y ${age.months}m"
                                            age.months > 0 -> " - ${age.months}m ${age.days}d"
                                            else -> " - ${age.days}d"
                                        }
                                    } ?: ""
                                    InfoRow(child.classification.name, "$childName$childTag$childAge")
                                }
                            }
                        }
                    }
                    
                    // Activities
                    if (uiState.activities.isNotEmpty()) {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Recent Activities (${uiState.activities.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                uiState.activities.take(5).forEach { activity ->
                                    val dateStr = activity.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                    InfoRow(activity.activityType.name, "$dateStr - ${activity.notes ?: "No notes"}")
                                }
                                
                                if (uiState.activities.size > 5) {
                                    Text(
                                        text = "... and ${uiState.activities.size - 5} more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Error message
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}