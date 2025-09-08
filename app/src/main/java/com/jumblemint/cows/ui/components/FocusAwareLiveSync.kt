package com.jumblemint.cows.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.jumblemint.cows.sync.SyncOrchestrator

@Composable
fun FocusAwareLiveSync(
    orchestrator: SyncOrchestrator,
    screenKey: String,
    intervalMs: Long = 30_000L,
    leadingRun: Boolean = true,
    networkAvailable: suspend () -> Boolean = { true }
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, screenKey, intervalMs, leadingRun) {
        orchestrator.startLiveForLifecycle(
            screenKey = screenKey,
            lifecycle = lifecycleOwner.lifecycle,
            scope = scope,
            intervalMs = intervalMs,
            leadingRun = leadingRun,
            networkAvailable = networkAvailable
        )
        onDispose { orchestrator.stopLive(screenKey) }
    }
}
