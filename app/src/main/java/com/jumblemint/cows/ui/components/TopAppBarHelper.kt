package com.jumblemint.cows.ui.components

import androidx.compose.runtime.Composable
import com.jumblemint.cows.navigation.Screen

object TopAppBarHelper {
    fun getActionsForScreen(
        screen: Screen?,
        onEdit: (() -> Unit)? = null,
        onSave: (() -> Unit)? = null,
        onClose: (() -> Unit)? = null,
        saveEnabled: Boolean = true,
        customActions: @Composable () -> Unit = {}
    ): TopAppBarActions {
        return when (screen) {
            Screen.CowInfo -> TopAppBarActions(
                onEdit = onEdit,
                onClose = onClose,
                customActions = customActions
            )
            Screen.CowDetail -> TopAppBarActions(
                onSave = onSave,
                saveEnabled = saveEnabled,
                customActions = customActions
            )
            Screen.PastureDetail -> TopAppBarActions(
                onEdit = onEdit,
                customActions = customActions
            )
            Screen.AddActivity, Screen.AddActivityWithId -> TopAppBarActions(
                onSave = onSave,
                saveEnabled = saveEnabled,
                customActions = customActions
            )
            Screen.AddPasture, Screen.EditPasture -> TopAppBarActions(
                onSave = onSave,
                saveEnabled = saveEnabled,
                customActions = customActions
            )
            Screen.AddBirth -> TopAppBarActions(
                onSave = onSave,
                saveEnabled = saveEnabled,
                customActions = customActions
            )
            else -> TopAppBarActions(
                onEdit = onEdit,
                onSave = onSave,
                onClose = onClose,
                saveEnabled = saveEnabled,
                customActions = customActions
            )
        }
    }
}