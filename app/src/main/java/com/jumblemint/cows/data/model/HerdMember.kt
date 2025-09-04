package com.jumblemint.cows.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "herd_members",
    foreignKeys = [
        ForeignKey(
            entity = Herd::class,
            parentColumns = ["id"],
            childColumns = ["herdId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["uid"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["herdId"]),
        Index(value = ["userId"]),
        Index(value = ["herdId", "userId"], unique = true)
    ]
)
data class HerdMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val herdId: String,
    val userId: String,
    val role: HerdRole = HerdRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis(),
    val invitedBy: String? = null,
    val isActive: Boolean = true
)

enum class HerdRole {
    OWNER,    // Full access, can manage members
    MEMBER    // Read/write access to herd data
}