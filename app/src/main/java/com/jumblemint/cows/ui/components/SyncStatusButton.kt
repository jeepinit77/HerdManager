package com.jumblemint.cows.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.sync.ItemSyncStatus
import com.jumblemint.cows.sync.SyncStatus
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusButton(modifier: Modifier = Modifier, onSignInClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication

    val itemStatus by app.syncService.itemSyncStatus.collectAsState(initial = ItemSyncStatus.IDLE)
    val passStatus by app.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    val currentUser by app.authService.currentUser.collectAsState(initial = null)

    // Basic connectivity monitor
    var isOnline by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        fun checkNow(): Boolean {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        isOnline = checkNow()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = true }
            override fun onLost(network: Network) { isOnline = checkNow() }
        }
        cm.registerDefaultNetworkCallback(callback)
    }

    val displayStatus = when {
        currentUser == null || currentUser?.isLocalUser == true -> "NOT_SIGNED_IN"
        !isOnline -> "OFFLINE"
        itemStatus == ItemSyncStatus.SYNCING -> ItemSyncStatus.SYNCING.name
        itemStatus == ItemSyncStatus.ERROR -> ItemSyncStatus.ERROR.name
        passStatus == SyncStatus.SYNCING -> SyncStatus.SYNCING.name
        passStatus == SyncStatus.ERROR -> SyncStatus.ERROR.name
        else -> SyncStatus.SUCCESS.name // treat idle as synced for display
    }

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Pending outbox count (approx): items without firestoreId or lastSyncAt == 0
    val cows by app.repository.getAllCows().collectAsState(initial = emptyList())
    val pastures by app.repository.getAllPastures().collectAsState(initial = emptyList())
    val activities by app.repository.getAllActivities().collectAsState(initial = emptyList())
    val notes by app.repository.getAllNotes().collectAsState(initial = emptyList())
    val pendingCount = remember(cows, pastures, activities, notes) {
        val c = cows.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
        val p = pastures.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
        val a = activities.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
        val n = notes.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
        c + p + a + n
    }

    val (icon, tint) = when (displayStatus) {
        ItemSyncStatus.SYNCING.name, SyncStatus.SYNCING.name -> Pair(Icons.Default.Sync, MaterialTheme.colorScheme.primary)
        ItemSyncStatus.ERROR.name, SyncStatus.ERROR.name -> Pair(Icons.Default.Error, MaterialTheme.colorScheme.error)
        "OFFLINE" -> Pair(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant)
        "NOT_SIGNED_IN" -> Pair(Icons.Default.AccountCircle, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Pair(Icons.Default.CheckCircle, Color(0xFF2E7D32))
    }

    Row(modifier = modifier) {
        IconButton(onClick = {
            // If signed in, trigger a manual sync then open details; if not, just open details
            val notSignedIn = currentUser == null || currentUser?.isLocalUser == true
            if (!notSignedIn) {
                scope.launch { app.syncOrchestrator.runOnce("manual") }
            }
            showSheet = true
        }) {
            BadgedBox(badge = {
                if (pendingCount > 0) {
                    Badge { Text(pendingCount.toString()) }
                }
            }) {
                Icon(imageVector = icon, contentDescription = "Sync status", tint = tint)
            }
            Spacer(Modifier.width(4.dp))
            Text(text = "Sync", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            SyncDetailsSheetContent(
                isOnline = isOnline,
                notSignedIn = currentUser == null || currentUser?.isLocalUser == true,
                onClose = { showSheet = false },
                onSignInClick = onSignInClick
            )
        }
    }
}

@Composable
fun SyncDetailsSheetContent(isOnline: Boolean, notSignedIn: Boolean, onClose: () -> Unit, onSignInClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication

    val itemStatus by app.syncService.itemSyncStatus.collectAsState(initial = ItemSyncStatus.IDLE)
    val passStatus by app.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)

    val scope = rememberCoroutineScope()
    val currentUser by app.authService.currentUser.collectAsState(initial = null)

    Column(Modifier) {
        Text(
            text = "Sync",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))

        if (notSignedIn) {
            // When not signed in, don't show sync statuses/details
            Text(
                text = "You are not signed in. Sign in to enable cloud sync.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(12.dp))
            Row {
                Button(onClick = { onClose(); onSignInClick?.invoke() }) {
                    Text("Sign in")
                }
            }
        } else {
            // Status row and online info
            Row {
                Icon(
                    imageVector = when {
                        itemStatus == ItemSyncStatus.SYNCING || passStatus == SyncStatus.SYNCING -> Icons.Default.Sync
                        itemStatus == ItemSyncStatus.ERROR || passStatus == SyncStatus.ERROR -> Icons.Default.Error
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        itemStatus == ItemSyncStatus.SYNCING || passStatus == SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                        itemStatus == ItemSyncStatus.ERROR || passStatus == SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> Color(0xFF2E7D32)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Overall: ${'$'}passStatus  |  Items: ${'$'}itemStatus",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (!isOnline) "You are offline. Changes will sync when connection is back." else "Online",
                style = MaterialTheme.typography.bodyMedium
            )
            currentUser?.let { user ->
                Spacer(Modifier.width(4.dp))
                Text(text = "Signed in as: ${'$'}{user.displayName ?: user.email}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.width(12.dp))
            Row {
                Button(onClick = { scope.launch { app.syncOrchestrator.runOnce("details_sheet") } }) {
                    Text("Sync now")
                }
            }
        }
    }
}
