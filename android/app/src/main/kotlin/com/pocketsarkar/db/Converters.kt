package com.pocketsarkar.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    // List<String> ↔ JSON string (for storing state lists, category lists, etc.)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }

    // Map<String, String> ↔ JSON string (for storing localised names map)
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? =
        value?.let {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, type)
        }
}
