package com.jumblemint.cows.util

import com.jumblemint.cows.data.model.Cow
import java.time.LocalDate
import java.time.Period

object AgeRangeKeys {
    const val AGE_0_6_M = "AGE_0_6_M"
    const val AGE_6_12_M = "AGE_6_12_M"
    const val AGE_1_2_Y = "AGE_1_2_Y"
    const val AGE_2_5_Y = "AGE_2_5_Y"
    const val AGE_5_10_Y = "AGE_5_10_Y"
    const val AGE_10_PLUS_Y = "AGE_10_PLUS_Y"

    // For dashboard/report links that might use older keys initially
    // These can be phased out once navigation is updated
    const val LEGACY_UNDER_1 = "UNDER_1" 
    const val LEGACY_1_5 = "1_5"
    const val LEGACY_5_10 = "5_10"
    const val LEGACY_10_PLUS = "10_PLUS"
}

data class AgeRange(val key: String, val label: String)

object AgeUtils {
    val ageRanges = listOf(
        AgeRange(AgeRangeKeys.AGE_0_6_M, "0-6 Months"),
        AgeRange(AgeRangeKeys.AGE_6_12_M, "6-12 Months"),
        AgeRange(AgeRangeKeys.AGE_1_2_Y, "1-2 Years"),
        AgeRange(AgeRangeKeys.AGE_2_5_Y, "2-5 Years"),
        AgeRange(AgeRangeKeys.AGE_5_10_Y, "5-10 Years"),
        AgeRange(AgeRangeKeys.AGE_10_PLUS_Y, "10+ Years")
    )

    fun getLabel(key: String?): String {
        return when (key) {
            AgeRangeKeys.AGE_0_6_M -> "0-6 Months"
            AgeRangeKeys.AGE_6_12_M -> "6-12 Months"
            AgeRangeKeys.AGE_1_2_Y -> "1-2 Years"
            AgeRangeKeys.AGE_2_5_Y -> "2-5 Years"
            AgeRangeKeys.AGE_5_10_Y -> "5-10 Years"
            AgeRangeKeys.AGE_10_PLUS_Y -> "10+ Years"
            // Handle legacy keys for smoother transition in CowListScreen title
            AgeRangeKeys.LEGACY_UNDER_1 -> "Under 1 Year"
            AgeRangeKeys.LEGACY_1_5 -> "1-5 Years"
            AgeRangeKeys.LEGACY_5_10 -> "5-10 Years"
            AgeRangeKeys.LEGACY_10_PLUS -> "Over 10 Years"
            else -> "By Age"
        }
    }

    fun cowMatchesAgeRangeKey(cow: Cow, rangeKey: String, today: LocalDate): Boolean {
        cow.birthDate?.let {
            val ageInMonths = Period.between(it, today).toTotalMonths().toInt()
            return when (rangeKey) {
                AgeRangeKeys.AGE_0_6_M -> ageInMonths in 0..6
                AgeRangeKeys.AGE_6_12_M -> ageInMonths in 7..12
                AgeRangeKeys.AGE_1_2_Y -> ageInMonths in 13..24
                AgeRangeKeys.AGE_2_5_Y -> ageInMonths in 25..60
                AgeRangeKeys.AGE_5_10_Y -> ageInMonths in 61..120
                AgeRangeKeys.AGE_10_PLUS_Y -> ageInMonths > 120
                // Handle legacy keys for CowListScreen's filterByAgeGroup
                AgeRangeKeys.LEGACY_UNDER_1 -> ageInMonths < 12 // Approx under 1 year
                AgeRangeKeys.LEGACY_1_5 -> ageInMonths in 12..59 // Approx 1-5 years
                AgeRangeKeys.LEGACY_5_10 -> ageInMonths in 60..119 // Approx 5-10 years
                AgeRangeKeys.LEGACY_10_PLUS -> ageInMonths >= 120 // Approx 10+ years
                else -> false
            }
        }
        return false
    }
}
