package com.jumblemint.cows.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.flow.collectLatest
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.sync.ItemSyncStatus
import com.jumblemint.cows.sync.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusNavIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication
    val itemStatus by app.syncService.itemSyncStatus.collectAsState(initial = ItemSyncStatus.IDLE)
    val passStatus by app.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    val currentUser by app.authService.currentUser.collectAsState(initial = null)

    // Connectivity
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

    // Pending outbox count (approx): items without firestoreId or lastSyncAt == 0
    val cows by app.repository.getAllCows().collectAsState(initial = emptyList())
    val pastures by app.repository.getAllPastures().collectAsState(initial = emptyList())
    val activities by app.repository.getAllActivities().collectAsState(initial = emptyList())
    val notes by app.repository.getAllNotes().collectAsState(initial = emptyList())

    val pendingCount by remember(cows, pastures, activities, notes) {
        derivedStateOf {
            val c = cows.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
            val p = pastures.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
            val a = activities.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
            val n = notes.count { it.firestoreId.isNullOrEmpty() || it.lastSyncAt == 0L }
            c + p + a + n
        }
    }

    val isSyncing = itemStatus == ItemSyncStatus.SYNCING || passStatus == SyncStatus.SYNCING

    val rotation = if (isSyncing) {
        val transition = rememberInfiniteTransition(label = "sync-rotate")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        ).value
    } else 0f

    val icon = when {
        !isOnline && (currentUser != null && currentUser?.isLocalUser != true) -> Icons.Default.CloudOff
        isSyncing -> Icons.Default.Sync
        itemStatus == ItemSyncStatus.ERROR || passStatus == SyncStatus.ERROR -> Icons.Default.Error
        currentUser == null || currentUser?.isLocalUser == true -> Icons.Default.AccountCircle
        else -> Icons.Default.CheckCircle
    }
    val tint = when {
        isSyncing -> Color(0xFF1976D2) // blue during sync
        else -> Color.Unspecified
    }

    BadgedBox(badge = {
        // Show badge only if user is signed in (not local) and there's a pending count
        if (pendingCount > 0 && currentUser != null && currentUser?.isLocalUser == false) {
            Badge { androidx.compose.material3.Text(pendingCount.toString()) }
        }
    }) {
        Icon(imageVector = icon, contentDescription = "Sync", modifier = modifier.rotate(rotation), tint = tint)
    }
}
