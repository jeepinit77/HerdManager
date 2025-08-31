package com.jumblemint.cows.data.database

import androidx.room.TypeConverter
import com.jumblemint.cows.data.model.*
import java.time.LocalDate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    // For LocalDate
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    // For Gender Enum
    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }

    @TypeConverter
    fun toGender(genderString: String): Gender {
        return Gender.valueOf(genderString)
    }

    // For Classification Enum
    @TypeConverter
    fun fromClassification(classification: Classification): String {
        return classification.name
    }

    @TypeConverter
    fun toClassification(classificationString: String): Classification {
        return Classification.valueOf(classificationString)
    }

    // For Status Enum
    @TypeConverter
    fun fromStatus(status: Status): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(statusString: String): Status {
        return Status.valueOf(statusString)
    }

    // For ActivityType Enum
    @TypeConverter
    fun fromActivityType(activityType: ActivityType): String {
        return activityType.name
    }

    @TypeConverter
    fun toActivityType(activityTypeString: String): ActivityType {
        return ActivityType.valueOf(activityTypeString)
    }

    // For List<String>
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    // For Map<String, String> using Gson
    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        if (value == null) {
            return null
        }
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>?): String? {
        if (map == null) {
            return null
        }
        return Gson().toJson(map)
    }
}
