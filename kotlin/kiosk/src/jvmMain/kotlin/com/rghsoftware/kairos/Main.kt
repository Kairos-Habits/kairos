package com.rghsoftware.kairos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rghsoftware.kairos.checklist.ChecklistFlowConfig
import com.rghsoftware.kairos.checklist.ChecklistFlowEvent
import com.rghsoftware.kairos.checklist.ChecklistFlowState
import com.rghsoftware.kairos.checklist.SessionState
import com.rghsoftware.kairos.checklist.TransitionEffect
import com.rghsoftware.kairos.checklist.reduce
import com.rghsoftware.kairos.display.DisplayMode
import com.rghsoftware.kairos.display.DisplayModeConfig
import com.rghsoftware.kairos.display.DisplayModeEvent
import com.rghsoftware.kairos.display.DisplayModeState
import com.rghsoftware.kairos.display.PresenceState
import com.rghsoftware.kairos.display.reduce as displayReduce
import com.rghsoftware.kairos.domain.Mode
import com.rghsoftware.kairos.domain.Task
import com.rghsoftware.kairos.domain.TaskId
import com.rghsoftware.kairos.serial.SensorConfigManager
import com.rghsoftware.kairos.serial.SerialConfig
import com.rghsoftware.kairos.serial.SerialPresenceListener
import com.rghsoftware.kairos.ui.ModeSelectionScreen
import com.rghsoftware.kairos.ui.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds

private enum class Screen {
    MAIN,
    SETTINGS,
}

fun main() =
    application {
        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        val listener = remember { SerialPresenceListener(SerialConfig.DEFAULT, scope) }
        val configManager = remember { SensorConfigManager(scope) }

        // Display state (for display mode switching)
        var displayState by remember { mutableStateOf(DisplayModeState()) }

        // Checklist state (for checklist flow)
        var checklistState by remember { mutableStateOf(ChecklistFlowState()) }
        val checklistConfig = remember { ChecklistFlowConfig() }

        // Sample tasks for testing (in production, these would come from Supabase)
        val sampleTasks = remember {
            listOf(
                Task(TaskId("task-1"), "Check door", Mode.LEAVING),
                Task(TaskId("task-2"), "Turn off lights", Mode.LEAVING),
                Task(TaskId("task-3"), "Grab keys", Mode.LEAVING),
                Task(TaskId("task-4"), "Lock door", Mode.LEAVING),
                Task(TaskId("task-5"), "Check stove", Mode.LEAVING),
                Task(TaskId("task-6"), "Set thermostat", Mode.LEAVING),
                Task(TaskId("task-7"), "Grab keys", Mode.ARRIVING),
                Task(TaskId("task-8"), "Check mail", Mode.ARRIVING),
                Task(TaskId("task-9"), "Lock door", Mode.ARRIVING),
            )
        }

        var currentScreen by remember { mutableStateOf(Screen.MAIN) }
        var currentEnergy by remember { mutableIntStateOf(0) }
        var distanceCm by remember { mutableIntStateOf(0) }
        var connectionStatus by remember { mutableStateOf("Connecting...") }

        LaunchedEffect(Unit) {
            configManager.setListener(listener)
            listener.start()
            listener.events.collectLatest { event ->
                val now = Clock.System.now()

                when (event) {
                    is com.rghsoftware.kairos.serial.PresenceEvent.Presence -> {
                        val isPresent = event.state == com.rghsoftware.kairos.serial.PresenceState.PRESENT
                        val presenceState = if (isPresent) PresenceState.Present else PresenceState.Absent

                        // Update display mode state machine
                        val displayEvent = DisplayModeEvent.PresenceChanged(
                            state = presenceState,
                            timestamp = Instant.fromEpochMilliseconds(now.toEpochMilliseconds()),
                        )
                        displayState = displayState.displayReduce(
                            event = displayEvent,
                            config = DisplayModeConfig(),
                        ).newState
                        // Update checklist state machine
                        val checklistEvent = ChecklistFlowEvent.PresenceChanged(
                            isPresent = isPresent,
                            timestamp = now,
                        )
                        val transition = checklistState.reduce(
                            event = checklistEvent,
                            config = checklistConfig,
                        )
                        checklistState = transition.newState

                        currentEnergy = event.movingCm
                        distanceCm = if (isPresent) maxOf(event.movingCm, event.staticCm) else 0
                        connectionStatus = "Connected"
                    }
                    is com.rghsoftware.kairos.serial.PresenceEvent.Heartbeat -> {
                        connectionStatus = "Connected"
                    }
                }
            }
        }

        LaunchedEffect(currentScreen) {
            if (currentScreen == Screen.SETTINGS) {
                configManager.requestSettings()
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "kairos",
        ) {
            MaterialTheme {
                when (currentScreen) {
                    Screen.MAIN -> {
                        MainScreen(
                            displayState = displayState,
                            checklistState = checklistState,
                            sampleTasks = sampleTasks,
                            onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                            onChecklistEvent = { event ->
                                val transition = checklistState.reduce(event, checklistConfig)
                                checklistState = transition.newState
                                // In production: handle transition.effect to record events
                            },
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            configManager = configManager,
                            presenceState = displayState.presenceState,
                            currentEnergy = currentEnergy,
                            distanceCm = distanceCm,
                            connectionStatus = connectionStatus,
                            onNavigateBack = { currentScreen = Screen.MAIN },
                        )
                    }
                }
            }
        }
    }

@Composable
private fun MainScreen(
    displayState: DisplayModeState,
    checklistState: ChecklistFlowState,
    sampleTasks: List<Task>,
    onNavigateToSettings: () -> Unit,
    onChecklistEvent: (ChecklistFlowEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        floatingActionButton = {
            Button(onClick = onNavigateToSettings) {
                Text("Settings")
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)

        when {
            // Show checklist UI when in KIOSK mode with active checklist
            displayState.displayMode == DisplayMode.KIOSK && checklistState.isActive -> {
                when (val sessionState = checklistState.sessionState) {
                    is SessionState.SelectingMode -> {
                        ModeSelectionScreen(
                            onModeSelected = { mode ->
                                onChecklistEvent(
                                    ChecklistFlowEvent.ModeSelected(
                                        mode = mode,
                                        timestamp = Clock.System.now(),
                                    ),
                                )
                            },
                            onSkip = {
                                onChecklistEvent(
                                    ChecklistFlowEvent.Cancelled(
                                        timestamp = Clock.System.now(),
                                    ),
                                )
                            },
                            modifier = contentModifier,
                        )
                    }
                    is SessionState.InSession -> {
                        ChecklistScreenContent(
                            mode = sessionState.mode,
                            tasks = sampleTasks.filter { it.mode == sessionState.mode },
                            completedTaskIds = sessionState.completedTaskIds,
                            onTaskToggle = { taskId ->
                                onChecklistEvent(
                                    ChecklistFlowEvent.TaskToggled(
                                        taskId = taskId,
                                        timestamp = Clock.System.now(),
                                    ),
                                )
                            },
                            onComplete = {
                                onChecklistEvent(
                                    ChecklistFlowEvent.SessionCompleted(
                                        timestamp = Clock.System.now(),
                                    ),
                                )
                            },
                            onSkip = {
                                onChecklistEvent(
                                    ChecklistFlowEvent.Cancelled(
                                        timestamp = Clock.System.now(),
                                    ),
                                )
                            },
                            modifier = contentModifier,
                        )
                    }
                    else -> {
                        // Idle or unexpected state - show default
                        DefaultContent(displayState, contentModifier)
                    }
                }
            }
            // Show HA screen or default
            else -> {
                DefaultContent(displayState, contentModifier)
            }
        }
    }
}

@Composable
private fun ChecklistScreenContent(
    mode: Mode,
    tasks: List<Task>,
    completedTaskIds: Set<TaskId>,
    onTaskToggle: (TaskId) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeLabel = if (mode == Mode.LEAVING) "Leaving" else "Arriving"

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        // Header
        Text(
            text = "$modeLabel Checklist",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val completedCount = completedTaskIds.size
        val totalCount = tasks.size
        Text(
            text = if (totalCount > 0) "$completedCount of $totalCount complete" else "No tasks configured",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Task list
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No tasks for $modeLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Add tasks in the web app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tasks, key = { it.id.value }) { task ->
                    TaskItem(
                        task = task,
                        isCompleted = task.id in completedTaskIds,
                        onToggle = { onTaskToggle(task.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text("Skip")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
            )

            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun DefaultContent(
    displayState: DisplayModeState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when (displayState.displayMode) {
                DisplayMode.KIOSK -> "Kiosk Mode"
                DisplayMode.HA -> "Home Assistant Mode"
            },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (displayState.presenceState) {
                PresenceState.Present -> "Presence detected"
                PresenceState.Absent -> "No presence"
                PresenceState.Unknown -> "Status unknown"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
