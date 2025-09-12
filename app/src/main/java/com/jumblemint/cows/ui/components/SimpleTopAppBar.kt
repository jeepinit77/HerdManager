package com.jumblemint.cows.ui.components
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            onEdit?.let { editAction ->
                IconButton(onClick = editAction) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
            onClose?.let { closeAction ->
                IconButton(onClick = closeAction) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            actions()
        }
    )
}