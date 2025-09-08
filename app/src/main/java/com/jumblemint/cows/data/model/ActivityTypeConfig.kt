package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "activity_type_configs")
data class ActivityTypeConfig(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String, // Internal, non-display name, should be unique for custom types
    val displayName: String = name,
    val description: String? = null,
    val iconName: String? = null, // <<< ADDED FIELD (e.g., "Vaccines", "DriveFileMove")
    val isActive: Boolean = true,
    val isDefault: Boolean = false, // True if this is a system-provided default type (cannot be deleted by user, only reset)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null, // Firestore document ID for sync
    val lastSyncAt: Long? = null, // Last time synced with Firestore
    val updatedBy: String? = null, // User UID who last updated this record
    val isDeleted: Boolean = false // Soft delete flag for sync
) {
    fun toFirestoreMap(userId: String): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "displayName" to displayName,
            "description" to description,
            "iconName" to iconName, // <<< ADDED TO MAP
            "isActive" to isActive,
            "isDefault" to isDefault,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "updatedBy" to userId,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any?>): ActivityTypeConfig {
            return ActivityTypeConfig(
                id = id,
                name = data["name"] as? String ?: "",
                displayName = data["displayName"] as? String ?: (data["name"] as? String ?: ""),
                description = (data["description"] as? String)?.takeIf { it.isNotBlank() },
                iconName = data["iconName"] as? String, // <<< ADDED FROM MAP
                isActive = data["isActive"] as? Boolean ?: true,
                isDefault = data["isDefault"] as? Boolean ?: false,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                firestoreId = id,
                lastSyncAt = data["updatedAt"] as? Long,
                updatedBy = data["updatedBy"] as? String,
                isDeleted = data["isDeleted"] as? Boolean ?: false
            )
        }

        // Default activity types
        fun getDefaultActivityTypes(): List<ActivityTypeConfig> {
            return listOf(
                ActivityTypeConfig(name = "MOVED", displayName = "Moved", isDefault = true, iconName = "DriveFileMove"),
                ActivityTypeConfig(name = "WEANED", displayName = "Weaned", isDefault = true, iconName = "ChildCare"),
                ActivityTypeConfig(name = "SOLD", displayName = "Sold", isDefault = true, iconName = "Sell"),
                ActivityTypeConfig(name = "DECEASED", displayName = "Deceased", isDefault = true, iconName = "Dangerous"),
                ActivityTypeConfig(name = "CASTRATED", displayName = "Castrated", isDefault = true, iconName = "MedicalServices"),
                ActivityTypeConfig(name = "BRED", displayName = "Bred", isDefault = true, iconName = "Favorite"),
                ActivityTypeConfig(name = "CALVED", displayName = "Calved", isDefault = true, iconName = "BabyChangingStation"),
                ActivityTypeConfig(name = "VACCINATED", displayName = "Vaccinated", isDefault = true, iconName = "Vaccines"),
                ActivityTypeConfig(name = "TREATED", displayName = "Treated", isDefault = true, iconName = "LocalHospital"),
                ActivityTypeConfig(name = "WEIGHED", displayName = "Weighed", isDefault = true, iconName = "Scale"),
                ActivityTypeConfig(name = "PURCHASED", displayName = "Purchased", isDefault = true, iconName = "Pets"),
                ActivityTypeConfig(name = "HEALTH_CHECK", displayName = "Health Check", isDefault = true, iconName = "Healing"),
                ActivityTypeConfig(name = "TAGGED", displayName = "Tagged", isDefault = true, iconName = "ContentCut"),
                ActivityTypeConfig(name = "NOTE", displayName = "Note", isDefault = true, iconName = "EditNote"),
                ActivityTypeConfig(name = "OTHER", displayName = "Other", isDefault = true, iconName = "Assignment") // Default icon for OTHER
            )
        }
    }
}