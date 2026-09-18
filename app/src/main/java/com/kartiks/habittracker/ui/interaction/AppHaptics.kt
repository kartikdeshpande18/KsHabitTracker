package com.kartiks.habittracker.ui.interaction

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class AppHaptics(
    private val view: View
) {
    /**
     * Fired when a Yes/No habit is marked completed or toggled.
     */
    fun habitCompleted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Fired when a measurable habit crosses its target value.
     */
    fun targetReached() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Fired when measurable habit value is incremented or decremented without crossing target.
     */
    fun stepTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Fired when FAB is opened/closed or popout item is selected.
     */
    fun fabToggle() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Fired when switching navigation tabs.
     */
    fun tabSelected() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Fired when selecting a date in Today or Calendar.
     */
    fun dateSelected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    return remember(view) { AppHaptics(view) }
}
