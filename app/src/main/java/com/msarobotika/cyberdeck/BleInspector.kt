package com.msarobotika.cyberdeck

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.time.Instant

data class BleObservation(val address: String, val name: String, val rssi: Int,
    val uuids: List<String>, val manufacturerIds: List<Int>)
data class BleSnapshot(val running: Boolean = false, val status: String = "Sin búsqueda iniciada",
    val observedAt: String = "", val devices: List<BleObservation> = emptyList(),
    val gattAddress: String = "", val gattBusy: Boolean = false, val gatt: List<String> = emptyList()) {
    fun evidence() = DiagnosticResult("ble", "Anuncios Bluetooth LE", Instant.now().toString(), status,
        listOf("Última observación" to observedAt,
            "Método" to "Búsqueda BLE manual de 10 s, hasta 64 direcciones. RSSI no determina distancia. Las direcciones pueden ser aleatorias; un anuncio no autentica la identidad.") +
            devices.map { it.address to "${it.name}\nRSSI: ${it.rssi} dBm\nUUID anunciados: ${it.uuids.joinToString()}\nIDs de fabricante anunciados: ${it.manufacturerIds.joinToString { id -> "0x%04X".format(id) }}" } +
            gatt.map { "GATT observado en $gattAddress" to it } +
            ("Alcance GATT" to "Solo descubrimiento de servicios tras confirmación. No se leen ni escriben características. Las propiedades anunciadas no demuestran que una operación esté permitida sin autenticación."))
}

@Suppress("MissingPermission")
class BleInspector(private val context: Context, private val changed: (BleSnapshot) -> Unit) {
    companion object {
        fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        fun hasPermissions(context: Context) = permissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    private val main = Handler(Looper.getMainLooper())
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private var scanner: BluetoothLeScanner? = null
    private var state = BleSnapshot()
    private var gatt: BluetoothGatt? = null
    private var generation = 0
    private val deadline = Runnable { stopScan() }
    private val gattDeadline = Runnable { finishGatt("GATT: tiempo de espera agotado") }
    private fun publish() = changed(state)

    private var scanGeneration = 0
    private var activeCallback: ScanCallback? = null
    private fun createCallback(token: Int) = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) { main.post {
            if (!state.running || token != scanGeneration) return@post
            val address = result.device.address
            if (state.devices.none { it.address == address } && state.devices.size >= 64) return@post
            val row = BleObservation(address, result.scanRecord?.deviceName?.take(160) ?: "Sin nombre anunciado", result.rssi,
                result.scanRecord?.serviceUuids?.take(32)?.map { it.toString() } ?: emptyList(),
                result.scanRecord?.manufacturerSpecificData?.let { data -> (0 until minOf(data.size(), 32)).map { data.keyAt(it) } } ?: emptyList())
            state = state.copy(observedAt = Instant.now().toString(),
                devices = (state.devices.filterNot { it.address == address } + row).sortedByDescending { it.rssi })
            publish()
        } }
        override fun onScanFailed(errorCode: Int) { main.post {
            if (state.running && token == scanGeneration) { stopScan(); state = state.copy(status = "Android no pudo iniciar BLE (código $errorCode)"); publish() }
        } }
    }

    fun start() {
        stop()
        if (!hasPermissions(context)) { state = state.copy(status = "Concede los permisos Bluetooth para iniciar"); publish(); return }
        val radio = adapter
        if (radio == null || !radio.isEnabled) { state = state.copy(status = "Bluetooth no disponible o apagado"); publish(); return }
        state = BleSnapshot(running = true, status = "Búsqueda BLE en curso", observedAt = Instant.now().toString())
        publish()
        runCatching {
            scanner = radio.bluetoothLeScanner ?: error("Escáner BLE no disponible")
            activeCallback = createCallback(++scanGeneration)
            scanner!!.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(), activeCallback)
            main.postDelayed(deadline, 10000)
        }.onFailure { stopScan(); state = state.copy(status = "BLE: ${it.message}"); publish() }
    }
    fun stopScan() {
        scanGeneration++
        main.removeCallbacks(deadline)
        runCatching { activeCallback?.let { scanner?.stopScan(it) } }
        activeCallback = null
        scanner = null
        if (state.running) { state = state.copy(running = false, status = "Búsqueda BLE finalizada · ${state.devices.size} direcciones observadas"); publish() }
    }
    fun inspect(address: String) {
        stop()
        if (!hasPermissions(context) || state.devices.none { it.address == address }) return
        val token = ++generation
        state = state.copy(gattAddress = address, gattBusy = true, gatt = emptyList(), status = "Descubriendo servicios GATT")
        publish()
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(client: BluetoothGatt, status: Int, newState: Int) { main.post {
                if (token != generation) { client.close(); return@post }
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    if (!client.discoverServices()) finishGatt("No se pudo iniciar el descubrimiento GATT")
                } else if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED)
                    finishGatt("Conexión GATT terminada (código $status)")
            } }
            override fun onServicesDiscovered(client: BluetoothGatt, status: Int) { main.post {
                if (token != generation) return@post
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val rows = client.services.take(64).flatMap { service ->
                        listOf("Servicio ${service.uuid}") + service.characteristics.take(32).map { c ->
                            val properties = listOf(BluetoothGattCharacteristic.PROPERTY_READ to "READ",
                                BluetoothGattCharacteristic.PROPERTY_WRITE to "WRITE", BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE to "WRITE SIN RESPUESTA",
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY to "NOTIFY", BluetoothGattCharacteristic.PROPERTY_INDICATE to "INDICATE")
                                .filter { (flag, _) -> c.properties and flag != 0 }.joinToString { it.second }
                            "Característica ${c.uuid}: $properties"
                        }
                    }.take(128)
                    state = state.copy(gatt = rows)
                    finishGatt("Servicios GATT observados; características sin leer ni escribir")
                } else finishGatt("Descubrimiento GATT fallido (código $status)")
            } }
        }
        runCatching {
            gatt = adapter?.getRemoteDevice(address)?.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                ?: error("No se pudo crear la conexión GATT")
            main.postDelayed(gattDeadline, 8000)
        }.onFailure { finishGatt("GATT: ${it.message}") }
    }
    private fun finishGatt(message: String) {
        generation++
        main.removeCallbacks(gattDeadline)
        runCatching { gatt?.disconnect(); gatt?.close() }; gatt = null
        state = state.copy(gattBusy = false, status = message); publish()
    }
    fun stop() { stopScan(); if (gatt != null || state.gattBusy) finishGatt("Sesión GATT detenida") }
}
