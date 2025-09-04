package com.jumblemint.cows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.navigation.CattleNavigation
import com.jumblemint.cows.navigation.Screen
import com.jumblemint.cows.ui.components.SyncIndicator
import com.jumblemint.cows.ui.theme.CowsTheme

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
fun CattleManagerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Cows, Icons.Default.GroupWork, "Cows"),
        BottomNavItem(Screen.Pastures, Icons.Default.Landscape, "Fields"),
        BottomNavItem(Screen.Activities, Icons.Default.Assignment, "Activity"),
        BottomNavItem(Screen.Notes, Icons.Default.Note, "Notes"),
        BottomNavItem(Screen.Settings, Icons.Default.Settings, "Settings")
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Only show bottom navigation on main screens
                val currentRoute = currentDestination?.route
                val shouldShowBottomNav = bottomNavItems.any { item ->
                    currentRoute == item.screen.route || 
                    (item.screen == Screen.Cows && currentRoute?.startsWith("${Screen.Cows.route}?") == true)
                }
                if (shouldShowBottomNav) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route || 
                                          (item.screen == Screen.Cows && currentRoute?.startsWith("${Screen.Cows.route}?") == true),
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            CattleNavigation(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
        
        // Floating sync indicator at the top
        SyncIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
    }
}

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)