package com.fishai.chatzen.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fishai.chatzen.data.database.dao.ChatDao
import com.fishai.chatzen.data.database.entity.ChatMessageEntity

@Database(entities = [UsageEntity::class, ChatMessageEntity::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun chatDao(): ChatDao
}
