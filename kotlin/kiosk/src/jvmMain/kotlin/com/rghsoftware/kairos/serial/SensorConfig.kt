package com.rghsoftware.kairos.serial

data class SensorConfig(
    val motionThreshold: Int = 50,
    val framesPresent: Int = 5,
    val framesAbsent: Int = 15,
    val sensor: SensorHardwareConfig = SensorHardwareConfig(),
)

data class SensorHardwareConfig(
    val maxGate: Int = 6,
    val movingSensitivity: List<Int> = List(9) { if (it < 6) 50 else 0 },
    val staticSensitivity: List<Int> = List(9) { 0 },
)

sealed interface ConfigEvent {
    data class Settings(
        val config: SensorConfig,
    ) : ConfigEvent

    data class HardwareConfig(
        val maxGate: Int,
        val movingSensitivity: List<Int>,
        val staticSensitivity: List<Int>,
    ) : ConfigEvent

    data class ConfigResult(
        val success: Boolean,
        val param: String,
        val value: Int,
    ) : ConfigEvent

    data class CalibrationStatus(
        val phase: String,
        val progress: Int,
        val samplesCollected: Int,
    ) : ConfigEvent
}
