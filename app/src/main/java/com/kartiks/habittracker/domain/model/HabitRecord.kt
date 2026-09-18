package com.kartiks.habittracker.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HabitRecord(
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: LocalDate,
    val currentValue: Double = 0.0,
    val isCompleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
