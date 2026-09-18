package com.kartiks.habittracker.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledBlackEnabled: Boolean = false,
    val remindersEnabled: Boolean = false,
    val defaultReminderTime: LocalTime = LocalTime.of(20, 0),
    val weekStartsOn: DayOfWeek = DayOfWeek.MONDAY
)
