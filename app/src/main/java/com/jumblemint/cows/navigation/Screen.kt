package com.jumblemint.cows.navigation

// Using constants from CattleNavigation.kt for route definitions
// It might be cleaner to define these directly in the Screen objects if preferred.
const val MAIN_PAGER_ROUTE_TEMPLATE = "main_pager_route?initialPage={initialPage}"

sealed class Screen(val route: String, val title: String? = null, val hasOwnTopAppBar: Boolean = false) {
    data object Splash : Screen("splash_route", "Splash", true)
    data object SignIn : Screen("sign_in_route", "Sign In", true)
    data object MainPager : Screen(MAIN_PAGER_ROUTE_TEMPLATE, null, false) { // Title will be set by pager content
        fun buildRoute(initialPage: Int = 0): String {
            return route.replace("{initialPage}", initialPage.toString())
        }
    }
    data object Dashboard : Screen("dashboard_route", "Dashboard") // Now a page in MainPager
    data object Cows : Screen("cows_route", "Cows") // Now a page in MainPager
    data object Pastures : Screen("pastures_route", "Pastures") // Now a page in MainPager
    data object Activities : Screen("activities_route", "Activities") // Now a page in MainPager
    data object Notes : Screen("notes_route", "Notes") // Now a page in MainPager

    data object CowInfo : Screen("cow_info_route/{cowId}?returnToRoute={returnToRoute}", "Cow Info", true) {
        fun createRoute(cowId: Long, returnToRoute: String?): String {
            val baseRoute = route.replace("{cowId}", cowId.toString())
            return returnToRoute?.let { "$baseRoute?returnToRoute=$it" } ?: baseRoute
        }
    }
    data object CowDetail : Screen("cow_detail_route/{cowId}", "Cow Details", true) {
        fun createRoute(cowId: Long): String = route.replace("{cowId}", cowId.toString())
    }
    data object AddActivity : Screen("add_activity_route", "Add Activity", true) { // For navigating to add (no ID)
         fun createRoute(): String = route // No ID needed for initial navigation
    }
     data object AddActivityWithId : Screen("add_activity_route/{activityId}", "Edit Activity", true) { // For composable route definition
        fun createRoute(activityId: Long): String = route.replace("{activityId}", activityId.toString())
     }


    data object CowList : Screen("cow_list_route?type={type}&value={value}", "Cow List", true) {
        fun createRoute(type: String?, value: String?): String {
            var result = route
            result = type?.let { result.replace("{type}", it) } ?: result.replace("type={type}", "").replace("?&","?")
            result = value?.let { result.replace("{value}", it) } ?: result.replace("&value={value}", "")
            return result.trimEnd('?').trimEnd('&')
        }
    }
    data object Settings : Screen("settings_route", "Settings", true)
    data object PastureDetail : Screen("pasture_detail_route/{pastureId}", "Pasture Details", false) { // hasOwnTopAppBar depends on ID (0 for add)
        fun createRoute(pastureId: String): String = route.replace("{pastureId}", pastureId)
    }
    data object AddPasture : Screen("add_pasture_route", "Add Pasture", true)
    data object EditPasture : Screen("edit_pasture_route/{pastureId}", "Edit Pasture", true) {
        fun createRoute(pastureId: String): String = route.replace("{pastureId}", pastureId)
    }
    data object AddBirth : Screen("add_birth_route", "Add Birth", true)
    data object AccountManagement : Screen("account_management_route", "Account", true)
    data object TagColorsManagement : Screen("tag_colors_management_route", "Tag Colors", true)
    data object ActivityTypesManagement : Screen("activity_types_management_route", "Activity Types", true)
    data object Sync : Screen("sync_route", "Sync", true) 
    data object WorkingList : Screen("working_list_route", "Working List", true)
    data object HerdSelection : Screen("herd_selection_route", "Select Herd", true)

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
                CowInfo, CowDetail, AddActivity, AddActivityWithId, CowList, Settings, PastureDetail,
                AddPasture, EditPasture, AddBirth, AccountManagement, TagColorsManagement, ActivityTypesManagement,
                Sync, WorkingList, HerdSelection
            ).distinct() // Ensure no duplicates if some routes are similar (e.g. AddActivity vs AddActivityWithId for fromRoute)
        }
    }
}