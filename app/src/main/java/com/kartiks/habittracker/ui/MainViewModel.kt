package com.kartiks.habittracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kartiks.habittracker.data.backup.BackupManager
import com.kartiks.habittracker.data.local.AppDatabase
import com.kartiks.habittracker.data.repository.RoomHabitRepository
import com.kartiks.habittracker.domain.model.GlobalStats
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitStats
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.Settings
import com.kartiks.habittracker.domain.model.StatsPeriod
import com.kartiks.habittracker.domain.model.ThemeMode
import com.kartiks.habittracker.domain.model.isCompletedWith
import com.kartiks.habittracker.domain.model.isScheduledOn
import com.kartiks.habittracker.domain.repository.HabitRepository
import com.kartiks.habittracker.notifications.HabitReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

enum class NavigationTab {
    TODAY,
    CALENDAR,
    STATISTICS
}

data class HabitWithRecord(
    val habit: Habit,
    val record: HabitRecord?,
    val isCompleted: Boolean,
    val currentValue: Double,
    val currentStreak: Int
)

data class MainUiState(
    val selectedTab: NavigationTab = NavigationTab.TODAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val isSettingsOpen: Boolean = false,
    val isFabExpanded: Boolean = false,
    val activeCreationType: HabitType? = null,
    val editingHabit: Habit? = null,
    val selectedHabitDetail: Pair<Habit, HabitStats>? = null,
    val statsPeriod: StatsPeriod = StatsPeriod.WEEK,
    val habitsForDate: List<HabitWithRecord> = emptyList(),
    val completedCount: Int = 0,
    val scheduledCount: Int = 0,
    val globalProgress: Float = 0f,
    val allHabits: List<Habit> = emptyList(),
    val allRecords: List<HabitRecord> = emptyList(),
    val globalStats: GlobalStats = GlobalStats(),
    val settings: Settings = Settings(),
    val userMessage: String? = null
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: HabitRepository = RoomHabitRepository.getInstance(application)

    private val _selectedTab = MutableStateFlow(NavigationTab.TODAY)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _isFabExpanded = MutableStateFlow(false)
    private val _activeCreationType = MutableStateFlow<HabitType?>(null)
    private val _editingHabit = MutableStateFlow<Habit?>(null)
    private val _selectedHabitIdForDetail = MutableStateFlow<String?>(null)
    private val _statsPeriod = MutableStateFlow(StatsPeriod.WEEK)
    private val _userMessage = MutableStateFlow<String?>(null)

    private data class LocalUiControls(
        val tab: NavigationTab,
        val date: LocalDate,
        val isSettingsOpen: Boolean,
        val isFabExpanded: Boolean,
        val activeCreationType: HabitType?,
        val editingHabit: Habit?,
        val statsPeriod: StatsPeriod,
        val userMessage: String?
    )

    private val uiControls = combine(
        combine(_selectedTab, _selectedDate, _isSettingsOpen) { tab, date, settingsOpen ->
            Triple(tab, date, settingsOpen)
        },
        combine(_isFabExpanded, _activeCreationType, _editingHabit) { fab, createType, editingHabit ->
            Triple(fab, createType, editingHabit)
        },
        combine(_statsPeriod, _userMessage) { period, msg ->
            Pair(period, msg)
        }
    ) { (tab, date, settingsOpen), (fab, createType, editingHabit), (period, msg) ->
        LocalUiControls(tab, date, settingsOpen, fab, createType, editingHabit, period, msg)
    }

    val uiState: StateFlow<MainUiState> = combine(
        uiControls,
        _selectedHabitIdForDetail,
        repository.getHabitsStream(),
        repository.getRecordsStream(),
        repository.getSettingsStream()
    ) { controls, detailHabitId, habits, records, settings ->
        val selectedDate = controls.date
        val allActiveHabits = habits.filter { !it.archived }
        val scheduledHabitsForDate = allActiveHabits.filter { it.isScheduledOn(selectedDate) }

        val recordsMap = records.filter { it.date == selectedDate }.associateBy { it.habitId }

        val habitsWithRecords = scheduledHabitsForDate.map { habit ->
            val record = recordsMap[habit.id]
            val isCompleted = habit.isCompletedWith(record)
            val currentValue = record?.currentValue ?: 0.0
            val stats = repository.calculateHabitStats(habit, records, selectedDate)
            HabitWithRecord(
                habit = habit,
                record = record,
                isCompleted = isCompleted,
                currentValue = currentValue,
                currentStreak = stats.currentStreak
            )
        }

        val completedCount = habitsWithRecords.count { it.isCompleted }
        val scheduledCount = habitsWithRecords.size
        val progress = if (scheduledCount > 0) completedCount.toFloat() / scheduledCount.toFloat() else 0f

        val globalStats = repository.calculateGlobalStats(allActiveHabits, records, controls.statsPeriod, selectedDate)

        val detailHabitAndStats = detailHabitId?.let { id ->
            allActiveHabits.find { it.id == id }?.let { habit ->
                habit to repository.calculateHabitStats(habit, records, selectedDate)
            }
        }

        MainUiState(
            selectedTab = controls.tab,
            selectedDate = selectedDate,
            isSettingsOpen = controls.isSettingsOpen,
            isFabExpanded = controls.isFabExpanded,
            activeCreationType = controls.activeCreationType,
            editingHabit = controls.editingHabit,
            selectedHabitDetail = detailHabitAndStats,
            statsPeriod = controls.statsPeriod,
            habitsForDate = habitsWithRecords,
            completedCount = completedCount,
            scheduledCount = scheduledCount,
            globalProgress = progress,
            allHabits = allActiveHabits,
            allRecords = records,
            globalStats = globalStats,
            settings = settings,
            userMessage = controls.userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun selectTab(tab: NavigationTab) {
        _selectedTab.value = tab
        _isFabExpanded.value = false
        _selectedHabitIdForDetail.value = null
        _isSettingsOpen.value = false
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun openSettings() {
        _isSettingsOpen.value = true
        _isFabExpanded.value = false
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun toggleFab() {
        _isFabExpanded.update { !it }
    }

    fun closeFab() {
        _isFabExpanded.value = false
    }

    fun openCreateHabit(type: HabitType) {
        _isFabExpanded.value = false
        _editingHabit.value = null
        _activeCreationType.value = type
    }

    fun openEditHabit(habit: Habit) {
        _editingHabit.value = habit
        _activeCreationType.value = habit.type
    }

    fun closeCreateOrEditHabit() {
        _activeCreationType.value = null
        _editingHabit.value = null
    }

    fun saveHabit(
        title: String,
        type: HabitType,
        targetValue: Double = 1.0,
        unit: String = "",
        increment: Double = 1.0,
        color: Long = 0xFF006874,
        icon: String = "check",
        frequency: HabitFrequency = HabitFrequency.DAILY,
        selectedDays: Set<java.time.DayOfWeek> = java.time.DayOfWeek.entries.toSet(),
        reminderEnabled: Boolean = false,
        reminderTime: java.time.LocalTime? = null
    ) {
        viewModelScope.launch {
            val existing = _editingHabit.value
            val habitToPersist = if (existing != null) {
                existing.copy(
                    title = title,
                    type = type,
                    targetValue = targetValue,
                    unit = unit,
                    increment = increment,
                    color = color,
                    icon = icon,
                    frequency = frequency,
                    selectedDays = selectedDays,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    updatedAt = java.time.Instant.now()
                )
            } else {
                Habit(
                    title = title,
                    type = type,
                    targetValue = targetValue,
                    unit = unit,
                    increment = increment,
                    color = color,
                    icon = icon,
                    frequency = frequency,
                    selectedDays = selectedDays,
                    startDate = _selectedDate.value,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime
                )
            }

            if (existing != null) {
                repository.updateHabit(habitToPersist)
            } else {
                repository.createHabit(habitToPersist)
            }

            if (habitToPersist.reminderEnabled && uiState.value.settings.remindersEnabled) {
                HabitReminderScheduler.scheduleReminderForHabit(getApplication(), habitToPersist)
            } else {
                HabitReminderScheduler.cancelReminderForHabit(getApplication(), habitToPersist.id)
            }

            _activeCreationType.value = null
            _editingHabit.value = null
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            HabitReminderScheduler.cancelReminderForHabit(getApplication(), habitId)
            if (_selectedHabitIdForDetail.value == habitId) {
                _selectedHabitIdForDetail.value = null
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            HabitReminderScheduler.cancelAll(getApplication())
            _selectedHabitIdForDetail.value = null
            _userMessage.value = "All habit data cleared."
        }
    }

    fun toggleHabitCompletion(habitId: String) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, _selectedDate.value)
        }
    }

    fun updateMeasurable(habitId: String, delta: Double) {
        viewModelScope.launch {
            repository.updateMeasurableValue(habitId, _selectedDate.value, delta)
        }
    }

    fun openHabitDetail(habitId: String) {
        _selectedHabitIdForDetail.value = habitId
    }

    fun closeHabitDetail() {
        _selectedHabitIdForDetail.value = null
    }

    fun setStatsPeriod(period: StatsPeriod) {
        _statsPeriod.value = period
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateSettings(uiState.value.settings.copy(themeMode = mode))
        }
    }

    fun updateOledBlack(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(uiState.value.settings.copy(oledBlackEnabled = enabled))
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(uiState.value.settings.copy(dynamicColorEnabled = enabled))
        }
    }

    fun updateReminders(enabled: Boolean) {
        viewModelScope.launch {
            val updatedSettings = uiState.value.settings.copy(remindersEnabled = enabled)
            repository.updateSettings(updatedSettings)
            if (enabled) {
                HabitReminderScheduler.rescheduleAll(getApplication())
            } else {
                HabitReminderScheduler.cancelAll(getApplication())
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = uiState.value
                val jsonString = BackupManager.exportToJson(
                    habits = state.allHabits,
                    records = state.allRecords,
                    settings = state.settings
                )
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(jsonString)
                        writer.flush()
                    }
                }
                _userMessage.value = "Data exported successfully!"
            } catch (e: Exception) {
                _userMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                } ?: throw IllegalArgumentException("Could not read from selected file.")

                val backupData = BackupManager.importFromJson(content)

                // Restore to Room DB
                val db = AppDatabase.getInstance(getApplication())
                db.habitRecordDao().deleteAllRecords()
                db.habitDao().deleteAllHabits()

                db.habitDao().insertHabits(backupData.habits.map { com.kartiks.habittracker.data.local.entity.HabitEntity.fromDomainModel(it) })
                db.habitRecordDao().insertRecords(backupData.records.map { com.kartiks.habittracker.data.local.entity.HabitRecordEntity.fromDomainModel(it) })
                repository.updateSettings(backupData.settings)

                _userMessage.value = "Data imported successfully! (${backupData.habits.size} habits restored)"
            } catch (e: Exception) {
                _userMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
