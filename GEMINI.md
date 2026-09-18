# Project Rules: K's Habit Tracker

## 1. Product Principles
- **Pure, Offline-First Application**: K's Habit Tracker operates completely offline with no network dependency required for normal operation.
- **Strictly Prohibited Features**:
  - No backend or server infrastructure.
  - No account creation, login, or authentication.
  - No social networking, community, friends, leaderboards, or public challenges.
  - No subscriptions, in-app purchases, or paywalls.
  - No advertisements or ad SDKs.
  - No analytics SDKs, telemetry, trackers, or unnecessary online services.
- All habit data and settings must be kept strictly local on the user's device.

---

## 2. Android Technology Stack & Native Architecture
- **Language**: Kotlin.
- **UI Framework**: Jetpack Compose with Material 3 and Material 3 Expressive.
- **Local Persistence**: Room database.
- **Asynchronous & Reactive Programming**: Kotlin Coroutines and Kotlin `Flow` / `StateFlow`.
- **Notifications**: Native Android local notifications (`NotificationManager`, `WorkManager` / `AlarmManager` without exact alarms unless strictly necessary).
- **Prohibited Frameworks**: Do not introduce cross-platform UI frameworks (e.g., Flutter, React Native, Ionic). Use native Android APIs and architecture only.

---

## 3. Approved Information Architecture (Locked)
The product structure and navigation are locked. Do not independently redesign or reorder:
1. **Primary Navigation Tabs** (Exact Order):
   1. `Today`
   2. `Calendar`
   3. `Statistics`
   4. `Settings`
2. **Action Controls**:
   - Separate floating Add Habit FAB expanding into:
     - `Yes / No`
     - `Measurable`
3. **Detail Navigation**:
   - Dedicated `Habit Detail / KPI` screen accessible from the `Statistics` tab.

---

## 4. Visual Direction & Material 3 Expressive
- **Reference**: The existing approved prototype is the visual and information-architecture reference.
- **Native Implementation**: Implement as a genuine native Material 3 Expressive Android application, not a web UI port.
- **Preserve**:
  - Screen structure and layout.
  - Information hierarchy.
  - Navigation tab order.
  - Habit interaction concepts.
  - FAB interaction and states.
  - Distinct Yes/No vs. Measurable handling.
- **Adapt to Material 3 Expressive Conventions**:
  - Material color roles and tonal surfaces.
  - Typography scale and hierarchy.
  - Shape scale and expressive shape variations.
  - Elevation and tonal contrast.
  - State animations, spring-like motion, and touch feedback.

---

## 5. Navigation + Floating Action Button (FAB)
- **Layout**: `[ Today | Calendar | Statistics | Settings ] + [ Floating Action Button ]`
- **FAB Closed State**:
  - Displays `+` icon.
- **FAB Opened State**:
  - Displays semi-transparent scrim over background.
  - FAB morphs/changes to `×` icon.
  - `Yes / No` action appears.
  - `Measurable` action appears.
  - Actions use expressive spring-like motion.
- **Selection**: Tapping either action opens the corresponding habit creation flow.

---

## 6. Habit Types & Card Interactions
### YES_NO Habit
- Binary habit (e.g., "Exercise").
- States: `incomplete`, `complete`.
- Single tap on the card toggles today's completion state.

### MEASURABLE Habit
- Habit with a numerical target (e.g., "Drink water", "0/8 glasses").
- Properties:
  - Target value.
  - Unit of measurement.
  - Increment value.
  - Current daily value.
- Completion condition: `currentValue >= targetValue`.
- **Today Screen Card Layout**:
  - Left side: Shows current value and target once (e.g., "0/8 glasses").
  - Right side: Contains only the decrement and increment controls (`-` / `+`).
  - **Constraint**: Do not duplicate "0/8 glasses" inside the action buttons/controls.

---

## 7. Domain Model & Statistics Truth
- **Explicit Domain Types** (at minimum):
  - `Habit`: Configuration and metadata (type, title, schedule, target, unit, etc.).
  - `HabitRecord`: Historical daily record (date, completion state, achieved value).
  - `Settings`: User preferences and app settings.
- **Source of Truth Rules**:
  - Do NOT store derived statistics as authoritative persisted entities.
  - Do NOT use persisted columns/values as the source of truth for:
    - Current streak.
    - Best streak.
    - Completion rate.
    - Calendar completion percentage.
  - All metrics and streaks must be dynamically calculated from `HabitRecord` history.

---

## 8. Today Screen Workflow
- Primary user workflow.
- **Capabilities**:
  - Date selector / calendar strip.
  - Daily progress indicator.
  - List of all scheduled habits for the selected date.
  - Yes / No completion toggling.
  - Measurable increment / decrement.
  - Real-time completion status and streak information.
  - Immediate progress updates upon interaction.
- **Global Progress Calculation**:
  $$\text{Global Progress} = \frac{\text{completed scheduled habits}}{\text{scheduled habits}}$$
  - Examples: `0/6 = 0%`, `3/6 = 50%`, `6/6 = 100%`.
- **100% Completion Treatment**:
  - Keep the approved neutral completed-state percentage treatment.
  - Do NOT add decorative colored patches or jarring celebration artifacts around the percentage indicator.

---

## 9. Calendar Screen
- **Purpose**: Completion calendar showing daily adherence across scheduled habits.
- **Daily Adherence Metric**:
  $$\text{Daily Completion} = \frac{\text{completed scheduled habits}}{\text{scheduled habits}}$$
- **Calendar States**:
  - Future date: Neutral.
  - No scheduled habits: Neutral.
  - No completion: Neutral.
  - Partial completion: Partial completion tone.
  - Full completion: Completed tone.
  - Today: Completion tone + today indicator.
  - Selected date: Selection state layered appropriately over completion tone.
- **Rules**:
  - Calendar colors must be calculated directly from actual habit records.
  - Do not hard-code arbitrary date colors.
  - The calendar must remain a completion calendar, not a measurement/chart view.

---

## 10. Statistics Screen
- **Global Summary Metrics**:
  - Completion rate.
  - Current streak.
  - Best streak.
  - Total completions.
- **Period Filter Selector**:
  - `Week` | `Month` | `Year` | `All`
- **Interactive "By habit" Section**:
  - Lists individual habits with performance summaries.
  - Tapping an individual habit navigates directly to that habit's KPI/detail screen.

---

## 11. Habit KPI / Detail Screen
- **Required Metrics for Every Habit**:
  - Current streak.
  - Best streak.
  - Completion rate.
  - Total completions.
  - Completed this month.
  - Recent performance trend.
  - Historical completion calendar for this habit.
- **Additional Metrics for Measurable Habits**:
  - Target value.
  - Average achieved value.
  - Target attainment rate.
  - Total units recorded.
  - Number of days target was reached.
- **Data Integrity**: All KPI metrics must be calculated from actual stored history.

---

## 12. Settings Screen
- **Appearance**:
  - System theme.
  - Light mode.
  - Dark mode.
  - Dynamic color (Material You).
  - OLED Black mode.
- **Reminders**:
  - Habit reminders toggle.
  - Default reminder time picker.
- **Data Management**:
  - Local export.
  - Local import.
  - Delete all data (with confirmation).

---

## 13. OLED Black Mode
- True OLED black mode implementation:
  - Background: `#000000` for deepest application background surfaces.
  - Material Tonal Hierarchy: Do not flatten the UI into pure black. Retain subtle Material tonal surface differentiation for cards, controls, navigation bar, selected states, dialogs, and bottom sheets.

---

## 14. Notifications & Scheduling
- **Local Notifications Only**: No push notification services or external network infrastructure.
- **Permission Timing**: Request notification permission contextually when the user enables reminders, never at cold app startup.
- **Scheduling**: Use standard Android scheduling APIs (`WorkManager` or `AlarmManager`). Avoid exact alarms unless explicitly required.

---

## 15. Privacy & Telemetry
- Maintain 100% local operation.
- No analytics SDKs (e.g., Firebase Analytics, Mixpanel, Amplitude).
- No crash reporting SDKs that phone home unless explicitly requested.
- No advertising or tracking SDKs.
- Zero network requests for core habit tracking functionality.

---

## 16. Local Backup & Migration
- Implement local export and import (JSON format).
- **Versioned Backup Schema**:
  ```json
  {
    "schemaVersion": 1,
    "habits": [],
    "habitRecords": [],
    "settings": []
  }
  ```
- **Migration Contract**: Future schema versions must provide backward compatibility and migrate older backup formats cleanly.

---

## 17. Code Architecture & Design Patterns
- **Architecture Pattern**: Unidirectional Data Flow (UDF)
  $$\text{UI (Compose)} \longrightarrow \text{ViewModel} \longrightarrow \text{Repository} \longrightarrow \text{Room DAO}$$
- **State Management**:
  - Immutable UI state data classes (`StateFlow` / `Flow`).
  - Coroutines for background and database operations.
  - Clear separation between UI presentation, domain logic, and data layer.
  - Testable business logic without unnecessary framework coupling.
- **Simplicity**: Avoid unnecessary architectural boilerplate, over-engineering, or speculative layers.

---

## 18. Material 3 Expressive Design System
- Utilize official Android Material 3 and Material 3 Expressive components:
  - Color roles (`primary`, `secondary`, `tertiary`, `surface`, `surfaceContainer`, `outline`, etc.).
  - Material 3 shape scale (small, medium, large, extra-large) and expressive shape variation.
  - Navigation bar, floating action buttons, modal bottom sheets, assist/filter chips, filled/tonal/outlined buttons.
- **Meaningful Motion**:
  - Animations must communicate state changes, not act as decorative noise.
  - Spring-like physics for interactive elements (FAB expansion `+` $\rightarrow$ `×`, card clicks, progress updates, sheet appearances, tab transitions).

---

## 19. Developer Authority & Execution Workflow
- Full authority to create, edit, move, rename, delete project files, update Gradle dependencies, and run builds/tests within this project.
- Always inspect existing code, determine the minimal coherent change, apply it directly, run builds/tests, diagnose and fix any errors encountered.
- Do not stop merely because a build error occurs—diagnose and fix it.
- Confine all file changes to `~/AndroidStudioProjects/KsHabitTracker`.

---

## 20. Architectural Guardrails
- The UI and information architecture are approved and locked.
- Do not independently redesign the product, navigation order, or visual structure.
- In case of ambiguity:
  - Preserve approved information architecture.
  - Use native Material 3 Expressive conventions.
  - Select the simplest maintainable solution.
  - Ask the user before making any major architectural changes.

---

## 21. Target Device & Tooling
- Primary test and QA target: Physical Android device connected via ADB.
- Do not configure or require an Android Emulator unless explicitly requested.
- Android Studio and terminal CLI (`./gradlew`, `adb`) are both authorized for building, installing, running, and debugging.
