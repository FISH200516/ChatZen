package com.fishai.chatzen.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val provider: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int
)
