package com.rghsoftware.kairos.serial

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

class SerialPresenceListener(
    private val config: SerialConfig = SerialConfig.DEFAULT,
    private val scope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<PresenceEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PresenceEvent> = _events.asSharedFlow()

    private val _configEvents = MutableSharedFlow<ConfigEvent>(extraBufferCapacity = 64)
    val configEvents: SharedFlow<ConfigEvent> = _configEvents.asSharedFlow()

    private var currentPort: SerialPort? = null
    private var writer: PrintWriter? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    connectAndRead()
                } catch (e: Exception) {
                    println(
                        "[SerialPresenceListener] Connection error: ${e.message}, reconnecting in ${config.reconnectDelayMs}ms",
                    )
                }

                if (isActive) {
                    delay(config.reconnectDelayMs)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        closePort()
    }

    fun sendCommand(command: String): Boolean {
        val w = writer ?: return false
        return try {
            w.println(command)
            w.flush()
            true
        } catch (e: Exception) {
            println("[SerialPresenceListener] Failed to send command: ${e.message}")
            false
        }
    }

    fun requestSettings(): Boolean = sendCommand(JsonlParser.buildGetSettingsCommand())

    fun setConfig(
        param: String,
        value: Int,
        gate: Int? = null,
    ): Boolean = sendCommand(JsonlParser.buildSetConfigCommand(param, value, gate))

    fun startCalibration(): Boolean = sendCommand(JsonlParser.buildCalibrateCommand("start"))

    fun cancelCalibration(): Boolean = sendCommand(JsonlParser.buildCalibrateCommand("cancel"))

    fun applyCalibration(): Boolean = sendCommand(JsonlParser.buildCalibrateCommand("apply"))

    private suspend fun connectAndRead() {
        val port = findAndOpenPort()
        if (port == null) {
            println(
                "[SerialPresenceListener] No serial device found, retrying in ${config.reconnectDelayMs}ms",
            )
            return
        }

        currentPort = port
        writer = PrintWriter(OutputStreamWriter(port.outputStream, Charsets.UTF_8), true)
        println("[SerialPresenceListener] Connected to ${port.systemPortName}")

        try {
            readLoop(port)
        } finally {
            writer?.close()
            writer = null
            closePort()
        }
    }

    private fun findAndOpenPort(): SerialPort? {
        val allPaths = listOf(config.devicePath) + config.fallbackDevicePaths

        for (path in allPaths) {
            val ports = SerialPort.getCommPorts()
            val port =
                ports.find {
                    it.systemPortName == path ||
                        it.systemPortName.contains(path.removePrefix("/dev/"))
                }

            if (port != null) {
                port.baudRate = config.baudRate
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0)

                if (port.openPort()) {
                    return port
                } else {
                    println("[SerialPresenceListener] Failed to open ${port.systemPortName}")
                }
            }
        }

        return tryOpenBySystemPortName()
    }

    private fun tryOpenBySystemPortName(): SerialPort? {
        val ports = SerialPort.getCommPorts()
        for (port in ports) {
            val name = port.systemPortName.lowercase()
            if (name.contains("ttyacm") || name.contains("ttyusb") || name.contains("kairos")) {
                port.baudRate = config.baudRate
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0)
                if (port.openPort()) {
                    println("[SerialPresenceListener] Opened serial port ${port.systemPortName}")
                    return port
                }
            }
        }
        return null
    }

    private suspend fun readLoop(port: SerialPort) =
        withContext(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(port.inputStream))

            while (isRunning && port.isOpen) {
                try {
                    val line = reader.readLine()
                    if (line != null) {
                        // Try parsing as presence event first
                        val event = JsonlParser.parse(line)
                        if (event != null) {
                            _events.emit(event)
                        } else {
                            // Try parsing as config event
                            val configEvent = JsonlParser.parseConfigEvent(line)
                            if (configEvent != null) {
                                _configEvents.emit(configEvent)
                            } else {
                                println("[SerialPresenceListener] Unknown message: $line")
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!port.isOpen) {
                        println(
                            "[SerialPresenceListener] Serial port ${port.systemPortName} disconnected",
                        )
                        return@withContext
                    }
                }
            }
        }

    private fun closePort() {
        currentPort?.let { port ->
            if (port.isOpen) {
                port.closePort()
                println("[SerialPresenceListener] Closed serial port ${port.systemPortName}")
            }
        }
        currentPort = null
    }
}
