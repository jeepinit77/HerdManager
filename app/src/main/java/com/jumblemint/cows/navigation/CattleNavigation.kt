package com.jumblemint.cows.navigation

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Import for creating screenModifierWithPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
            val pastureId = backStackEntry.arguments?.getString("pastureId")?.toLongOrNull()
            CowsScreen(
                modifier = screenModifierWithPadding,
                pastureId = pastureId,
                onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId") },
                onCowEdit = { cowId -> navController.navigate("${Screen.CowDetail.route}/$cowId") },
                onAddCowClick = { navController.navigate("${Screen.CowDetail.route}/0") }
            )
        }

        composable(Screen.Cows.route) {
            CowsScreen(
                modifier = screenModifierWithPadding,
                pastureId = null,
                onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId") },
                onCowEdit = { cowId -> navController.navigate("${Screen.CowDetail.route}/$cowId") },
                onAddCowClick = { navController.navigate("${Screen.CowDetail.route}/0") }
            )
        }

        composable("${Screen.CowInfo.route}/{cowId}") { backStackEntry ->
            val cowId = backStackEntry.arguments?.getString("cowId")?.toLongOrNull() ?: 0L
            CowInfoScreen(
                modifier = screenModifierWithPadding,
                cowId = cowId,
                onNavigateBack = { navController.popBackStack() },
                onEditCow = { navController.navigate("${Screen.CowDetail.route}/$cowId") },
                onNavigateToCow = { selectedCowId -> navController.navigate("${Screen.CowInfo.route}/$selectedCowId") }
            )
        }

        composable("${Screen.CowDetail.route}/{cowId}") { backStackEntry ->
            val cowId = backStackEntry.arguments?.getString("cowId")?.toLongOrNull() ?: 0L
            CowDetailScreen( // You confirmed this screen accepts a modifier
                modifier = screenModifierNoPadding,
                cowId = cowId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Pastures.route) {
            PasturesScreen(
                modifier = screenModifierWithPadding,
                onNavigateToAddPasture = { navController.navigate("${Screen.PastureDetail.route}/0") },
                onNavigateToPastureDetails = { pastureId -> navController.navigate("${Screen.PastureDetail.route}/$pastureId") },
                onNavigateBack = { navController.popBackStack() }
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
                modifier = screenModifierNoPadding, // Assumes own Scaffold
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("${Screen.AddActivity.route}/{activityId}") { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")?.toLongOrNull()
            AddActivityScreen(
                modifier = screenModifierNoPadding, // Assumes own Scaffold
                editId = activityId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            val currentUser by application.authService.currentUser.collectAsState(initial = null)
            LaunchedEffect(currentUser) { if (currentUser != null && !currentUser!!.isLocalUser) { application.authService.startUserSync(application.syncService) } }
            ReportsScreen(
                modifier = screenModifierWithPadding,
                onShowList = { type, value ->
                    if (type == "workingList") { navController.navigate(Screen.WorkingList.route) }
                    else { navController.navigate(if (value != null) "${Screen.CowList.route}?type=$type&value=$value" else "${Screen.CowList.route}?type=$type") }
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
            val value = backStackEntry.arguments?.getString("value")
            CowListScreen(
                modifier = screenModifierWithPadding,
                type = type, value = value,
                onCowClick = { cowId: Long -> navController.navigate("${Screen.CowInfo.route}/$cowId") },
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
            val pastureIdArg = backStackEntry.arguments?.getString("pastureId") ?: "0"
            val contextL = LocalContext.current // Renamed to avoid conflict
            val applicationL = contextL.applicationContext as CattleApplication // Renamed
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

            if (pastureIdArg == "0" || pastureIdArg.isEmpty()) { // AddPasture
                AddPastureScreen(
                    modifier = screenModifierNoPadding, // Assumes own Scaffold
                    onAddPasture = { newPasture -> pasturesViewModel.insertNewPasture(newPasture); navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            } else { // PastureDetail (viewing existing)
                PastureDetailScreen(
                    modifier = screenModifierWithPadding,
                    pastureId = pastureIdArg,
                    onNavigateBack = { navController.popBackStack() },
                    onCowClick = { cowId -> navController.navigate("${Screen.CowInfo.route}/$cowId") },
                    onCowEdit = { cowId -> navController.navigate("${Screen.CowDetail.route}/$cowId") },
                    onEditPasture = { /* No-op */ }
                )
            }
        }

        composable(Screen.AddBirth.route) {
            AddBirthScreen(
                modifier = screenModifierNoPadding, // Assumes own Scaffold
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
                modifier = screenModifierNoPadding, // Assumes own Scaffold
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) }
            )
        }

        composable(Screen.AccountManagement.route) {
            AccountManagementScreen(
                modifier = screenModifierNoPadding, // Assumes own Scaffold
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TagColorsManagement.route) {
            TagColorsManagementScreen(
                modifier = screenModifierNoPadding, // Assumes own Scaffold
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ActivityTypesManagement.route) {
            ActivityTypesManagementScreen(
                modifier = screenModifierNoPadding, // Assumes own Scaffold
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Sealed class Screen definition remains 
sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object SignIn : Screen("sign_in", "Sign In")
    object Dashboard : Screen("dashboard", "Dashboard")
    object CowList : Screen("cow_list", "Cows") 
    object Cows : Screen("cows", "Cows")
    object CowInfo : Screen("cow_info", "Cow Info")
    object CowDetail : Screen("cow_detail", "Cow Details") 
    object Pastures : Screen("pastures", "Pastures")
    object PastureDetail : Screen("pasture_detail", "Pasture Details") 
    object Activities : Screen("activities", "Activities")
    object AddActivity : Screen("add_activity", "Add Activity")
    object Settings : Screen("settings", "Settings")
    object Notes : Screen("notes", "Notes")
    object AddBirth : Screen("add_birth", "Add Birth")
    object WorkingList : Screen("working_list", "Working List")
    object AccountManagement : Screen("account_management", "Account Management")
    object TagColorsManagement : Screen("tag_colors_management", "Tag Colors Management")
    object ActivityTypesManagement : Screen("activity_types_management", "Activity Types Management")
    object Sync : Screen("sync", "Sync Details")
}
