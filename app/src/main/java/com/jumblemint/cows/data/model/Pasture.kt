package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pastures")
data class Pasture(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val sizeAcres: Double? = null,
    
    // Multi-user and sync fields
    val herdId: String? = null, // Which herd this pasture belongs to
    val firestoreId: String? = null, // Firestore document ID for sync
    val lastSyncAt: Long = 0L, // Last time synced with Firestore
    val isDeleted: Boolean = false, // Soft delete flag for sync
    val createdBy: String? = null, // User UID who created this record
    val updatedBy: String? = null // User UID who last updated this record
)
