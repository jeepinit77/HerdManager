package com.jumblemint.cows.navigation

// Using constants from CattleNavigation.kt for route definitions
// It might be cleaner to define these directly in the Screen objects if preferred.
const val MAIN_PAGER_ROUTE_TEMPLATE = "main_pager_route?initialPage={initialPage}"

sealed class Screen(val route: String, val title: String? = null, val hasOwnTopAppBar: Boolean = false) {
    data object Splash : Screen("splash_route", "Splash", false)
    data object SignIn : Screen("sign_in_route", "Sign In", false)
    data object MainPager : Screen(MAIN_PAGER_ROUTE_TEMPLATE, null, false) { // Title will be set by pager content
        fun buildRoute(initialPage: Int = 0): String {
            return route.replace("{initialPage}", initialPage.toString())
        }
    }
    data object Dashboard : Screen("dashboard_route", "Dashboard") // Now a page in MainPager
    data object Cows : Screen("cows_route", "Cattle") // Now a page in MainPager
    data object Pastures : Screen("pastures_route", "Pastures") // Now a page in MainPager
    data object Activities : Screen("activities_route", "Activities") // Now a page in MainPager
    data object Notes : Screen("notes_route", "Notes") // Now a page in MainPager

    data object CowInfo : Screen("cow_info_route/{cowId}?returnToRoute={returnToRoute}", "Cow Info", false) {
        fun createRoute(cowId: Long, returnToRoute: String?): String {
            val baseRoute = route.replace("{cowId}", cowId.toString())
            return returnToRoute?.let { "$baseRoute?returnToRoute=${android.net.Uri.encode(it)}" } ?: baseRoute
        }
    }
    data object CowDetail : Screen("cow_detail_route/{cowId}", "Cow Details", false) {
        fun createRoute(cowId: Long): String = route.replace("{cowId}", cowId.toString())
    }
    data object AddActivity : Screen("add_activity_route", "Add Activity", false) { // For navigating to add (no ID)
         fun createRoute(): String = route // No ID needed for initial navigation
    }
     data object AddActivityWithId : Screen("add_activity_route/{activityId}", "Edit Activity", false) { // For composable route definition
        fun createRoute(activityId: Long): String = route.replace("{activityId}", activityId.toString())
     }
    // New Screen for Activity Info
    data object ActivityInfo : Screen("activity_info_route/{activityId}", "Activity Info", false) {
        fun createRoute(activityId: Long): String = route.replace("{activityId}", activityId.toString())
    }


    data object CowList : Screen("cow_list_route?type={type}&value={value}", "Cow List", false) {
        fun createRoute(type: String?, value: String?): String {
            var result = route
            result = type?.let { result.replace("{type}", it) } ?: result.replace("type={type}", "").replace("?&","?")
            result = value?.let { result.replace("{value}", it) } ?: result.replace("&value={value}", "")
            return result.trimEnd('?').trimEnd('&')
        }
    }
    data object Settings : Screen("settings_route", "Settings", false)
    data object PastureDetail : Screen("pasture_detail_route/{pastureId}", "Pasture Details", false) { // hasOwnTopAppBar depends on ID (0 for add)
        fun createRoute(pastureId: String): String = route.replace("{pastureId}", pastureId)
    }
    data object AddPasture : Screen("add_pasture_route", "Add Pasture", false)
    data object EditPasture : Screen("edit_pasture_route/{pastureId}", "Edit Pasture", false) {
        fun createRoute(pastureId: String): String = route.replace("{pastureId}", pastureId)
    }
    data object AddBirth : Screen("add_birth_route", "Add Birth", false)
    data object AccountManagement : Screen("account_management_route", "Account", false)
    data object TagColorsManagement : Screen("tag_colors_management_route", "Tagging Colors", false)
    data object ActivityTypesManagement : Screen("activity_types_management_route", "Activity Types", false)
    data object BreedsManagement : Screen("breeds_management_route", "Breeds", false)
    data object ThemeSettings : Screen("theme_settings_route", "Theme Settings", false)
    data object Sync : Screen("sync_route", "Sync", false) 
    data object WorkingList : Screen("working_list_route", "Working List", false)
    data object HerdSelection : Screen("herd_selection_route", "Select Herd", false)
    data object NoteDetail : Screen("note_detail_route/{noteId}", "Note Details", false) {
        fun createRoute(noteId: Long): String = route.replace("{noteId}", noteId.toString())
    }
    data object NoteInfo : Screen("note_info_route/{noteId}", "Note Info", false) {
        fun createRoute(noteId: Long): String = route.replace("{noteId}", noteId.toString())
    }
    data object TodoList : Screen("todo_list_route", "ToDo List", false)

    companion object {
        fun fromRoute(route: String?): Screen? {
            if (route == null) return null
            val coreRoute = route.split("?").firstOrNull() ?: route
            // Check MainPager specifically due to its argument in the base route definition
            if (coreRoute == MainPager.route.split("?").firstOrNull()) return MainPager

            return sealedObjects.find { 
                // Ensure the route pattern correctly matches even with arguments for other screens
                val patternBase = it.route.split("?").firstOrNull() ?: it.route
                // Line 68 (approx): Explicitly using double backslashes for Kotlin string to pass single backslash to Regex
                val regexPattern = patternBase.replace(Regex("\\{.*?\\}"), "[^/]+") + "(/.*)?(\\?.*)?"
                 coreRoute.matches(Regex(regexPattern))
            }
        }

        val sealedObjects: List<Screen> by lazy {
            listOf(
                Splash, SignIn, MainPager, Dashboard, Cows, Pastures, Activities, Notes,
                CowInfo, CowDetail, AddActivity, AddActivityWithId, ActivityInfo, // Added ActivityInfo here
                CowList, Settings, PastureDetail,
                AddPasture, EditPasture, AddBirth, AccountManagement, TagColorsManagement, ActivityTypesManagement,
                BreedsManagement, ThemeSettings, Sync, WorkingList, HerdSelection, NoteDetail, NoteInfo, TodoList
            ).distinct() // Ensure no duplicates if some routes are similar (e.g. AddActivity vs AddActivityWithId for fromRoute)
        }
    }
}