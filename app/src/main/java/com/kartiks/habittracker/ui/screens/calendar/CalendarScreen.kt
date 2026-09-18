package com.kartiks.habittracker.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.filled.Settings
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitRecord
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.domain.model.isCompletedWith
import com.kartiks.habittracker.domain.model.isScheduledOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    allHabits: List<Habit>,
    allRecords: List<HabitRecord>,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = LocalDate.now()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Calendar",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track your daily completion history",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Material 3 Expressive Settings Icon Button
                Surface(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Month Navigation Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header: Month, Year + Prev/Next buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row {
                            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month"
                                )
                            }
                            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week header (Mon - Sun)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY,
                            DayOfWeek.SUNDAY
                        ).forEach { dow ->
                            Text(
                                text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Days Grid
                    val firstDayOfMonth = displayedMonth.atDay(1)
                    val daysInMonth = displayedMonth.lengthOfMonth()
                    // 1 = Monday ... 7 = Sunday
                    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1

                    val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7

                    val recordsByDate = allRecords.groupBy { it.date }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - dayOfWeekOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val date = displayedMonth.atDay(dayNum)
                                        val isToday = date == today
                                        val isSelected = date == selectedDate
                                        val isFuture = date.isAfter(today)

                                        val dayRecords = recordsByDate[date] ?: emptyList()
                                        val scheduledHabits = allHabits.filter { it.isScheduledOn(date) }
                                        val scheduledCount = scheduledHabits.size
                                        val completedCount = if (scheduledCount > 0) {
                                            scheduledHabits.count { habit ->
                                                val rec = dayRecords.find { it.habitId == habit.id }
                                                habit.isCompletedWith(rec)
                                            }
                                        } else 0

                                        // Data-driven completion tones (0%, partial, 100%), with Today and Selected rings layered without erasing completion colors
                                        val containerColor = when {
                                            isFuture -> Color.Transparent
                                            scheduledCount == 0 -> Color.Transparent
                                            completedCount == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
                                            completedCount == scheduledCount -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                                        }

                                        val border = when {
                                            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                            isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                                            else -> null
                                        }

                                        val textColor = when {
                                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            scheduledCount == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            completedCount == scheduledCount -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = containerColor,
                                            border = border,
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clickable { onSelectDate(date) }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Details for Selected Date
        item {
            val selectedRecords = allRecords.filter { it.date == selectedDate }
            val scheduledHabitsForSelectedDate = allHabits.filter { it.isScheduledOn(selectedDate) }
            val completed = scheduledHabitsForSelectedDate.count { habit ->
                val rec = selectedRecords.find { it.habitId == habit.id }
                habit.isCompletedWith(rec)
            }
            val scheduled = scheduledHabitsForSelectedDate.size

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$completed / $scheduled completed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (scheduledHabitsForSelectedDate.isEmpty()) {
                        Text(
                            text = "No habits scheduled for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        scheduledHabitsForSelectedDate.forEach { habit ->
                            val rec = selectedRecords.find { it.habitId == habit.id }
                            val isDone = habit.isCompletedWith(rec)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (habit.type == HabitType.MEASURABLE) {
                                        val cur = rec?.currentValue ?: 0.0
                                        val curStr = if (cur % 1.0 == 0.0) cur.toInt().toString() else String.format("%.1f", cur)
                                        val targetStr = if (habit.targetValue % 1.0 == 0.0) habit.targetValue.toInt().toString() else String.format("%.1f", habit.targetValue)
                                        Text(
                                            text = "$curStr / $targetStr ${habit.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
