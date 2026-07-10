package com.karuhundeveloper.notification.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the history of a notification interception and its subsequent API push result.
 */
@Entity(
    tableName = "intercept_history",
    foreignKeys = [
        ForeignKey(
            entity = InterceptRule::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ruleId")]
)
data class InterceptHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Notification Details
    val appName: String,
    val notificationTitle: String,
    val notificationText: String,
    
    // Push Result Details
    val isSuccess: Boolean,
    val httpStatusCode: Int?,
    val errorMessage: String? = null
)
