package com.jumblemint.cows.ui.screens.reports

import com.jumblemint.cows.data.model.Status

sealed class DashboardFilter {
    data class ByStatus(val status: Status?) : DashboardFilter() // null means All
    data class ByClassification(val classification: String) : DashboardFilter()
    data class ByPasture(val pastureId: Long?) : DashboardFilter() // null for Unassigned
    data class AgeGroup(val range: String) : DashboardFilter() // e.g., "UNDER_1","1_5","5_10","10_PLUS"
    data object NotCalvedIn9Months : DashboardFilter()
    data object CalvedInPast9Months : DashboardFilter()
}