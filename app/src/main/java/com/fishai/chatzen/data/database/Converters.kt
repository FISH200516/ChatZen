package com.fishai.chatzen.data.database

import androidx.room.TypeConverter
import com.fishai.chatzen.data.model.Role
import com.fishai.chatzen.data.model.WebSearchResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRole(role: Role): String {
        return role.name
    }

    @TypeConverter
    fun toRole(value: String): Role {
        return try {
            Role.valueOf(value)
        } catch (e: Exception) {
            Role.USER // Default fallback
        }
    }

    @TypeConverter
    fun fromWebSearchResults(list: List<WebSearchResult>?): String {
        return if (list == null) "[]" else json.encodeToString(list)
    }

    @TypeConverter
    fun toWebSearchResults(value: String): List<WebSearchResult> {
        return try {
            if (value.isBlank()) emptyList() else json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return if (list == null) "[]" else json.encodeToString(list)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            if (value.isBlank()) emptyList() else json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
