package com.jumblemint.cows.ui.components
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class TopAppBarActions(
    val onEdit: (() -> Unit)? = null,
    val onSave: (() -> Unit)? = null,
    val onClose: (() -> Unit)? = null,
    val saveEnabled: Boolean = true,
    val customActions: @Composable () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: TopAppBarActions = TopAppBarActions()
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            actions.onEdit?.let { editAction ->
                IconButton(onClick = editAction) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
            actions.onSave?.let { saveAction ->
                IconButton(
                    onClick = saveAction,
                    enabled = actions.saveEnabled
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save")
                }
            }
            actions.onClose?.let { closeAction ->
                IconButton(onClick = closeAction) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            actions.customActions()
        }
    )
}

// Legacy function for backward compatibility
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    SimpleTopAppBar(
        title = title,
        onBack = onBack,
        actions = TopAppBarActions(
            onEdit = onEdit,
            onSave = onSave,
            onClose = onClose,
            customActions = actions
        )
    )
}