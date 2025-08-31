package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pastures")
data class Pasture(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?, // This line is critical
    val sizeAcres: Double?
)
