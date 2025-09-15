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
    ],
    indices = [Index("cowId"), Index("groupId")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cowId: Long, // This likely needs to change to a List<Long> or be handled differently for activities involving multiple cows.
                     // For now, keeping as is, but ActivityInfoScreen implies multiple cows.
                     // This single cowId might be the primary cow or an old field.
    val date: LocalDate = LocalDate.now(),
    val activityType: ActivityType,
    val notes: String? = null,
    val fromPastureId: String? = null,
    val toPastureId: String? = null,
    val details: String? = null,
    val groupId: String? = null,

    // Fields from ActivityInfoScreen
    val result: String? = null,
    val quantity: Double? = null,
    val technician: String? = null,
    val cost: Double? = null,

    // Multi-user and sync fields
    val herdId: String? = null,
    val firestoreId: String? = null,
    val lastSyncAt: Long = 0L,
    val isDeleted: Boolean = false,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    // New field to store associated cow IDs for activities involving multiple cows
    val cowIds: List<Long> = emptyList() // Added to match usage in ActivityInfoViewModel
)
