package com.kartiks.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kartiks.habittracker.data.local.dao.HabitDao
import com.kartiks.habittracker.data.local.dao.HabitRecordDao
import com.kartiks.habittracker.data.local.dao.SettingsDao
import com.kartiks.habittracker.data.local.entity.HabitEntity
import com.kartiks.habittracker.data.local.entity.HabitRecordEntity
import com.kartiks.habittracker.data.local.entity.SettingsEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitRecordEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "ks_habit_tracker.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
