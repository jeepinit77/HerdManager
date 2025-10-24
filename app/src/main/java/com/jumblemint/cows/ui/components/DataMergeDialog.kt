package com.jumblemint.cows.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class DataMergeOption {
    MERGE_WITH_SERVER,
    REPLACE_SERVER_WITH_DEVICE,
    REPLACE_DEVICE_WITH_SERVER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataMergeDialog(
    onDismiss: () -> Unit,
    onOptionSelected: (DataMergeOption) -> Unit,
    hasLocalData: Boolean = true,
    hasServerData: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Data Sync Options",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (hasLocalData && hasServerData) {
                        "You have data both on this device and in your cloud account. How would you like to handle the data?"
                    } else if (hasLocalData) {
                        "You have data on this device. How would you like to sync it with your cloud account?"
                    } else {
                        "How would you like to sync your cloud data to this device?"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                DataMergeOptionCard(
                    icon = Icons.Default.MergeType,
                    title = "Merge Data",
                    description = "Combine data from both sources. Newer records will take priority.",
                    recommended = hasLocalData && hasServerData,
                    onClick = { onOptionSelected(DataMergeOption.MERGE_WITH_SERVER) }
                )

                if (hasLocalData) {
                    DataMergeOptionCard(
                        icon = Icons.Default.CloudUpload,
                        title = "Upload Device Data",
                        description = "Replace cloud data with data from this device. Cloud data will be lost.",
                        recommended = false,
                        onClick = { onOptionSelected(DataMergeOption.REPLACE_SERVER_WITH_DEVICE) }
                    )
                }

                if (hasServerData) {
                    DataMergeOptionCard(
                        icon = Icons.Default.CloudDownload,
                        title = "Download Cloud Data",
                        description = "Replace device data with cloud data. Device data will be lost.",
                        recommended = !hasLocalData,
                        onClick = { onOptionSelected(DataMergeOption.REPLACE_DEVICE_WITH_SERVER) }
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataMergeOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    recommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (recommended) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Recommended",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
