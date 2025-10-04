package com.jumblemint.cows.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jumblemint.cows.CattleApplication
import com.jumblemint.cows.data.model.Activity // Corrected: Import for Activity model
import com.jumblemint.cows.ui.screens.account.AccountManagementScreen
import com.jumblemint.cows.ui.screens.activities.ActivitiesScreen
import com.jumblemint.cows.ui.screens.activities.ActivityInfoScreen
import com.jumblemint.cows.ui.screens.activities.AddActivityScreen
import com.jumblemint.cows.ui.screens.activities.AddBirthScreen
import com.jumblemint.cows.ui.screens.auth.SignInScreen
import com.jumblemint.cows.ui.screens.cows.CowEditScreen
import com.jumblemint.cows.ui.screens.splash.SplashScreen
import com.jumblemint.cows.ui.screens.cows.CowInfoScreen
import com.jumblemint.cows.ui.screens.cows.CowListScreen

import com.jumblemint.cows.ui.screens.herds.HerdSelectionScreen // सुनिश्चित किया गया आयात
import com.jumblemint.cows.ui.screens.notes.NotesScreen
import com.jumblemint.cows.ui.screens.pastures.PastureDetailScreen
import com.jumblemint.cows.ui.screens.pastures.PasturesScreen
import com.jumblemint.cows.ui.screens.reports.ReportsScreen
import com.jumblemint.cows.ui.screens.settings.ActivityTypesManagementScreen
import com.jumblemint.cows.ui.screens.settings.SettingsScreen
import com.jumblemint.cows.ui.screens.settings.TagColorsManagementScreen
import com.jumblemint.cows.ui.screens.settings.BreedsManagementScreen
import com.jumblemint.cows.ui.screens.settings.ThemeSettingsScreen
import com.jumblemint.cows.ui.screens.sync.SyncDetailsScreen
import com.jumblemint.cows.ui.screens.workinglist.WorkingListScreen
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModel
import com.jumblemint.cows.ui.viewmodel.AddBirthViewModelFactory
import com.jumblemint.cows.ui.viewmodel.AuthViewModel
import com.jumblemint.cows.ui.viewmodel.AuthViewModelFactory
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModel
import com.jumblemint.cows.ui.viewmodel.CowDetailViewModelFactory
import com.jumblemint.cows.ui.viewmodel.PasturesViewModel
import com.jumblemint.cows.ui.viewmodel.PasturesViewModelFactory
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.components.*

// MAIN_PAGER_ROUTE_TEMPLATE is used from Screen.kt (same package)
fun mainPagerRoute(initialPage: Int = 0) = MAIN_PAGER_ROUTE_TEMPLATE.replace("{initialPage}", initialPage.toString())

const val DASHBOARD_PAGE_INDEX = 0
const val COWS_PAGE_INDEX = 1
const val PASTURES_PAGE_INDEX = 2
const val ACTIVITIES_PAGE_INDEX = 3
const val NOTES_PAGE_INDEX = 4
const val MAIN_SCREEN_PAGE_COUNT = 5


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreensViewPager(
    navController: NavHostController,
    mainScaffoldPadding: PaddingValues,
    pagerState: PagerState,
    globalSnackbarState: com.jumblemint.cows.ui.components.GlobalSnackbarState
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication



    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .padding(mainScaffoldPadding)
            .fillMaxSize(),
        userScrollEnabled = !globalSnackbarState.isSnackbarVisible
    ) { page ->
        when (page) {
            DASHBOARD_PAGE_INDEX -> {
                val currentUser by application.authService.currentUser.collectAsState(initial = null)
                LaunchedEffect(currentUser) { if (currentUser != null && !currentUser!!.isLocalUser) { application.authService.startUserSync(application.syncService) } }
                ReportsScreen(
                    modifier = Modifier.fillMaxSize(),
                    onShowList = { type, value ->
                        if (type == "workingList") {
                            navController.navigate(Screen.WorkingList.route)
                        } else {
                            navController.navigate(Screen.CowList.createRoute(type = type, value = value))
                        }
                    },
                    onNavigateToAddBirth = { navController.navigate(Screen.AddBirth.route) }
                )
            }
            COWS_PAGE_INDEX -> CowListScreen(
                modifier = Modifier.fillMaxSize(),
                pastureId = null,
                onCowClick = { cowId: Long -> navController.navigate(Screen.CowInfo.createRoute(cowId = cowId, returnToRoute = mainPagerRoute(COWS_PAGE_INDEX))) },
                onCowEdit = { cowId: Long -> navController.navigate(Screen.CowDetail.createRoute(cowId)) },
                onAddCowClick = { navController.navigate(Screen.CowDetail.createRoute(0L)) },
                showSearchAndFilters = true,
                showFab = true
            )
            PASTURES_PAGE_INDEX -> PasturesScreen(
                modifier = Modifier.fillMaxSize(),
                onNavigateToAddPasture = { navController.navigate(Screen.AddPasture.route) },
                onNavigateToPastureDetails = { pastureIdString: String ->
                    navController.navigate(Screen.PastureDetail.createRoute(pastureIdString))
                },
                onNavigateToEditPasture = { pastureIdString: String ->
                    navController.navigate(Screen.EditPasture.createRoute(pastureIdString))
                },
                onNavigateToUnassignedList = { navController.navigate(Screen.CowList.createRoute(type = "unassigned", value = null)) }, // Added this line
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(mainPagerRoute(DASHBOARD_PAGE_INDEX))
                }
            )
            ACTIVITIES_PAGE_INDEX -> ActivitiesScreen(
                modifier = Modifier.fillMaxSize(),
                onAddActivityClick = { navController.navigate(Screen.AddActivity.createRoute()) },
                onEditActivityClick = { activity: Activity ->
                    navController.navigate(Screen.AddActivityWithId.createRoute(activity.id))
                },
                // Navigate to ActivityInfoScreen when an activity is clicked
                onActivityClick = { activityId: Long ->
                    navController.navigate(Screen.ActivityInfo.createRoute(activityId))
                }
            )
            NOTES_PAGE_INDEX -> NotesScreen(
                modifier = Modifier.fillMaxSize(),
                onNavigateBack = { navController.popBackStack() },
                onAddNote = { navController.navigate(Screen.NoteDetail.createRoute(0L)) },
                onEditNote = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) }
            )
            else -> Text("Unknown Page")
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CattleNavigation(
    navController: NavHostController,
    mainScaffoldPadding: PaddingValues,
    pagerState: PagerState,
    saveTriggered: Boolean = false,
    onSaveHandled: () -> Unit = {},
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    backPressed: Boolean = false,
    onBackHandled: () -> Unit = {},
    globalSnackbarState: com.jumblemint.cows.ui.components.GlobalSnackbarState
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val screenModifierNoPadding = Modifier.fillMaxSize()
    val screenModifierWithPadding = Modifier
        .padding(mainScaffoldPadding)
        .fillMaxSize()

    NavHost(
        navController = navController,
        startDestination = Screen.MainPager.buildRoute(DASHBOARD_PAGE_INDEX),
        modifier = Modifier.fillMaxSize()
    ) {

        composable(
            route = Screen.MainPager.route,
            arguments = listOf(
                navArgument("initialPage") {
                    type = NavType.IntType
                    defaultValue = DASHBOARD_PAGE_INDEX
                }
            )
        ) {
            MainScreensViewPager(
                navController = navController,
                mainScaffoldPadding = mainScaffoldPadding,
                pagerState = pagerState,
                globalSnackbarState = globalSnackbarState
            )
        }

        composable(
            route = Screen.CowInfo.route,
            arguments = listOf(
                navArgument("cowId") {
                    type = NavType.LongType
                },
                navArgument("returnToRoute") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) }
        ) { backStackEntry ->
            val cowId = backStackEntry.arguments?.getLong("cowId") ?: 0L
            val returnToRouteArg = backStackEntry.arguments?.getString("returnToRoute")?.let { android.net.Uri.decode(it) }

            CowInfoScreen(
                modifier = screenModifierWithPadding,
                cowId = cowId,
                onNavigateBack = { navController.popBackStack() },
                onEditCow = { navController.navigate(Screen.CowDetail.createRoute(cowId)) },
                onNavigateToCow = { selectedCowId: Long ->
                    if (cowId != selectedCowId) {
                        val finalRoute = Screen.CowInfo.createRoute(cowId = selectedCowId, returnToRoute = returnToRouteArg ?: mainPagerRoute(COWS_PAGE_INDEX))
                        navController.navigate(finalRoute)
                    }
                },
                onCloseFlow = {
                    val targetRoute = returnToRouteArg ?: mainPagerRoute(COWS_PAGE_INDEX)
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(
            route = Screen.CowDetail.route,
            arguments = listOf(
                navArgument("cowId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val cowId = backStackEntry.arguments?.getLong("cowId") ?: 0L
            val localContext = LocalContext.current // Renamed to avoid conflict
            val database = CattleDatabase.getDatabase(localContext)
            val repository = remember {
                CattleRepository(
                    database.cowDao(),
                    database.pastureDao(),
                    database.activityDao(),
                    database.settingsDao(),
                    database.noteDao(),
                    database.userDao(),
                    database.herdDao(),
                    database.herdMemberDao(),
                    database.tagColorDao(),
                    database.activityTypeConfigDao(),
                    database.breedDao()
                )
            }
            val viewModel: CowDetailViewModel = viewModel(
                factory = CowDetailViewModelFactory(application, repository, cowId)
            )
            var hasUnsavedChanges by remember { mutableStateOf(false) }
            
            LaunchedEffect(hasUnsavedChanges) {
                onUnsavedChangesChanged(hasUnsavedChanges)
            }
            
            CowEditScreen(
                modifier = screenModifierWithPadding,
                cowId = cowId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                saveTriggered = saveTriggered,
                onSaveHandled = onSaveHandled,
                onUnsavedChangesChanged = { hasUnsavedChanges = it },
                backPressed = backPressed,
                onBackHandled = onBackHandled
            )
        }

        composable(Screen.AddActivity.route) {
            var hasUnsavedChanges by remember { mutableStateOf(false) }
            
            LaunchedEffect(hasUnsavedChanges) {
                onUnsavedChangesChanged(hasUnsavedChanges)
            }
            
            AddActivityScreen(
                modifier = screenModifierWithPadding,
                editId = null,
                onNavigateBack = { navController.popBackStack() },
                saveTriggered = saveTriggered,
                onSaveHandled = onSaveHandled,
                onUnsavedChangesChanged = { hasUnsavedChanges = it },
                backPressed = backPressed,
                onBackHandled = onBackHandled
            )
        }

        composable(Screen.AddActivityWithId.route,
            arguments = listOf(navArgument("activityId") { type = NavType.LongType }) // Ensure argument is defined
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getLong("activityId")
            var hasUnsavedChanges by remember { mutableStateOf(false) }
            
            LaunchedEffect(hasUnsavedChanges) {
                onUnsavedChangesChanged(hasUnsavedChanges)
            }
            
            AddActivityScreen(
                modifier = screenModifierWithPadding,
                editId = activityId,
                onNavigateBack = { navController.popBackStack() },
                saveTriggered = saveTriggered,
                onSaveHandled = onSaveHandled,
                onUnsavedChangesChanged = { hasUnsavedChanges = it },
                backPressed = backPressed,
                onBackHandled = onBackHandled
            )
        }
        
        // Composable for ActivityInfoScreen
        composable(
            route = Screen.ActivityInfo.route,
            arguments = listOf(
                navArgument("activityId") { type = NavType.LongType }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) }
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getLong("activityId") ?: 0L
            ActivityInfoScreen(
                modifier = screenModifierWithPadding,
                activityId = activityId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCow = { cowId ->
                    navController.navigate(Screen.CowInfo.createRoute(cowId = cowId, returnToRoute = Screen.ActivityInfo.createRoute(activityId)))
                },
                onEditActivity = { currentActivityId ->
                    navController.navigate(Screen.AddActivityWithId.createRoute(currentActivityId))
                }
            )
        }

        composable(Screen.CowList.route) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val valueString = backStackEntry.arguments?.getString("value")
            CowListScreen(
                modifier = screenModifierWithPadding,
                type = type,
                value = valueString,
                onCowClick = { cowId: Long -> navController.navigate(Screen.CowInfo.createRoute(cowId, Screen.CowList.createRoute(type, valueString))) },
                onBack = { navController.popBackStack() },
                showSearchAndFilters = false,
                showFab = false
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                onNavigateToHerds = { navController.navigate(Screen.HerdSelection.route) },
                onNavigateToAccountManagement = { navController.navigate(Screen.AccountManagement.route) },
                onNavigateToTagColors = { navController.navigate(Screen.TagColorsManagement.route) },
                onNavigateToActivityTypes = { navController.navigate(Screen.ActivityTypesManagement.route) },
                onNavigateToBreeds = { navController.navigate(Screen.BreedsManagement.route) },
                onNavigateToThemeSettings = { navController.navigate(Screen.ThemeSettings.route) }
            )
        }

        composable(Screen.SignIn.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authService, application.repository, application.syncService))
            SignInScreen(
                modifier = screenModifierWithPadding,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSignInSuccess = {
                    navController.navigate(Screen.MainPager.buildRoute(DASHBOARD_PAGE_INDEX)) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PastureDetail.route,
            arguments = listOf(
                navArgument("pastureId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val pastureId = backStackEntry.arguments?.getString("pastureId") ?: ""
            PastureDetailScreen(
                modifier = screenModifierWithPadding,
                pastureId = pastureId,
                onNavigateBack = { 
                    navController.navigate(mainPagerRoute(PASTURES_PAGE_INDEX)) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                onCowClick = { cowId: Long ->
                    navController.navigate(
                        Screen.CowInfo.createRoute(
                            cowId = cowId,
                            returnToRoute = Screen.PastureDetail.createRoute(pastureId)
                        )
                    )
                },
                onCowEdit = { cowId: Long ->
                    navController.navigate(Screen.CowDetail.createRoute(cowId))
                },
                onEditPasture = {
                    navController.navigate(Screen.EditPasture.createRoute(pastureId))
                }
            )
        }

        composable(Screen.AddBirth.route){
            val localContext = LocalContext.current
            val database = CattleDatabase.getDatabase(localContext)
            val repository = remember {
                CattleRepository(
                    cowDao = database.cowDao(),
                    pastureDao = database.pastureDao(),
                    activityDao = database.activityDao(),
                    settingsDao = database.settingsDao(),
                    noteDao = database.noteDao(),
                    userDao = database.userDao(),
                    herdDao = database.herdDao(),
                    herdMemberDao = database.herdMemberDao(),
                    tagColorDao = database.tagColorDao(),
                    activityTypeConfigDao = database.activityTypeConfigDao(),
                    breedDao = database.breedDao()
                )
            }

            val addBirthViewModel: AddBirthViewModel = viewModel(
                factory = AddBirthViewModelFactory(
                    repository,
                    application.authService,
                    application.syncService
                )
            )

            AddBirthScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() },
                viewModel = addBirthViewModel
            )
        }
        composable(Screen.AddPasture.route) {
            val localContext = LocalContext.current // Renamed
            val applicationContext = localContext.applicationContext as CattleApplication // Renamed
            val database = CattleDatabase.getDatabase(applicationContext)
            val repository = remember {
                CattleRepository(
                    cowDao = database.cowDao(),
                    pastureDao = database.pastureDao(),
                    activityDao = database.activityDao(),
                    settingsDao = database.settingsDao(),
                    noteDao = database.noteDao(),
                    userDao = database.userDao(),
                    herdDao = database.herdDao(),
                    herdMemberDao = database.herdMemberDao(),
                    tagColorDao = database.tagColorDao(),
                    activityTypeConfigDao = database.activityTypeConfigDao(),
                    breedDao = database.breedDao()
                )
            }
            val pasturesViewModel: PasturesViewModel = viewModel(
                factory = PasturesViewModelFactory(applicationContext, repository)
            )
            
            var hasUnsavedChanges by remember { mutableStateOf(false) }
            
            LaunchedEffect(hasUnsavedChanges) {
                onUnsavedChangesChanged(hasUnsavedChanges)
            }
            
            PastureDetailScreen(
                modifier = screenModifierWithPadding,
                onSave = { pasture ->
                    pasturesViewModel.insertNewPasture(pasture)
                    navController.navigate(mainPagerRoute(PASTURES_PAGE_INDEX)) {
                        popUpTo(Screen.AddPasture.route) { inclusive = true }
                    }
                },
                onCancel = { 
                    navController.navigate(mainPagerRoute(PASTURES_PAGE_INDEX)) {
                        popUpTo(Screen.AddPasture.route) { inclusive = true }
                    }
                },
                saveTriggered = saveTriggered,
                onSaveHandled = onSaveHandled,
                onUnsavedChangesChanged = { hasUnsavedChanges = it },
                backPressed = backPressed,
                onBackHandled = onBackHandled
            )
        }
        composable(
            route = Screen.EditPasture.route,
            arguments = listOf(
                navArgument("pastureId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val pastureId = backStackEntry.arguments?.getString("pastureId") ?: ""
            val localContext = LocalContext.current // Renamed
            val applicationContext = localContext.applicationContext as CattleApplication // Renamed
            val database = CattleDatabase.getDatabase(applicationContext)
            val repository = remember {
                CattleRepository(
                    cowDao = database.cowDao(),
                    pastureDao = database.pastureDao(),
                    activityDao = database.activityDao(),
                    settingsDao = database.settingsDao(),
                    noteDao = database.noteDao(),
                    userDao = database.userDao(),
                    herdDao = database.herdDao(),
                    herdMemberDao = database.herdMemberDao(),
                    tagColorDao = database.tagColorDao(),
                    activityTypeConfigDao = database.activityTypeConfigDao(),
                    breedDao = database.breedDao()
                )
            }
            val pasturesViewModel: PasturesViewModel = viewModel(
                factory = PasturesViewModelFactory(applicationContext, repository)
            )
            
            val pasture by repository.getPastureById(pastureId).collectAsState(initial = null)
            var hasUnsavedChanges by remember { mutableStateOf(false) }
            
            LaunchedEffect(hasUnsavedChanges) {
                onUnsavedChangesChanged(hasUnsavedChanges)
            }
            
            PastureDetailScreen(
                modifier = screenModifierWithPadding,
                editPasture = pasture,
                onSave = { updatedPasture ->
                    pasturesViewModel.insertNewPasture(updatedPasture)
                    navController.navigate(mainPagerRoute(PASTURES_PAGE_INDEX)) {
                        popUpTo(Screen.EditPasture.route) { inclusive = true }
                    }
                },
                onCancel = { 
                    navController.navigate(mainPagerRoute(PASTURES_PAGE_INDEX)) {
                        popUpTo(Screen.EditPasture.route) { inclusive = true }
                    }
                },
                saveTriggered = saveTriggered,
                onSaveHandled = onSaveHandled,
                onUnsavedChangesChanged = { hasUnsavedChanges = it },
                backPressed = backPressed,
                onBackHandled = onBackHandled
            )
        }
        composable(Screen.AccountManagement.route){
            AccountManagementScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TagColorsManagement.route){
            TagColorsManagementScreen(
                modifier = screenModifierWithPadding,
                 onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ActivityTypesManagement.route){
            ActivityTypesManagementScreen(
                modifier = screenModifierWithPadding,
                 onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BreedsManagement.route){
            BreedsManagementScreen(
                modifier = screenModifierWithPadding,
                 onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ThemeSettings.route){
            ThemeSettingsScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Sync.route) {
             SyncDetailsScreen(
                 modifier = screenModifierWithPadding,
                 onNavigateBack = { navController.popBackStack() }
             )
        }
        composable(Screen.WorkingList.route) {
            WorkingListScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() },
                onCowClick = { cowId: Long -> navController.navigate(Screen.CowInfo.createRoute(cowId, Screen.WorkingList.route)) },
            )
        }
        composable(Screen.HerdSelection.route) { 
            HerdSelectionScreen(
                onHerdSelected = { herdId -> /* TODO: Implement herd selection action */ },
                onCreateHerd = { /* TODO: Implement create herd action */ },
                onNavigateBack = { navController.popBackStack() },
                onCowClick = { cowId: Long -> navController.navigate(Screen.CowInfo.createRoute(cowId, null)) }
            )
        }
        
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            com.jumblemint.cows.ui.screens.notes.NoteDetailScreen(
                modifier = screenModifierWithPadding,
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
                saveTriggered = saveTriggered,
                onSaveHandled = onSaveHandled,
                onUnsavedChangesChanged = onUnsavedChangesChanged,
                backPressed = backPressed,
                onBackHandled = onBackHandled
            )
        }
    }
}
