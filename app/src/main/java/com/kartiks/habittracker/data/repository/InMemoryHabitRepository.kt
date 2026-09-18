package com.kartiks.habittracker.data.repository

import com.kartiks.habittracker.domain.model.GlobalStats
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitStats
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.model.StatsPeriod
import com.kartiks.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Temporary In-Memory repository providing deterministic prototype demo data.
 * Structured to be replaced by RoomHabitRepository in Priority 2 without
 * modifying UI or ViewModel interfaces.
 */
class InMemoryHabitRepository : HabitRepository {

    private val _habits = MutableStateFlow<List<Habit>>(createInitialHabits())
    private val _records = MutableStateFlow<List<HabitRecord>>(createInitialRecords())
    private val _settings = MutableStateFlow(Settings())

    override fun getHabitsStream(): Flow<List<Habit>> = _habits.asStateFlow()
    override fun getRecordsStream(): Flow<List<HabitRecord>> = _records.asStateFlow()
    override fun getSettingsStream(): Flow<Settings> = _settings.asStateFlow()

    override suspend fun toggleHabitCompletion(habitId: String, date: LocalDate) {
        _records.update { currentRecords ->
            val existing = currentRecords.find { it.habitId == habitId && it.date == date }
            if (existing != null) {
                currentRecords.map {
                    if (it.id == existing.id) it.copy(isCompleted = !it.isCompleted) else it
                }
            } else {
                currentRecords + HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = date,
                    currentValue = 1.0,
                    isCompleted = true
                )
            }
        }
    }

    override suspend fun updateMeasurableValue(habitId: String, date: LocalDate, delta: Double) {
        val habit = _habits.value.find { it.id == habitId } ?: return
        _records.update { currentRecords ->
            val existing = currentRecords.find { it.habitId == habitId && it.date == date }
            if (existing != null) {
                val newVal = (existing.currentValue + delta).coerceAtLeast(0.0)
                val completed = newVal >= habit.targetValue
                currentRecords.map {
                    if (it.id == existing.id) it.copy(currentValue = newVal, isCompleted = completed) else it
                }
            } else {
                val newVal = delta.coerceAtLeast(0.0)
                val completed = newVal >= habit.targetValue
                currentRecords + HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = date,
                    currentValue = newVal,
                    isCompleted = completed
                )
            }
        }
    }

    override suspend fun createHabit(habit: Habit) {
        _habits.update { it + habit }
    }

    override suspend fun updateHabit(habit: Habit) {
        _habits.update { habits ->
            habits.map { if (it.id == habit.id) habit else it }
        }
    }

    override suspend fun deleteHabit(habitId: String) {
        _habits.update { habits -> habits.filterNot { it.id == habitId } }
        _records.update { records -> records.filterNot { it.habitId == habitId } }
    }

    override suspend fun deleteAllData() {
        _habits.value = emptyList()
        _records.value = emptyList()
    }

    override suspend fun updateSettings(settings: Settings) {
        _settings.value = settings
    }

    override fun calculateHabitStats(
        habit: Habit,
        records: List<HabitRecord>,
        today: LocalDate
    ): HabitStats {
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

    override fun calculateGlobalStats(
        habits: List<Habit>,
        records: List<HabitRecord>,
        period: StatsPeriod,
        today: LocalDate
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

    companion object {
        fun createInitialHabits(): List<Habit> {
            val startDate = LocalDate.now().minusDays(60)
            return listOf(
                Habit(
                    id = "habit_1",
                    title = "Drink water",
                    type = HabitType.MEASURABLE,
                    targetValue = 8.0,
                    unit = "glasses",
                    increment = 1.0,
                    color = 0xFF006874,
                    icon = "water_drop",
                    startDate = startDate
                ),
                Habit(
                    id = "habit_2",
                    title = "Exercise",
                    type = HabitType.YES_NO,
                    color = 0xFFBA1A1A,
                    icon = "fitness_center",
                    startDate = startDate
                ),
                Habit(
                    id = "habit_3",
                    title = "Meditate",
                    type = HabitType.YES_NO,
                    color = 0xFF6750A4,
                    icon = "self_improvement",
                    startDate = startDate
                ),
                Habit(
                    id = "habit_4",
                    title = "Stretch",
                    type = HabitType.YES_NO,
                    color = 0xFF2E6B27,
                    icon = "directions_run",
                    startDate = startDate
                ),
                Habit(
                    id = "habit_5",
                    title = "Journal",
                    type = HabitType.YES_NO,
                    color = 0xFF196489,
                    icon = "menu_book",
                    startDate = startDate
                ),
                Habit(
                    id = "habit_6",
                    title = "Take vitamins",
                    type = HabitType.YES_NO,
                    color = 0xFF8A5100,
                    icon = "medication",
                    startDate = startDate
                )
            )
        }

        fun createInitialRecords(): List<HabitRecord> {
            val today = LocalDate.now()
            val records = mutableListOf<HabitRecord>()

            // Past 60 days deterministic records
            for (offset in 60 downTo 1) {
                val date = today.minusDays(offset.toLong())
                val dayNum = date.dayOfMonth

                // Habit 1: Drink water (Measurable)
                val waterVal = if (dayNum % 4 == 0) 6.0 else 8.0
                records.add(
                    HabitRecord(
                        id = UUID.randomUUID().toString(),
                        habitId = "habit_1",
                        date = date,
                        currentValue = waterVal,
                        isCompleted = waterVal >= 8.0
                    )
                )

                // Habit 2: Exercise (Yes/No)
                val exerciseDone = (dayNum % 3) != 0
                records.add(
                    HabitRecord(
                        id = UUID.randomUUID().toString(),
                        habitId = "habit_2",
                        date = date,
                        isCompleted = exerciseDone
                    )
                )

                // Habit 3: Meditate (Yes/No)
                val medDone = (dayNum % 2) == 0 || (dayNum % 5) == 0
                records.add(
                    HabitRecord(
                        id = UUID.randomUUID().toString(),
                        habitId = "habit_3",
                        date = date,
                        isCompleted = medDone
                    )
                )

                // Habit 4: Stretch (Yes/No)
                val stretchDone = (dayNum % 7) != 0
                records.add(
                    HabitRecord(
                        id = UUID.randomUUID().toString(),
                        habitId = "habit_4",
                        date = date,
                        isCompleted = stretchDone
                    )
                )

                // Habit 5: Journal (Yes/No)
                val journalDone = (dayNum % 2) != 0
                records.add(
                    HabitRecord(
                        id = UUID.randomUUID().toString(),
                        habitId = "habit_5",
                        date = date,
                        isCompleted = journalDone
                    )
                )

                // Habit 6: Take vitamins (Yes/No)
                val vitDone = (dayNum % 6) != 0
                records.add(
                    HabitRecord(
                        id = UUID.randomUUID().toString(),
                        habitId = "habit_6",
                        date = date,
                        isCompleted = vitDone
                    )
                )
            }

            // Today's state:
            // Drink water: 5/8 (incomplete)
            records.add(
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = "habit_1",
                    date = today,
                    currentValue = 5.0,
                    isCompleted = false
                )
            )
            // Exercise: complete
            records.add(
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = "habit_2",
                    date = today,
                    isCompleted = true
                )
            )
            // Meditate: complete
            records.add(
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = "habit_3",
                    date = today,
                    isCompleted = true
                )
            )
            // Stretch: incomplete
            records.add(
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = "habit_4",
                    date = today,
                    isCompleted = false
                )
            )
            // Journal: incomplete
            records.add(
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = "habit_5",
                    date = today,
                    isCompleted = false
                )
            )
            // Take vitamins: incomplete
            records.add(
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = "habit_6",
                    date = today,
                    isCompleted = false
                )
            )

            return records
        }
    }
}
