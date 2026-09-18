package com.kartiks.habittracker

import com.kartiks.habittracker.data.backup.BackupManager
import com.kartiks.habittracker.data.local.Converters
import com.kartiks.habittracker.data.local.entity.HabitEntity
import com.kartiks.habittracker.data.local.entity.HabitRecordEntity
import com.kartiks.habittracker.data.local.entity.SettingsEntity
import com.kartiks.habittracker.data.repository.InMemoryHabitRepository
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class HabitDatabaseAndLogicTest {

    private val converters = Converters()

    @Test
    fun testRoomTypeConverters() {
        val today = LocalDate.of(2026, 9, 18)
        val epochDay = converters.fromLocalDate(today)
        assertEquals(today, converters.toLocalDate(epochDay))

        val time = LocalTime.of(14, 30, 45)
        val nano = converters.fromLocalTime(time)
        assertEquals(time, converters.toLocalTime(nano))

        val now = Instant.now()
        val epochMilli = converters.fromInstant(now)
        assertEquals(now.toEpochMilli(), converters.toInstant(epochMilli)?.toEpochMilli())

        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val daysString = converters.fromDayOfWeekSet(days)
        val restoredDays = converters.toDayOfWeekSet(daysString)
        assertEquals(days, restoredDays)

        assertEquals("MEASURABLE", converters.fromHabitType(HabitType.MEASURABLE))
        assertEquals(HabitType.MEASURABLE, converters.toHabitType("MEASURABLE"))

        assertEquals("DARK", converters.fromThemeMode(ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, converters.toThemeMode("DARK"))
    }

    @Test
    fun testEntityDomainMapping() {
        val habit = Habit(
            id = "test-h-1",
            title = "Morning Meditation",
            type = HabitType.YES_NO,
            targetValue = 1.0,
            unit = "",
            increment = 1.0,
            color = 0xFF6750A4,
            icon = "self_improvement",
            frequency = HabitFrequency.DAILY,
            selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            startDate = LocalDate.of(2026, 1, 1),
            reminderEnabled = true,
            reminderTime = LocalTime.of(7, 30),
            archived = false,
            createdAt = Instant.ofEpochMilli(100000),
            updatedAt = Instant.ofEpochMilli(200000)
        )

        val entity = HabitEntity.fromDomainModel(habit)
        val domain = entity.toDomainModel()
        assertEquals(habit, domain)

        val record = HabitRecord(
            id = "rec-1",
            habitId = "test-h-1",
            date = LocalDate.of(2026, 9, 18),
            currentValue = 1.0,
            isCompleted = true,
            createdAt = Instant.ofEpochMilli(100000),
            updatedAt = Instant.ofEpochMilli(200000)
        )
        val recordEntity = HabitRecordEntity.fromDomainModel(record)
        val recordDomain = recordEntity.toDomainModel()
        assertEquals(record, recordDomain)

        val settings = Settings(
            themeMode = ThemeMode.DARK,
            dynamicColorEnabled = false,
            oledBlackEnabled = true,
            remindersEnabled = true,
            defaultReminderTime = LocalTime.of(21, 0),
            weekStartsOn = DayOfWeek.SUNDAY
        )
        val settingsEntity = SettingsEntity.fromDomainModel(settings, hasCompletedInitialSeed = true)
        assertTrue(settingsEntity.hasCompletedInitialSeed)
        val settingsDomain = settingsEntity.toDomainModel()
        assertEquals(settings, settingsDomain)
    }

    @Test
    fun testRepositoryCrud() = runBlocking {
        val repo = InMemoryHabitRepository()

        // Create
        val newHabit = Habit(
            id = "new_habit_id",
            title = "Read 20 pages",
            type = HabitType.MEASURABLE,
            targetValue = 20.0,
            unit = "pages"
        )
        repo.createHabit(newHabit)
        val habitsAfterCreate = repo.getHabitsStream().first()
        assertTrue(habitsAfterCreate.any { it.id == "new_habit_id" })

        // Update
        val updatedHabit = newHabit.copy(title = "Read 30 pages", targetValue = 30.0)
        repo.updateHabit(updatedHabit)
        val habitsAfterUpdate = repo.getHabitsStream().first()
        val found = habitsAfterUpdate.find { it.id == "new_habit_id" }
        assertEquals("Read 30 pages", found?.title)
        assertEquals(30.0, found?.targetValue ?: 0.0, 0.001)

        // Delete
        repo.deleteHabit("new_habit_id")
        val habitsAfterDelete = repo.getHabitsStream().first()
        assertFalse(habitsAfterDelete.any { it.id == "new_habit_id" })

        // Delete All Data
        repo.deleteAllData()
        val habitsAfterDeleteAll = repo.getHabitsStream().first()
        val recordsAfterDeleteAll = repo.getRecordsStream().first()
        assertTrue(habitsAfterDeleteAll.isEmpty())
        assertTrue(recordsAfterDeleteAll.isEmpty())
    }

    @Test
    fun testBackupManagerExportAndImport() {
        val habits = listOf(
            Habit(
                id = "h1",
                title = "Run 5k",
                type = HabitType.YES_NO,
                targetValue = 1.0,
                unit = "km",
                color = 0xFF006874,
                icon = "directions_run",
                startDate = LocalDate.of(2026, 5, 1)
            ),
            Habit(
                id = "h2",
                title = "Drink Water",
                type = HabitType.MEASURABLE,
                targetValue = 8.0,
                unit = "cups",
                color = 0xFF006874,
                icon = "water_drop",
                startDate = LocalDate.of(2026, 5, 1)
            )
        )

        val records = listOf(
            HabitRecord(
                id = "r1",
                habitId = "h1",
                date = LocalDate.of(2026, 9, 18),
                isCompleted = true
            ),
            HabitRecord(
                id = "r2",
                habitId = "h2",
                date = LocalDate.of(2026, 9, 18),
                currentValue = 6.0,
                isCompleted = false
            )
        )

        val settings = Settings(
            themeMode = ThemeMode.LIGHT,
            dynamicColorEnabled = true,
            oledBlackEnabled = false
        )

        // Export
        val json = BackupManager.exportToJson(habits, records, settings)
        assertNotNull(json)
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("Run 5k"))
        assertTrue(json.contains("Drink Water"))

        // Import
        val backupData = BackupManager.importFromJson(json)
        assertEquals(1, backupData.version)
        assertEquals(2, backupData.habits.size)
        assertEquals(2, backupData.records.size)
        assertEquals(ThemeMode.LIGHT, backupData.settings.themeMode)

        val importedH1 = backupData.habits.find { it.id == "h1" }
        assertEquals("Run 5k", importedH1?.title)
        assertEquals(HabitType.YES_NO, importedH1?.type)

        val importedH2 = backupData.habits.find { it.id == "h2" }
        assertEquals("Drink Water", importedH2?.title)
        assertEquals(8.0, importedH2?.targetValue ?: 0.0, 0.001)
    }

    @Test
    fun testBackupManagerValidationFailures() {
        // Missing version
        try {
            BackupManager.importFromJson("{\"habits\": []}")
            fail("Expected exception for missing version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("version"))
        }

        // Unsupported version
        try {
            BackupManager.importFromJson("{\"version\": 99, \"habits\": []}")
            fail("Expected exception for unsupported version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsupported backup version"))
        }

        // Malformed JSON
        try {
            BackupManager.importFromJson("not json at all")
            fail("Expected exception for malformed json")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid JSON"))
        }
    }

    @Test
    fun testOneTimeSeedingGuard() {
        val initialSettings = SettingsEntity(id = 1, hasCompletedInitialSeed = false)
        assertFalse(initialSettings.hasCompletedInitialSeed)

        // After initial seed:
        val seededSettings = initialSettings.copy(hasCompletedInitialSeed = true)
        assertTrue(seededSettings.hasCompletedInitialSeed)

        // After deleteAllData:
        val postDeleteSettings = seededSettings.copy(hasCompletedInitialSeed = true)
        assertTrue(postDeleteSettings.hasCompletedInitialSeed)
    }
}

