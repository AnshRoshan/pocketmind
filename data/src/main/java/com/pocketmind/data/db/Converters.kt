package com.pocketmind.data.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun floatArrayToString(value: FloatArray?): String {
        return value?.joinToString(",") { it.toString() } ?: ""
    }

    @TypeConverter
    fun stringToFloatArray(value: String?): FloatArray {
        if (value.isNullOrBlank()) return FloatArray(0)
        return value.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
    }

    @TypeConverter
    fun stringListToString(value: List<String>?): String {
        return value?.joinToString("\u0001") ?: ""
    }

    @TypeConverter
    fun stringToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("\u0001")
    }
}
