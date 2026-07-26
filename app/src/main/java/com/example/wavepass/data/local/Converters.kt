package com.example.wavepass.data.local

import androidx.room.TypeConverter

// Room can only store primitive types in columns.
// This converter lets us store a List<String> as a single delimited String.
class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(separator = "|")
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        return if (data.isEmpty()) emptyList() else data.split("|")
    }
}