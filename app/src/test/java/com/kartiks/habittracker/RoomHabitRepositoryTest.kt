package com.kartiks.habittracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kartiks.habittracker.data.local.AppDatabase
import com.kartiks.habittracker.data.local.entity.SettingsEntity
import com.kartiks.habittracker.data.repository.RoomHabitRepository
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class RoomHabitRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomHabitRepository
    private lateinit var testScope: kotlinx.coroutines.CompletableJob

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        testScope = kotlinx.coroutines.SupervisorJob()
        repository = RoomHabitRepository(
            database = database,
            ioDispatcher = Dispatchers.Unconfined,
            scope = kotlinx.coroutines.CoroutineScope(testScope + Dispatchers.Unconfined)
        )
    }

    @After
    fun teardown() {
        testScope.cancel()
        database.close()
    }

    @Test
    fun testRoomCreateAndRead() = runBlocking {
        val habit = Habit(
            id = "habit_room_1",
            title = "Morning Workout",
            type = HabitType.YES_NO,
            color = 0xFFBA1A1A,
            icon = "fitness_center",
            startDate = LocalDate.of(2026, 9, 1)
        )

        repository.createHabit(habit)

        val habitsFromStream = repository.getHabitsStream().first()
        val persisted = habitsFromStream.find { it.id == "habit_room_1" }
        assertNotNull(persisted)
        assertEquals("Morning Workout", persisted?.title)
        assertEquals(HabitType.YES_NO, persisted?.type)
        assertEquals(0xFFBA1A1A, persisted?.color)

        // Verify directly in DAO
        val directEntity = database.habitDao().getHabitById("habit_room_1")
        assertNotNull(directEntity)
        assertEquals("Morning Workout", directEntity?.title)
    }

    @Test
    fun testRoomToggleCompletionAndRecords() = runBlocking {
        val habit = Habit(
            id = "habit_room_toggle",
            title = "Daily Walk",
            type = HabitType.YES_NO,
            startDate = LocalDate.of(2026, 9, 1)
        )
        repository.createHabit(habit)

        val date = LocalDate.of(2026, 9, 18)
        repository.toggleHabitCompletion(habit.id, date)

        val records = repository.getRecordsStream().first()
        val record = records.find { it.habitId == habit.id && it.date == date }
        assertNotNull(record)
        assertTrue(record!!.isCompleted)

        // Toggle again -> becomes incomplete
        repository.toggleHabitCompletion(habit.id, date)
        val recordsToggled = repository.getRecordsStream().first()
        val recordToggled = recordsToggled.find { it.habitId == habit.id && it.date == date }
        assertNotNull(recordToggled)
        assertFalse(recordToggled!!.isCompleted)
    }

    @Test
    fun testRoomUpdateHabitPreservesHistoricalRecords() = runBlocking {
        val habit = Habit(
            id = "habit_room_update",
            title = "Drink Water",
            type = HabitType.MEASURABLE,
            targetValue = 8.0,
            unit = "cups",
            increment = 1.0
        )
        repository.createHabit(habit)

        // Add a record
        val date = LocalDate.of(2026, 9, 15)
        repository.updateMeasurableValue(habit.id, date, 5.0)

        // Verify record before update
        val recordBefore = repository.getRecordsStream().first().find { it.habitId == habit.id }
        assertEquals(5.0, recordBefore?.currentValue ?: 0.0, 0.001)

        // Update habit definition: target 10, unit "glasses"
        val updatedHabit = habit.copy(
            title = "Drink More Water",
            targetValue = 10.0,
            unit = "glasses"
        )
        repository.updateHabit(updatedHabit)

        // Reload habit from Room
        val reloadedHabit = repository.getHabitsStream().first().find { it.id == habit.id }
        assertEquals("Drink More Water", reloadedHabit?.title)
        assertEquals(10.0, reloadedHabit?.targetValue ?: 0.0, 0.001)
        assertEquals("glasses", reloadedHabit?.unit)

        // Verify historical record was NOT deleted or overwritten
        val recordAfter = repository.getRecordsStream().first().find { it.habitId == habit.id }
        assertNotNull(recordAfter)
        assertEquals(5.0, recordAfter?.currentValue ?: 0.0, 0.001)
    }

    @Test
    fun testRoomDeleteHabitCascadesRecords() = runBlocking {
        val habit = Habit(
            id = "habit_to_delete",
            title = "Delete Me",
            type = HabitType.YES_NO
        )
        repository.createHabit(habit)

        val date1 = LocalDate.of(2026, 9, 10)
        val date2 = LocalDate.of(2026, 9, 11)
        repository.toggleHabitCompletion(habit.id, date1)
        repository.toggleHabitCompletion(habit.id, date2)

        // Confirm 2 records exist
        val recordsBefore = repository.getRecordsStream().first().filter { it.habitId == habit.id }
        assertEquals(2, recordsBefore.size)

        // Delete habit
        repository.deleteHabit(habit.id)

        // Verify habit is gone
        val habitsAfter = repository.getHabitsStream().first()
        assertFalse(habitsAfter.any { it.id == habit.id })
        assertNull(database.habitDao().getHabitById(habit.id))

        // Verify related records are gone
        val recordsAfter = repository.getRecordsStream().first().filter { it.habitId == habit.id }
        assertTrue(recordsAfter.isEmpty())
        assertTrue(database.habitRecordDao().getRecordsForHabit(habit.id).isEmpty())
    }

    @Test
    fun testRoomDeleteAllDataAndNeverReseed() = runBlocking {
        // Initial setup: ensure seed has run
        repository.initSeedIfNeeded()
        val habitsBefore = repository.getHabitsStream().first()
        assertTrue(habitsBefore.isNotEmpty())

        // Execute Delete All Data
        repository.deleteAllData()

        val habitsAfterDelete = repository.getHabitsStream().first()
        val recordsAfterDelete = repository.getRecordsStream().first()
        assertTrue(habitsAfterDelete.isEmpty())
        assertTrue(recordsAfterDelete.isEmpty())

        // Verify hasCompletedInitialSeed is preserved as TRUE
        val settings = database.settingsDao().getSettings()
        assertNotNull(settings)
        assertTrue(settings!!.hasCompletedInitialSeed)

        // Recreate repository access (simulating app restart / process restart)
        val restartedRepo = RoomHabitRepository(
            database = database,
            ioDispatcher = Dispatchers.Unconfined,
            scope = kotlinx.coroutines.CoroutineScope(testScope + Dispatchers.Unconfined)
        )
        restartedRepo.initSeedIfNeeded()

        // Verify database remains completely empty (NEVER reseeds!)
        val habitsAfterRestart = restartedRepo.getHabitsStream().first()
        val recordsAfterRestart = restartedRepo.getRecordsStream().first()
        assertTrue(habitsAfterRestart.isEmpty())
        assertTrue(recordsAfterRestart.isEmpty())
    }

    @Test
    fun testRoomPersistenceAcrossRepositoryInstances() = runBlocking {
        val habit = Habit(
            id = "habit_persisted",
            title = "Persisted Habit",
            type = HabitType.MEASURABLE,
            targetValue = 5.0,
            unit = "miles"
        )
        repository.createHabit(habit)

        // Create new repository instance pointing to the same database
        val newRepoInstance = RoomHabitRepository(
            database = database,
            ioDispatcher = Dispatchers.Unconfined,
            scope = kotlinx.coroutines.CoroutineScope(testScope + Dispatchers.Unconfined)
        )

        val habits = newRepoInstance.getHabitsStream().first()
        val found = habits.find { it.id == "habit_persisted" }
        assertNotNull(found)
        assertEquals("Persisted Habit", found?.title)
        assertEquals(5.0, found?.targetValue ?: 0.0, 0.001)
    }
}
