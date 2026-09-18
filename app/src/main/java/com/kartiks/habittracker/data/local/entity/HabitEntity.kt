package com.kartiks.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val type: HabitType,
    val targetValue: Double,
    val unit: String,
    val increment: Double,
    val color: Long,
    val icon: String,
    val frequency: HabitFrequency,
    val selectedDays: Set<DayOfWeek>,
    val startDate: LocalDate,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toDomainModel(): Habit {
        return Habit(
            id = id,
            title = title,
            type = type,
            targetValue = targetValue,
            unit = unit,
            increment = increment,
            color = color,
            icon = icon,
            frequency = frequency,
            selectedDays = selectedDays,
            startDate = startDate,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            archived = archived,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomainModel(habit: Habit): HabitEntity {
            return HabitEntity(
                id = habit.id,
                title = habit.title,
                type = habit.type,
                targetValue = habit.targetValue,
                unit = habit.unit,
                increment = habit.increment,
                color = habit.color,
                icon = habit.icon,
                frequency = habit.frequency,
                selectedDays = habit.selectedDays,
                startDate = habit.startDate,
                reminderEnabled = habit.reminderEnabled,
                reminderTime = habit.reminderTime,
                archived = habit.archived,
                createdAt = habit.createdAt,
                updatedAt = habit.updatedAt
            )
        }
    }
}

fun Habit.toEntity(): HabitEntity = HabitEntity.fromDomainModel(this)
