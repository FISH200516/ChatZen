package com.fishai.chatzen.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(usage: UsageEntity)

    @Query("SELECT * FROM usage_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getUsageInRange(startTime: Long, endTime: Long): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_logs WHERE timestamp BETWEEN :startTime AND :endTime AND model = :modelId ORDER BY timestamp ASC")
    fun getUsageInRangeForModel(startTime: Long, endTime: Long, modelId: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC")
    fun getAllUsage(): Flow<List<UsageEntity>>
}
