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

        fun getDefaultBreeds(): List<Breed> {
            return listOf(
                Breed(name = "Angus", isDefault = true),
                Breed(name = "Hereford", isDefault = true),
                Breed(name = "Holstein", isDefault = true),
                Breed(name = "Charolais", isDefault = true),
                Breed(name = "Simmental", isDefault = true),
                Breed(name = "Limousin", isDefault = true),
                Breed(name = "Brahman", isDefault = true),
                Breed(name = "Shorthorn", isDefault = true),
                Breed(name = "Jersey", isDefault = true),
                Breed(name = "Guernsey", isDefault = true),
                Breed(name = "Texas Longhorn", isDefault = true),
                Breed(name = "Wagyu", isDefault = true),
                Breed(name = "Crossbred", isDefault = true)
            )
        }
    }
}