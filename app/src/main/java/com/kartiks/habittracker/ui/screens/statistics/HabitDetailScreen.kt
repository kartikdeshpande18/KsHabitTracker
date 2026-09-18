package com.kartiks.habittracker.ui.screens.statistics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitStats
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.isCompletedWith
import com.kartiks.habittracker.domain.model.isScheduledOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import com.kartiks.habittracker.ui.components.KpiStatCard
import java.time.YearMonth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habit: Habit,
    stats: HabitStats,
    allRecords: List<HabitRecord> = emptyList(),
    onEditHabit: (Habit) -> Unit = {},
    onDeleteHabit: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val today = LocalDate.now()
    val currentYearMonth = remember(today) { YearMonth.from(today) }
    val daysInMonth = remember(currentYearMonth) { currentYearMonth.lengthOfMonth() }
    val startOffset = remember(currentYearMonth) { currentYearMonth.atDay(1).dayOfWeek.value - 1 }

    val monthWeeks = remember(currentYearMonth) {
        val weeks = mutableListOf<List<LocalDate?>>()
        var currentWeek = MutableList<LocalDate?>(7) { null }
        var col = startOffset
        for (day in 1..daysInMonth) {
            currentWeek[col] = currentYearMonth.atDay(day)
            col++
            if (col == 7) {
                weeks.add(currentWeek)
                currentWeek = MutableList(7) { null }
                col = 0
            }
        }
        if (col > 0) {
            weeks.add(currentWeek)
        }
        weeks
    }

    var selectedConsistencyDate by remember { mutableStateOf<LocalDate?>(today) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val haptics = com.kartiks.habittracker.ui.interaction.rememberAppHaptics()

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete \"${habit.title}\"? All historical records for this habit will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteHabit(habit.id)
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Habit") },
                                onClick = {
                                    showOverflowMenu = false
                                    onEditHabit(habit)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Habit", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Habit Header Badge Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(habit.color).copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(habit.color),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = if (habit.type == HabitType.YES_NO) "Yes / No Habit" else "Measurable (${habit.targetValue.toInt()} ${habit.unit}/day)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🔥 ${stats.currentStreak} Day Current Streak",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Consistency Card (Full Current Month Compact Grid, M T W T F S S, tappable inspection)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consistency",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Weekday labels: M T W T F S S
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Full Current Month Weeks
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            monthWeeks.forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    week.forEach { date ->
                                        if (date == null) {
                                            Box(modifier = Modifier.size(32.dp))
                                        } else {
                                            val record = allRecords.find { it.habitId == habit.id && it.date == date }
                                            val isScheduled = habit.isScheduledOn(date)
                                            val isFuture = date.isAfter(today)
                                            val isDone = habit.isCompletedWith(record)
                                            val isPartial = habit.type == HabitType.MEASURABLE && !isDone && (record?.currentValue ?: 0.0) > 0.0
                                            val isSelected = date == selectedConsistencyDate

                                            val indicatorColor = when {
                                                isFuture || !isScheduled -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
                                                isDone -> MaterialTheme.colorScheme.primary
                                                isPartial -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                            }

                                            val border = when {
                                                isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                                date == today -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                                                !isDone && !isPartial && isScheduled && !isFuture -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                                else -> null
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable {
                                                        haptics.dateSelected()
                                                        selectedConsistencyDate = date
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = indicatorColor,
                                                    border = border,
                                                    modifier = Modifier.size(14.dp)
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Selected indicator date details
                        selectedConsistencyDate?.let { date ->
                            val record = allRecords.find { it.habitId == habit.id && it.date == date }
                            val isScheduled = habit.isScheduledOn(date)
                            val isFuture = date.isAfter(today)
                            val isDone = habit.isCompletedWith(record)
                            val isPartial = habit.type == HabitType.MEASURABLE && !isDone && (record?.currentValue ?: 0.0) > 0.0

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = date.format(DateTimeFormatter.ofPattern("MMM d")),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val statusText = when {
                                        isFuture -> "Future"
                                        !isScheduled -> "Not scheduled"
                                        habit.type == HabitType.MEASURABLE -> {
                                            val cur = record?.currentValue ?: 0.0
                                            val target = habit.targetValue
                                            val curStr = if (cur % 1.0 == 0.0) cur.toInt().toString() else String.format("%.1f", cur)
                                            val targetStr = if (target % 1.0 == 0.0) target.toInt().toString() else String.format("%.1f", target)
                                            val attainment = if (isDone) "Completed" else if (isPartial) "Partial" else "No progress"
                                            "$curStr / $targetStr ${habit.unit} • $attainment"
                                        }
                                        isDone -> "Completed"
                                        else -> "Not completed"
                                    }

                                    val statusColor = when {
                                        isDone -> MaterialTheme.colorScheme.primary
                                        isPartial -> MaterialTheme.colorScheme.tertiary
                                        isFuture || !isScheduled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Standard Habit KPIs Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = "Current Streak",
                            value = "${stats.currentStreak} days",
                            subtitle = "Consecutive completions",
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "Best Streak",
                            value = "${stats.bestStreak} days",
                            subtitle = "Personal record",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = "Completion Rate",
                            value = "${(stats.completionRate * 100).toInt()}%",
                            subtitle = "Historical adherence",
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "Total Completions",
                            value = "${stats.totalCompletions}",
                            subtitle = "Lifetime recorded days",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = "Completed This Month",
                            value = "${stats.completedThisMonth} days",
                            subtitle = "Current calendar month",
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "Target Attainment",
                            value = "${(stats.targetAttainmentRate * 100).toInt()}%",
                            subtitle = "Goal adherence",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Additional Measurable Metrics (when habit is MEASURABLE)
            if (habit.type == HabitType.MEASURABLE) {
                item {
                    Text(
                        text = "Measurable Targets & Units",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiStatCard(
                                title = "Daily Target",
                                value = "${habit.targetValue.toInt()} ${habit.unit}",
                                subtitle = "Scheduled daily goal",
                                modifier = Modifier.weight(1f)
                            )
                            KpiStatCard(
                                title = "Avg Achieved Value",
                                value = String.format("%.1f %s", stats.averageAchievedValue, habit.unit),
                                subtitle = "Per recorded day",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiStatCard(
                                title = "Total Units Recorded",
                                value = "${stats.totalUnitsRecorded.toInt()} ${habit.unit}",
                                subtitle = "Cumulative volume",
                                modifier = Modifier.weight(1f)
                            )
                            KpiStatCard(
                                title = "Days Target Reached",
                                value = "${stats.daysTargetReached} days",
                                subtitle = "Full target completion",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
