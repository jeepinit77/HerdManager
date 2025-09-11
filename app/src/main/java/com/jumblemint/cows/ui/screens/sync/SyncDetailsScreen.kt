package com.jumblemint.cows.ui.screens.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Ensure Modifier is imported
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.R
import com.jumblemint.cows.ui.components.SyncDetailsSheetContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.sync.ItemSyncStatus
import com.jumblemint.cows.sync.SyncStatus
import androidx.compose.runtime.rememberCoroutineScope // <<< ADDED IMPORT
import kotlinx.coroutines.launch // <<< ADDED IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDetailsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier // <<< ADDED MODIFIER PARAMETER
) {
    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication
    val scope = rememberCoroutineScope() // <<< ADDED SCOPE

    val itemStatus by app.syncService.itemSyncStatus.collectAsState(initial = ItemSyncStatus.IDLE)
    val passStatus by app.syncService.syncStatus.collectAsState(initial = SyncStatus.IDLE)
    val currentUser by app.authService.currentUser.collectAsState(initial = null)

    // Kick off a manual sync when this screen is shown if user is signed in
    LaunchedEffect(currentUser) {
        if (currentUser != null && !currentUser!!.isLocalUser) {
            // This runOnce is already in a LaunchedEffect, which provides a CoroutineScope
            app.syncOrchestrator.runOnce("sync_screen_manual_trigger")
        }
    }

    Scaffold(
        modifier = modifier, // <<< APPLIED MODIFIER HERE
        topBar = {
            TopAppBar(
                title = { Text("Sync Status & Details") }, // More descriptive title
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Action to manually trigger sync if user is signed in
                    if (currentUser != null && !currentUser!!.isLocalUser && passStatus != SyncStatus.SYNCING) {
                        IconButton(onClick = { 
                            scope.launch { // <<< WRAPPED IN SCOPE.LAUNCH
                                app.syncOrchestrator.runOnce("sync_screen_manual_retry") 
                            }
                        }) {
                            Icon(Icons.Default.Sync, contentDescription = "Retry Sync")
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Consider if this is still needed or handled by Scaffold
    ) { paddingValues: PaddingValues -> // Renamed for clarity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Use the padding from Scaffold
            contentAlignment = Alignment.TopStart // Content usually starts at the top
        ) {
            // isOnline could be a state collected from a connectivity service
            // For now, assuming SyncDetailsSheetContent handles its own online status display if necessary.
            val isOnline = true 
            val notSignedIn = currentUser == null || currentUser!!.isLocalUser

            SyncDetailsSheetContent( // This component seems to handle different states internally
                isOnline = isOnline, // Pass current online status
                notSignedIn = notSignedIn,
                onClose = onNavigateBack, // onClose might be redundant if back navigation is handled by TopAppBar
                onSignInClick = { onNavigateToSignIn?.invoke() } // Ensure null safety
            )
        }
    }
}
