package com.jumblemint.cows.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.sync.SyncStatus
import com.jumblemint.cows.ui.components.FocusAwareLiveSync
import android.text.format.DateUtils
// Consider adding a proper date/time formatting utility if more detailed time is needed

private fun formatRelativeSyncDescription(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "Never"
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

@Composable
fun AccountManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val coroutineScope = rememberCoroutineScope()

    val currentUser by application.authService.currentUser.collectAsState(initial = null)
    val syncStatus by application.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    val lastSyncTimestamp by application.syncService.lastSyncTime.collectAsState(initial = null)

    FocusAwareLiveSync(
        orchestrator = application.syncOrchestrator,
        screenKey = "AccountManagement",
        intervalMs = 20_000L,
        leadingRun = true
    )

    var showSignOutDialog by remember { mutableStateOf(false) }

    val lastSyncTimeText = remember(currentUser?.lastSyncAt, currentUser?.isLocalUser, syncStatus, lastSyncTimestamp) {
        when {
            currentUser?.isLocalUser == true -> "Not applicable"
            syncStatus == SyncStatus.SYNCING -> "Syncing..."
            else -> {
                val effectiveTimestamp = lastSyncTimestamp ?: currentUser?.lastSyncAt
                formatRelativeSyncDescription(effectiveTimestamp)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
            // User Information Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentUser?.displayName ?: currentUser?.email ?: "Unknown User",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (currentUser?.isLocalUser == false) "Cloud Account" else "Local Account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentUser?.email?.isNotBlank() == true && currentUser?.displayName != currentUser?.email) {
                                 Text(
                                    text = currentUser?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            SyncStatus.SYNCING -> "Syncing your data with cloud..."
                            SyncStatus.SUCCESS -> "Data is synchronized. Last sync: $lastSyncTimeText"
                            SyncStatus.ERROR -> "Sync failed. Tap to try again."
                            SyncStatus.IDLE -> "Tap to sync with cloud. Last sync: $lastSyncTimeText"
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
                        "Disconnect from cloud. Your data stays on this device but sync will stop."
                    else
                        "Reset user profile. Your cattle data will remain safe.",
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

    if (showSignOutDialog) {
        com.jumblemint.cows.ui.components.AppAlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(if (currentUser?.isLocalUser == false) "Disconnect Account?" else "Reset Local Account?") },
            text = {
                Text(
                    if (currentUser?.isLocalUser == false)
                        "Your cloud account will be disconnected. All cattle data will remain on this device for offline use, but will no longer sync with the cloud unless you sign in again."
                    else
                        "This will reset your user profile. Your cattle data will remain safe and unchanged. Only user settings will be reset."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            application.authService.signOut()
                            showSignOutDialog = false
                            onNavigateBack()
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
                FilledTonalButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
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
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
                    color = onCardColor,
                    lineHeight = 16.sp
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = iconColor, strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Action",
                    tint = onCardColor
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
