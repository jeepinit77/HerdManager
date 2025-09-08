package com.jumblemint.cows.sync

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jumblemint.cows.auth.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SyncOrchestrator(
    private val syncService: SyncService,
    private val authService: AuthService,
    private val logger: (String) -> Unit = {}
) {
    private val liveJobs = mutableMapOf<String, Job>()

    suspend fun runOnce(reason: String = "manual_or_focus") {
        logger("runOnce: $reason")
        val user = authService.currentUser.first() ?: return
        if (user.isLocalUser) {
            // Skip syncing for local users (no remote)
            logger("runOnce skipped: local user")
            return
        }
        syncService.syncUserData(user.uid)
    }

    fun startLiveForLifecycle(
        screenKey: String,
        lifecycle: Lifecycle,
        scope: CoroutineScope,
        intervalMs: Long = 30_000L,
        leadingRun: Boolean = true,
        networkAvailable: suspend () -> Boolean = { true }
    ) {
        if (liveJobs[screenKey]?.isActive == true) return

        val job = scope.launch(Dispatchers.Main.immediate) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (leadingRun && networkAvailable()) {
                    try { runOnce("focus_leading") } catch (_: Exception) {}
                }
                while (currentCoroutineContext().isActive) {
                    delay(intervalMs)
                    if (networkAvailable()) {
                        try { runOnce("focus_poll") } catch (_: Exception) {}
                    }
                }
            }
        }
        liveJobs[screenKey] = job
    }

    fun stopLive(screenKey: String) {
        liveJobs.remove(screenKey)?.cancel()
    }
}
