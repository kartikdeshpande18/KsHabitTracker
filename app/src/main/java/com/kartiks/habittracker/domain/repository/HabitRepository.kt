package com.kartiks.habittracker.domain.repository

import com.kartiks.habittracker.domain.model.GlobalStats
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitStats
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.model.StatsPeriod
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

import java.time.YearMonth

interface HabitRepository {
    fun getHabitsStream(): Flow<List<Habit>>
    fun getRecordsStream(): Flow<List<HabitRecord>>
    fun getSettingsStream(): Flow<Settings>

    suspend fun toggleHabitCompletion(habitId: String, date: LocalDate)
    suspend fun updateMeasurableValue(habitId: String, date: LocalDate, delta: Double)
    suspend fun createHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habitId: String)
    suspend fun deleteAllData()
    suspend fun updateSettings(settings: Settings)

    fun calculateHabitStats(habit: Habit, records: List<HabitRecord>, today: LocalDate = LocalDate.now()): HabitStats {
        val habitRecords = records.filter { it.habitId == habit.id }.sortedBy { it.date }
        val completedDates = habitRecords.filter { it.isCompleted }.map { it.date }.toSet()

        // Current Streak
        var currentStreak = 0
        var checkDate = if (completedDates.contains(today)) today else today.minusDays(1)
        while (completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Best Streak
        var bestStreak = 0
        var runningStreak = 0
        var prevDate: LocalDate? = null
        for (date in completedDates.sorted()) {
            if (prevDate != null && date == prevDate.plusDays(1)) {
                runningStreak++
            } else {
                runningStreak = 1
            }
            if (runningStreak > bestStreak) bestStreak = runningStreak
            prevDate = date
        }
        if (currentStreak > bestStreak) bestStreak = currentStreak

        // Total completions
        val totalCompletions = completedDates.size

        // Completed this month
        val currentYearMonth = YearMonth.from(today)
        val completedThisMonth = completedDates.count { YearMonth.from(it) == currentYearMonth }

        // Completion rate over total active days
        val totalDays = (today.toEpochDay() - habit.startDate.toEpochDay() + 1).coerceAtLeast(1)
        val completionRate = (totalCompletions.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)

        // Recent performance (last 14 days)
        val recentPerformance = (13 downTo 0).map { offset ->
            completedDates.contains(today.minusDays(offset.toLong()))
        }

        // Measurable stats
        val totalUnits = habitRecords.sumOf { it.currentValue }
        val daysTargetReached = totalCompletions
        val avgAchieved = if (habitRecords.isNotEmpty()) totalUnits / habitRecords.size else 0.0
        val targetAttainmentRate = if (habit.targetValue > 0) {
            (avgAchieved / habit.targetValue).toFloat().coerceIn(0f, 1f)
        } else 0f

        return HabitStats(
            habitId = habit.id,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionRate = completionRate,
            totalCompletions = totalCompletions,
            completedThisMonth = completedThisMonth,
            recentPerformance = recentPerformance,
            targetAttainmentRate = targetAttainmentRate,
            averageAchievedValue = avgAchieved,
            totalUnitsRecorded = totalUnits,
            daysTargetReached = daysTargetReached
        )
    }

    fun calculateGlobalStats(
        habits: List<Habit>,
        records: List<HabitRecord>,
        period: StatsPeriod,
        today: LocalDate = LocalDate.now()
    ): GlobalStats {
        val daysCount = when (period) {
            StatsPeriod.WEEK -> 7
            StatsPeriod.MONTH -> 30
            StatsPeriod.YEAR -> 365
            StatsPeriod.ALL -> 60
        }
        val startDate = today.minusDays((daysCount - 1).toLong())
        val periodRecords = records.filter { !it.date.isBefore(startDate) && !it.date.isAfter(today) }

        val habitStatsList = habits.map { habit ->
            habit to calculateHabitStats(habit, periodRecords, today)
        }

        val totalScheduledInstances = habits.size * daysCount
        val totalCompletions = periodRecords.count { it.isCompleted }
        val completionRate = if (totalScheduledInstances > 0) {
            (totalCompletions.toFloat() / totalScheduledInstances.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val currentStreak = habitStatsList.maxOfOrNull { it.second.currentStreak } ?: 0
        val bestStreak = habitStatsList.maxOfOrNull { it.second.bestStreak } ?: 0

        return GlobalStats(
            completionRate = completionRate,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            habitStatsList = habitStatsList
        )
    }
}
