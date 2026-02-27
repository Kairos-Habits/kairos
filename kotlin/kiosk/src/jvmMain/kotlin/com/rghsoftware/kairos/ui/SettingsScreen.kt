package com.rghsoftware.kairos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rghsoftware.kairos.serial.SensorConfig
import com.rghsoftware.kairos.serial.SensorConfigManager
import com.rghsoftware.kairos.display.PresenceState as DisplayPresenceState

@Composable
fun SettingsScreen(
    configManager: SensorConfigManager,
    presenceState: DisplayPresenceState,
    currentEnergy: Int,
    distanceCm: Int?,
    connectionStatus: String,
    onNavigateBack: () -> Unit,
) {
    val config by configManager.config.collectAsState()
    val calibrationPhase by configManager.calibrationPhase.collectAsState()
    val calibrationProgress by configManager.calibrationProgress.collectAsState()
    val calibrationResult by configManager.calibrationResult.collectAsState()

    Row(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Left panel: Sensor Status
        SensorStatusPanel(
            presenceState = presenceState,
            currentEnergy = currentEnergy,
            distanceCm = distanceCm,
            connectionStatus = connectionStatus,
            calibrationPhase = calibrationPhase,
            calibrationProgress = calibrationProgress,
            calibrationResult = calibrationResult,
            onCalibrate = { configManager.startCalibration() },
            onApplyCalibration = { configManager.applyCalibration() },
            onCancelCalibration = { configManager.cancelCalibration() },
            modifier = Modifier.weight(1f),
        )

        // Right panel: Configuration
        ConfigurationPanel(
            config = config,
            onMotionThresholdChange = { configManager.setConfig("motion_threshold", it) },
            onFramesPresentChange = { configManager.setConfig("frames_present", it) },
            onFramesAbsentChange = { configManager.setConfig("frames_absent", it) },
            onMaxGateChange = { configManager.setConfig("max_gate", it) },
            onMovingSensitivityChange = { gate, value ->
                configManager.setConfig("moving_sensitivity", value, gate)
            },
            onNavigateBack = onNavigateBack,
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun SensorStatusPanel(
    presenceState: DisplayPresenceState,
    currentEnergy: Int,
    distanceCm: Int?,
    connectionStatus: String,
    calibrationPhase: String?,
    calibrationProgress: Int,
    calibrationResult: Int?,
    onCalibrate: () -> Unit,
    onApplyCalibration: () -> Unit,
    onCancelCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "SENSOR STATUS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "State: ${when (presenceState) {
                DisplayPresenceState.Present -> "PRESENT"
                DisplayPresenceState.Absent -> "ABSENT"
                DisplayPresenceState.Unknown -> "UNKNOWN"
            }}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = "Energy: $currentEnergy",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = "Distance: ${distanceCm?.let { "${it}cm" } ?: "--"}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Calibration button/status
        when (calibrationPhase) {
            "sampling" -> {
                Text(
                    text = "Calibrating...",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { calibrationProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Text(
                    text = "$calibrationProgress%",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onCancelCalibration,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text("CANCEL")
                }
            }
            "complete" -> {
                Text(
                    text = "Calibration Complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Recommended threshold: $calibrationResult",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onApplyCalibration,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text("APPLY")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancelCalibration,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text("DISMISS")
                }
            }
            else -> {
                Button(
                    onClick = onCalibrate,
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Text(
                        text = "CALIBRATE",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Connection: $connectionStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ConfigurationPanel(
    config: SensorConfig?,
    onMotionThresholdChange: (Int) -> Unit,
    onFramesPresentChange: (Int) -> Unit,
    onFramesAbsentChange: (Int) -> Unit,
    onMaxGateChange: (Int) -> Unit,
    onMovingSensitivityChange: (Int, Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "CONFIGURATION",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (config == null) {
            Text(
                text = "Loading configuration...",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            // Software Filter section
            Text(
                text = "Software Filter",
                style = MaterialTheme.typography.titleSmall,
            )

            LabeledSlider(
                label = "Motion Threshold",
                value = config.motionThreshold,
                range = 0..100,
                onValueChange = onMotionThresholdChange,
            )

            LabeledSlider(
                label = "Frames to Present",
                value = config.framesPresent,
                range = 1..30,
                onValueChange = onFramesPresentChange,
            )

            LabeledSlider(
                label = "Frames to Absent",
                value = config.framesAbsent,
                range = 1..50,
                onValueChange = onFramesAbsentChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sensor Hardware section
            Text(
                text = "Sensor Hardware (LD2410C)",
                style = MaterialTheme.typography.titleSmall,
            )

            val maxGate = config.sensor.maxGate
            LabeledSlider(
                label = "Max Distance",
                value = maxGate,
                range = 1..8,
                onValueChange = onMaxGateChange,
                valueText = "${(maxGate + 1) * 0.75}m",
            )

            Text(
                text = "Distance Sensitivity:",
                style = MaterialTheme.typography.bodyMedium,
            )

            val distanceRanges =
                listOf(
                    "0 - 0.75m",
                    "0.75 - 1.5m",
                    "1.5 - 2.25m",
                    "2.25 - 3.0m",
                    "3.0 - 3.75m",
                    "3.75 - 4.5m",
                    "4.5 - 5.25m",
                    "5.25 - 6.0m",
                    "6.0 - 6.75m",
                )

            config.sensor.movingSensitivity.forEachIndexed { gate, sensitivity ->
                LabeledSlider(
                    label = distanceRanges.getOrElse(gate) { "Gate $gate" },
                    value = sensitivity,
                    range = 0..100,
                    onValueChange = { onMovingSensitivityChange(gate, it) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f).height(64.dp),
            ) {
                Text("BACK")
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    valueText: String? = null,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = valueText ?: value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        )
    }
}
