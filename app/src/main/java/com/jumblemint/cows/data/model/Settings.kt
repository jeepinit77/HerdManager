package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val key: String,
    val value: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null,
    val lastSyncAt: Long? = null,
    val updatedBy: String? = null
) {
    fun toFirestoreMap(userId: String): Map<String, Any> {
        return mapOf(
            "key" to key,
            "value" to value,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "updatedBy" to userId
        )
    }
    
    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any>): Settings {
            return Settings(
                key = data["key"] as? String ?: id,
                value = data["value"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis(),
                firestoreId = id,
                lastSyncAt = data["updatedAt"] as? Long,
                updatedBy = data["updatedBy"] as? String
            )
        }
    }
}

