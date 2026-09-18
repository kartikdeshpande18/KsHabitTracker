package com.kartiks.habittracker

import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.notifications.ReminderCalculationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderCalculationTest {

    @Test
    fun testReminderDisabledReturnsNull() {
        val habit = Habit(
            id = "h1",
            title = "Meditate",
            type = HabitType.YES_NO,
            reminderEnabled = false,
            reminderTime = LocalTime.of(8, 0)
        )
        val next = ReminderCalculationHelper.calculateNextReminderTime(
            habit = habit,
            afterDateTime = LocalDateTime.of(2026, 9, 18, 7, 0)
        )
        assertNull(next)
    }

    @Test
    fun testReminderNullTimeReturnsNull() {
        val habit = Habit(
            id = "h2",
            title = "Meditate",
            type = HabitType.YES_NO,
            reminderEnabled = true,
            reminderTime = null
        )
        val next = ReminderCalculationHelper.calculateNextReminderTime(
            habit = habit,
            afterDateTime = LocalDateTime.of(2026, 9, 18, 7, 0)
        )
        assertNull(next)
    }

    @Test
    fun testArchivedHabitReturnsNull() {
        val habit = Habit(
            id = "h3",
            title = "Meditate",
            type = HabitType.YES_NO,
            reminderEnabled = true,
            reminderTime = LocalTime.of(8, 0),
            archived = true
        )
        val next = ReminderCalculationHelper.calculateNextReminderTime(
            habit = habit,
            afterDateTime = LocalDateTime.of(2026, 9, 18, 7, 0)
        )
        assertNull(next)
    }

    @Test
    fun testDailyReminderTodayBeforeTime() {
        val habit = Habit(
            id = "h4",
            title = "Daily Reading",
            type = HabitType.YES_NO,
            frequency = HabitFrequency.DAILY,
            reminderEnabled = true,
            reminderTime = LocalTime.of(20, 0),
            startDate = LocalDate.of(2026, 9, 1)
        )
        val now = LocalDateTime.of(2026, 9, 18, 14, 30)
        val next = ReminderCalculationHelper.calculateNextReminderTime(habit, now)

        assertNotNull(next)
        assertEquals(LocalDateTime.of(2026, 9, 18, 20, 0), next)
    }

    @Test
    fun testDailyReminderTodayAfterTimeAdvancesToTomorrow() {
        val habit = Habit(
            id = "h5",
            title = "Morning Run",
            type = HabitType.YES_NO,
            frequency = HabitFrequency.DAILY,
            reminderEnabled = true,
            reminderTime = LocalTime.of(6, 30),
            startDate = LocalDate.of(2026, 9, 1)
        )
        val now = LocalDateTime.of(2026, 9, 18, 7, 0)
        val next = ReminderCalculationHelper.calculateNextReminderTime(habit, now)

        assertNotNull(next)
        assertEquals(LocalDateTime.of(2026, 9, 19, 6, 30), next)
    }

    @Test
    fun testWeeklySpecificDaysAdvancesToNextScheduledDay() {
        // Schedule: Mon, Wed, Fri at 09:00
        val habit = Habit(
            id = "h6",
            title = "Gym Session",
            type = HabitType.YES_NO,
            frequency = HabitFrequency.WEEKLY_DAYS,
            selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            reminderEnabled = true,
            reminderTime = LocalTime.of(9, 0),
            startDate = LocalDate.of(2026, 9, 1)
        )
        // Given 2026-09-18 is Friday. If now is Friday 10:00 (after reminder),
        // next occurrence should be Monday 2026-09-21 at 09:00.
        val fridayAfterTime = LocalDateTime.of(2026, 9, 18, 10, 0)
        assertEquals(DayOfWeek.FRIDAY, fridayAfterTime.dayOfWeek)

        val nextAfterFriday = ReminderCalculationHelper.calculateNextReminderTime(habit, fridayAfterTime)
        assertNotNull(nextAfterFriday)
        assertEquals(LocalDateTime.of(2026, 9, 21, 9, 0), nextAfterFriday)
        assertEquals(DayOfWeek.MONDAY, nextAfterFriday?.dayOfWeek)
    }

    @Test
    fun testFutureStartDateIsRespected() {
        val futureStart = LocalDate.of(2026, 10, 1)
        val habit = Habit(
            id = "h7",
            title = "Future Habit",
            type = HabitType.YES_NO,
            frequency = HabitFrequency.DAILY,
            reminderEnabled = true,
            reminderTime = LocalTime.of(8, 0),
            startDate = futureStart
        )
        val now = LocalDateTime.of(2026, 9, 18, 7, 0)
        val next = ReminderCalculationHelper.calculateNextReminderTime(habit, now)

        assertNotNull(next)
        assertEquals(LocalDateTime.of(2026, 10, 1, 8, 0), next)
    }

    @Test
    fun testStableRequestCodeDerivation() {
        val code1 = ReminderCalculationHelper.getRequestCodeForHabit("habit_abc")
        val code1Repeat = ReminderCalculationHelper.getRequestCodeForHabit("habit_abc")
        val code2 = ReminderCalculationHelper.getRequestCodeForHabit("habit_xyz")

        assertTrue("Request code must be non-negative", code1 >= 0)
        assertTrue("Request code must be non-negative", code2 >= 0)
        assertEquals("Request code must be stable", code1, code1Repeat)
        assertNotEquals("Different habit IDs should yield different codes", code1, code2)
    }
}
