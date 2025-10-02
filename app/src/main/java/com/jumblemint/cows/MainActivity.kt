package com.jumblemint.cows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.ExperimentalFoundationApi
// import androidx.compose.foundation.layout.Box // No longer needed after Scaffold refactor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Uncommented: Used via Modifier.padding
import androidx.compose.foundation.pager.PagerState 
import androidx.compose.foundation.pager.rememberPagerState 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp // Uncommented: Used via 4.dp
// import androidx.navigation.NavGraph.Companion.findStartDestination // Potentially unused
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.navigation.* 
import com.jumblemint.cows.ui.components.*
import com.jumblemint.cows.ui.theme.CowsTheme
import kotlinx.coroutines.launch 

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

// Helper function to determine if the screen is one of the main tab screens
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
    // val currentUser by app.authService.currentUser.collectAsState(initial = null) // Consider if needed here or just in TopAppBar
    val coroutineScope = rememberCoroutineScope()

    val lastPagerPage = remember { mutableStateOf(DASHBOARD_PAGE_INDEX) }

    val initialPageForPagerState = lastPagerPage.value

    val pagerState = rememberPagerState(
        initialPage = initialPageForPagerState,
        pageCount = { MAIN_SCREEN_PAGE_COUNT }
    )

    LaunchedEffect(currentScreenFromNav, initialPageForPagerState) {
        if (currentScreenFromNav == Screen.MainPager && pagerState.currentPage != initialPageForPagerState) {
            pagerState.scrollToPage(initialPageForPagerState)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        lastPagerPage.value = pagerState.currentPage
    }
    
    val currentScreenForUI = if (currentScreenFromNav == Screen.MainPager) {
        getScreenForPageIndex(pagerState.currentPage)
    } else {
        currentScreenFromNav
    }

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, Icons.Filled.Home, "Home"),
        BottomNavItem(Screen.Cows, Icons.Filled.GroupWork, "Cows"),
        BottomNavItem(Screen.Pastures, Icons.Filled.Landscape, "Fields"),
        BottomNavItem(Screen.Activities, Icons.Filled.Assignment, "Activity"),
        BottomNavItem(Screen.Notes, Icons.Filled.Note, "Notes")
    )

    val showMainTopAppBar = isMainTabScreen(currentScreenForUI)
    // Only show simple top app bar if screen is not null and not a main tab screen
    val showSimpleTopAppBar = !showMainTopAppBar && currentScreenForUI != null 

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showMainTopAppBar) {
                TopAppBarWithMenu(
                    currentScreenForTitle = currentScreenForUI,
                    onNavigateSettings = { navController.navigate(Screen.Settings.route) }, 
                    navController = navController
                )
            } else if (showSimpleTopAppBar) {
                CenterAlignedTopAppBar(
                    title = { Text(currentScreenForUI?.title ?: "Cattle Manager") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (currentScreenForUI == Screen.CowInfo || currentScreenForUI == Screen.PastureDetail) {
                            IconButton(onClick = { navController.popBackStack(Screen.MainPager.route, inclusive = false) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    }
                )
            }
            // Else, no top bar is rendered by Scaffold if this block is empty or returns Unit
        },
        bottomBar = {
            if (currentScreenFromNav == Screen.MainPager) { // Show bottom bar only on main pager screens
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
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
                        selected = currentScreenForUI == Screen.Settings, // Changed to currentScreenForUI
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                // Optional: Configure navigation (e.g., launchSingleTop = true)
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, "Settings") }, // Changed from Icons.Default
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

data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)
