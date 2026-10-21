package com.msarobotika.cyberdeck

import android.bluetooth.*
import android.bluetooth.le.*
import android.os.ParcelUuid
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Explicit fixture on the second owner-authorized phone; no arbitrary device is connected. */
@Suppress("MissingPermission")
class BlePairDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun scanAndDiscoverFixtureGattWithoutReadingOrWriting() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val role = InstrumentationRegistry.getArguments().getString("role") ?: error("Set role")
        val folder = File(context.filesDir, "validation").apply { mkdirs() }
        instrumentation.runOnMainSync { ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        if (role == "peripheral") {
            val adapter = context.getSystemService(BluetoothManager::class.java).adapter
            assertTrue("Enable Bluetooth on the fixture phone", adapter.isEnabled)
            assertTrue("BLE advertising not supported on this phone", adapter.isMultipleAdvertisementSupported)
            val uuid = UUID.randomUUID()
            val connected = AtomicBoolean(false)
            val readOrWrite = AtomicBoolean(false)
            val done = CountDownLatch(1)
            val registered = CountDownLatch(1)
            val registrationStatus = AtomicReference<Int>()
            lateinit var server: BluetoothGattServer
            server = context.getSystemService(BluetoothManager::class.java).openGattServer(context, object : BluetoothGattServerCallback() {
                override fun onServiceAdded(status: Int, service: BluetoothGattService) { registrationStatus.set(status); registered.countDown() }
                override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) connected.set(true)
                    if (newState == BluetoothProfile.STATE_DISCONNECTED && connected.get()) done.countDown()
                }
                override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
                    readOrWrite.set(true); server.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
                }
                override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
                    preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
                    readOrWrite.set(true)
                    if (responseNeeded) server.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
                }
            }) ?: error("Cannot create GATT fixture")
            val advertiser = adapter.bluetoothLeAdvertiser ?: error("Advertiser not available")
            val callback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    File(folder, "ble-fixture-ready.json").writeText(JSONObject().put("uuid", uuid.toString()).put("ready", true).toString())
                }
                override fun onStartFailure(errorCode: Int) {
                    File(folder, "ble-fixture-ready.json").writeText(JSONObject().put("ready", false).put("error", errorCode).toString())
                    done.countDown()
                }
            }
            try {
                val service = BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
                service.addCharacteristic(BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ))
                assertTrue(server.addService(service))
                assertTrue(registered.await(10, TimeUnit.SECONDS))
                assertEquals(BluetoothGatt.GATT_SUCCESS, registrationStatus.get())
                advertiser.startAdvertising(AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW).setConnectable(true).setTimeout(90000).build(),
                    AdvertiseData.Builder().addServiceUuid(ParcelUuid(uuid)).setIncludeDeviceName(false).build(), callback)
                assertTrue("GATT fixture was not inspected", done.await(85, TimeUnit.SECONDS))
                assertTrue(connected.get())
                assertFalse("Inspector must not read/write characteristics", readOrWrite.get())
            } finally { advertiser.stopAdvertising(callback); server.close() }
        } else {
            require(role == "scanner")
            val uuid = File(folder, "ble-pair-uuid.txt").readText().trim()
            UUID.fromString(uuid)
            assertTrue("Bluetooth permissions required", BleInspector.hasPermissions(context))
            val startedConnection = AtomicBoolean(false)
            val done = CountDownLatch(1)
            val result = AtomicReference(BleSnapshot())
            lateinit var inspector: BleInspector
            instrumentation.runOnMainSync {
                inspector = BleInspector(context) { state ->
                    result.set(state)
                    val fixture = state.devices.firstOrNull { uuid in it.uuids }
                    if (fixture != null && startedConnection.compareAndSet(false, true)) inspector.inspect(fixture.address)
                    if (startedConnection.get() && !state.gattBusy && state.gatt.any { uuid in it }) done.countDown()
                }
                inspector.start()
            }
            try {
                assertTrue("Fixture not resolved: ${result.get().status}", done.await(25, TimeUnit.SECONDS))
                assertTrue(result.get().gatt.any { it.startsWith("Característica") })
                File(folder, "ble-pair-result.json").writeText(result.get().evidence().toEvidenceJson())
            } finally { instrumentation.runOnMainSync { inspector.stop() } }
        }
    }
}
