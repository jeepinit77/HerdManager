package com.jumblemint.cows

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import kotlinx.coroutines.delay
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.navigation.*
import com.jumblemint.cows.ui.theme.CowsTheme
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory
import com.jumblemint.cows.ui.components.GlobalSnackbarState
import com.jumblemint.cows.ui.components.LocalGlobalSnackbarState
import com.jumblemint.cows.ui.components.rememberGlobalSnackbarState
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

fun getPageIndexForScreen(screen: Screen): Int = when (screen) {
    Screen.Dashboard -> DASHBOARD_PAGE_INDEX
    Screen.Cows -> COWS_PAGE_INDEX
    Screen.Pastures -> PASTURES_PAGE_INDEX
    Screen.Activities -> ACTIVITIES_PAGE_INDEX
    Screen.Notes -> NOTES_PAGE_INDEX
    else -> -1
}

fun getScreenForPageIndex(index: Int): Screen? = when (index) {
    DASHBOARD_PAGE_INDEX -> Screen.Dashboard
    COWS_PAGE_INDEX -> Screen.Cows
    PASTURES_PAGE_INDEX -> Screen.Pastures
    ACTIVITIES_PAGE_INDEX -> Screen.Activities
    NOTES_PAGE_INDEX -> Screen.Notes
    else -> null
}

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
    val globalSnackbarState = rememberGlobalSnackbarState(coroutineScope)

    var saveTriggered by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var backPressed by remember { mutableStateOf(false) }
    
    // Double back to exit functionality
    var backPressedTime by remember { mutableStateOf(0L) }
    val context = LocalContext.current

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

    CompositionLocalProvider(LocalGlobalSnackbarState provides globalSnackbarState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            globalSnackbarState.snackbarHostState.currentSnackbarData?.dismiss()
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                    content = {
                        SnackbarHost(hostState = globalSnackbarState.snackbarHostState)
                    }
                )
            },
            topBar = {
                if (isMainTabScreen(currentScreenForUI)) {
                    CenterAlignedTopAppBar(
                        title = { Text(currentScreenForUI?.title ?: "Cattle Manager") }
                    )
                } else if (currentScreenForUI == Screen.CowDetail || currentScreenForUI == Screen.NoteDetail || currentScreenForUI == Screen.AddActivity || currentScreenForUI == Screen.AddActivityWithId) {
                    CenterAlignedTopAppBar(
                        title = {
                            when (currentScreenForUI) {
                                Screen.CowDetail -> {
                                    val cowId = navBackStackEntry?.arguments?.getLong("cowId") ?: 0L
                                    val context = LocalContext.current
                                    val application = context.applicationContext as CattleApplication
                                    val database = CattleDatabase.getDatabase(context)
                                    val repository = remember(database) {
                                        CattleRepository(
                                            database.cowDao(), database.pastureDao(), database.activityDao(),
                                            database.settingsDao(), database.noteDao(), database.userDao(),
                                            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
                                            database.activityTypeConfigDao(), database.breedDao()
                                        )
                                    }
                                    val viewModel: CowDetailViewModel = viewModel(
                                        factory = CowDetailViewModelFactory(application, repository, cowId)
                                    )
                                    val uiState by viewModel.uiState.collectAsState()
                                    if (cowId == 0L) {
                                        Text("Add Animal")
                                    } else {
                                        val name = uiState.name
                                        Text(if (name.isNotBlank()) "Edit $name" else "Edit Animal")
                                    }
                                }
                                Screen.NoteDetail -> {
                                    val noteId = navBackStackEntry?.arguments?.getLong("noteId") ?: 0L
                                    Text(if (noteId == 0L) "Add Note" else "Edit Note")
                                }
                                Screen.AddActivity -> Text("Add Activity")
                                Screen.AddActivityWithId -> Text("Edit Activity")
                                else -> Text("Details")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                backPressed = true
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (hasUnsavedChanges) {
                                IconButton(onClick = { 
                                    saveTriggered = true
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Save")
                                }
                            }
                        }
                    )
                } else if (currentScreenForUI != null) {
                    CenterAlignedTopAppBar(
                        title = { Text(currentScreenForUI.title ?: "Cattle Manager") },
                        navigationIcon = {
                            IconButton(onClick = { 
                                if (currentScreenForUI == Screen.AddPasture || currentScreenForUI == Screen.EditPasture || currentScreenForUI == Screen.NoteDetail) {
                                    backPressed = true
                                } else {
                                    navController.popBackStack()
                                }
                            }) {
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
                            if (currentScreenForUI == Screen.NoteInfo) {
                                val noteIdFromArgs = navBackStackEntry?.arguments?.getLong("noteId")
                                noteIdFromArgs?.let { idVal ->
                                    IconButton(onClick = { 
                                        navController.navigate(Screen.NoteDetail.createRoute(idVal)) 
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                }
                            }
                            if ((currentScreenForUI == Screen.AddPasture || currentScreenForUI == Screen.EditPasture || currentScreenForUI == Screen.NoteDetail || currentScreenForUI == Screen.AddActivity || currentScreenForUI == Screen.AddActivityWithId) && hasUnsavedChanges) {
                                IconButton(onClick = { 
                                    saveTriggered = true
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Save")
                                }
                            }
                            if (currentScreenForUI == Screen.CowInfo || 
                                currentScreenForUI == Screen.PastureDetail || 
                                currentScreenForUI == Screen.CowList ||
                                currentScreenForUI == Screen.ActivityInfo ||
                                currentScreenForUI == Screen.WorkingList ||
                                currentScreenForUI == Screen.NoteInfo) {
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
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        NavigationBarItem(
                            selected = currentScreenForUI == Screen.Settings, 
                            onClick = {
                                navController.navigate(Screen.Settings.route) {
                                }
                            },
                            icon = { Icon(Icons.Filled.Settings, "Settings") }, 
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            // Handle back button for specific screens with unsaved changes
            if (currentScreenForUI == Screen.AddPasture || currentScreenForUI == Screen.EditPasture || currentScreenForUI == Screen.CowDetail || currentScreenForUI == Screen.NoteDetail || currentScreenForUI == Screen.AddActivity || currentScreenForUI == Screen.AddActivityWithId) {
                BackHandler {
                    backPressed = true
                }
            }
            
            // Handle double back to exit for MainPager screens
            if (currentScreenFromNav == Screen.MainPager) {
                BackHandler {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - backPressedTime < 2000) {
                        // Second back press within 2 seconds - exit app
                        (context as? ComponentActivity)?.finish()
                    } else {
                        // First back press - show toast and record time
                        backPressedTime = currentTime
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            CattleNavigation(
                navController = navController,
                mainScaffoldPadding = innerPadding,
                pagerState = pagerState,
                saveTriggered = saveTriggered,
                onSaveHandled = { saveTriggered = false },
                onUnsavedChangesChanged = { hasUnsavedChanges = it },
                backPressed = backPressed,
                onBackHandled = { backPressed = false },
                globalSnackbarState = globalSnackbarState
            )
        }
    }
}

data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)