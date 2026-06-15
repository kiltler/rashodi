package com.rashodi.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String =
        list.orEmpty().joinToString("\n")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList()
        else value.split("\n").filter { it.isNotEmpty() }
}
