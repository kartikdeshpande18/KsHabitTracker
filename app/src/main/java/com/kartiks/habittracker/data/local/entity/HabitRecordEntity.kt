package com.kartiks.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kartiks.habittracker.domain.model.HabitRecord
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "habit_records",
    indices = [
        Index(value = ["habitId", "date"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitRecordEntity(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val currentValue: Double,
    val isCompleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toDomainModel(): HabitRecord {
        return HabitRecord(
            id = id,
            habitId = habitId,
            date = date,
            currentValue = currentValue,
            isCompleted = isCompleted,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomainModel(record: HabitRecord): HabitRecordEntity {
            return HabitRecordEntity(
                id = record.id,
                habitId = record.habitId,
                date = record.date,
                currentValue = record.currentValue,
                isCompleted = record.isCompleted,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt
            )
        }
    }
}

fun HabitRecord.toEntity(): HabitRecordEntity = HabitRecordEntity.fromDomainModel(this)
