package com.kartiks.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kartiks.habittracker.data.local.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitRecordDao {

    @Query("SELECT * FROM habit_records ORDER BY date ASC")
    fun getRecordsFlow(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records ORDER BY date ASC")
    suspend fun getAllRecordsList(): List<HabitRecordEntity>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getRecordsForHabit(habitId: String): List<HabitRecordEntity>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getRecord(habitId: String, date: LocalDate): HabitRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: HabitRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<HabitRecordEntity>)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId")
    suspend fun deleteRecordsForHabit(habitId: String)

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllRecords()
}
