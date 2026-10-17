package com.msarobotika.cyberdeck
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val permissionAction get() = "$packageName.USB_PERMISSION"
    private var permissionResult: (UsbDevice?) -> Unit = {}
    private var detached: () -> Unit = {}
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) { detached(); return }
            if (intent.action != permissionAction) return
            val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            permissionResult(device)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(this, receiver, IntentFilter(permissionAction).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { Deck() }
    }
    override fun onDestroy() { unregisterReceiver(receiver); worker.shutdown(); super.onDestroy() }

    @Composable private fun Deck() {
        val store = remember { EvidenceStore(this) }
        var entries by remember { mutableStateOf(emptyList<Evidence>()) }
        var tab by remember { mutableStateOf("Resumen") }
        var message by remember { mutableStateOf("Datos locales · sin sensores enlazados") }
        var busy by remember { mutableStateOf(false) }
        var devices by remember { mutableStateOf(emptyList<UsbDevice>()) }
        var sensor by remember { mutableStateOf<SensorSnapshot?>(null) }
        var permissionPending by remember { mutableStateOf(false) }
        val usb = remember { getSystemService(USB_SERVICE) as UsbManager }
        var report by remember { mutableStateOf("") }
        fun run(action: () -> String) {
            busy = true
            worker.execute {
                val result = try { action() } catch (e: Exception) { "Error: ${e.message}" }
                runOnUiThread { message = result; busy = false }
            }
        }
        fun readSensor(device: UsbDevice) {
            sensor = null
            run {
                val snapshot = SentinelUsb.snapshot(usb, device)
                check(usb.deviceList.values.any { it.deviceId == device.deviceId }) { "Sensor desconectado" }
                runOnUiThread { sensor = snapshot }
                "Lectura completada: ${snapshot.networks.size} redes · ${java.time.Instant.now()}"
            }
        }
        SideEffect {
            permissionResult = { device ->
                permissionPending = false
                if (device != null && usb.hasPermission(device)) readSensor(device)
                else message = "Permiso USB denegado o dispositivo desconectado"
            }
            detached = {
                sensor = null
                permissionPending = false
                devices = SentinelUsb.supported(usb)
                message = "Desconexión USB: actualiza y vuelve a leer el sensor"
            }
        }
        LaunchedEffect(Unit) { run {
            val loaded = store.list()
            runOnUiThread { entries = loaded }
            "${loaded.size} evidencias guardadas"
        } }
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) run {
                val loaded = store.import(uri)
                runOnUiThread { entries = loaded }
                "Archivo copiado y SHA-256 calculado"
            }
        }
        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri != null) run {
                contentResolver.openOutputStream(uri)?.use { it.write(report.toByteArray(Charsets.UTF_8)) }
                    ?: error("No se pudo escribir el informe")
                "Informe guardado en la ubicación elegida"
            }
        }
        MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF071018), surface = Color(0xFF101E2D), primary = Color(0xFF5DE7FF))) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LazyColumn(Modifier.systemBarsPadding(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { Text("MSA CyberDeck", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp); Text("CUADERNO DE AUDITORÍA · Desarrollo") }
                    item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Resumen","USB","Radio","Evidencias","Informes").forEach { label ->
                            FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                        }
                    } }
                    item { if(busy) LinearProgressIndicator(Modifier.fillMaxWidth()); Text(message) }
                    when(tab) {
                        "Resumen" -> {
                            item { RobotHero(sensor != null, sensor?.mismatches ?: 0L) }
                            item { Info("TU ESPACIO", "${entries.size} evidencias importadas · ${devices.size} periféricos USB enumerados") }
                            item { Info("EMPIEZA AQUÍ", "Importa un archivo en Evidencias. Se conserva una copia privada y se calcula su SHA-256. Puedes verificarla y exportar un informe.") }
                            item { Info("CONECTORES", "WiFi Sentinel: lectura USB disponible, pendiente de validar con Android. BLE Scout y ChipSafe pendientes de integración.") }
                        }
                        "USB" -> {
                            item { Button(enabled = !busy, onClick = {
                                devices = SentinelUsb.supported(usb)
                                sensor = null
                                message = "${devices.size} adaptadores USB-serie compatibles detectados"
                            }) { Text("Actualizar USB") } }
                            if(devices.isEmpty()) item { Info("SIN PERIFÉRICOS", "Conecta un dispositivo con un cable OTG compatible y actualiza la lista.") }
                            items(devices, key = { it.deviceId }) { device ->
                                Info("USB-SERIE", "VID %04X · PID %04X · dispositivo %d".format(device.vendorId, device.productId, device.deviceId))
                                Button(enabled = !busy && !permissionPending, onClick = {
                                    if (usb.hasPermission(device)) readSensor(device) else {
                                        permissionPending = true
                                        val permission = PendingIntent.getBroadcast(this@MainActivity, device.deviceId,
                                            Intent(permissionAction).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                                        runCatching { usb.requestPermission(device, permission) }.onFailure {
                                            permissionPending = false; message = "No se pudo solicitar acceso USB: ${it.message}"
                                        }
                                    }
                                }) { Text("Leer WiFi Sentinel") }
                            }
                            sensor?.let { snapshot ->
                                item { Info(snapshot.firmware, "MAC ${snapshot.mac}\nMonitor ${if (snapshot.running) "activo" else "pausado"} · canal ${snapshot.channel}\n${snapshot.frames} tramas · ${snapshot.networks.size} redes\nDeauth ${snapshot.deauth} · disassoc ${snapshot.disassoc}\nCambios de seguridad ${snapshot.mismatches}\nSon observaciones, no pruebas automáticas de ataque.") }
                                items(snapshot.networks, key = { it.id }) { ap ->
                                    Info(ap.ssid.ifEmpty { "SSID oculto" }, "${ap.bssid}\nCanal ${ap.channel} · ${ap.rssi} dBm · ${ap.security}")
                                }
                            }
                        }
                        "Radio" -> {
                            val current = sensor
                            if (current == null) item { Info("SIN LECTURA DE RADIO", "Conecta el sensor en USB y pulsa Leer WiFi Sentinel para ver datos reales.") }
                            else item { RadioDashboard(current) }
                        }
                        "Evidencias" -> {
                            item { Button(enabled = !busy,onClick = { importer.launch(arrayOf("*/*")) }) { Text("Importar archivo") } }
                            item { OutlinedButton(enabled = !busy && entries.isNotEmpty(),onClick = { run { store.verify(entries) } }) { Text("Verificar copias") } }
                            if(entries.isEmpty()) item { Info("SIN EVIDENCIAS","Todavía no has importado archivos.") }
                            items(entries,key = { it.id }) { Info(it.name,"${it.bytes} bytes\n${it.importedAt}\nSHA-256\n${it.sha256}") }
                        }
                        "Informes" -> {
                            item { Info("INFORME MARKDOWN","Incluye identificador, nombre, tamaño, fecha y hash de cada copia importada.") }
                            item { Button(enabled = !busy && entries.isNotEmpty(),onClick = { report = store.report(entries); exporter.launch("MSA-CyberDeck-informe.md") }) { Text("Guardar informe") } }
                            item { Info("ALCANCE","El hash comprueba cambios; no certifica la procedencia ni constituye una cadena de custodia pericial.") }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun Info(title: String,body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title,color = MaterialTheme.colorScheme.primary)
            Text(body)
        }
    }
}
