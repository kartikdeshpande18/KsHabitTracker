package com.kartiks.habittracker.notifications

import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.isScheduledOn
import java.time.LocalDateTime

object ReminderCalculationHelper {

    /**
     * Calculates the next LocalDateTime when a reminder for the given habit should trigger.
     * Returns null if reminders are disabled, reminderTime is null, or habit is archived.
     */
    fun calculateNextReminderTime(
        habit: Habit,
        afterDateTime: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        if (!habit.reminderEnabled || habit.reminderTime == null || habit.archived) {
            return null
        }

        val afterDate = afterDateTime.toLocalDate()
        val baseDate = if (afterDate.isBefore(habit.startDate)) habit.startDate else afterDate

        // Check up to 14 days ahead to find the next scheduled occurrence
        for (offset in 0L..14L) {
            val candidateDate = baseDate.plusDays(offset)
            if (habit.isScheduledOn(candidateDate)) {
                val candidateDateTime = LocalDateTime.of(candidateDate, habit.reminderTime)
                if (candidateDateTime.isAfter(afterDateTime)) {
                    return candidateDateTime
                }
            }
        }
        return null
    }

    /**
     * Derives a stable, unique 31-bit positive integer request code from habitId
     * to guarantee per-habit AlarmManager PendingIntent isolation.
     */
    fun getRequestCodeForHabit(habitId: String): Int {
        return habitId.hashCode() and 0x7FFFFFFF
    }
}
