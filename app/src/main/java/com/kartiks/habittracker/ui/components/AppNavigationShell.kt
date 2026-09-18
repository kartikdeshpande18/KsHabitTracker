package com.kartiks.habittracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kartiks.habittracker.domain.model.HabitType
import com.kartiks.habittracker.ui.NavigationTab

@Composable
fun AppNavigationShell(
    selectedTab: NavigationTab,
    onSelectTab: (NavigationTab) -> Unit,
    isFabExpanded: Boolean,
    onToggleFab: () -> Unit,
    onCloseFab: () -> Unit,
    onSelectCreationType: (HabitType) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val haptics = com.kartiks.habittracker.ui.interaction.rememberAppHaptics()

    // Dynamically derived insets:
    // Top: status bar height + 16dp margin
    // Bottom: nav bar height + 64dp capsule + 24dp bottom offset + 16dp spacing = navBarInset + 104dp
    val screenContentPadding = PaddingValues(
        top = statusBarInset + 16.dp,
        bottom = navBarInset + 104.dp
    )

    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Screen Content (scrolls behind floating controls with derived padding)
        content(screenContentPadding)

        // 2. Animated Scrim for FAB expansion
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessHigh))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptics.fabToggle()
                            onCloseFab()
                        }
                    )
            )
        }

        // 3. Expanded FAB Options Overlay (anchored directly above FAB, sharing the 268dp centered control group bounds)
        // Mathematically locks the option pills' right edge to the 64dp FAB's right edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarInset + 24.dp + 64.dp + 10.dp)
                .width(268.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Yes / No Option Pill (top item in vertical stack)
                // Enters after Measurable on open; exits first on dismiss
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, delayMillis = 60)
                    ) + slideInVertically(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    ) { it / 2 },
                    exit = fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, delayMillis = 0)
                    ) + slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    ) { it / 2 }
                ) {
                    FabActionItem(
                        label = "Yes / No",
                        icon = Icons.Rounded.CheckBox,
                        onClick = {
                            haptics.fabToggle()
                            onCloseFab()
                            onSelectCreationType(HabitType.YES_NO)
                        }
                    )
                }

                // Measurable Option Pill (middle item, directly above FAB)
                // Enters first on open; exits after Yes / No on dismiss
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, delayMillis = 0)
                    ) + slideInVertically(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    ) { it / 2 },
                    exit = fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 120, delayMillis = 50)
                    ) + slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    ) { it / 2 }
                ) {
                    FabActionItem(
                        label = "Measurable",
                        icon = Icons.Rounded.Tag,
                        onClick = {
                            haptics.fabToggle()
                            onCloseFab()
                            onSelectCreationType(HabitType.MEASURABLE)
                        }
                    )
                }
            }
        }

        // 4. Centered Bottom Controls Group: Capsule (192dp) + 12dp Gap + FAB (64dp)
        // Group total width: 192dp + 12dp + 64dp = 268dp
        // Sits at (navBarInset + 24.dp) above system navigation bar; center X == screen center X
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarInset + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating Compact Capsule Navigation Bar (192dp x 64dp)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTabItem(
                        icon = Icons.Default.Today,
                        label = "Today",
                        isSelected = selectedTab == NavigationTab.TODAY,
                        onClick = {
                            if (isFabExpanded) onCloseFab()
                            if (selectedTab != NavigationTab.TODAY) haptics.tabSelected()
                            onSelectTab(NavigationTab.TODAY)
                        }
                    )
                    NavigationTabItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Calendar",
                        isSelected = selectedTab == NavigationTab.CALENDAR,
                        onClick = {
                            if (isFabExpanded) onCloseFab()
                            if (selectedTab != NavigationTab.CALENDAR) haptics.tabSelected()
                            onSelectTab(NavigationTab.CALENDAR)
                        }
                    )
                    NavigationTabItem(
                        icon = Icons.Default.BarChart,
                        label = "Statistics",
                        isSelected = selectedTab == NavigationTab.STATISTICS,
                        onClick = {
                            if (isFabExpanded) onCloseFab()
                            if (selectedTab != NavigationTab.STATISTICS) haptics.tabSelected()
                            onSelectTab(NavigationTab.STATISTICS)
                        }
                    )
                }
            }

            // Main FAB Button (64dp circle)
            FloatingActionButton(
                onClick = {
                    haptics.fabToggle()
                    onToggleFab()
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isFabExpanded) "Close add options" else "Add habit",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(fabRotation)
                )
            }
        }
    }
}

@Composable
private fun NavigationTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = label
                    selected = isSelected
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
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
