package com.rghsoftware.kairos.serial

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object JsonlParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parse(line: String): PresenceEvent? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        return try {
            val dto = json.decodeFromString<PresenceEventDto>(trimmed)
            when (dto.type) {
                "presence" -> {
                    val state =
                        PresenceState.fromString(dto.state)
                            ?: return null
                    PresenceEvent.Presence(
                        state = state,
                        timestampMs = dto.timestampMs,
                        movingCm = dto.movingCm,
                        staticCm = dto.staticCm,
                    )
                }
                "heartbeat" -> PresenceEvent.Heartbeat(dto.timestampMs)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parseConfigEvent(line: String): ConfigEvent? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        return try {
            val dto = json.decodeFromString<ConfigEventDto>(trimmed)
            when (dto.type) {
                "settings" -> {
                    val sensor =
                        dto.sensor?.let { s ->
                            SensorHardwareConfig(
                                maxGate = s.maxGate,
                                movingSensitivity = s.movingSensitivity,
                                staticSensitivity = s.staticSensitivity,
                            )
                        } ?: SensorHardwareConfig()
                    ConfigEvent.Settings(
                        config =
                            SensorConfig(
                                motionThreshold = dto.motionThreshold ?: 50,
                                framesPresent = dto.framesPresent ?: 5,
                                framesAbsent = dto.framesAbsent ?: 15,
                                sensor = sensor,
                            ),
                    )
                }
                "sensor_config" -> {
                    ConfigEvent.HardwareConfig(
                        maxGate = dto.maxGate ?: 6,
                        movingSensitivity = dto.movingSensitivity ?: List(9) { 50 },
                        staticSensitivity = dto.staticSensitivity ?: List(9) { 0 },
                    )
                }
                "config_result" -> {
                    ConfigEvent.ConfigResult(
                        success = dto.success ?: false,
                        param = dto.param ?: "",
                        value = dto.value ?: 0,
                    )
                }
                "calibration_status" -> {
                    ConfigEvent.CalibrationStatus(
                        phase = dto.phase ?: "",
                        progress = dto.progress ?: 0,
                        samplesCollected = dto.samplesCollected ?: 0,
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun buildGetSettingsCommand(): String = """{"type":"get_settings"}"""

    fun buildSetConfigCommand(
        param: String,
        value: Int,
        gate: Int? = null,
    ): String {
        return if (gate != null) {
            """{"type":"set_config","param":"$param","value":$value,"gate":$gate}"""
        } else {
            """{"type":"set_config","param":"$param","value":$value}"""
        }
    }

    fun buildCalibrateCommand(action: String): String =
        """{"type":"calibrate","action":"$action"}"""

    fun buildGetSensorConfigCommand(): String = """{"type":"get_sensor_config"}"""
}

@Serializable
private data class PresenceEventDto(
    val type: String,
    val state: String = "",
    @SerialName("timestamp_ms")
    val timestampMs: Long,
    @SerialName("moving_cm")
    val movingCm: Int = 0,
    @SerialName("static_cm")
    val staticCm: Int = 0,
)

@Serializable
private data class ConfigEventDto(
    val type: String,
    @SerialName("motion_threshold")
    val motionThreshold: Int? = null,
    @SerialName("frames_present")
    val framesPresent: Int? = null,
    @SerialName("frames_absent")
    val framesAbsent: Int? = null,
    val sensor: SensorHardwareDto? = null,
    @SerialName("max_gate")
    val maxGate: Int? = null,
    @SerialName("moving_sensitivity")
    val movingSensitivity: List<Int>? = null,
    @SerialName("static_sensitivity")
    val staticSensitivity: List<Int>? = null,
    val success: Boolean? = null,
    val param: String? = null,
    val value: Int? = null,
    val phase: String? = null,
    val progress: Int? = null,
    @SerialName("samples_collected")
    val samplesCollected: Int? = null,
)

@Serializable
private data class SensorHardwareDto(
    @SerialName("max_gate")
    val maxGate: Int = 6,
    @SerialName("moving_sensitivity")
    val movingSensitivity: List<Int> = List(9) { 50 },
    @SerialName("static_sensitivity")
    val staticSensitivity: List<Int> = List(9) { 0 },
)
