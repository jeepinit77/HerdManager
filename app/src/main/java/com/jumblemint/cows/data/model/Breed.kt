package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale
import java.util.UUID

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

        private val DEFAULT_BREED_NAMES = listOf(
            "Angus",
            "Beefmaster",
            "Brahman",
            "Brown Swiss",
            "Charolais",
            "Dexter",
            "Gelbvieh",
            "Hereford",
            "Highland",
            "Holstein",
            "Jersey",
            "Limousin",
            "Shorthorn",
            "Simmental"
        )

        fun getDefaultBreeds(): List<Breed> {
            return DEFAULT_BREED_NAMES.map { name ->
                Breed(
                    id = getDefaultBreedId(name),
                    name = name,
                    isDefault = true
                )
            }
        }

        fun getDefaultBreedId(name: String): String {
            val normalized = name.trim().lowercase(Locale.US)
            return UUID.nameUUIDFromBytes(normalized.toByteArray()).toString()
        }
    }
}
