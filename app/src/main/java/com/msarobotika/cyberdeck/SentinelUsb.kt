package com.msarobotika.cyberdeck

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

/** Open only the device explicitly chosen by the user, after Android permission. */
object SentinelUsb {
    fun supported(manager: UsbManager): List<UsbDevice> =
        UsbSerialProber.getDefaultProber().findAllDrivers(manager).map { it.device }

    fun snapshot(manager: UsbManager, device: UsbDevice): SensorSnapshot {
        require(manager.hasPermission(device)) { "Falta permiso USB de Android" }
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: error("USB-serie no compatible")
        val connection = manager.openDevice(device) ?: error("Dispositivo desconectado o no disponible")
        val port = driver.ports.firstOrNull() ?: error("Sin puerto serie")
        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            // Keep bootloader/reset lines inactive, as in Sentinel's Python client.
            port.dtr = false
            port.rts = false
            return SentinelProtocol(object : SensorTransport {
                override fun write(bytes: ByteArray) { port.write(bytes, 2000) }
                override fun read(buffer: ByteArray, timeoutMs: Int): Int = port.read(buffer, timeoutMs)
            }).snapshot()
        } finally {
            runCatching { port.close() }
            connection.close()
        }
    }
}
