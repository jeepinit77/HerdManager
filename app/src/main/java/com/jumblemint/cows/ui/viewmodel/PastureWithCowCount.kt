package com.jumblemint.cows.ui.viewmodel

import androidx.room.Embedded
import com.jumblemint.cows.data.model.Pasture
import com.jumblemint.cows.data.model.Classification

data class PastureWithCowCount(
    @Embedded(prefix = "p_") // Added prefix "p_"
    val pasture: Pasture,
    val cowCount: Int
)

// Extended data class for UI that includes classification breakdown
data class PastureWithDetails(
    val pastureWithCount: PastureWithCowCount,
    val classificationBreakdown: Map<Classification, Int> = emptyMap()
)