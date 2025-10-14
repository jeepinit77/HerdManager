package com.jumblemint.cows

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
//import com.jumblemint.cows.ui.icons.Cow

import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.navigation.*
import com.jumblemint.cows.navigation.SETTINGS_PAGE_INDEX
import com.jumblemint.cows.ui.screens.settings.SettingsScreen
import com.jumblemint.cows.ui.theme.CowsTheme
import com.jumblemint.cows.util.AgeUtils
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory
import com.jumblemint.cows.ui.components.GlobalSnackbarState
import com.jumblemint.cows.ui.components.LocalGlobalSnackbarState
import com.jumblemint.cows.ui.components.rememberGlobalSnackbarState
import com.jumblemint.cows.ui.theme.ThemeManager
import com.jumblemint.cows.ui.theme.ThemeMode
import com.jumblemint.cows.CattleApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
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
            val themeManager = remember(repository) { ThemeManager(repository) }
            val themeMode by themeManager.getThemeModeFlow().collectAsState(initial = ThemeMode.SYSTEM)

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
    Screen.Settings -> SETTINGS_PAGE_INDEX
    else -> -1
}

fun getScreenForPageIndex(index: Int): Screen? = when (index) {
    DASHBOARD_PAGE_INDEX -> Screen.Dashboard
    COWS_PAGE_INDEX -> Screen.Cows
    PASTURES_PAGE_INDEX -> Screen.Pastures
    ACTIVITIES_PAGE_INDEX -> Screen.Activities
    NOTES_PAGE_INDEX -> Screen.Notes
    SETTINGS_PAGE_INDEX -> Screen.Settings
    else -> null
}

fun isMainTabScreen(screen: Screen?): Boolean {
    return screen == Screen.Dashboard ||
            screen == Screen.Cows ||
            screen == Screen.Pastures ||
            screen == Screen.Activities ||
            screen == Screen.Notes ||
            screen == Screen.Settings
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
    var resetTriggered by remember { mutableStateOf(false) }

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
        BottomNavItem(Screen.Cows, painterResource(R.drawable.ear_tag), "Cattle"),
        BottomNavItem(Screen.Pastures, Icons.Filled.Landscape, "Pastures"),
        BottomNavItem(Screen.Activities, Icons.Filled.Assignment, "Activity"),
        BottomNavItem(Screen.Notes, Icons.Filled.Note, "Notes"),
        BottomNavItem(Screen.Settings, Icons.Filled.Settings, "Settings")
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
                // --- Unified TopAppBar styling via AppTopBar ---
                if (isMainTabScreen(currentScreenForUI)) {
                    AppTopBar(
                        title = currentScreenForUI?.title ?: "Cattle Manager"
                    )
                } else if (
                    currentScreenForUI == Screen.CowDetail ||
                    currentScreenForUI == Screen.NoteDetail ||
                    currentScreenForUI == Screen.AddActivity ||
                    currentScreenForUI == Screen.AddActivityWithId
                ) {
                    val detailTitle: String = when (currentScreenForUI) {
                        Screen.CowDetail -> {
                            val cowId = navBackStackEntry?.arguments?.getLong("cowId") ?: 0L
                            val contextLocal = LocalContext.current
                            val application = contextLocal.applicationContext as CattleApplication
                            val database = CattleDatabase.getDatabase(contextLocal)
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
                            if (cowId == 0L) "Add Animal"
                            else {
                                val name = uiState.name
                                if (name.isNotBlank()) "Edit $name" else "Edit Animal"
                            }
                        }
                        Screen.NoteDetail -> {
                            val noteId = navBackStackEntry?.arguments?.getLong("noteId") ?: 0L
                            if (noteId == 0L) "Add Note" else "Edit Note"
                        }
                        Screen.AddActivity -> "Add Activity"
                        Screen.AddActivityWithId -> "Edit Activity"
                        else -> "Details"
                    }

                    AppTopBar(
                        title = detailTitle,
                        navigationIcon = {
                            IconButton(onClick = { backPressed = true }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (hasUnsavedChanges) {
                                IconButton(onClick = { saveTriggered = true }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    )
                } else if (currentScreenForUI != null) {
                    // Screens with dynamic titles and various actions
                    val dynamicTitle: String = if (currentScreenForUI == Screen.CowList) {
                        val type = navBackStackEntry?.arguments?.getString("type")
                        val value = navBackStackEntry?.arguments?.getString("value")
                        val contextLocal = LocalContext.current
                        val database = CattleDatabase.getDatabase(contextLocal)
                        val repository = remember(database) {
                            CattleRepository(
                                database.cowDao(), database.pastureDao(), database.activityDao(),
                                database.settingsDao(), database.noteDao(), database.userDao(),
                                database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
                                database.activityTypeConfigDao(), database.breedDao()
                            )
                        }
                        val pasture = if (type == "pasture" && value != null) {
                            repository.getPastureById(value).collectAsState(initial = null).value
                        } else null
                        val pastureName = pasture?.name ?: value
                        when (type) {
                            "status" -> {
                                value?.let {
                                    val statusName = it.lowercase().replaceFirstChar { ch ->
                                        if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                                    }
                                    "Status: $statusName"
                                } ?: "All Cattle"
                            }
                            "classification" -> value?.let { "Classification: $it" } ?: "Cattle by Classification"
                            "pasture" -> pastureName?.let { "Pasture: $it" } ?: "Cattle by Pasture"
                            "pastureName" -> if (value == "Unassigned") "Unassigned Animals" else value?.let { "Pasture: $it" } ?: "Cattle by Pasture"
                            "unassigned" -> "Unassigned Animals"
                            "notCalved" -> "Not Calved (9+ Months)"
                            "calved" -> "Cattle with Active Calves"
                            "age" -> "Age: ${AgeUtils.getLabel(value)}"
                            "watching" -> "Watched Cattle"
                            else -> "Cattle"
                        }
                    } else {
                        currentScreenForUI.title ?: "Cattle Manager"
                    }

                    AppTopBar(
                        title = dynamicTitle,
                        navigationIcon = {
                            IconButton(onClick = {
                                if (currentScreenForUI == Screen.AddPasture ||
                                    currentScreenForUI == Screen.EditPasture ||
                                    currentScreenForUI == Screen.NoteDetail
                                ) {
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
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface)
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
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            if ((currentScreenForUI == Screen.AddPasture ||
                                        currentScreenForUI == Screen.EditPasture ||
                                        currentScreenForUI == Screen.NoteDetail ||
                                        currentScreenForUI == Screen.AddActivity ||
                                        currentScreenForUI == Screen.AddActivityWithId) && hasUnsavedChanges
                            ) {
                                IconButton(onClick = { saveTriggered = true }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (currentScreenForUI == Screen.TagColorsManagement) {
                                IconButton(onClick = { resetTriggered = true }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Reset to Defaults", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (currentScreenForUI == Screen.ThemeSettings) {
                                IconButton(onClick = { resetTriggered = true }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Reset Theme to Default", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (currentScreenForUI == Screen.CowInfo ||
                                currentScreenForUI == Screen.PastureDetail ||
                                currentScreenForUI == Screen.CowList ||
                                currentScreenForUI == Screen.ActivityInfo ||
                                currentScreenForUI == Screen.WorkingList ||
                                currentScreenForUI == Screen.NoteInfo
                            ) {
                                IconButton(onClick = {
                                    navController.popBackStack(Screen.MainPager.route, inclusive = false)
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentScreenFromNav == Screen.MainPager) {
                    val context = LocalContext.current
                    val database = CattleDatabase.getDatabase(context)
                    val repository = remember(database) {
                        CattleRepository(
                            database.cowDao(), database.pastureDao(), database.activityDao(),
                            database.settingsDao(), database.noteDao(), database.userDao(),
                            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
                            database.activityTypeConfigDao(), database.breedDao()
                        )
                    }
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val pageIndex = getPageIndexForScreen(item.screen)
                            val isSelected = pagerState.currentPage == pageIndex
                            NavigationBarItem(
                                icon = { 
                                    when (val iconData = item.icon) {
                                        is ImageVector -> Icon(iconData, item.label)
                                        is Painter -> Icon(iconData, item.label, modifier = Modifier.size(24.dp))
                                        else -> Icon(Icons.Filled.Help, item.label)
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (pageIndex != -1) {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(pageIndex)
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
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
                resetTriggered = resetTriggered,
                onResetHandled = { resetTriggered = false },
                globalSnackbarState = globalSnackbarState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val database = CattleDatabase.getDatabase(context)
    val repository = remember(database) {
        CattleRepository(
            database.cowDao(), database.pastureDao(), database.activityDao(),
            database.settingsDao(), database.noteDao(), database.userDao(),
            database.herdDao(), database.herdMemberDao(), database.tagColorDao(),
            database.activityTypeConfigDao(), database.breedDao()
        )
    }
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions
    )
}

data class BottomNavItem(val screen: Screen, val icon: Any, val label: String) // Can be ImageVector or Painter
