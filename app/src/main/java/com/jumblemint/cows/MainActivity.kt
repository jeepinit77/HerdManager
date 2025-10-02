package com.jumblemint.cows

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.navigation.*
import com.jumblemint.cows.ui.theme.CowsTheme
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CattleManagerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteFromNav = navBackStackEntry?.destination?.route
    val currentScreenFromNav = Screen.fromRoute(currentRouteFromNav)

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isMainTabScreen(currentScreenForUI)) {
                CenterAlignedTopAppBar(
                    title = { Text(currentScreenForUI?.title ?: "Cattle Manager") }
                )
            } else if (currentScreenForUI == Screen.CowDetail) {
                val cowId = navBackStackEntry?.arguments?.getLong("cowId")
                CenterAlignedTopAppBar(
                    title = {
                        if (cowId == null) { // Should not happen based on route definition
                            Text(Screen.CowDetail.title ?: "Cow Details")
                        } else if (cowId == 0L) {
                            Text("Add Animal")
                        } else {
                            val context = LocalContext.current
                            val application = context.applicationContext as CattleApplication
                            val database = CattleDatabase.getDatabase(context)
                            val repository = remember(database) { // Keyed remember for repository
                                CattleRepository(
                                    database.cowDao(), database.pastureDao(), database.activityDao(),
                                    database.settingsDao(), database.noteDao(), database.userDao(),
                                    database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
                                    database.activityTypeConfigDao(), database.breedDao()
                                )
                            }
                            // Use a key for the viewModel to ensure it's specific to the cowId
                            val viewModel: CowDetailViewModel = viewModel(
                                key = "CowDetailTitleVM_$cowId", 
                                factory = CowDetailViewModelFactory(application, repository, cowId)
                            )
                            val uiState by viewModel.uiState.collectAsState()
                            val name = uiState.name
                            Text(if (name.isNotBlank()) "Edit $name" else "Edit Animal")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    // Actions for CowDetail are intentionally omitted here as per previous logic
                )
            } else if (currentScreenForUI != null) { // Other non-main, non-CowDetail screens
                CenterAlignedTopAppBar(
                    title = { Text(currentScreenForUI.title ?: "Cattle Manager") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (currentScreenForUI == Screen.CowInfo) {
                            val cowIdFromArgs = navBackStackEntry?.arguments?.getLong("cowId")
                            cowIdFromArgs?.let { idVal ->
                                if (idVal != 0L) { 
                                    IconButton(onClick = { 
                                        Log.d("EditButtonDebug", "Navigating to CowDetail with ID: $idVal")
                                        navController.navigate(Screen.CowDetail.createRoute(idVal)) 
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                } else {
                                     Log.w("EditButtonDebug", "CowId is 0L, Edit button not shown or disabled.")
                                }
                            } ?: run {
                                Log.w("EditButtonDebug", "Could not retrieve cowId from arguments for Edit button.")
                            }
                        }
                        if (currentScreenForUI == Screen.CowInfo || 
                            currentScreenForUI == Screen.PastureDetail || 
                            currentScreenForUI == Screen.CowList ||
                            currentScreenForUI == Screen.ActivityInfo ||
                            currentScreenForUI == Screen.WorkingList) {
                            IconButton(onClick = { navController.popBackStack(Screen.MainPager.route, inclusive = false) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreenFromNav == Screen.MainPager) { 
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
                        selected = currentScreenForUI == Screen.Settings, 
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                // Optional: Configure navigation (e.g., launchSingleTop = true)
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, "Settings") }, 
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
