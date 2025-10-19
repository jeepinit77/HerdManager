package com.jumblemint.cows.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSampleDataDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Filled.PlaylistAdd, 
                contentDescription = "Sample Data",
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        title = { 
            Text(
                "Welcome to Cattle Manager!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Text(
                "Would you like to add sample data to explore the app's features?\n\nIncludes:\n• 22 sample cattle with family relationships\n• 4 pastures with different purposes\n• Activity history and notes\n\nYou can safely remove just the sample data later from Settings without affecting any cattle you add yourself.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onInstall) {
                Text("Add Sample Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Start Empty")
            }
        }
    )
}