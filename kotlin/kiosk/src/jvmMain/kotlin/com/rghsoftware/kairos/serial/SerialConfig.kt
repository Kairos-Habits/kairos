package com.rghsoftware.kairos.serial

data class SerialConfig(
    val devicePath: String = "/dev/kairos-presence",
    val fallbackDevicePaths: List<String> = listOf("/dev/ttyACM0", "/dev/ttyUSB0"),
    val baudRate: Int = 115200,
    val reconnectDelayMs: Long = 5000,
) {
    companion object {
        val DEFAULT = SerialConfig()
    }
}
