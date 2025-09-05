package com.jumblemint.cows.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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
    Dialog(onDismissRequest = onDismiss) {
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
                // Title
                Text(
                    text = "Data Sync Options",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description
                Text(
                    text = if (hasLocalData && hasServerData) {
                        "You have data both on this device and in your cloud account. How would you like to handle the data?"
                    } else if (hasLocalData) {
                        "You have data on this device. How would you like to sync it with your cloud account?"
                    } else {
                        "How would you like to sync your cloud data to this device?"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Divider()
                
                // Option 1: Merge data
                DataMergeOptionCard(
                    icon = Icons.Default.MergeType,
                    title = "Merge Data",
                    description = "Combine data from both sources. Newer records will take priority.",
                    recommended = hasLocalData && hasServerData,
                    onClick = { onOptionSelected(DataMergeOption.MERGE_WITH_SERVER) }
                )
                
                // Option 2: Replace server with device data
                if (hasLocalData) {
                    DataMergeOptionCard(
                        icon = Icons.Default.CloudUpload,
                        title = "Upload Device Data",
                        description = "Replace cloud data with data from this device. Cloud data will be lost.",
                        recommended = false,
                        onClick = { onOptionSelected(DataMergeOption.REPLACE_SERVER_WITH_DEVICE) }
                    )
                }
                
                // Option 3: Replace device with server data
                if (hasServerData) {
                    DataMergeOptionCard(
                        icon = Icons.Default.CloudDownload,
                        title = "Download Cloud Data",
                        description = "Replace device data with cloud data. Device data will be lost.",
                        recommended = !hasLocalData,
                        onClick = { onOptionSelected(DataMergeOption.REPLACE_DEVICE_WITH_SERVER) }
                    )
                }
                
                // Cancel button
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
            containerColor = if (recommended) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (recommended) {
            BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (recommended) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (recommended) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    if (recommended) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = "Recommended",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (recommended) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    lineHeight = 16.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (recommended) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}