package com.jumblemint.cows.ui.screens.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.ui.screens.activities.ActivityInfoUiState
import com.jumblemint.cows.ui.components.CowCard
import com.jumblemint.cows.ui.viewmodel.ActivityInfoViewModel
import com.jumblemint.cows.ui.viewmodel.ActivityInfoViewModelFactory
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityInfoScreen(
    activityId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCow: (Long) -> Unit,
    onEditActivity: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val viewModel: ActivityInfoViewModel = viewModel(
        factory = ActivityInfoViewModelFactory(application.repository, activityId)
    )
    val uiState: ActivityInfoUiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.padding(PaddingValues(16.dp))) {
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Loading activity details...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        } else if (uiState.error != null) {
            Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        } else if (uiState.activity != null) {
            val activity = uiState.activity!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Activity Details", 
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        InfoRow("Type:", activity.activityType.displayName)
                        InfoRow("Date:", activity.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                        
                        val notes = activity.notes
                        if (!notes.isNullOrBlank()) {
                            InfoRow("Notes:", notes)
                        }
                        val details = activity.details
                        if (!details.isNullOrBlank()) {
                            InfoRow("Details:", details)
                        }
                        val result = activity.result
                        if (!result.isNullOrBlank()) {
                            InfoRow("Result:", result)
                        }
                        val quantity = activity.quantity
                        if (quantity != null) {
                            InfoRow("Quantity:", quantity.toString())
                        }
                        val technician = activity.technician
                        if (!technician.isNullOrBlank()) {
                            InfoRow("Technician:", technician)
                        }
                        val cost = activity.cost
                        if (cost != null) {
                            InfoRow("Cost:", "$${String.format("%.2f", cost)}")
                        } 
                    }
                }

                // Display associated animals section
                Text(
                    text = "Associated Animals", 
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (uiState.associatedCows.isNotEmpty()) {
                    uiState.associatedCows.forEach { cow ->
                        CowCard(
                            cow = cow,
                            identifierMode = uiState.identifierMode,
                            onClick = { onNavigateToCow(cow.id) }
                        )
                    }
                } else {
                    Text(
                        text = "No associated animals found for this activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text("Activity not found.", style = MaterialTheme.typography.titleMedium)
        }
    }
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(0.4f).padding(end = 8.dp) 
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(0.6f)
        )
    }
}
