package com.jumblemint.cows.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.sync.SyncStatus
// Consider adding a proper date/time formatting utility if more detailed time is needed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val coroutineScope = rememberCoroutineScope()
    
    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val syncStatus by application.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    
    var showSignOutDialog by remember { mutableStateOf(false) }

    val lastSyncTimeText = remember(currentUser?.lastSyncAt, currentUser?.isLocalUser) {
        when {
            currentUser?.isLocalUser == true -> "N/A for local account"
            currentUser?.lastSyncAt == 0L || currentUser?.lastSyncAt == null -> "Never"
            else -> "Recently" // Placeholder - enhance with actual date formatting for better UX
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Account Management") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Information Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = currentUser?.displayName ?: "Unknown User",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentUser?.email ?: "No email",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                if (currentUser?.isLocalUser == false) {
                                    Text(
                                        text = "Cloud Account", // Changed from Google Account
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Sync & Data Section
            if (currentUser?.isLocalUser == false) {
                item {
                    Text(
                        text = "Sync & Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    AccountManagementCard(
                        title = "Cloud Sync",
                        subtitle = when (syncStatus) {
                            SyncStatus.SYNCING -> "Syncing your data..."
                            SyncStatus.SUCCESS -> "Data is up to date. (Last synced: $lastSyncTimeText)"
                            SyncStatus.ERROR -> "Sync error - tap to retry"
                            else -> "Tap to sync with Cloud. (Last synced: $lastSyncTimeText)" // IDLE state
                        },
                        icon = when (syncStatus) {
                            SyncStatus.SYNCING -> Icons.Default.CloudSync
                            SyncStatus.SUCCESS -> Icons.Default.CloudDone
                            SyncStatus.ERROR -> Icons.Default.CloudOff
                            else -> Icons.Default.Refresh 
                        },
                        onClick = {
                            if (syncStatus != SyncStatus.SYNCING) {
                                coroutineScope.launch {
                                    application.authService.startUserSync(application.syncService)
                                }
                            }
                        }
                    )
                }
            }
            
            // Account Actions Section
            item {
                Text(
                    text = "Account Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                AccountManagementCard(
                    title = if (currentUser?.isLocalUser == false) "Disconnect Account" else "Reset Local Account",
                    subtitle = if (currentUser?.isLocalUser == false) 
                        "Disconnect Cloud. Data stays on device, sync stops." // Changed from Google
                    else 
                        "Clear local user data and create new local account",
                    icon = Icons.Default.Logout,
                    onClick = { showSignOutDialog = true }
                )
            }
            
            // Account Information Section
            item {
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        AccountInfoRow("User ID", currentUser?.uid ?: "Unknown")
                        AccountInfoRow("Account Type", if (currentUser?.isLocalUser == true) "Local" else "Cloud") // Changed from Google
                        AccountInfoRow("Created", "Recently") 
                        if (currentUser?.isLocalUser == false) {
                            AccountInfoRow("Last Sync", lastSyncTimeText)
                        }
                    }
                }
            }
        }
    }
    
    // Sign Out/Disconnect Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { 
                Text(
                    if (currentUser?.isLocalUser == false) "Disconnect Account?" else "Reset Local Account?"
                ) 
            },
            text = { 
                Text(
                    if (currentUser?.isLocalUser == false)
                        "Your Cloud account will be disconnected from this app. Your app data will remain on this device for offline use but will no longer sync with the Cloud unless you connect your account again." // Changed from Google
                    else
                        "This will clear your local user data and create a new local account. Your cattle data will remain."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            application.authService.signOut() // This handles both disconnect and reset local logic
                            showSignOutDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountManagementCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AccountInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}