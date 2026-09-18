package com.kartiks.habittracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kartiks.habittracker.MainActivity
import com.kartiks.habittracker.R
import com.kartiks.habittracker.data.local.AppDatabase
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.isCompletedWith
import com.kartiks.habittracker.domain.model.isScheduledOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val ACTION_HABIT_REMINDER = "com.kartiks.habittracker.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "habit_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)

                // Check global reminders setting
                val settings = db.settingsDao().getSettings()
                if (settings == null || !settings.remindersEnabled) {
                    return@launch
                }

                // 1. Load specific habit from Room
                val habitEntity = db.habitDao().getHabitById(habitId) ?: return@launch
                val habit = habitEntity.toDomainModel()

                // 2. Verify not archived
                if (habit.archived) return@launch

                // 3. Verify reminderEnabled
                if (!habit.reminderEnabled) return@launch

                val today = LocalDate.now()

                // 4. Verify the habit is scheduled for today
                val isScheduledToday = habit.isScheduledOn(today)

                if (isScheduledToday) {
                    // 5. Load today's HabitRecord
                    val recordEntity = db.habitRecordDao().getRecord(habit.id, today)
                    val record = recordEntity?.toDomainModel()

                    // 6. Determine whether it is actually incomplete
                    val isComplete = habit.isCompletedWith(record)

                    if (!isComplete) {
                        showHabitNotification(context, habit, record)
                    }
                }

                // 7. Always schedule the next appropriate reminder occurrence for this habit
                HabitReminderScheduler.scheduleReminderForHabit(context, habit)

            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showHabitNotification(context: Context, habit: Habit, record: HabitRecord?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to track and complete your scheduled habits"
        }
        notificationManager.createNotificationChannel(channel)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habit.id.hashCode() and 0x7FFFFFFF,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (habit.type == HabitType.MEASURABLE) {
            val cur = record?.currentValue ?: 0.0
            val curStr = if (cur % 1.0 == 0.0) cur.toInt().toString() else String.format("%.1f", cur)
            val targetStr = if (habit.targetValue % 1.0 == 0.0) habit.targetValue.toInt().toString() else String.format("%.1f", habit.targetValue)
            "$curStr / $targetStr ${habit.unit} recorded today. Keep your streak going!"
        } else {
            "Don't forget to complete ${habit.title} today!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Reminder: ${habit.title}")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = habit.id.hashCode() and 0x7FFFFFFF
        notificationManager.notify(notificationId, notification)
    }
}
