package com.kartiks.habittracker.domain.model

enum class StatsPeriod {
    WEEK,
    MONTH,
    YEAR,
    ALL
}

data class HabitStats(
    val habitId: String,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completionRate: Float = 0f,
    val totalCompletions: Int = 0,
    val completedThisMonth: Int = 0,
    val recentPerformance: List<Boolean> = emptyList(),
    // Measurable specific metrics
    val targetAttainmentRate: Float = 0f,
    val averageAchievedValue: Double = 0.0,
    val totalUnitsRecorded: Double = 0.0,
    val daysTargetReached: Int = 0
)

data class GlobalStats(
    val completionRate: Float = 0f,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val habitStatsList: List<Pair<Habit, HabitStats>> = emptyList()
)
