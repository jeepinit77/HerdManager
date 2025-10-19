package com.jumblemint.cows.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope

class GlobalSnackbarState(
    val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        // Dismiss any currently showing snackbar to prevent stacking
        snackbarHostState.currentSnackbarData?.dismiss()
        
        return snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
    }
    
    val isSnackbarVisible: Boolean
        get() = snackbarHostState.currentSnackbarData != null
}

val LocalGlobalSnackbarState = staticCompositionLocalOf<GlobalSnackbarState?> { null }

@Composable
fun rememberGlobalSnackbarState(scope: CoroutineScope): GlobalSnackbarState {
    val snackbarHostState = remember { SnackbarHostState() }
    return remember(scope) { GlobalSnackbarState(snackbarHostState, scope) }
}
