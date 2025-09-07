package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "activity_type_configs")
data class ActivityTypeConfig(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val displayName: String = name,
    val description: String? = null,
    val isActive: Boolean = true,
    val isDefault: Boolean = false, // Whether this is a system default type
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null,
    val lastSyncAt: Long? = null,
    val updatedBy: String? = null,
    val isDeleted: Boolean = false
) {
    fun toFirestoreMap(userId: String): Map<String, Any> {
        return mapOf(
            "name" to name,
            "displayName" to displayName,
            "description" to (description ?: ""),
            "isActive" to isActive,
            "isDefault" to isDefault,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "updatedBy" to userId,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any>): ActivityTypeConfig {
            return ActivityTypeConfig(
                id = id,
                name = data["name"] as? String ?: "",
                displayName = data["displayName"] as? String ?: (data["name"] as? String ?: ""),
                description = (data["description"] as? String)?.takeIf { it.isNotBlank() },
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

        // Default activity types that correspond to the existing ActivityType enum
        fun getDefaultActivityTypes(): List<ActivityTypeConfig> {
            return listOf(
                ActivityTypeConfig(name = "MOVED", displayName = "Moved", isDefault = true),
                ActivityTypeConfig(name = "WEANED", displayName = "Weaned", isDefault = true),
                ActivityTypeConfig(name = "SOLD", displayName = "Sold", isDefault = true),
                ActivityTypeConfig(name = "DECEASED", displayName = "Deceased", isDefault = true),
                ActivityTypeConfig(name = "CASTRATED", displayName = "Castrated", isDefault = true),
                ActivityTypeConfig(name = "BRED", displayName = "Bred", isDefault = true),
                ActivityTypeConfig(name = "CALVED", displayName = "Calved", isDefault = true),
                ActivityTypeConfig(name = "VACCINATED", displayName = "Vaccinated", isDefault = true),
                ActivityTypeConfig(name = "TREATED", displayName = "Treated", isDefault = true),
                ActivityTypeConfig(name = "WEIGHED", displayName = "Weighed", isDefault = true),
                ActivityTypeConfig(name = "OTHER", displayName = "Other", isDefault = true)
            )
        }
    }
}