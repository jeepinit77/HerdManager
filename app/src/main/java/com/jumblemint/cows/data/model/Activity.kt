package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = Cow::class,
            parentColumns = ["id"],
            childColumns = ["cowId"],
            onDelete = ForeignKey.CASCADE
        )
        // We are removing Pasture foreign keys for now due to type mismatch (Pasture.id is String)
        // If Pasture.id becomes Long, these can be re-added.
        // Or, if Cow.pastureId becomes String, these would need to reference Pasture.id as String.
        /*
        ForeignKey(
            entity = Pasture::class,
            parentColumns = ["id"],
            childColumns = ["fromPastureId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Pasture::class,
            parentColumns = ["id"],
            childColumns = ["toPastureId"],
            onDelete = ForeignKey.SET_NULL
        )
        */
    ],
    indices = [Index("cowId"), Index("groupId")] // Added groupId index
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cowId: Long,
    val date: LocalDate = LocalDate.now(),
    val activityType: ActivityType,
    val notes: String? = null,
    val fromPastureId: String? = null, // MODIFIED: Was Long?
    val toPastureId: String? = null,   // MODIFIED: Was Long?
    val details: String? = null, // For medication, sales info, etc.
    val groupId: String? = null, // NEW: Links related activities together
    
    // Multi-user and sync fields
    val herdId: String? = null, // Which herd this activity belongs to
    val firestoreId: String? = null, // Firestore document ID for sync
    val lastSyncAt: Long = 0L, // Last time synced with Firestore
    val isDeleted: Boolean = false, // Soft delete flag for sync
    val createdBy: String? = null, // User UID who created this record
    val updatedBy: String? = null // User UID who last updated this record
)
