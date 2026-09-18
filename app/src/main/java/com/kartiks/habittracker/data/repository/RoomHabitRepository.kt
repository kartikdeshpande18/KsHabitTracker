package com.kartiks.habittracker.data.repository

import android.content.Context
import com.kartiks.habittracker.data.local.AppDatabase
import com.kartiks.habittracker.data.local.entity.HabitEntity
import com.kartiks.habittracker.data.local.entity.HabitRecordEntity
import com.kartiks.habittracker.data.local.entity.SettingsEntity
import com.kartiks.habittracker.data.local.entity.toEntity
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RoomHabitRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) : HabitRepository {

    init {
        scope.launch {
            initSeedIfNeeded()
        }
    }

    suspend fun initSeedIfNeeded() = withContext(ioDispatcher) {
        val existingSettings = database.settingsDao().getSettings()
        if (existingSettings == null) {
            // First run on fresh install: seed sample habits and records
            val initialHabits = InMemoryHabitRepository.createInitialHabits()
            val initialRecords = InMemoryHabitRepository.createInitialRecords()
            database.habitDao().insertHabits(initialHabits.map { it.toEntity() })
            database.habitRecordDao().insertRecords(initialRecords.map { it.toEntity() })
            database.settingsDao().insertOrUpdateSettings(
                SettingsEntity(
                    id = 1,
                    hasCompletedInitialSeed = true
                )
            )
        } else if (!existingSettings.hasCompletedInitialSeed) {
            val initialHabits = InMemoryHabitRepository.createInitialHabits()
            val initialRecords = InMemoryHabitRepository.createInitialRecords()
            database.habitDao().insertHabits(initialHabits.map { it.toEntity() })
            database.habitRecordDao().insertRecords(initialRecords.map { it.toEntity() })
            database.settingsDao().insertOrUpdateSettings(
                existingSettings.copy(hasCompletedInitialSeed = true)
            )
        }
        // If hasCompletedInitialSeed is already true, NEVER reseed (even if empty)
    }

    override fun getHabitsStream(): Flow<List<Habit>> {
        return database.habitDao().getHabitsFlow()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getRecordsStream(): Flow<List<HabitRecord>> {
        return database.habitRecordDao().getRecordsFlow()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getSettingsStream(): Flow<Settings> {
        return database.settingsDao().getSettingsFlow()
            .map { it?.toDomainModel() ?: Settings() }
            .flowOn(ioDispatcher)
    }

    override suspend fun toggleHabitCompletion(habitId: String, date: LocalDate) = withContext(ioDispatcher) {
        val habitEntity = database.habitDao().getHabitById(habitId) ?: return@withContext
        val existing = database.habitRecordDao().getRecord(habitId, date)
        if (existing != null) {
            val newCompleted = !existing.isCompleted
            val newValue = if (habitEntity.type == HabitType.MEASURABLE) {
                if (newCompleted && existing.currentValue < habitEntity.targetValue) habitEntity.targetValue
                else if (!newCompleted) 0.0
                else existing.currentValue
            } else {
                if (newCompleted) 1.0 else 0.0
            }
            database.habitRecordDao().insertOrUpdateRecord(
                existing.copy(
                    isCompleted = newCompleted,
                    currentValue = newValue,
                    updatedAt = Instant.now()
                )
            )
        } else {
            val targetVal = if (habitEntity.type == HabitType.MEASURABLE) habitEntity.targetValue else 1.0
            database.habitRecordDao().insertOrUpdateRecord(
                HabitRecordEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = date,
                    currentValue = targetVal,
                    isCompleted = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
    }

    override suspend fun updateMeasurableValue(habitId: String, date: LocalDate, delta: Double) = withContext(ioDispatcher) {
        val habitEntity = database.habitDao().getHabitById(habitId) ?: return@withContext
        val existing = database.habitRecordDao().getRecord(habitId, date)
        val currentVal = existing?.currentValue ?: 0.0
        val newVal = (currentVal + delta).coerceAtLeast(0.0)
        val isCompleted = newVal >= habitEntity.targetValue
        if (existing != null) {
            database.habitRecordDao().insertOrUpdateRecord(
                existing.copy(
                    currentValue = newVal,
                    isCompleted = isCompleted,
                    updatedAt = Instant.now()
                )
            )
        } else {
            database.habitRecordDao().insertOrUpdateRecord(
                HabitRecordEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = date,
                    currentValue = newVal,
                    isCompleted = isCompleted,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
    }

    override suspend fun createHabit(habit: Habit) = withContext(ioDispatcher) {
        database.habitDao().insertHabit(habit.toEntity())
    }

    override suspend fun updateHabit(habit: Habit) = withContext(ioDispatcher) {
        database.habitDao().updateHabit(habit.toEntity().copy(updatedAt = Instant.now()))
    }

    override suspend fun deleteHabit(habitId: String) = withContext(ioDispatcher) {
        database.habitRecordDao().deleteRecordsForHabit(habitId)
        database.habitDao().deleteHabitById(habitId)
    }

    override suspend fun deleteAllData() = withContext(ioDispatcher) {
        database.habitRecordDao().deleteAllRecords()
        database.habitDao().deleteAllHabits()
        val currentSettings = database.settingsDao().getSettings() ?: SettingsEntity(id = 1)
        database.settingsDao().insertOrUpdateSettings(
            currentSettings.copy(hasCompletedInitialSeed = true)
        )
    }

    override suspend fun updateSettings(settings: Settings) = withContext(ioDispatcher) {
        val current = database.settingsDao().getSettings()
        val hasSeed = current?.hasCompletedInitialSeed ?: true
        database.settingsDao().insertOrUpdateSettings(
            SettingsEntity.fromDomainModel(settings, hasCompletedInitialSeed = hasSeed)
        )
    }

    companion object {
        @Volatile
        private var instance: RoomHabitRepository? = null

        fun getInstance(context: Context): RoomHabitRepository {
            return instance ?: synchronized(this) {
                instance ?: RoomHabitRepository(AppDatabase.getInstance(context.applicationContext)).also {
                    instance = it
                }
            }
        }
    }
}
