package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale
import java.util.UUID

@Entity(
    tableName = "activity_type_configs",
    indices = [Index(value = ["name"], unique = true)]
)
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

        private data class ActivityTypeTemplate(
            val name: String,
            val displayName: String,
            val iconName: String
        )

        private val DEFAULT_ACTIVITY_TYPE_TEMPLATES = listOf(
            ActivityTypeTemplate("MOVED", "Moved", "DriveFileMove"),
            ActivityTypeTemplate("WEANED", "Weaned", "ChildCare"),
            ActivityTypeTemplate("SOLD", "Sold", "Sell"),
            ActivityTypeTemplate("DECEASED", "Deceased", "Dangerous"),
            ActivityTypeTemplate("WORKED", "Worked", "Handyman"),
            ActivityTypeTemplate("CASTRATED", "Castrated", "MedicalServices"),
            ActivityTypeTemplate("BRED", "Bred", "Favorite"),
            ActivityTypeTemplate("CALVED", "Calved", "BabyChangingStation"),
            ActivityTypeTemplate("VACCINATED", "Vaccinated", "Vaccines"),
            ActivityTypeTemplate("TREATED", "Treated", "LocalHospital"),
            ActivityTypeTemplate("WEIGHED", "Weighed", "Scale"),
            ActivityTypeTemplate("PURCHASED", "Purchased", "Pets"),
            ActivityTypeTemplate("HEALTH_CHECK", "Health Check", "Healing"),
            ActivityTypeTemplate("TAGGED", "Tagged", "ContentCut"),
            ActivityTypeTemplate("NOTE", "Note", "EditNote"),
            ActivityTypeTemplate("OTHER", "Other", "Assignment")
        )

        // Default activity types
        fun getDefaultActivityTypes(): List<ActivityTypeConfig> {
            return DEFAULT_ACTIVITY_TYPE_TEMPLATES.map { template ->
                ActivityTypeConfig(
                    id = getDefaultActivityTypeId(template.name),
                    name = template.name,
                    displayName = template.displayName,
                    iconName = template.iconName,
                    isDefault = true
                )
            }
        }

        fun getDefaultActivityTypeId(name: String): String {
            val normalized = name.trim().lowercase(Locale.US)
            return UUID.nameUUIDFromBytes(normalized.toByteArray()).toString()
        }
    }
}
