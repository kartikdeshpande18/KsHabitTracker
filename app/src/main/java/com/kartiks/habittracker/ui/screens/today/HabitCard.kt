package com.kartiks.habittracker.ui.screens.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.ui.HabitWithRecord

@Composable
fun HabitCard(
    item: HabitWithRecord,
    onToggleCompletion: () -> Unit,
    onIncrementMeasurable: () -> Unit,
    onDecrementMeasurable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = item.habit
    val isCompleted = item.isCompleted

    val containerColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_container_color"
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (habit.type == HabitType.YES_NO) {
            YesNoHabitContent(
                item = item,
                onToggle = onToggleCompletion
            )
        } else {
            MeasurableHabitContent(
                item = item,
                onIncrement = onIncrementMeasurable,
                onDecrement = onDecrementMeasurable
            )
        }
    }
}

@Composable
private fun YesNoHabitContent(
    item: HabitWithRecord,
    onToggle: () -> Unit
) {
    val habit = item.habit
    val isCompleted = item.isCompleted
    val haptics = com.kartiks.habittracker.ui.interaction.rememberAppHaptics()

    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptics.habitCompleted()
                onToggle()
            })
            .padding(16.dp)
    ) {
        // Left side: Icon, Title, Streak badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            HabitIconBadge(
                icon = habit.icon,
                color = Color(habit.color)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                StreakBadge(streak = item.currentStreak)
            }
        }

        // Right side: Circular Completion Control
        Surface(
            shape = CircleShape,
            color = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
            border = if (!isCompleted) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
            modifier = Modifier
                .size(36.dp)
                .scale(checkScale)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurableHabitContent(
    item: HabitWithRecord,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val habit = item.habit
    val currentVal = item.currentValue
    val targetVal = habit.targetValue
    val progress = (currentVal / targetVal).toFloat().coerceIn(0f, 1f)
    val haptics = com.kartiks.habittracker.ui.interaction.rememberAppHaptics()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Left side: Value/target shown ONCE + Progress bar
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HabitIconBadge(
                    icon = habit.icon,
                    color = Color(habit.color)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StreakBadge(streak = item.currentStreak)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value / Target display ONCE
            Text(
                text = "${formatValue(currentVal)} / ${formatValue(targetVal)} ${habit.unit}".trim(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Linear Progress Indicator without endpoint dot
            com.kartiks.habittracker.ui.components.HabitLinearProgress(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.9f),
                height = 6.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }


        Spacer(modifier = Modifier.width(12.dp))

        // Right side: ONLY − and + action buttons. No duplicate text inside buttons!
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decrement Button
            OutlinedIconButton(
                onClick = {
                    haptics.stepTick()
                    onDecrement()
                },
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Increment Button
            Surface(
                onClick = {
                    if (currentVal < targetVal && currentVal + habit.increment >= targetVal) {
                        haptics.targetReached()
                    } else {
                        haptics.stepTick()
                    }
                    onIncrement()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitIconBadge(
    icon: String,
    color: Color
) {
    val vector = resolveHabitIcon(icon)
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.18f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    if (streak > 0) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "🔥 $streak day streak",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun resolveHabitIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "water_drop" -> Icons.Default.WaterDrop
        "fitness_center" -> Icons.Default.FitnessCenter
        "self_improvement" -> Icons.Default.SelfImprovement
        "directions_run" -> Icons.AutoMirrored.Filled.DirectionsRun
        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
        "medication" -> Icons.Default.Medication

        else -> Icons.Default.Check
    }
}

private fun formatValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
