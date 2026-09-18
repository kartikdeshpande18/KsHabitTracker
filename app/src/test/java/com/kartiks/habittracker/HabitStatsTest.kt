package com.kartiks.habittracker

import com.kartiks.habittracker.data.repository.InMemoryHabitRepository
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.StatsPeriod
import com.kartiks.habittracker.domain.model.isCompletedWith
import com.kartiks.habittracker.domain.model.isScheduledOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatsTest {

    private val repository = InMemoryHabitRepository()
    private val today = LocalDate.now()

    @Test
    fun testInitialHabitsCount() = runBlocking {
        val habits = repository.getHabitsStream().first()
        assertEquals(6, habits.size)

        val waterHabit = habits.find { it.title == "Drink water" }
        assertEquals(HabitType.MEASURABLE, waterHabit?.type)
        assertEquals(8.0, waterHabit?.targetValue ?: 0.0, 0.001)

        val exerciseHabit = habits.find { it.title == "Exercise" }
        assertEquals(HabitType.YES_NO, exerciseHabit?.type)
    }

    @Test
    fun testCurrentStreakCalculation() {
        val habit = Habit(
            id = "test_habit",
            title = "Test Habit",
            type = HabitType.YES_NO,
            startDate = today.minusDays(10)
        )

        // Completed today, yesterday, and 2 days ago -> streak 3
        val records = listOf(
            HabitRecord(habitId = "test_habit", date = today, isCompleted = true),
            HabitRecord(habitId = "test_habit", date = today.minusDays(1), isCompleted = true),
            HabitRecord(habitId = "test_habit", date = today.minusDays(2), isCompleted = true),
            HabitRecord(habitId = "test_habit", date = today.minusDays(3), isCompleted = false)
        )

        val stats = repository.calculateHabitStats(habit, records, today)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.totalCompletions)
    }

    @Test
    fun testMeasurableTargetAttainment() {
        val habit = Habit(
            id = "water",
            title = "Water",
            type = HabitType.MEASURABLE,
            targetValue = 8.0,
            unit = "glasses",
            startDate = today.minusDays(5)
        )

        val records = listOf(
            HabitRecord(habitId = "water", date = today, currentValue = 8.0, isCompleted = true),
            HabitRecord(habitId = "water", date = today.minusDays(1), currentValue = 6.0, isCompleted = false),
            HabitRecord(habitId = "water", date = today.minusDays(2), currentValue = 8.0, isCompleted = true)
        )

        val stats = repository.calculateHabitStats(habit, records, today)
        assertEquals(2, stats.daysTargetReached)
        assertEquals(22.0, stats.totalUnitsRecorded, 0.001)
    }

    @Test
    fun testToggleCompletion() = runBlocking {
        val habitId = "habit_4" // Stretch (initially incomplete today)
        val recordsBefore = repository.getRecordsStream().first()
        val stretchTodayBefore = recordsBefore.find { it.habitId == habitId && it.date == today }
        assertFalse(stretchTodayBefore?.isCompleted == true)

        repository.toggleHabitCompletion(habitId, today)

        val recordsAfter = repository.getRecordsStream().first()
        val stretchTodayAfter = recordsAfter.find { it.habitId == habitId && it.date == today }
        assertTrue(stretchTodayAfter?.isCompleted == true)
    }

    @Test
    fun testUpdateMeasurableDelta() = runBlocking {
        val habitId = "habit_1" // Drink water (target 8, initially 5 today)
        repository.updateMeasurableValue(habitId, today, 2.0)

        val records = repository.getRecordsStream().first()
        val waterToday = records.find { it.habitId == habitId && it.date == today }
        assertEquals(7.0, waterToday?.currentValue ?: 0.0, 0.001)
        assertFalse(waterToday?.isCompleted == true)

        // Reach target (7 + 1 = 8)
        repository.updateMeasurableValue(habitId, today, 1.0)
        val recordsCompleted = repository.getRecordsStream().first()
        val waterCompleted = recordsCompleted.find { it.habitId == habitId && it.date == today }
        assertEquals(8.0, waterCompleted?.currentValue ?: 0.0, 0.001)
        assertTrue(waterCompleted?.isCompleted == true)
    }

    @Test
    fun testHabitSchedulingLogic() {
        val startDate = today.minusDays(5)
        val habit = Habit(
            title = "Reading",
            type = HabitType.YES_NO,
            startDate = startDate
        )

        // Date before start date is not scheduled
        assertFalse(habit.isScheduledOn(startDate.minusDays(1)))
        // Start date and future dates are scheduled
        assertTrue(habit.isScheduledOn(startDate))
        assertTrue(habit.isScheduledOn(today))

        // Archived habit is never scheduled
        val archivedHabit = habit.copy(archived = true)
        assertFalse(archivedHabit.isScheduledOn(today))
    }

    @Test
    fun testHabitCompletionLogic() {
        val yesNoHabit = Habit(title = "Meditation", type = HabitType.YES_NO)
        val yesNoDone = HabitRecord(habitId = yesNoHabit.id, date = today, isCompleted = true)
        val yesNoIncomplete = HabitRecord(habitId = yesNoHabit.id, date = today, isCompleted = false)

        assertTrue(yesNoHabit.isCompletedWith(yesNoDone))
        assertFalse(yesNoHabit.isCompletedWith(yesNoIncomplete))
        assertFalse(yesNoHabit.isCompletedWith(null))

        val measurableHabit = Habit(
            title = "Water",
            type = HabitType.MEASURABLE,
            targetValue = 8.0,
            unit = "glasses"
        )
        val measurablePartial = HabitRecord(habitId = measurableHabit.id, date = today, currentValue = 5.0, isCompleted = false)
        val measurableComplete = HabitRecord(habitId = measurableHabit.id, date = today, currentValue = 8.0, isCompleted = true)
        val measurableExceeded = HabitRecord(habitId = measurableHabit.id, date = today, currentValue = 10.0, isCompleted = true)

        assertFalse(measurableHabit.isCompletedWith(measurablePartial))
        assertTrue(measurableHabit.isCompletedWith(measurableComplete))
        assertTrue(measurableHabit.isCompletedWith(measurableExceeded))
    }
}
