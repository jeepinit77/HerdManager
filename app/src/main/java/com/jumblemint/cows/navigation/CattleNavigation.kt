package com.jumblemint.cows.navigation

import android.app.Application
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.jumblemint.cows.data.database.CattleDatabase
import com.jumblemint.cows.data.repository.CattleRepository
import com.jumblemint.cows.ui.screens.activities.ActivitiesScreen
import com.jumblemint.cows.ui.screens.activities.AddActivityScreen
import com.jumblemint.cows.ui.screens.activities.AddBirthScreen
import com.jumblemint.cows.ui.screens.account.AccountManagementScreen
import com.jumblemint.cows.ui.screens.auth.LoginScreen
import com.jumblemint.cows.ui.screens.auth.SignInScreen
import com.jumblemint.cows.ui.screens.cows.CowDetailScreen
import com.jumblemint.cows.ui.screens.cows.CowInfoScreen
import com.jumblemint.cows.ui.screens.cows.CowListScreen
import com.jumblemint.cows.ui.screens.cows.CowsScreen
import com.jumblemint.cows.ui.screens.notes.NotesScreen
import com.jumblemint.cows.ui.screens.pastures.AddPastureScreen
import com.jumblemint.cows.ui.screens.pastures.PasturesScreen
import com.jumblemint.cows.ui.screens.pastures.PastureDetailScreen
import com.jumblemint.cows.ui.screens.reports.ReportsScreen
import com.jumblemint.cows.ui.screens.settings.SettingsScreen
import com.jumblemint.cows.ui.screens.settings.TagColorsManagementScreen
import com.jumblemint.cows.ui.screens.settings.ActivityTypesManagementScreen
import com.jumblemint.cows.ui.screens.sync.SyncDetailsScreen
import com.jumblemint.cows.ui.screens.workinglist.WorkingListScreen
import com.jumblemint.cows.ui.viewmodel.*

@Composable
fun CattleNavigation(
    navController: NavHostController,
    mainScaffoldPadding: PaddingValues
) {
    val context = LocalContext.current
    val application = context.applicationContext as CattleApplication

    val screenModifierWithPadding = Modifier
        .padding(mainScaffoldPadding)
        .fillMaxSize()

    val screenModifierNoPadding = Modifier.fillMaxSize()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application.authService, application.repository, application.syncService))
            LoginScreen(
                modifier = screenModifierNoPadding,
                authViewModel = authViewModel,
                onLoginSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } }
            )
        }

        composable("${Screen.Cows.route}?pastureId={pastureId}") { backStackEntry ->
            val pastureIdString = backStackEntry.arguments?.getString("pastureId")
            val pastureId = pastureIdString?.toLongOrNull() 
            CowsScreen(
                modifier = screenModifierWithPadding,
                pastureId = pastureId, 
                onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId?returnToRoute=${Screen.Cows.route}") }, 
                onCowEdit = { cowId -> navController.navigate("${Screen.CowDetail.route}/$cowId") }, 
                onAddCowClick = { navController.navigate("${Screen.CowDetail.route}/0") } 
            )
        }

        composable(Screen.Cows.route) {
            CowsScreen(
                modifier = screenModifierWithPadding,
                pastureId = null, 
                onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId?returnToRoute=${Screen.Cows.route}") }, 
                onCowEdit = { cowId -> navController.navigate("${Screen.CowDetail.route}/$cowId") }, 
                onAddCowClick = { navController.navigate("${Screen.CowDetail.route}/0") } 
            )
        }

        composable(
            route = "${Screen.CowInfo.route}/{cowId}?returnToRoute={returnToRoute}",
            arguments = listOf(
                navArgument("cowId") { type = NavType.LongType }, 
                navArgument("returnToRoute") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
            }
        ) { backStackEntry ->
            val cowId = backStackEntry.arguments?.getLong("cowId") ?: 0L 
            val returnToRouteArg = backStackEntry.arguments?.getString("returnToRoute")
            
            CowInfoScreen(
                modifier = screenModifierNoPadding, 
                cowId = cowId,
                onNavigateBack = { navController.popBackStack() },
                onEditCow = { navController.navigate("${Screen.CowDetail.route}/$cowId") }, 
                onNavigateToCow = { selectedCowId -> 
                    if (cowId != selectedCowId) { 
                        val routePrefix = "${Screen.CowInfo.route}/$selectedCowId"
                        val finalRoute = if (!returnToRouteArg.isNullOrEmpty()) {
                            "$routePrefix?returnToRoute=$returnToRouteArg"
                        } else {
                            routePrefix
                        }
                        navController.navigate(finalRoute)
                    }
                },
                onCloseFlow = {
                    if (!returnToRouteArg.isNullOrEmpty()) {
                        val success = navController.popBackStack(returnToRouteArg, inclusive = false)
                        if (!success) {
                            navController.navigate(returnToRouteArg) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    } else {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("${Screen.CowDetail.route}/{cowId}") { backStackEntry ->
            val cowIdString = backStackEntry.arguments?.getString("cowId")
            val cowId = cowIdString?.toLongOrNull() ?: 0L 
            CowDetailScreen( 
                modifier = screenModifierNoPadding,
                cowId = cowId, 
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Pastures.route) {
            PasturesScreen(
                modifier = screenModifierNoPadding, 
                onNavigateToAddPasture = { navController.navigate("${Screen.PastureDetail.route}/0") }, 
                onNavigateToPastureDetails = { pastureId -> navController.navigate("${Screen.PastureDetail.route}/$pastureId") }, 
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = { 
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Activities.route) {
            ActivitiesScreen(
                modifier = screenModifierWithPadding,
                onAddActivityClick = { navController.navigate(Screen.AddActivity.route) }, 
                onEditActivityClick = { activity -> navController.navigate("${Screen.AddActivity.route}/${activity.id}") } 
            )
        }

        composable(Screen.AddActivity.route) { 
            AddActivityScreen(
                modifier = screenModifierNoPadding, 
                editId = null, 
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("${Screen.AddActivity.route}/{activityId}") { backStackEntry -> 
            val activityIdString = backStackEntry.arguments?.getString("activityId")
            val activityId = activityIdString?.toLongOrNull() 
            AddActivityScreen(
                modifier = screenModifierNoPadding, 
                editId = activityId, 
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            val currentUser by application.authService.currentUser.collectAsState(initial = null)
            LaunchedEffect(currentUser) { if (currentUser != null && !currentUser!!.isLocalUser) { application.authService.startUserSync(application.syncService) } }
            ReportsScreen(
                modifier = screenModifierWithPadding,
                onShowList = { type, value -> // value here is Int?
                    if (type == "workingList") { 
                        navController.navigate(Screen.WorkingList.route) 
                    } else { 
                        // ERROR FIX: Ensure 'value' (Int?) is converted to String for the route parameter
                        val valueQueryParam = if (value != null) "&value=${value.toString()}" else "" 
                        navController.navigate("${Screen.CowList.route}?type=$type$valueQueryParam")
                    }
                },
                onNavigateToAddBirth = { navController.navigate(Screen.AddBirth.route) }
            )
        }

        composable(Screen.Notes.route) {
            NotesScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("${Screen.CowList.route}?type={type}&value={value}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val valueString = backStackEntry.arguments?.getString("value")
            CowListScreen(
                modifier = screenModifierWithPadding,
                type = type, 
                value = valueString, 
                onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId?returnToRoute=${Screen.CowList.route}") }, 
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                onNavigateToHerds = null,
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
                onSignInSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.SignIn.route) { inclusive = true } } }
            )
        }

        composable("${Screen.PastureDetail.route}/{pastureId}") { backStackEntry ->
            val pastureIdString = backStackEntry.arguments?.getString("pastureId") 
            val pastureIdArg = pastureIdString ?: "0"
            
            val contextL = LocalContext.current 
            val applicationL = contextL.applicationContext as CattleApplication 
            val database = CattleDatabase.getDatabase(applicationL)
            val repository = remember {
                CattleRepository(
                    cowDao = database.cowDao(), pastureDao = database.pastureDao(),
                    activityDao = database.activityDao(), settingsDao = database.settingsDao(),
                    noteDao = database.noteDao(), userDao = database.userDao(),
                    herdDao = database.herdDao(), herdMemberDao = database.herdMemberDao(),
                    tagColorDao = database.tagColorDao(), activityTypeConfigDao = database.activityTypeConfigDao()
                )
            }
            val pasturesViewModel: PasturesViewModel = viewModel(factory = PasturesViewModelFactory(applicationL, repository))

            if (pastureIdArg == "0") { 
                AddPastureScreen(
                    modifier = screenModifierNoPadding, 
                    onAddPasture = { newPasture -> pasturesViewModel.insertNewPasture(newPasture); navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            } else { 
                PastureDetailScreen(
                    modifier = screenModifierWithPadding,
                    pastureId = pastureIdArg, 
                    onNavigateBack = { navController.popBackStack() },
                    onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId?returnToRoute=${Screen.PastureDetail.route}/$pastureIdArg") }, 
                    onCowEdit = { cowId -> navController.navigate("${Screen.CowDetail.route}/$cowId") },
                    onEditPasture = { /* No-op */ }
                )
            }
        }

        composable(Screen.AddBirth.route) {
            AddBirthScreen(
                modifier = screenModifierNoPadding, 
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkingList.route) {
            WorkingListScreen(
                modifier = screenModifierWithPadding,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Sync.route) {
            SyncDetailsScreen(
                modifier = screenModifierNoPadding, 
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) }
            )
        }

        composable(Screen.AccountManagement.route) {
            AccountManagementScreen(
                modifier = screenModifierNoPadding, 
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TagColorsManagement.route) {
            TagColorsManagementScreen(
                modifier = screenModifierNoPadding, 
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ActivityTypesManagement.route) {
            ActivityTypesManagementScreen(
                modifier = screenModifierNoPadding, 
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class Screen(val route: String, val title: String, val hasOwnTopAppBar: Boolean = false) {
    object Login : Screen("login", "Login", true)
    object SignIn : Screen("sign_in", "Sign In", true)
    object Dashboard : Screen("dashboard", "Dashboard")
    object CowList : Screen("cow_list", "Cows") 
    object Cows : Screen("cows", "Cows") 
    object CowInfo : Screen("cow_info", "Cow Info", true) 
    object CowDetail : Screen("cow_detail", "Cow Details", true) 
    object Pastures : Screen("pastures", "Pastures", true) 
    object PastureDetail : Screen("pasture_detail", "Pasture Details", false) 
    object Activities : Screen("activities", "Activities")
    object AddActivity : Screen("add_activity", "Add Activity", true) 
    object Settings : Screen("settings", "Settings")
    object Notes : Screen("notes", "Notes")
    object AddBirth : Screen("add_birth", "Add Birth", true)
    object WorkingList : Screen("working_list", "Working List")
    object AccountManagement : Screen("account_management", "Account Management", true)
    object TagColorsManagement : Screen("tag_colors_management", "Tag Colors Management", true)
    object ActivityTypesManagement : Screen("activity_types_management", "Activity Types Management", true)
    object Sync : Screen("sync", "Sync Details", true)

    companion object {
        val allScreens: List<Screen> by lazy {
            Screen::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }

        fun fromRoute(routePattern: String?): Screen? {
            if (routePattern == null) return null
            return allScreens.find { screen ->
                val baseScreenRoute = screen.route.substringBefore("/").substringBefore("?")
                val patternBaseRoute = routePattern.substringBefore("/").substringBefore("?")
                baseScreenRoute == patternBaseRoute
            }
        }
    }
}
