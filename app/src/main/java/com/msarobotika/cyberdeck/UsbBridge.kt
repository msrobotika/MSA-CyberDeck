package com.msarobotika.cyberdeck

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * USB Host bridge for classic ESP32 (CH340/CP210x/FTDI) and USB CDC boards.
 * No automatic probing commands and no writes except the SF1 read-only allowlist.
 */
class UsbBridge(private val activity: Activity) {
    private val usb = activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private val main = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private val permissionAction = activity.packageName + ".USB_PERMISSION"
    private val buffer = StringBuilder()
    private val allowedCommands = setOf("HELLO", "STATUS", "CAPABILITIES")

    var drivers by mutableStateOf<List<UsbSerialDriver>>(emptyList())
        private set
    var status by mutableStateOf("Sin conexión USB")
        private set
    var connected by mutableStateOf(false)
        private set
    var lastPacket by mutableStateOf("")
        private set
    var receivedLines by mutableStateOf<List<String>>(emptyList())
        private set

    @Volatile private var port: UsbSerialPort? = null
    private var manager: SerialInputOutputManager? = null
    private var pendingDeviceId: Int? = null
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != permissionAction) return
            val id = pendingDeviceId
            pendingDeviceId = null
            if (id == null) return
            val driver = drivers.firstOrNull { it.device.deviceId == id }
            if (driver != null && usb.hasPermission(driver.device)) {
                open(driver)
            } else {
                status = "Permiso USB denegado o dispositivo retirado"
            }
        }
    }

    init {
        val filter = IntentFilter(permissionAction)
        if (Build.VERSION.SDK_INT >= 33) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            activity.registerReceiver(receiver, filter)
        }
        registered = true
        refresh()
    }

    fun refresh() {
        drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb)
        if (!connected) {
            status = if (drivers.isEmpty()) {
                "No hay adaptadores USB serie compatibles"
            } else {
                "Detectados " + drivers.size + " adaptadores · selecciona uno"
            }
        }
    }

    fun connect(driver: UsbSerialDriver) {
        disconnect()
        if (!usb.hasPermission(driver.device)) {
            pendingDeviceId = driver.device.deviceId
            val intent = Intent(permissionAction).setPackage(activity.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pending = PendingIntent.getBroadcast(activity, 0, intent, flags)
            status = "Solicitando autorización USB"
            usb.requestPermission(driver.device, pending)
            return
        }
        open(driver)
    }

    private fun open(driver: UsbSerialDriver) {
        writer.execute {
            try {
                val connection = usb.openDevice(driver.device)
                    ?: error("Android no ha podido abrir el dispositivo USB")
                val serial = driver.ports.firstOrNull()
                    ?: error("Dispositivo sin puertos serie")
                try {
                    serial.open(connection)
                    serial.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                } catch (e: Exception) {
                    connection.close()
                    throw e
                }
                port = serial
                val io = SerialInputOutputManager(serial, object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) = receive(data)
                    override fun onRunError(e: Exception) {
                        main.post {
                            if (port === serial) {
                                disconnect()
                                status = "USB interrumpido: " + (e.message ?: "error de lectura")
                            }
                        }
                    }
                })
                manager = io
                io.start()
                main.post {
                    connected = true
                    status = "USB serie conectado · 115200 baudios"
                }
            } catch (e: Exception) {
                main.post { status = "Error de conexión: " + (e.message ?: "desconocido") }
            }
        }
    }

    fun send(command: String) {
        if (command !in allowedCommands) {
            status = "Comando no permitido"
            return
        }
        writer.execute {
            try {
                val active = port ?: error("No hay ESP32 conectado")
                active.write(("SF1 " + command + "\n").toByteArray(StandardCharsets.US_ASCII), 2000)
            } catch (e: Exception) {
                main.post { status = "Error USB al enviar: " + (e.message ?: "desconocido") }
            }
        }
    }

    private fun receive(bytes: ByteArray) {
        val lines = mutableListOf<String>()
        synchronized(buffer) {
            for (b in bytes) {
                val c = (b.toInt() and 0xff).toChar()
                when {
                    c == '\n' -> {
                        lines.add(buffer.toString().trimEnd('\r'))
                        buffer.setLength(0)
                    }
                    buffer.length < 4096 -> buffer.append(c)
                    else -> buffer.setLength(0) // discard oversize frame
                }
            }
        }
        if (lines.isNotEmpty()) main.post {
            for (line in lines.filter { it.isNotBlank() }) {
                lastPacket = line
                receivedLines = (receivedLines + line).takeLast(25)
            }
        }
    }

    fun disconnect() {
        manager?.stop()
        manager = null
        try { port?.close() } catch (_: Exception) { }
        port = null
        connected = false
        status = "Sin conexión USB"
        synchronized(buffer) { buffer.setLength(0) }
    }

    fun release() {
        disconnect()
        if (registered) {
            activity.unregisterReceiver(receiver)
            registered = false
        }
        writer.shutdownNow()
    }
}
