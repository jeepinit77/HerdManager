package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "breeds")
data class Breed(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
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
            "isActive" to isActive,
            "isDefault" to isDefault,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "updatedBy" to userId,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        private fun stableIdForName(name: String): String {
            val normalized = name.trim().lowercase()
            return UUID.nameUUIDFromBytes("breed::$normalized".toByteArray()).toString()
        }

        fun fromFirestoreMap(id: String, data: Map<String, Any>): Breed {
            return Breed(
                id = id,
                name = data["name"] as? String ?: "",
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

        fun getDefaultBreeds(timestamp: Long = System.currentTimeMillis()): List<Breed> {
            val defaultNames = listOf(
                "Angus",
                "Hereford",
                "Holstein",
                "Charolais",
                "Simmental",
                "Limousin",
                "Brahman",
                "Shorthorn",
                "Jersey",
                "Guernsey",
                "Texas Longhorn",
                "Wagyu",
                "Crossbred"
            )

            return defaultNames.map { name ->
                val id = stableIdForName(name)
                Breed(
                    id = id,
                    name = name,
                    isDefault = true,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    firestoreId = id,
                    isDeleted = false
                )
            }
        }
    }
}