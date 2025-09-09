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
import com.jumblemint.cows.ui.screens.activities.AddActivityScreen
import com.jumblemint.cows.ui.screens.activities.AddBirthScreen
import com.jumblemint.cows.ui.screens.auth.LoginScreen
import com.jumblemint.cows.ui.screens.auth.SignInScreen
import com.jumblemint.cows.ui.screens.cows.CowDetailScreen
import com.jumblemint.cows.ui.screens.cows.CowInfoScreen
import com.jumblemint.cows.ui.screens.cows.CowListScreen
import com.jumblemint.cows.ui.screens.cows.CowsScreen
import com.jumblemint.cows.ui.screens.herds.HerdSelectionScreen // सुनिश्चित किया गया आयात
import com.jumblemint.cows.ui.screens.notes.NotesScreen
import com.jumblemint.cows.ui.screens.pastures.PastureDetailScreen
import com.jumblemint.cows.ui.screens.pastures.PasturesScreen
import com.jumblemint.cows.ui.screens.reports.ReportsScreen
import com.jumblemint.cows.ui.screens.settings.ActivityTypesManagementScreen
import com.jumblemint.cows.ui.screens.settings.SettingsScreen
import com.jumblemint.cows.ui.screens.settings.TagColorsManagementScreen
import com.jumblemint.cows.ui.screens.sync.SyncDetailsScreen
import com.jumblemint.cows.ui.screens.workinglist.WorkingListScreen
import com.jumblemint.cows.ui.viewmodel.AuthViewModel
import com.jumblemint.cows.ui.viewmodel.AuthViewModelFactory

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
    pagerState: PagerState
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .padding(mainScaffoldPadding)
            .fillMaxSize()
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
            COWS_PAGE_INDEX -> CowsScreen(
                modifier = Modifier.fillMaxSize(),
                pastureId = null,
                onCowClick = { cowId: Long -> navController.navigate(Screen.CowInfo.createRoute(cowId = cowId, returnToRoute = mainPagerRoute(COWS_PAGE_INDEX))) },
                onCowEdit = { cowId: Long -> navController.navigate(Screen.CowDetail.createRoute(cowId)) }, // Screen.CowDetail.createRoute expects Long
                onAddCowClick = { navController.navigate(Screen.CowDetail.createRoute(0L)) } // Screen.CowDetail.createRoute expects Long
            )
            PASTURES_PAGE_INDEX -> PasturesScreen(
                modifier = Modifier.fillMaxSize(),
                onNavigateToAddPasture = { navController.navigate(Screen.PastureDetail.createRoute(0L)) }, // Corrected: Screen.PastureDetail.createRoute expects Long
                onNavigateToPastureDetails = { pastureIdString: String ->
                    navController.navigate(Screen.PastureDetail.createRoute(pastureIdString.toLongOrNull() ?: 0L)) // Corrected: Screen.PastureDetail.createRoute expects Long
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                     navController.navigate(mainPagerRoute(DASHBOARD_PAGE_INDEX)) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            )
            ACTIVITIES_PAGE_INDEX -> ActivitiesScreen(
                modifier = Modifier.fillMaxSize(),
                onAddActivityClick = { navController.navigate(Screen.AddActivity.createRoute()) },
                onEditActivityClick = { activity: Activity ->
                    navController.navigate(Screen.AddActivityWithId.createRoute(activity.id))
                }
            )
            NOTES_PAGE_INDEX -> NotesScreen(
                modifier = Modifier.fillMaxSize(),
                onNavigateBack = { navController.popBackStack() }
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
    pagerState: PagerState
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication
    val screenModifierNoPadding = Modifier.fillMaxSize()
    val screenModifierWithPadding = Modifier // This variable is defined but might not be used if HerdSelectionScreen doesn't take a modifier
        .padding(mainScaffoldPadding)
        .fillMaxSize()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authService, application.repository, application.syncService))
            LoginScreen(
                modifier = screenModifierNoPadding,
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.MainPager.buildRoute(DASHBOARD_PAGE_INDEX)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

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
                pagerState = pagerState
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
            val returnToRouteArg = backStackEntry.arguments?.getString("returnToRoute")

            CowInfoScreen(
                modifier = screenModifierNoPadding,
                cowId = cowId,
                onNavigateBack = { navController.popBackStack() },
                onEditCow = { navController.navigate(Screen.CowDetail.createRoute(cowId)) }, // Screen.CowDetail.createRoute expects Long
                onNavigateToCow = { selectedCowId: Long ->
                    if (cowId != selectedCowId) {
                        val finalRoute = Screen.CowInfo.createRoute(cowId = selectedCowId, returnToRoute = returnToRouteArg ?: mainPagerRoute(COWS_PAGE_INDEX))
                        navController.navigate(finalRoute) { launchSingleTop = true; popUpTo(finalRoute) {inclusive = true} }
                    }
                },
                onCloseFlow = {
                    if (!returnToRouteArg.isNullOrEmpty()) {
                         if (returnToRouteArg.startsWith(MAIN_PAGER_ROUTE_TEMPLATE.split("?").first())) {
                             navController.navigate(returnToRouteArg) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                         } else {
                            val success = navController.popBackStack(returnToRouteArg, inclusive = false, saveState = false)
                            if (!success) {
                                 navController.navigate(mainPagerRoute(DASHBOARD_PAGE_INDEX)) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            }
                         }
                    } else {
                        navController.navigate(mainPagerRoute(DASHBOARD_PAGE_INDEX)) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.CowDetail.route) { backStackEntry ->
            // Nav arg for CowDetail is Long, as Screen.CowDetail.createRoute expects Long
            val cowId = backStackEntry.arguments?.getLong("cowId") ?: 0L 
            CowDetailScreen(
                modifier = screenModifierNoPadding,
                cowId = cowId, // Screen itself expects Long
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddActivity.route) {
            AddActivityScreen(
                modifier = screenModifierNoPadding,
                editId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddActivityWithId.route) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")?.toLongOrNull()
            AddActivityScreen(
                modifier = screenModifierNoPadding,
                editId = activityId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CowList.route) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val valueString = backStackEntry.arguments?.getString("value")
            CowListScreen(
                modifier = screenModifierNoPadding,
                type = type,
                value = valueString,
                onCowClick = { cowId: Long -> navController.navigate(Screen.CowInfo.createRoute(cowId, Screen.CowList.createRoute(type, valueString))) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                onNavigateToHerds = { navController.navigate(Screen.HerdSelection.route) }, // This line uses Screen.HerdSelection.route
                onNavigateToAccountManagement = { navController.navigate(Screen.AccountManagement.route) },
                onNavigateToTagColors = { navController.navigate(Screen.TagColorsManagement.route) },
                onNavigateToActivityTypes = { navController.navigate(Screen.ActivityTypesManagement.route) }
            )
        }

        composable(Screen.SignIn.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authService, application.repository, application.syncService))
            SignInScreen(
                modifier = screenModifierNoPadding,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSignInSuccess = {
                    navController.navigate(Screen.MainPager.buildRoute(DASHBOARD_PAGE_INDEX)) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PastureDetail.route) { backStackEntry ->
            // Nav arg for PastureDetail is Long (retrieved), PastureDetailScreen expects String for its pastureId param
            val pastureIdLong = backStackEntry.arguments?.getLong("pastureId") ?: 0L 
            PastureDetailScreen(
                modifier = screenModifierNoPadding,
                pastureId = pastureIdLong.toString(), // Screen itself expects String
                onNavigateBack = { navController.popBackStack() },
                onCowClick = { cowId: Long ->
                    navController.navigate(
                        Screen.CowInfo.createRoute(
                            cowId = cowId,
                            returnToRoute = Screen.PastureDetail.createRoute(pastureIdLong) // Corrected: Screen.PastureDetail.createRoute expects Long
                        )
                    )
                },
                onCowEdit = { cowId: Long ->
                    navController.navigate(Screen.CowDetail.createRoute(cowId)) // Screen.CowDetail.createRoute expects Long
                }
            )
        }

        composable(Screen.AddBirth.route){
            AddBirthScreen(
                modifier = screenModifierNoPadding,
                onNavigateBack = { navController.popBackStack() }
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
    }
}
