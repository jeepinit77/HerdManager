package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "herds")
data class Herd(
    @PrimaryKey
    val id: String, // Firestore document ID
    val name: String,
    val description: String? = null,
    val ownerId: String, // User UID who created the herd
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)