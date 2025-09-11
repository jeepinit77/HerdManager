package com.jumblemint.cows.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
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
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val coroutineScope = rememberCoroutineScope()

    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val syncStatus by application.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    // val lastSyncTime by application.syncService.lastSuccessfulSyncTime.collectAsState(initial = 0L) // Example

    var showSignOutDialog by remember { mutableStateOf(false) }

    val lastSyncTimeText = remember(currentUser?.lastSyncAt, currentUser?.isLocalUser, syncStatus) {
        when {
            currentUser?.isLocalUser == true -> "N/A (Local Account)"
            syncStatus == SyncStatus.SYNCING -> "Syncing..."
            // TODO: Replace with actual formatted date/time from a utility
            currentUser?.lastSyncAt == 0L || currentUser?.lastSyncAt == null -> "Never Synced"
            else -> "Recently" // Placeholder, use formatted application.syncService.lastSuccessfulSyncTime
        }
    }

    Scaffold( // <<< CHANGED Column to Scaffold
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        topBar = {
            TopAppBar(
                title = { Text("Account Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { scaffoldPadding -> // Padding from this Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding) // Apply padding from Scaffold
                .padding(horizontal = 16.dp), // Horizontal padding for content
            contentPadding = PaddingValues(vertical = 16.dp), // Vertical padding for LazyColumn items
            verticalArrangement = Arrangement.spacedBy(20.dp) // Spacing between items/groups
        ) {
            // User Information Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Account",
                            modifier = Modifier.size(56.dp), // Slightly larger icon
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentUser?.displayName ?: currentUser?.email ?: "Unknown User",
                                style = MaterialTheme.typography.titleLarge, // Adjusted style
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (currentUser?.isLocalUser == false) "Cloud Account" else "Local Account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            if (currentUser?.email?.isNotBlank() == true && currentUser?.displayName != currentUser?.email) {
                                 Text(
                                    text = currentUser?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Sync & Data Section (Only if not a local user)
            if (currentUser?.isLocalUser == false) {
                item {
                    SectionTitle("Sync & Data")
                }
                item {
                    AccountManagementCard(
                        title = "Cloud Sync Status",
                        subtitle = when (syncStatus) {
                            SyncStatus.SYNCING -> "Syncing your data..."
                            SyncStatus.SUCCESS -> "Data is up to date. (Last sync: $lastSyncTimeText)"
                            SyncStatus.ERROR -> "Sync error occurred. Tap to retry."
                            SyncStatus.IDLE -> "Tap to sync with Cloud. (Last sync: $lastSyncTimeText)"
                        },
                        icon = when (syncStatus) {
                            SyncStatus.SYNCING -> Icons.Filled.CloudSync
                            SyncStatus.SUCCESS -> Icons.Filled.CloudDone
                            SyncStatus.ERROR -> Icons.Filled.SyncProblem // More specific icon for error
                            SyncStatus.IDLE -> Icons.Filled.Refresh
                        },
                        onClick = {
                            if (syncStatus != SyncStatus.SYNCING) {
                                coroutineScope.launch {
                                    // Trigger sync via AuthService or SyncService
                                    application.authService.startUserSync(application.syncService)
                                }
                            }
                        },
                        isLoading = syncStatus == SyncStatus.SYNCING
                    )
                }
            }

            // Account Actions Section
            item {
                SectionTitle("Account Actions")
            }
            item {
                AccountManagementCard(
                    title = if (currentUser?.isLocalUser == false) "Disconnect Cloud Account" else "Reset Local Account",
                    subtitle = if (currentUser?.isLocalUser == false)
                        "Disconnect cloud. Data stays on device, sync stops."
                    else
                        "Clear local user, data remains. App will restart.", // Updated subtitle
                    icon = if (currentUser?.isLocalUser == false) Icons.Filled.CloudOff else Icons.Filled.DeleteForever, // Changed icon
                    onClick = { showSignOutDialog = true },
                    isDestructive = true // Indicate this is a potentially destructive action
                )
            }

            // Account Information Section
            item {
                SectionTitle("Account Details")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing for info rows
                    ) {
                        AccountInfoRow("User ID", currentUser?.uid?.take(16)?.plus("...") ?: "N/A") // Show partial UID
                        AccountInfoRow("Account Type", if (currentUser?.isLocalUser == true) "Local" else "Cloud")
                        // AccountInfoRow("Created", "TODO: Format creation date") // Placeholder
                        if (currentUser?.isLocalUser == false) {
                            AccountInfoRow("Last Sync Attempt", lastSyncTimeText)
                        }
                    }
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(if (currentUser?.isLocalUser == false) "Disconnect Account?" else "Reset Local Account?") },
            text = {
                Text(
                    if (currentUser?.isLocalUser == false)
                        "Your Cloud account will be disconnected. Your app data will remain on this device for offline use but will no longer sync with the Cloud unless you sign in again."
                    else
                        "This will reset the current local user profile. Your underlying cattle data will NOT be deleted. The app will behave as if it's a fresh install for user settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            application.authService.signOut() // Handles both cases
                            showSignOutDialog = false
                            onNavigateBack() // Navigate back after action
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentUser?.isLocalUser == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (currentUser?.isLocalUser == false) "Disconnect" else "Reset")
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

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp) // Adjusted padding
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountManagementCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isDestructive: Boolean = false
) {
    val cardColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val onCardColor = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title, // More descriptive content description
                modifier = Modifier.size(28.dp), // Slightly larger icon
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = onCardColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onCardColor.copy(alpha = 0.8f),
                    lineHeight = 16.sp // Improved line spacing for subtitle
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = iconColor, strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Action", // More descriptive
                    tint = onCardColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp), // Adjusted padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) // Consistent color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Consistent color
        )
    }
}
