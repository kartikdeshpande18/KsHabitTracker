package com.kartiks.habittracker.data.backup

import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class BackupData(
    val version: Int,
    val exportedAt: Instant,
    val habits: List<Habit>,
    val records: List<HabitRecord>,
    val settings: Settings
)

object BackupManager {

    private const val CURRENT_VERSION = 1

    fun exportToJson(
        habits: List<Habit>,
        records: List<HabitRecord>,
        settings: Settings
    ): String {
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", Instant.now().toString())

        // Habits
        val habitsArray = JSONArray()
        for (habit in habits) {
            val hObj = JSONObject()
            hObj.put("id", habit.id)
            hObj.put("title", habit.title)
            hObj.put("type", habit.type.name)
            hObj.put("targetValue", habit.targetValue)
            hObj.put("unit", habit.unit)
            hObj.put("increment", habit.increment)
            hObj.put("color", habit.color)
            hObj.put("icon", habit.icon)
            hObj.put("frequency", habit.frequency.name)

            val daysArray = JSONArray()
            habit.selectedDays.forEach { daysArray.put(it.name) }
            hObj.put("selectedDays", daysArray)

            hObj.put("startDate", habit.startDate.toString())
            hObj.put("reminderEnabled", habit.reminderEnabled)
            hObj.put("reminderTime", habit.reminderTime?.toString())
            hObj.put("archived", habit.archived)
            hObj.put("createdAt", habit.createdAt.toString())
            hObj.put("updatedAt", habit.updatedAt.toString())
            habitsArray.put(hObj)
        }
        root.put("habits", habitsArray)

        // Records
        val recordsArray = JSONArray()
        for (record in records) {
            val rObj = JSONObject()
            rObj.put("id", record.id)
            rObj.put("habitId", record.habitId)
            rObj.put("date", record.date.toString())
            rObj.put("currentValue", record.currentValue)
            rObj.put("isCompleted", record.isCompleted)
            rObj.put("createdAt", record.createdAt.toString())
            rObj.put("updatedAt", record.updatedAt.toString())
            recordsArray.put(rObj)
        }
        root.put("records", recordsArray)

        // Settings
        val sObj = JSONObject()
        sObj.put("themeMode", settings.themeMode.name)
        sObj.put("dynamicColorEnabled", settings.dynamicColorEnabled)
        sObj.put("oledBlackEnabled", settings.oledBlackEnabled)
        sObj.put("remindersEnabled", settings.remindersEnabled)
        sObj.put("defaultReminderTime", settings.defaultReminderTime.toString())
        sObj.put("weekStartsOn", settings.weekStartsOn.name)
        root.put("settings", sObj)

        return root.toString(2)
    }

    @Throws(IllegalArgumentException::class)
    fun importFromJson(jsonString: String): BackupData {
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format: ${e.message}", e)
        }

        if (!root.has("version")) {
            throw IllegalArgumentException("Missing backup version identifier.")
        }
        val version = root.getInt("version")
        if (version != CURRENT_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version. Expected: $CURRENT_VERSION.")
        }

        val exportedAt = if (root.has("exportedAt")) {
            Instant.parse(root.getString("exportedAt"))
        } else {
            Instant.now()
        }

        // Habits
        val habits = mutableListOf<Habit>()
        if (root.has("habits")) {
            val habitsArray = root.getJSONArray("habits")
            for (i in 0 until habitsArray.length()) {
                val hObj = habitsArray.getJSONObject(i)
                val daysSet = mutableSetOf<DayOfWeek>()
                if (hObj.has("selectedDays")) {
                    val daysArr = hObj.getJSONArray("selectedDays")
                    for (d in 0 until daysArr.length()) {
                        try {
                            daysSet.add(DayOfWeek.valueOf(daysArr.getString(d)))
                        } catch (_: Exception) {}
                    }
                }
                val reminderTimeStr = if (hObj.has("reminderTime") && !hObj.isNull("reminderTime")) {
                    hObj.getString("reminderTime")
                } else null

                habits.add(
                    Habit(
                        id = hObj.getString("id"),
                        title = hObj.getString("title"),
                        type = HabitType.valueOf(hObj.optString("type", HabitType.YES_NO.name)),
                        targetValue = hObj.optDouble("targetValue", 1.0),
                        unit = hObj.optString("unit", ""),
                        increment = hObj.optDouble("increment", 1.0),
                        color = hObj.optLong("color", 0xFF006874),
                        icon = hObj.optString("icon", "check"),
                        frequency = HabitFrequency.valueOf(hObj.optString("frequency", HabitFrequency.DAILY.name)),
                        selectedDays = if (daysSet.isNotEmpty()) daysSet else DayOfWeek.entries.toSet(),
                        startDate = LocalDate.parse(hObj.getString("startDate")),
                        reminderEnabled = hObj.optBoolean("reminderEnabled", false),
                        reminderTime = reminderTimeStr?.let { LocalTime.parse(it) },
                        archived = hObj.optBoolean("archived", false),
                        createdAt = if (hObj.has("createdAt")) Instant.parse(hObj.getString("createdAt")) else Instant.now(),
                        updatedAt = if (hObj.has("updatedAt")) Instant.parse(hObj.getString("updatedAt")) else Instant.now()
                    )
                )
            }
        }

        // Records
        val records = mutableListOf<HabitRecord>()
        if (root.has("records")) {
            val recordsArray = root.getJSONArray("records")
            for (i in 0 until recordsArray.length()) {
                val rObj = recordsArray.getJSONObject(i)
                records.add(
                    HabitRecord(
                        id = rObj.getString("id"),
                        habitId = rObj.getString("habitId"),
                        date = LocalDate.parse(rObj.getString("date")),
                        currentValue = rObj.optDouble("currentValue", 0.0),
                        isCompleted = rObj.optBoolean("isCompleted", false),
                        createdAt = if (rObj.has("createdAt")) Instant.parse(rObj.getString("createdAt")) else Instant.now(),
                        updatedAt = if (rObj.has("updatedAt")) Instant.parse(rObj.getString("updatedAt")) else Instant.now()
                    )
                )
            }
        }

        // Settings
        var settings = Settings()
        if (root.has("settings")) {
            val sObj = root.getJSONObject("settings")
            settings = Settings(
                themeMode = ThemeMode.valueOf(sObj.optString("themeMode", ThemeMode.SYSTEM.name)),
                dynamicColorEnabled = sObj.optBoolean("dynamicColorEnabled", true),
                oledBlackEnabled = sObj.optBoolean("oledBlackEnabled", false),
                remindersEnabled = sObj.optBoolean("remindersEnabled", false),
                defaultReminderTime = if (sObj.has("defaultReminderTime")) LocalTime.parse(sObj.getString("defaultReminderTime")) else LocalTime.of(20, 0),
                weekStartsOn = DayOfWeek.valueOf(sObj.optString("weekStartsOn", DayOfWeek.MONDAY.name))
            )
        }

        return BackupData(
            version = version,
            exportedAt = exportedAt,
            habits = habits,
            records = records,
            settings = settings
        )
    }
}
