package com.fishai.chatzen.data.repository

import com.fishai.chatzen.data.database.UsageDao
import com.fishai.chatzen.data.database.UsageEntity
import kotlinx.coroutines.flow.Flow

class UsageRepository(private val usageDao: UsageDao) {

    suspend fun logUsage(provider: String, model: String, inputTokens: Int, outputTokens: Int) {
        val usage = UsageEntity(
            timestamp = System.currentTimeMillis(),
            provider = provider,
            model = model,
            inputTokens = inputTokens,
            outputTokens = outputTokens
        )
        usageDao.insert(usage)
    }

    fun getUsageInRange(startTime: Long, endTime: Long): Flow<List<UsageEntity>> {
        return usageDao.getUsageInRange(startTime, endTime)
    }

    fun getUsageInRangeForModel(startTime: Long, endTime: Long, modelId: String): Flow<List<UsageEntity>> {
        return usageDao.getUsageInRangeForModel(startTime, endTime, modelId)
    }
}
