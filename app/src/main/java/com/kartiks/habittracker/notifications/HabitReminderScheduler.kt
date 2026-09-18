package com.kartiks.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kartiks.habittracker.data.local.AppDatabase
import com.kartiks.habittracker.domain.model.Habit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

object HabitReminderScheduler {

    /**
     * Schedules an independent alarm for a specific habit based on its own
     * schedule, reminderTime, and startDate.
     */
    fun scheduleReminderForHabit(
        context: Context,
        habit: Habit,
        afterDateTime: LocalDateTime = LocalDateTime.now()
    ) {
        val nextDateTime = ReminderCalculationHelper.calculateNextReminderTime(habit, afterDateTime)
        if (nextDateTime == null) {
            cancelReminderForHabit(context, habit.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = ReminderCalculationHelper.getRequestCodeForHabit(habit.id)

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habit.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels the scheduled reminder for a specific habit without affecting any other habits.
     */
    fun cancelReminderForHabit(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = ReminderCalculationHelper.getRequestCodeForHabit(habitId)

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules reminders for all active habits if global reminders are enabled in Settings.
     */
    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val settings = db.settingsDao().getSettings()
            val habits = db.habitDao().getAllHabitsList()

            if (settings != null && settings.remindersEnabled) {
                for (habitEntity in habits) {
                    val habit = habitEntity.toDomainModel()
                    if (habit.reminderEnabled && !habit.archived) {
                        scheduleReminderForHabit(context, habit)
                    } else {
                        cancelReminderForHabit(context, habit.id)
                    }
                }
            } else {
                for (habitEntity in habits) {
                    cancelReminderForHabit(context, habitEntity.id)
                }
            }
        }
    }

    /**
     * Cancels all scheduled habit reminders.
     */
    fun cancelAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val habits = db.habitDao().getAllHabitsList()
            for (habitEntity in habits) {
                cancelReminderForHabit(context, habitEntity.id)
            }
        }
    }
}
