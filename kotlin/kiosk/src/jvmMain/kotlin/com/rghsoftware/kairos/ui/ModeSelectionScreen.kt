package com.rghsoftware.kairos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rghsoftware.kairos.domain.Mode

/**
 * Screen displayed when presence is detected, allowing user to select
 * whether they are LEAVING or ARRIVING.
 *
 * @param onModeSelected Callback when a mode is selected
 * @param onSkip Callback when user skips the checklist
 */
@Composable
fun ModeSelectionScreen(
    onModeSelected: (Mode) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "What brings you here?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // LEAVING button
        ModeButton(
            mode = Mode.LEAVING,
            label = "Leaving",
            description = "Heading out for the day",
            onClick = { onModeSelected(Mode.LEAVING) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ARRIVING button
        ModeButton(
            mode = Mode.ARRIVING,
            label = "Arriving",
            description = "Just got back",
            onClick = { onModeSelected(Mode.ARRIVING) },
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Skip button
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.padding(8.dp),
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun ModeButton(
    mode: Mode,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (mode) {
        Mode.LEAVING -> MaterialTheme.colorScheme.primaryContainer
        Mode.ARRIVING -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (mode) {
        Mode.LEAVING -> MaterialTheme.colorScheme.onPrimaryContainer
        Mode.ARRIVING -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(120.dp)
            .padding(horizontal = 32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
