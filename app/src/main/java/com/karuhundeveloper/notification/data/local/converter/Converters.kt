package com.karuhundeveloper.notification.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for complex types that cannot be stored natively in SQLite.
 *
 * Handles serialization/deserialization of:
 * - [List<String>]  → stored as a JSON array string  (e.g. ["com.whatsapp","com.telegram"])
 * - [Map<String,String>] → stored as a JSON object string (e.g. {"Authorization":"Bearer xyz"})
 *
 * Gson is used because it is already pulled in transitively by many Android libraries and
 * avoids adding another JSON dependency. If the project moves to kotlinx.serialization in the
 * future, replace the Gson calls here.
 */
class Converters {

    private val gson = Gson()

    // ── List<String> ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // ── Map<String, String> ───────────────────────────────────────────────────

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        return gson.toJson(map ?: emptyMap<String, String>())
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
}
