package com.kartiks.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kartiks.habittracker.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)
}
