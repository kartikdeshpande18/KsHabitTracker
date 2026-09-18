package com.kartiks.habittracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HabitLinearProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
            .height(height)
            .clip(CircleShape),
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}
