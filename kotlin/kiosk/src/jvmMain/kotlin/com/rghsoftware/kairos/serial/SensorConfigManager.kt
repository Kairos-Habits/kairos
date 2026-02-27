package com.rghsoftware.kairos.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorConfigManager(
    private val scope: CoroutineScope,
) {
    private val _config = MutableStateFlow<SensorConfig?>(null)
    val config: StateFlow<SensorConfig?> = _config.asStateFlow()

    private val _calibrationPhase = MutableStateFlow<String?>(null)
    val calibrationPhase: StateFlow<String?> = _calibrationPhase.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress.asStateFlow()

    private val _calibrationResult = MutableStateFlow<Int?>(null)
    val calibrationResult: StateFlow<Int?> = _calibrationResult.asStateFlow()

    private var listener: SerialPresenceListener? = null

    fun setListener(listener: SerialPresenceListener) {
        this.listener = listener
        scope.launch {
            listener.configEvents.collect { event ->
                when (event) {
                    is ConfigEvent.Settings -> {
                        _config.value = event.config
                    }
                    is ConfigEvent.HardwareConfig -> {
                        // Update hardware config within existing settings
                        _config.value?.let { current ->
                            _config.value =
                                current.copy(
                                    sensor =
                                        current.sensor.copy(
                                            maxGate = event.maxGate,
                                            movingSensitivity = event.movingSensitivity,
                                            staticSensitivity = event.staticSensitivity,
                                        ),
                                )
                        }
                    }
                    is ConfigEvent.ConfigResult -> {
                        // Result of a config change
                    }
                    is ConfigEvent.CalibrationStatus -> {
                        _calibrationPhase.value = event.phase
                        _calibrationProgress.value = event.progress
                        if (event.phase == "complete") {
                            _calibrationResult.value = event.samplesCollected
                        }
                    }
                }
            }
        }
    }

    fun requestSettings() {
        listener?.requestSettings()
    }

    fun setConfig(
        param: String,
        value: Int,
        gate: Int? = null,
    ) {
        listener?.setConfig(param, value, gate)
    }

    fun startCalibration() {
        _calibrationPhase.value = "sampling"
        _calibrationProgress.value = 0
        _calibrationResult.value = null
        listener?.startCalibration()
    }

    fun cancelCalibration() {
        listener?.cancelCalibration()
        _calibrationPhase.value = null
        _calibrationProgress.value = 0
        _calibrationResult.value = null
    }

    fun applyCalibration() {
        listener?.applyCalibration()
        _calibrationResult.value = null
        _calibrationPhase.value = null
    }
}
