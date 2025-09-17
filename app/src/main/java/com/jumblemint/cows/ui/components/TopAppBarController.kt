package com.jumblemint.cows.ui.components

import androidx.compose.runtime.*

@Stable
class TopAppBarController {
    var title by mutableStateOf("")
        private set

    // When true, title/actions should override default Screen title/actions in SimpleTopAppBar
    var overrideActive by mutableStateOf(false)
        private set
    
    var actions by mutableStateOf(TopAppBarActions())
        private set
    
    fun updateTitle(newTitle: String) {
        title = newTitle
        overrideActive = true
    }
    
    fun updateActions(newActions: TopAppBarActions) {
        actions = newActions
        // Do not toggle overrideActive here; title decides if custom title is active
    }
    
    fun reset() {
        title = ""
        actions = TopAppBarActions()
        overrideActive = false
    }
}

@Composable
fun rememberTopAppBarController(): TopAppBarController {
    return remember { TopAppBarController() }
}