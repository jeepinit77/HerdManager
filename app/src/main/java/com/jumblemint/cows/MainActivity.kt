package com.jumblemint.cows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues // Added for type hint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Keep for Modifier.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.navigation.CattleNavigation
import com.jumblemint.cows.navigation.Screen
import com.jumblemint.cows.ui.theme.CowsTheme
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getString(R.string.default_web_client_id)

        setContent {
            CowsTheme {
                CattleManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithMenu(currentDestination: NavDestination?, onNavigateSettings: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val title = when (val route = currentDestination?.route) {
        Screen.Dashboard.route -> Screen.Dashboard.title
        Screen.Cows.route -> Screen.Cows.title
        Screen.Pastures.route -> Screen.Pastures.title
        Screen.Activities.route -> Screen.Activities.title
        Screen.Notes.route -> Screen.Notes.title
        else -> {
            when {
                route?.startsWith(Screen.CowInfo.route) == true -> Screen.CowInfo.title
                route?.startsWith(Screen.CowDetail.route) == true -> Screen.CowDetail.title
                route?.startsWith(Screen.PastureDetail.route) == true -> Screen.PastureDetail.title
                route?.startsWith(Screen.CowList.route) == true -> Screen.CowList.title
                route?.startsWith(Screen.Cows.route) == true -> Screen.Cows.title // Catch-all for cows?param=...
                else -> route?.substringBefore("/")
                            ?.replace("_", " ")
                            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            ?: "Cattle Manager"
            }
        }
    }
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
    val currentDestination = navBackStackEntry?.destination
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

    val currentRoute = currentDestination?.route
    // More robust check for screens providing their own TopAppBar
    val screenProvidesOwnTopAppBar = when {
        currentRoute?.startsWith(Screen.CowDetail.route) == true -> true
        currentRoute?.startsWith(Screen.AddActivity.route) == true -> true
        // AddPasture is PastureDetail with ID 0
        currentRoute?.startsWith(Screen.PastureDetail.route) == true && currentRoute.endsWith("/0") -> true
        currentRoute == Screen.AddBirth.route -> true
        currentRoute == Screen.Sync.route -> true
        currentRoute == Screen.AccountManagement.route -> true
        currentRoute == Screen.TagColorsManagement.route -> true
        currentRoute == Screen.ActivityTypesManagement.route -> true
        // Login/SignIn screens also don't use the main TopAppBar
        currentRoute == Screen.Login.route -> true
        currentRoute == Screen.SignIn.route -> true
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (!screenProvidesOwnTopAppBar) {
                    TopAppBarWithMenu(currentDestination) { navController.navigate(Screen.Settings.route) }
                }
            },
            bottomBar = {
                val shouldShowBottomNav = bottomNavItems.any { item ->
                    currentRoute == item.screen.route ||
                    (item.screen == Screen.Cows && currentRoute?.startsWith("${Screen.Cows.route}?") == true)
                }
                if (shouldShowBottomNav) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route ||
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
                            selected = currentRoute == Screen.Sync.route || currentRoute == Screen.SignIn.route,
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
        ) { innerPadding -> // These are PaddingValues from MainActivity's Scaffold
            CattleNavigation(
                navController = navController,
                mainScaffoldPadding = innerPadding // Pass PaddingValues directly
            )
        }
    }
}

data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)
