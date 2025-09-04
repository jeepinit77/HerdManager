package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "cows")
data class Cow(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String? = null,
    val tagNumber: String? = null,
    val tagColor: String? = null, // Assuming this is a color name or hex
    val birthDate: LocalDate? = null,
    val gender: Gender = Gender.TBD,
    val classification: Classification = Classification.CALF,
    val colorMarkings: String? = null,
    val motherId: Long? = null, // Assuming Cow IDs are Long
    val fatherId: Long? = null, // Assuming Cow IDs are Long
    val status: Status = Status.ACTIVE,
    val pastureId: String? = null, // MODIFIED: Was Long?
    val photos: List<String> = emptyList(), // List of photo URIs or paths
    val isWatched: Boolean = false,
    val createdAt: LocalDate? = LocalDate.now(),
    val updatedAt: LocalDate? = LocalDate.now(),
    
    // Multi-user and sync fields
    val herdId: String? = null, // Which herd this cow belongs to
    val firestoreId: String? = null, // Firestore document ID for sync
    val lastSyncAt: Long = 0L, // Last time synced with Firestore
    val isDeleted: Boolean = false, // Soft delete flag for sync
    val createdBy: String? = null, // User UID who created this record
    val updatedBy: String? = null // User UID who last updated this record
)
