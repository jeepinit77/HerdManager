package com.jumblemint.cows.ui.components

// import androidx.compose.material.icons.Icons // Not strictly needed if Lightbulb is removed
// import androidx.compose.material.icons.filled.Lightbulb // Removed
import androidx.compose.material3.AlertDialog
// import androidx.compose.material3.Divider // Not used in the provided snippet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
// import androidx.compose.ui.graphics.vector.ImageVector // Removed
import kotlinx.coroutines.launch
import com.jumblemint.cows.data.preferences.TipsManager
import com.jumblemint.cows.ui.components.PulsingLightbulbIcon // Added import

@Composable
fun TipOverlay(
    tipId: String,
    tipText: String,
    onClosed: () -> Unit,
    tipsManager: TipsManager
    // Removed: icon: ImageVector = Icons.Filled.Lightbulb
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { onClosed() },
        icon = { PulsingLightbulbIcon() }, // Changed to use PulsingLightbulbIcon
        title = { Text("Tip") },
        text = { Text(tipText) },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    tipsManager.dismissTip(tipId)
                    onClosed()
                }
            }) { Text("Got it") }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch {
                    tipsManager.hideAllTips()
                    onClosed()
                }
            }) { Text("Hide all tips") }
        }
    )
}