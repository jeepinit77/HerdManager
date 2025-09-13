package com.jumblemint.cows.ui.components

import androidx.compose.runtime.*

@Stable
class TopAppBarController {
    var title by mutableStateOf("")
        private set
    
    var actions by mutableStateOf(TopAppBarActions())
        private set
    
    fun updateTitle(newTitle: String) {
        title = newTitle
    }
    
    fun updateActions(newActions: TopAppBarActions) {
        actions = newActions
    }
    
    fun reset() {
        title = ""
        actions = TopAppBarActions()
    }
}

@Composable
fun rememberTopAppBarController(): TopAppBarController {
    return remember { TopAppBarController() }
}