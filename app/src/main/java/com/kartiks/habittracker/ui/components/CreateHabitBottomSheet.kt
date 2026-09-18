package com.kartiks.habittracker.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kartiks.habittracker.domain.model.Habit
import com.kartiks.habittracker.domain.model.HabitFrequency
import com.kartiks.habittracker.domain.model.HabitType
import java.time.DayOfWeek
import java.time.LocalTime

val AVAILABLE_HABIT_COLORS = listOf(
    0xFF006874, // Teal
    0xFFBA1A1A, // Red
    0xFF6750A4, // Purple
    0xFF2E6B27, // Green
    0xFF196489, // Deep Blue
    0xFF8A5100, // Amber
    0xFF9C4146, // Rose
    0xFF5D5B8D  // Indigo
)

val AVAILABLE_HABIT_ICONS = listOf(
    "check" to Icons.Default.Check,
    "water_drop" to Icons.Default.WaterDrop,
    "fitness_center" to Icons.Default.FitnessCenter,
    "self_improvement" to Icons.Default.SelfImprovement,
    "directions_run" to Icons.AutoMirrored.Filled.DirectionsRun,
    "menu_book" to Icons.AutoMirrored.Filled.MenuBook,
    "medication" to Icons.Default.Medication
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitBottomSheet(
    type: HabitType,
    initialHabit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        type: HabitType,
        target: Double,
        unit: String,
        increment: Double,
        color: Long,
        icon: String,
        frequency: HabitFrequency,
        selectedDays: Set<DayOfWeek>,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditMode = initialHabit != null

    var title by remember(initialHabit) { mutableStateOf(initialHabit?.title ?: "") }
    var targetValueText by remember(initialHabit, type) {
        mutableStateOf(
            if (initialHabit != null && initialHabit.type == HabitType.MEASURABLE) {
                if (initialHabit.targetValue % 1.0 == 0.0) initialHabit.targetValue.toInt().toString() else initialHabit.targetValue.toString()
            } else if (type == HabitType.MEASURABLE) "8" else "1"
        )
    }
    var unitText by remember(initialHabit, type) {
        mutableStateOf(initialHabit?.unit ?: if (type == HabitType.MEASURABLE) "glasses" else "")
    }
    var incrementText by remember(initialHabit) {
        mutableStateOf(
            if (initialHabit != null && initialHabit.type == HabitType.MEASURABLE) {
                if (initialHabit.increment % 1.0 == 0.0) initialHabit.increment.toInt().toString() else initialHabit.increment.toString()
            } else "1"
        )
    }

    var selectedColor by remember(initialHabit) {
        mutableLongStateOf(initialHabit?.color ?: AVAILABLE_HABIT_COLORS.first())
    }
    var selectedIcon by remember(initialHabit) {
        mutableStateOf(initialHabit?.icon ?: "check")
    }

    var frequency by remember(initialHabit) {
        mutableStateOf(initialHabit?.frequency ?: HabitFrequency.DAILY)
    }
    var selectedDays by remember(initialHabit) {
        mutableStateOf(initialHabit?.selectedDays ?: DayOfWeek.entries.toSet())
    }

    var reminderEnabled by remember(initialHabit) {
        mutableStateOf(initialHabit?.reminderEnabled ?: false)
    }
    var reminderHour by remember(initialHabit) {
        mutableIntStateOf(initialHabit?.reminderTime?.hour ?: 20)
    }
    var reminderMinute by remember(initialHabit) {
        mutableIntStateOf(initialHabit?.reminderTime?.minute ?: 0)
    }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when {
                    isEditMode -> "Edit Habit"
                    type == HabitType.YES_NO -> "New Yes / No Habit"
                    else -> "New Measurable Habit"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Habit Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Title") },
                placeholder = { Text(if (type == HabitType.YES_NO) "e.g. Read Book" else "e.g. Drink Water") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Measurable Options
            if (type == HabitType.MEASURABLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = { targetValueText = it },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Unit") },
                        placeholder = { Text("glasses, mins") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                OutlinedTextField(
                    value = incrementText,
                    onValueChange = { incrementText = it },
                    label = { Text("Increment step (+ / −)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Icon Picker
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AVAILABLE_HABIT_ICONS) { (iconKey, iconVector) ->
                    val isSelected = selectedIcon == iconKey
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { selectedIcon = iconKey }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = iconKey,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Color Palette Picker
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AVAILABLE_HABIT_COLORS) { colorHex ->
                    val isSelected = selectedColor == colorHex
                    Surface(
                        shape = CircleShape,
                        color = Color(colorHex),
                        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { selectedColor = colorHex }
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Frequency Selection
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = frequency == HabitFrequency.DAILY,
                    onClick = {
                        frequency = HabitFrequency.DAILY
                        selectedDays = DayOfWeek.entries.toSet()
                    },
                    label = { Text("Every Day") }
                )
                FilterChip(
                    selected = frequency == HabitFrequency.WEEKLY_DAYS,
                    onClick = { frequency = HabitFrequency.WEEKLY_DAYS },
                    label = { Text("Specific Days") }
                )
            }

            // Specific Day of Week Chips
            if (frequency == HabitFrequency.WEEKLY_DAYS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        DayOfWeek.MONDAY to "M",
                        DayOfWeek.TUESDAY to "T",
                        DayOfWeek.WEDNESDAY to "W",
                        DayOfWeek.THURSDAY to "T",
                        DayOfWeek.FRIDAY to "F",
                        DayOfWeek.SATURDAY to "S",
                        DayOfWeek.SUNDAY to "S"
                    ).forEach { (dow, label) ->
                        val isDaySelected = dow in selectedDays
                        Surface(
                            shape = CircleShape,
                            color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            border = if (!isDaySelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    selectedDays = if (isDaySelected) {
                                        if (selectedDays.size > 1) selectedDays - dow else selectedDays
                                    } else {
                                        selectedDays + dow
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Reminder Toggle & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Habit Reminder",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (reminderEnabled) {
                            String.format("Scheduled daily at %02d:%02d", reminderHour, reminderMinute)
                        } else {
                            "No alarm set"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }

            if (reminderEnabled) {
                Surface(
                    onClick = { showTimePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Reminder Time",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("Set Time")
                        }
                    }
                }
            }

            if (showTimePicker) {
                ReminderTimePickerDialog(
                    initialHour = reminderHour,
                    initialMinute = reminderMinute,
                    onDismiss = { showTimePicker = false },
                    onConfirm = { h, m ->
                        reminderHour = h
                        reminderMinute = m
                        showTimePicker = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val target = targetValueText.toDoubleOrNull() ?: 1.0
                            val increment = incrementText.toDoubleOrNull() ?: 1.0
                            val reminderTime = if (reminderEnabled) LocalTime.of(reminderHour, reminderMinute) else null
                            onSave(
                                title.trim(),
                                type,
                                target,
                                unitText.trim(),
                                increment,
                                selectedColor,
                                selectedIcon,
                                frequency,
                                selectedDays,
                                reminderEnabled,
                                reminderTime
                            )
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text(if (isEditMode) "Save Changes" else "Save Habit")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    var isInputMode by remember { mutableStateOf(false) }

    // Text field values for manual keyboard input mode
    var hourText by remember {
        mutableStateOf(
            TextFieldValue(
                text = String.format("%02d", initialHour),
                selection = TextRange(0, 2)
            )
        )
    }
    var minuteText by remember {
        mutableStateOf(
            TextFieldValue(
                text = String.format("%02d", initialMinute),
                selection = TextRange(0, 2)
            )
        )
    }

    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isInputMode) "Enter reminder time" else "Select reminder time",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isInputMode) {
                    // Manual / Keyboard Time Input Layout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        // Hour Field with auto-select on focus and replace on typing
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { newValue ->
                                val digits = newValue.text.filter { it.isDigit() }.take(2)
                                val num = digits.toIntOrNull()
                                if (digits.isEmpty() || (num != null && num in 0..23)) {
                                    hourText = newValue.copy(text = digits)
                                    if (digits.length == 2 || (num != null && num > 2)) {
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(96.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && hourText.text.isNotEmpty()) {
                                        hourText = hourText.copy(
                                            selection = TextRange(0, hourText.text.length)
                                        )
                                    }
                                },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center
                            ),
                            label = { Text("Hour", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("00", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            ),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minute Field with auto-select on focus and replace on typing
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { newValue ->
                                val digits = newValue.text.filter { it.isDigit() }.take(2)
                                val num = digits.toIntOrNull()
                                if (digits.isEmpty() || (num != null && num in 0..59)) {
                                    minuteText = newValue.copy(text = digits)
                                    if (digits.length == 2) {
                                        focusManager.clearFocus()
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(96.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && minuteText.text.isNotEmpty()) {
                                        minuteText = minuteText.copy(
                                            selection = TextRange(0, minuteText.text.length)
                                        )
                                    }
                                },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center
                            ),
                            label = { Text("Minute", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text("00", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                    Text(
                        text = "24-hour format (00:00 – 23:59)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Material 3 Clock Dial Picker
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalHour = if (isInputMode) {
                        (hourText.text.toIntOrNull() ?: timePickerState.hour).coerceIn(0, 23)
                    } else {
                        timePickerState.hour
                    }
                    val finalMinute = if (isInputMode) {
                        (minuteText.text.toIntOrNull() ?: timePickerState.minute).coerceIn(0, 59)
                    } else {
                        timePickerState.minute
                    }
                    onConfirm(finalHour, finalMinute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!isInputMode) {
                            hourText = TextFieldValue(
                                text = String.format("%02d", timePickerState.hour),
                                selection = TextRange(0, 2)
                            )
                            minuteText = TextFieldValue(
                                text = String.format("%02d", timePickerState.minute),
                                selection = TextRange(0, 2)
                            )
                            isInputMode = true
                        } else {
                            isInputMode = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isInputMode) Icons.Default.Schedule else Icons.Default.Keyboard,
                        contentDescription = if (isInputMode) "Switch to clock dial" else "Switch to keyboard input"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
