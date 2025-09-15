package com.jumblemint.cows.data.database.converters

import androidx.room.TypeConverter

object ListLongConverter {
    @TypeConverter
    @JvmStatic
    fun fromString(value: String?): List<Long>? {
        return value?.split(',')?.mapNotNull { it.trim().toLongOrNull() }
    }

    @TypeConverter
    @JvmStatic
    fun fromList(list: List<Long>?): String? {
        return list?.joinToString(",")
    }
}