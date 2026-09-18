package com.kartiks.habittracker.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class HabitType {
    YES_NO,
    MEASURABLE
}

enum class HabitFrequency {
    DAILY,
    WEEKLY_DAYS
}

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: HabitType,
    val targetValue: Double = 1.0,
    val unit: String = "",
    val increment: Double = 1.0,
    val color: Long = 0xFF006874,
    val icon: String = "check",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val selectedDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val startDate: LocalDate = LocalDate.now(),
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val archived: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

fun Habit.isScheduledOn(date: LocalDate): Boolean {
    if (archived) return false
    if (date.isBefore(startDate)) return false
    return when (frequency) {
        HabitFrequency.DAILY -> true
        HabitFrequency.WEEKLY_DAYS -> date.dayOfWeek in selectedDays
    }
}

fun Habit.isCompletedWith(record: HabitRecord?): Boolean {
    if (record == null) return false
    return if (type == HabitType.MEASURABLE) {
        record.currentValue >= targetValue
    } else {
        record.isCompleted
    }
}
