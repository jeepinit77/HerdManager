package com.jumblemint.cows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState // New Import
import androidx.compose.foundation.pager.rememberPagerState // New Import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.navigation.* // Import all from navigation for constants and Screen
import com.jumblemint.cows.ui.components.SimpleTopAppBar
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

// New helper function to determine if the screen is one of the main tab screens
fun isMainTabScreen(screen: Screen?): Boolean {
    return screen == Screen.Dashboard ||
           screen == Screen.Cows ||
           screen == Screen.Pastures ||
           screen == Screen.Activities ||
           screen == Screen.Notes
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithMenu(currentScreenForTitle: Screen?, onNavigateSettings: () -> Unit, navController: androidx.navigation.NavController) {
    val title = currentScreenForTitle?.title ?: "Cattle Manager"
    val context = LocalContext.current
    val app = context.applicationContext as CattleApplication
    val currentUser by app.authService.currentUser.collectAsState(initial = null)

    TopAppBar(
        title = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val targetRoute = if (currentUser == null || currentUser?.isLocalUser == true) {
                            Screen.SignIn.route
                        } else {
                            Screen.Sync.route
                        }
                        navController.navigate(targetRoute)
                    },
                    modifier = androidx.compose.ui.Modifier.padding(end = 4.dp)
                ) {
                    com.jumblemint.cows.ui.components.SyncStatusNavIcon()
                }
                Text(title)
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

    // Standardized logic: Only show TopAppBarWithMenu for main tabs
    val showMainTopAppBar = isMainTabScreen(currentScreenForUI)
    val showSimpleTopAppBar = !showMainTopAppBar && (currentScreenForUI?.hasOwnTopAppBar != true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (showMainTopAppBar || showSimpleTopAppBar) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (showMainTopAppBar) {
                        TopAppBarWithMenu(currentScreenForUI, { navController.navigate(Screen.Settings.route) }, navController)
                    } else if (showSimpleTopAppBar) {
                        SimpleTopAppBar(title = currentScreenForUI?.title ?: "Cattle Manager", onBack = { navController.popBackStack() })
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
                                        if (pageIndex != -1) {
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(pageIndex)
                                            }
                                        }
                                    }
                                )
                            }
                            NavigationBarItem(
                                selected = currentScreenFromNav == Screen.Settings,
                                onClick = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                icon = { Icon(Icons.Default.Settings, "Settings") },
                                label = { Text("Settings") }
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
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
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
                                        if (pageIndex != -1) {
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(pageIndex)
                                            }
                                        }
                                    }
                                )
                            }
                            NavigationBarItem(
                                selected = currentScreenFromNav == Screen.Settings,
                                onClick = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                icon = { Icon(Icons.Default.Settings, "Settings") },
                                label = { Text("Settings") }
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
}

data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)
