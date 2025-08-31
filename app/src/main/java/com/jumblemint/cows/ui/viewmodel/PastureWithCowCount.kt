package com.jumblemint.cows.ui.viewmodel

import androidx.room.Embedded
import com.jumblemint.cows.data.model.Pasture

data class PastureWithCowCount(
    @Embedded(prefix = "p_") // Added prefix "p_"
    val pasture: Pasture,
    val cowCount: Int
)