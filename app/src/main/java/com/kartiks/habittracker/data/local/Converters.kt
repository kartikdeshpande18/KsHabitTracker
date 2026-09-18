package com.kartiks.habittracker.data.local

import androidx.room.TypeConverter
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.ThemeMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromDayOfWeek(day: DayOfWeek?): String? = day?.name

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? = value?.let { DayOfWeek.valueOf(it) }

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String? {
        return days?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(value: String?): Set<DayOfWeek> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",").mapNotNull {
            try {
                DayOfWeek.valueOf(it.trim())
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }

    @TypeConverter
    fun fromHabitType(type: HabitType?): String? = type?.name

    @TypeConverter
    fun toHabitType(value: String?): HabitType? = value?.let { HabitType.valueOf(it) }

    @TypeConverter
    fun fromHabitFrequency(freq: HabitFrequency?): String? = freq?.name

    @TypeConverter
    fun toHabitFrequency(value: String?): HabitFrequency? = value?.let { HabitFrequency.valueOf(it) }

    @TypeConverter
    fun fromThemeMode(mode: ThemeMode?): String? = mode?.name

    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode? = value?.let { ThemeMode.valueOf(it) }
}
