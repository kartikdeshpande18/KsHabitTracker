package com.kartiks.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.model.ThemeMode
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val oledBlackEnabled: Boolean = false,
    val remindersEnabled: Boolean = false,
    val defaultReminderTime: LocalTime = LocalTime.of(20, 0),
    val weekStartsOn: DayOfWeek = DayOfWeek.MONDAY,
    val hasCompletedInitialSeed: Boolean = false
) {
    fun toDomainModel(): Settings {
        return Settings(
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            oledBlackEnabled = oledBlackEnabled,
            remindersEnabled = remindersEnabled,
            defaultReminderTime = defaultReminderTime,
            weekStartsOn = weekStartsOn
        )
    }

    companion object {
        fun fromDomainModel(settings: Settings, hasCompletedInitialSeed: Boolean): SettingsEntity {
            return SettingsEntity(
                id = 1,
                themeMode = settings.themeMode,
                dynamicColorEnabled = settings.dynamicColorEnabled,
                oledBlackEnabled = settings.oledBlackEnabled,
                remindersEnabled = settings.remindersEnabled,
                defaultReminderTime = settings.defaultReminderTime,
                weekStartsOn = settings.weekStartsOn,
                hasCompletedInitialSeed = hasCompletedInitialSeed
            )
        }
    }
}
