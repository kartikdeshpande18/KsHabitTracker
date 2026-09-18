package com.kartiks.habittracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kartiks.habittracker.domain.model.HabitType

@Composable
fun ExpandingFab(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelectType: (HabitType) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Yes / No Action Popout
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 180, delayMillis = 60)
            ) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ) { it / 2 },
            exit = fadeOut(
                animationSpec = tween(durationMillis = 100, delayMillis = 0)
            ) + slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessHigh)
            ) { it / 2 }
        ) {
            FabActionItem(
                label = "Yes / No",
                icon = Icons.Rounded.CheckBox,
                onClick = { onSelectType(HabitType.YES_NO) }
            )
        }

        // Measurable Action Popout
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 150, delayMillis = 0)
            ) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ) { it / 2 },
            exit = fadeOut(
                animationSpec = tween(durationMillis = 120, delayMillis = 50)
            ) + slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessHigh)
            ) { it / 2 }
        ) {
            FabActionItem(
                label = "Measurable",
                icon = Icons.Rounded.Tag,
                onClick = { onSelectType(HabitType.MEASURABLE) }
            )
        }

        // Main FAB Button (morphs + to ×)
        FloatingActionButton(
            onClick = onToggle,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Close add habit options" else "Add habit",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
