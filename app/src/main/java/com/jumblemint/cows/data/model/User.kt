package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String, // Firebase Auth UID or local user ID
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0L,
    val isLocalUser: Boolean = false, // True for local users, false for Google users
    val isPremium: Boolean = false // Premium status for feature access
)