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

        // Predefined colors that match common color names
        fun getDefaultColors(): List<TagColor> {
            return listOf(
                TagColor(name = "Red", colorValue = Color.Red.toArgb(), isDefault = true),
                TagColor(name = "Blue", colorValue = Color.Blue.toArgb(), isDefault = true),
                TagColor(name = "Green", colorValue = Color.Green.toArgb(), isDefault = true),
                TagColor(name = "Yellow", colorValue = Color.Yellow.toArgb(), isDefault = true),
                TagColor(name = "Orange", colorValue = Color(0xFFFFA500).toArgb(), isDefault = true),
                TagColor(name = "Purple", colorValue = Color(0xFF800080).toArgb(), isDefault = true),
                TagColor(name = "Pink", colorValue = Color(0xFFFFC0CB).toArgb(), isDefault = true),
                TagColor(name = "White", colorValue = Color.White.toArgb(), isDefault = true),
                TagColor(name = "Black", colorValue = Color.Black.toArgb(), isDefault = true),
                TagColor(name = "Brown", colorValue = Color(0xFF8B4513).toArgb(), isDefault = true)
            )
        }
    }
}