package com.jumblemint.cows.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jumblemint.cows.ui.screens.activities.ActivitiesScreen
import com.jumblemint.cows.ui.screens.activities.AddActivityScreen
import com.jumblemint.cows.ui.screens.cows.CowsScreen
import com.jumblemint.cows.ui.screens.cows.CowDetailScreen
import com.jumblemint.cows.ui.screens.cows.CowListScreen
import com.jumblemint.cows.ui.screens.pastures.PasturesScreen
import com.jumblemint.cows.ui.screens.reports.ReportsScreen
import com.jumblemint.cows.ui.screens.notes.NotesScreen
import com.jumblemint.cows.ui.screens.settings.SettingsScreen
// Import Text if you use the placeholder for PastureDetailScreen
// import androidx.compose.material3.Text

@Composable
fun CattleNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable("${Screen.Cows.route}?pastureId={pastureId}") { backStackEntry ->
            val pastureId = backStackEntry.arguments?.getString("pastureId")?.toLongOrNull()
            CowsScreen(
                pastureId = pastureId,
                onCowClick = { cowId ->
                    navController.navigate("${Screen.CowDetail.route}/$cowId")
                },
                onAddCowClick = {
                    navController.navigate("${Screen.CowDetail.route}/0")
                }
            )
        }
        
        composable(Screen.Cows.route) {
            CowsScreen(
                pastureId = null,
                onCowClick = { cowId ->
                    navController.navigate("${Screen.CowDetail.route}/$cowId")
                },
                onAddCowClick = {
                    navController.navigate("${Screen.CowDetail.route}/0")
                }
            )
        }
        
        composable("${Screen.CowDetail.route}/{cowId}") { backStackEntry ->
            val cowId = backStackEntry.arguments?.getString("cowId")?.toLongOrNull() ?: 0L
            CowDetailScreen(
                cowId = cowId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Pastures.route) {
            PasturesScreen(
                onNavigateToAddPasture = {
                    navController.navigate("${Screen.PastureDetail.route}/0") // "0" for new pasture
                },
                onNavigateToPastureDetails = { pastureId -> // pastureId is String
                    navController.navigate("${Screen.PastureDetail.route}/$pastureId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Activities.route) {
            ActivitiesScreen(
                onAddActivityClick = {
                    navController.navigate(Screen.AddActivity.route)
                }
            )
        }
        
        composable(Screen.AddActivity.route) {
            AddActivityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Dashboard.route) {
            ReportsScreen(
                onShowList = { type, value ->
                    val route = if (value != null) {
                        "${Screen.CowList.route}?type=$type&value=$value"
                    } else {
                        "${Screen.CowList.route}?type=$type"
                    }
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.Notes.route) {
            NotesScreen()
        }

        composable("${Screen.CowList.route}?type={type}&value={value}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val value = backStackEntry.arguments?.getString("value")
            CowListScreen(
                type = type,
                value = value,
                onCowClick = { cowId: Long -> navController.navigate("${Screen.CowDetail.route}/$cowId") },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // Placeholder for Pasture Detail / Add Screen
        composable("${Screen.PastureDetail.route}/{pastureId}") { backStackEntry ->
            val pastureId = backStackEntry.arguments?.getString("pastureId") ?: "0"
            // TODO: Replace with your actual PastureDetailScreen composable
            androidx.compose.material3.Text("Placeholder for PastureDetailScreen: pastureId = $pastureId. Navigate back to implement.")
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object CowList : Screen("cow_list", "Cows")
    object Cows : Screen("cows", "Cows")
    object CowDetail : Screen("cow_detail", "Cow Details")
    object Pastures : Screen("pastures", "Pastures")
    object PastureDetail : Screen("pasture_detail", "Pasture Details") // <<< ADDED
    object Activities : Screen("activities", "Activities")
    object AddActivity : Screen("add_activity", "Add Activity")
    object Settings : Screen("settings", "Settings")
    object Notes : Screen("notes", "Notes")
}
