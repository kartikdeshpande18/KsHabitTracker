package com.kartiks.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartiks.habittracker.ui.MainViewModel
import com.kartiks.habittracker.ui.NavigationTab
import com.kartiks.habittracker.ui.components.AppNavigationShell
import com.kartiks.habittracker.ui.components.CreateHabitBottomSheet
import com.kartiks.habittracker.ui.screens.calendar.CalendarScreen
import com.kartiks.habittracker.ui.screens.settings.SettingsScreen
import com.kartiks.habittracker.ui.screens.statistics.HabitDetailScreen
import com.kartiks.habittracker.ui.screens.statistics.StatisticsScreen
import com.kartiks.habittracker.ui.screens.today.TodayScreen
import com.kartiks.habittracker.ui.theme.KsHabitTrackerTheme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            KsHabitTrackerTheme(
                themeMode = uiState.settings.themeMode,
                oledBlackEnabled = uiState.settings.oledBlackEnabled,
                dynamicColor = uiState.settings.dynamicColorEnabled
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val currentScreen = when {
                        uiState.selectedHabitDetail != null -> "detail"
                        uiState.isSettingsOpen -> "settings"
                        else -> "main"
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        label = "root_screen_transition"
                    ) { screen ->
                        when (screen) {
                            "detail" -> {
                                uiState.selectedHabitDetail?.let { (habit, stats) ->
                                    HabitDetailScreen(
                                        habit = habit,
                                        stats = stats,
                                        allRecords = uiState.allRecords,
                                        onEditHabit = viewModel::openEditHabit,
                                        onDeleteHabit = viewModel::deleteHabit,
                                        onBack = viewModel::closeHabitDetail
                                    )
                                }
                            }
                            "settings" -> {
                                SettingsScreen(
                                    settings = uiState.settings,
                                    onUpdateThemeMode = viewModel::updateThemeMode,
                                    onUpdateDynamicColor = viewModel::updateDynamicColor,
                                    onUpdateOledBlack = viewModel::updateOledBlack,
                                    onUpdateReminders = viewModel::updateReminders,
                                    onExportData = viewModel::exportBackup,
                                    onImportData = viewModel::importBackup,
                                    onDeleteAllData = viewModel::deleteAllData,
                                    onBack = viewModel::closeSettings
                                )
                            }
                            else -> {
                                AppNavigationShell(
                                    selectedTab = uiState.selectedTab,
                                    onSelectTab = viewModel::selectTab,
                                    isFabExpanded = uiState.isFabExpanded,
                                    onToggleFab = viewModel::toggleFab,
                                    onCloseFab = viewModel::closeFab,
                                    onSelectCreationType = viewModel::openCreateHabit
                                ) { contentPadding ->
                                    AnimatedContent(
                                        targetState = uiState.selectedTab,
                                        transitionSpec = {
                                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                        },
                                        label = "tab_transition"
                                    ) { tab ->
                                        when (tab) {
                                            NavigationTab.TODAY -> TodayScreen(
                                                selectedDate = uiState.selectedDate,
                                                onSelectDate = viewModel::selectDate,
                                                habits = uiState.habitsForDate,
                                                completedCount = uiState.completedCount,
                                                scheduledCount = uiState.scheduledCount,
                                                globalProgress = uiState.globalProgress,
                                                onToggleCompletion = viewModel::toggleHabitCompletion,
                                                onIncrementMeasurable = { id -> viewModel.updateMeasurable(id, 1.0) },
                                                onDecrementMeasurable = { id -> viewModel.updateMeasurable(id, -1.0) },
                                                onOpenSettings = viewModel::openSettings,
                                                contentPadding = contentPadding
                                            )
                                            NavigationTab.CALENDAR -> CalendarScreen(
                                                selectedDate = uiState.selectedDate,
                                                onSelectDate = viewModel::selectDate,
                                                allHabits = uiState.allHabits,
                                                allRecords = uiState.allRecords,
                                                onOpenSettings = viewModel::openSettings,
                                                contentPadding = contentPadding
                                            )
                                            NavigationTab.STATISTICS -> StatisticsScreen(
                                                globalStats = uiState.globalStats,
                                                selectedPeriod = uiState.statsPeriod,
                                                onSelectPeriod = viewModel::setStatsPeriod,
                                                onSelectHabit = viewModel::openHabitDetail,
                                                onOpenSettings = viewModel::openSettings,
                                                contentPadding = contentPadding
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom sheet when FAB action tapped or Edit Habit tapped
                    uiState.activeCreationType?.let { creationType ->
                        CreateHabitBottomSheet(
                            type = creationType,
                            initialHabit = uiState.editingHabit,
                            onDismiss = viewModel::closeCreateOrEditHabit,
                            onSave = { title, type, target, unit, increment, color, icon, frequency, selectedDays, reminderEnabled, reminderTime ->
                                viewModel.saveHabit(
                                    title = title,
                                    type = type,
                                    targetValue = target,
                                    unit = unit,
                                    increment = increment,
                                    color = color,
                                    icon = icon,
                                    frequency = frequency,
                                    selectedDays = selectedDays,
                                    reminderEnabled = reminderEnabled,
                                    reminderTime = reminderTime
                                )
                            }
                        )
                    }

                    // User feedback dialog (e.g. Export/Import result)
                    uiState.userMessage?.let { msg ->
                        AlertDialog(
                            onDismissRequest = viewModel::dismissUserMessage,
                            title = { Text("Notice") },
                            text = { Text(msg) },
                            confirmButton = {
                                TextButton(onClick = viewModel::dismissUserMessage) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}