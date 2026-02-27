package com.rghsoftware.kairos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rghsoftware.kairos.display.DisplayMode
import com.rghsoftware.kairos.display.DisplayModeState
import com.rghsoftware.kairos.display.PresenceState

@Composable
@Preview
fun App(
    displayState: DisplayModeState = DisplayModeState(),
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Display Mode: ${displayState.displayMode.name}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Presence: ${when (displayState.presenceState) {
                    PresenceState.Present -> "Present"
                    PresenceState.Absent -> "Absent"
                    PresenceState.Unknown -> "Unknown"
                }}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
