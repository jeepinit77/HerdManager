package com.jumblemint.cows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState // New Import
import androidx.compose.foundation.pager.rememberPagerState // New Import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.navigation.* // Import all from navigation for constants and Screen
import com.jumblemint.cows.ui.theme.CowsTheme
import kotlinx.coroutines.launch // New Import

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CowsTheme {
                CattleManagerApp()
            }
        }
    }
}

// Helper function to get page index for a screen (must match Pager content)
fun getPageIndexForScreen(screen: Screen): Int = when (screen) {
    Screen.Dashboard -> DASHBOARD_PAGE_INDEX
    Screen.Cows -> COWS_PAGE_INDEX
    Screen.Pastures -> PASTURES_PAGE_INDEX
    Screen.Activities -> ACTIVITIES_PAGE_INDEX
    Screen.Notes -> NOTES_PAGE_INDEX
    else -> -1 // Not a main pager screen
}

// Helper function to get screen for a page index
fun getScreenForPageIndex(index: Int): Screen? = when (index) {
    DASHBOARD_PAGE_INDEX -> Screen.Dashboard
    COWS_PAGE_INDEX -> Screen.Cows
    PASTURES_PAGE_INDEX -> Screen.Pastures
    ACTIVITIES_PAGE_INDEX -> Screen.Activities
    NOTES_PAGE_INDEX -> Screen.Notes
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithMenu(currentScreenForTitle: Screen?, onNavigateSettings: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val title = currentScreenForTitle?.title ?: "Cattle Manager"

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CattleManagerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteFromNav = navBackStackEntry?.destination?.route
    val currentScreenFromNav = Screen.fromRoute(currentRouteFromNav)

    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication
    val currentUser by app.authService.currentUser.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val initialPageForPagerState = if (currentScreenFromNav == Screen.MainPager) {
        navBackStackEntry?.arguments?.getInt("initialPage", DASHBOARD_PAGE_INDEX) ?: DASHBOARD_PAGE_INDEX
    } else {
        DASHBOARD_PAGE_INDEX 
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageForPagerState,
        pageCount = { MAIN_SCREEN_PAGE_COUNT }
    )

    LaunchedEffect(currentScreenFromNav, initialPageForPagerState) {
        if (currentScreenFromNav == Screen.MainPager && pagerState.currentPage != initialPageForPagerState) {
            pagerState.scrollToPage(initialPageForPagerState)
        }
    }
    
    val currentScreenForUI = if (currentScreenFromNav == Screen.MainPager) {
        getScreenForPageIndex(pagerState.currentPage)
    } else {
        currentScreenFromNav
    }

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Cows, Icons.Default.GroupWork, "Cows"),
        BottomNavItem(Screen.Pastures, Icons.Default.Landscape, "Fields"),
        BottomNavItem(Screen.Activities, Icons.Default.Assignment, "Activity"),
        BottomNavItem(Screen.Notes, Icons.Default.Note, "Notes")
    )

    var showMainTopAppBar = currentScreenForUI?.hasOwnTopAppBar != true
    if (currentScreenFromNav == Screen.PastureDetail) {
        val pastureId = navBackStackEntry?.arguments?.getString("pastureId")
        showMainTopAppBar = pastureId != "0"
    }
    if (currentScreenFromNav == Screen.SignIn) {
        showMainTopAppBar = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showMainTopAppBar) {
                    TopAppBarWithMenu(currentScreenForUI) { navController.navigate(Screen.Settings.route) }
                }
            },
            bottomBar = {
                if (currentScreenFromNav == Screen.MainPager) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val pageIndex = getPageIndexForScreen(item.screen)
                            NavigationBarItem(
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) },
                                selected = pagerState.currentPage == pageIndex,
                                onClick = {
                                    if (pageIndex != -1) { // item.screen is one of the pager screens
                                        // Since this bottomBar is shown only when currentScreenFromNav == Screen.MainPager,
                                        // we are on the MainPager. Just scroll the pagerState.
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(pageIndex)
                                        }
                                    }
                                }
                            )
                        }
                        NavigationBarItem(
                            selected = currentScreenFromNav == Screen.Sync || currentScreenFromNav == Screen.SignIn,
                            onClick = {
                                val targetRoute = if (currentUser == null || currentUser?.isLocalUser == true) {
                                    Screen.SignIn.route
                                } else {
                                    Screen.Sync.route 
                                }
                                navController.navigate(targetRoute) {
                                     popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true 
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
                mainScaffoldPadding = innerPadding,
                pagerState = pagerState 
            )
        }
    }
}

data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)
