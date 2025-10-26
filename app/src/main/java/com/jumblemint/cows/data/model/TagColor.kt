package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.*

@Entity(tableName = "tag_colors")
data class TagColor(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorValue: Int, // ARGB color value
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null,
    val lastSyncAt: Long? = null,
    val updatedBy: String? = null,
    val isDeleted: Boolean = false
) {
    fun toColor(): Color = Color(colorValue)

    fun toFirestoreMap(userId: String): Map<String, Any> {
        return mapOf(
            "name" to name,
            "colorValue" to colorValue,
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
            return UUID.nameUUIDFromBytes("tag_color::$normalized".toByteArray()).toString()
        }

        fun fromFirestoreMap(id: String, data: Map<String, Any>): TagColor {
            return TagColor(
                id = id,
                name = data["name"] as? String ?: "",
                colorValue = (data["colorValue"] as? Number)?.toInt() ?: Color.Gray.toArgb(),
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

        private val defaultColorSpecs = listOf(
            "Blue" to Color.Blue,
            "Green" to Color.Green,
            "Red" to Color.Red,
            "Orange" to Color(0xFFFFA500),
            "White" to Color.White,
            "Yellow" to Color.Yellow
        )

        private val additionalColorSpecs = listOf(
            "Brown" to Color(0xFF795548),
            "Black" to Color.Black,
            "Pink" to Color(0xFFFFC0CB),
            "Purple" to Color(0xFF9C27B0)
        )

        // Predefined colors that match common color names
        fun getDefaultColors(): List<TagColor> {
            val timestamp = System.currentTimeMillis()
            return defaultColorSpecs.map { (name, color) ->
                val id = stableIdForName(name)
                TagColor(
                    id = id,
                    name = name,
                    colorValue = color.toArgb(),
                    isDefault = true,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    firestoreId = id,
                    isDeleted = false
                )
            }
        }

        fun getAdditionalColorOptions(): List<TagColor> {
            return additionalColorSpecs.map { (name, color) ->
                val id = stableIdForName(name)
                TagColor(
                    id = id,
                    name = name,
                    colorValue = color.toArgb(),
                    isDefault = false,
                    firestoreId = id,
                    isDeleted = false
                )
            }
        }

        fun getWizardColorOptions(): List<TagColor> {
            return getDefaultColors() + getAdditionalColorOptions()
        }

        fun isSystemProvidedColor(name: String): Boolean {
            val normalized = name.trim().lowercase()
            return (defaultColorSpecs + additionalColorSpecs).any { (colorName, _) ->
                colorName.lowercase() == normalized
            }
        }
    }
}
