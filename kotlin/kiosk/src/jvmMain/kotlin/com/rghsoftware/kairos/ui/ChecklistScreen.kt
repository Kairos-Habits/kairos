package com.rghsoftware.kairos.ui

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rghsoftware.kairos.domain.Mode
import com.rghsoftware.kairos.domain.Task
import com.rghsoftware.kairos.domain.TaskId

/**
 * Screen displaying the checklist tasks for the selected mode.
 * User can toggle tasks and complete the session.
 *
 * @param mode The selected mode (LEAVING or ARRIVING)
 * @param tasks List of tasks to display (filtered by mode)
 * @param completedTaskIds Set of task IDs that have been marked complete
 * @param onTaskToggle Callback when a task is toggled
 * @param onComplete Callback when user completes the session
 * @param onSkip Callback when user skips without completing
 */
@Composable
fun ChecklistScreen(
    mode: Mode,
    tasks: List<Task>,
    completedTaskIds: Set<TaskId>,
    onTaskToggle: (TaskId) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeLabel = when (mode) {
        Mode.LEAVING -> "Leaving"
        Mode.ARRIVING -> "Arriving"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
