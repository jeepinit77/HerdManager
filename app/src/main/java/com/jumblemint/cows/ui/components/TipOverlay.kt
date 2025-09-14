package com.jumblemint.cows.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties // Added import for DialogProperties
import kotlinx.coroutines.launch
import com.jumblemint.cows.data.preferences.TipsManager

@Composable
fun TipOverlay(
    tipId: String,
    tipText: String,
    onClosed: () -> Unit,
    tipsManager: TipsManager
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { onClosed() },
        properties = DialogProperties(usePlatformDefaultWidth = false), // Added to control width
        modifier = Modifier.fillMaxWidth(), // Allows dialog to use full screen width available to it
        icon = null,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WobblingLightbulbIcon()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tip",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onClosed() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close tip")
                }
            }
        },
        text = { Text(tipText, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp), // Padding for the button area
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onClosed()
                        },
                        modifier = Modifier
                            .weight(1f) 
                            .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
                    ) {
                        Text("Keep This Tip", textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                tipsManager.dismissTip(tipId)
                                onClosed()
                            }
                        },
                        modifier = Modifier
                            .weight(1f) 
                            .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
                    ) {
                        Text("Hide This Tip", textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            tipsManager.hideAllTips()
                            onClosed()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth() 
                        .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
                ) {
                    Text("Hide All Tips", textAlign = TextAlign.Center)
                }
            }
        },
        dismissButton = { }
    )
}
