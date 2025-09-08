package com.jumblemint.cows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues 
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
// NavDestination is no longer directly needed by TopAppBarWithMenu if we pass Screen
// import androidx.navigation.NavDestination
// import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.navigation.CattleNavigation
import com.jumblemint.cows.navigation.Screen // Import our Screen sealed class
import com.jumblemint.cows.ui.theme.CowsTheme
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getString(R.string.default_web_client_id) // Keep this if used elsewhere

        setContent {
            CowsTheme {
                CattleManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithMenu(currentScreen: Screen?, onNavigateSettings: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    // Simplified title logic using Screen object's title property
    val title = currentScreen?.title ?: "Cattle Manager"

    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Menu", tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = { showMenu = false; onNavigateSettings() },
                    leadingIcon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface) }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleManagerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoutePattern = navBackStackEntry?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoutePattern)

    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication
    val currentUser by app.authService.currentUser.collectAsState(initial = null)

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Cows, Icons.Default.GroupWork, "Cows"),
        BottomNavItem(Screen.Pastures, Icons.Default.Landscape, "Fields"),
        BottomNavItem(Screen.Activities, Icons.Default.Assignment, "Activity"),
        BottomNavItem(Screen.Notes, Icons.Default.Note, "Notes")
    )

    // Determine if the main TopAppBar should be shown
    var showMainTopAppBar = currentScreen?.hasOwnTopAppBar != true

    // Special handling for PastureDetail route based on arguments
    if (currentScreen == Screen.PastureDetail) {
        val pastureId = navBackStackEntry?.arguments?.getString("pastureId")
        if (pastureId == "0") { // This is AddPastureScreen, which has its own TopAppBar
            showMainTopAppBar = false
        } else { // This is viewing PastureDetailScreen, which should use the main TopAppBar
            showMainTopAppBar = true 
        }
    }
    // Also ensure Login and Sign In screens don't show the main top app bar, if not already covered by hasOwnTopAppBar
    if (currentScreen == Screen.Login || currentScreen == Screen.SignIn) {
        showMainTopAppBar = false
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showMainTopAppBar) {
                    TopAppBarWithMenu(currentScreen) { navController.navigate(Screen.Settings.route) }
                }
            },
            bottomBar = {
                val currentRoute = navBackStackEntry?.destination?.route // Use pattern for consistency
                val shouldShowBottomNav = bottomNavItems.any { item ->
                    // Check if currentRoute matches item.screen.route or starts with item.screen.route + "/" for argument routes
                    currentRoute == item.screen.route || currentRoute?.startsWith(item.screen.route + "/") == true ||
                    (item.screen == Screen.Cows && currentRoute?.startsWith("${Screen.Cows.route}?") == true) // Specific for ?param style
                }
                if (shouldShowBottomNav) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route || 
                                           currentRoute?.startsWith(item.screen.route + "/") == true ||
                                           (item.screen == Screen.Cows && currentRoute?.startsWith("${Screen.Cows.route}?") == true),
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        NavigationBarItem(
                            selected = currentScreen == Screen.Sync || currentScreen == Screen.SignIn,
                            onClick = {
                                if (currentUser == null || currentUser?.isLocalUser == true) {
                                    navController.navigate(Screen.SignIn.route) { launchSingleTop = true }
                                } else {
                                    navController.navigate(Screen.Sync.route) { launchSingleTop = true }
                                }
                            },
                            icon = { com.jumblemint.cows.ui.components.SyncStatusNavIcon() },
                            label = { Text("Sync") }
                        )
                    }
                }
            }
        ) { innerPadding -> 
            CattleNavigation(
                navController = navController,
                mainScaffoldPadding = innerPadding 
            )
        }
    }
}

data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)
