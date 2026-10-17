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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    private var stopDiscovery: () -> Unit = {}
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
    override fun onStop() { stopDiscovery(); super.onStop() }
    override fun onDestroy() { unregisterReceiver(receiver); worker.shutdown(); super.onDestroy() }

    @Composable private fun Deck() {
        val store = remember { EvidenceStore(this) }
        var entries by remember { mutableStateOf(emptyList<Evidence>()) }
        var tab by rememberSaveable { mutableStateOf("Resumen") }
        val contentScroll = rememberLazyListState()
        LaunchedEffect(tab) { contentScroll.scrollToItem(0) }
        var connection by remember { mutableStateOf<DiagnosticResult?>(null) }
        var message by remember { mutableStateOf("Datos locales · sin sensores enlazados") }
        var busy by remember { mutableStateOf(false) }
        var devices by remember { mutableStateOf(emptyList<UsbDevice>()) }
        var sensor by remember { mutableStateOf<SensorSnapshot?>(null) }
        var permissionPending by remember { mutableStateOf(false) }
        val usb = remember { getSystemService(USB_SERVICE) as UsbManager }
        var report by remember { mutableStateOf("") }
        var diagnostic by remember { mutableStateOf<DiagnosticResult?>(null) }
        var evidenceQuery by rememberSaveable { mutableStateOf("") }
        var evidenceFilter by rememberSaveable { mutableStateOf("Todas") }
        var evidenceNewest by rememberSaveable { mutableStateOf(true) }
        var openedEvidence by remember { mutableStateOf<DiagnosticResult?>(null) }
        var nfcReference by remember { mutableStateOf<Pair<String, DiagnosticResult>?>(null) }
        var pendingDelete by remember { mutableStateOf<List<Evidence>?>(null) }
        var inventorySnapshot by remember { mutableStateOf(LanSnapshot()) }
        val inventory = remember { LanInventory(this) { inventorySnapshot = it } }
        DisposableEffect(inventory) { onDispose { inventory.stop() } }
        var bleSnapshot by remember { mutableStateOf(BleSnapshot()) }
        var blePermission by remember { mutableStateOf(BleInspector.hasPermissions(this)) }
        val ble = remember { BleInspector(this) { bleSnapshot = it } }
        var nfcSnapshot by remember { mutableStateOf(NfcSnapshot()) }
        val nfc = remember { NfcInspector(this) { nfcSnapshot = it } }
        var peerState by remember { mutableStateOf(PeerState()) }
        val peer = remember { PeerSession(this, { peerState = it }, { name, bytes ->
            val loaded = store.saveBytes(name, bytes)
            runOnUiThread { entries = loaded; message = "Archivo recibido con SHA-256 verificado" }
        }) }
        var browserState by remember { mutableStateOf(BrowserState()) }
        val browser = remember { BrowserReceiver(this, { browserState = it }, { name, bytes ->
            val loaded = store.saveBytes(name, bytes)
            runOnUiThread { entries = loaded; message = "Archivo del navegador guardado con SHA-256" }
        }) }
        DisposableEffect(ble, nfc, peer, browser) { onDispose { ble.stop(); nfc.close(); peer.close(); browser.close() } }
        SideEffect { stopDiscovery = { inventory.stop(); ble.stop(); nfc.stop(); peer.stop(); browser.stop() } }
        LaunchedEffect(tab) { if (tab != "NFC") nfc.stop() }
        DisposableEffect(peerState.active, nfcSnapshot.armed, browserState.active) {
            if (peerState.active || nfcSnapshot.armed || browserState.active) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
        val bluetoothPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            blePermission = BleInspector.hasPermissions(this)
            message = if (blePermission) "Permisos Bluetooth concedidos; puedes iniciar una búsqueda" else "Bluetooth necesita los permisos solicitados"
        }
        fun run(action: () -> String) {
            busy = true
            worker.execute {
                val result = try { action() } catch (e: Exception) { "Error: ${e.message}" }
                runOnUiThread { message = result; busy = false }
            }
        }
        var pendingPeerInvitation by remember { mutableStateOf("") }
        val peerFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val invitation = pendingPeerInvitation
                run {
                    val name = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    } ?: "archivo.bin"
                    val bytes = contentResolver.openInputStream(uri)?.use { PeerProtocol.readSelectedFile(it) }
                        ?: error("No se pudo leer el archivo seleccionado")
                    runOnUiThread { runCatching { peer.send(invitation, name, bytes) }.onFailure { message = "Envío: ${it.message}" } }
                    "Archivo seleccionado preparado para la sesión local"
                }
            }
        }
        fun saveDiagnostic(result: DiagnosticResult) {
            run {
                val loaded = store.saveBytes("diagnostico-${result.kind}-${System.currentTimeMillis()}.json", result.toEvidenceJson().toByteArray(Charsets.UTF_8))
                runOnUiThread { entries = loaded }
                "Resultado guardado con SHA-256"
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
            val current = ConnectionDiagnostic.read(this@MainActivity)
            runOnUiThread { entries = loaded; connection = current }
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
                Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        Text("MSA CyberDeck", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Resumen","Mi red","Bluetooth","NFC","Recibir","Sesiones","Red","Web","USB","Radio","Evidencias","Informes").forEach { label ->
                                FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                            }
                        }
                        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = contentScroll, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when(tab) {
                        "Resumen" -> {
                            item {
                                Text("EMPIEZA AQUÍ", color = MaterialTheme.colorScheme.primary)
                                Text("La app puede diagnosticar la conexión y revisar una web sin ESP. El sensor añade observaciones de radio.")
                                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                                    run {
                                        val current = ConnectionDiagnostic.read(this@MainActivity)
                                        runOnUiThread { connection = current }
                                        current.summary
                                    }
                                }) { Text("Comprobar mi conexión") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { tab = "Bluetooth" }) { Text("Inspeccionar Bluetooth LE") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { tab = "NFC" }) { Text("Leer una etiqueta NFC") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { tab = "Sesiones" }) { Text("Transferir entre móviles") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { tab = "Recibir" }) { Text("Recibir sin instalar app en el otro móvil") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { tab = "Mi red" }) { Text("Descubrir servicios de mi red") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { tab = "Red" }) { Text("Abrir herramientas DNS / TCP") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { tab = "Web" }) { Text("Revisar certificado y cabeceras web") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { tab = "USB" }) { Text("Conectar sensor ESP por OTG") }
                            }
                            connection?.let { current ->
                                item { Info("CONEXIÓN DEL MÓVIL", current.summary + "\n" + current.details.joinToString("\n") { "${it.first}: ${it.second}" }) }
                            }
                            item { RobotHero(sensor != null, sensor?.mismatches ?: 0L) }
                            item { Info("TU ESPACIO", "${entries.size} evidencias importadas · ${devices.size} periféricos USB enumerados") }
                            item { Info("HERRAMIENTAS", "Red: resolución DNS y conexión TCP. Web: revisión de TLS, cabeceras y redirecciones. Guarda cada resultado como evidencia con SHA-256.") }
                            item { Info("CONECTORES", "WiFi Sentinel: lectura USB disponible, pendiente de validar con Android. BLE Scout y ChipSafe pendientes de integración.") }
                        }
                        "Bluetooth" -> {
                            item { BlePanel(bleSnapshot, blePermission, busy,
                                permissions = { bluetoothPermissions.launch(BleInspector.permissions()) },
                                start = { blePermission = BleInspector.hasPermissions(this@MainActivity); ble.start() },
                                stop = { ble.stop() }, inspect = { ble.inspect(it) }, save = { saveDiagnostic(it) }) }
                        }
                        "NFC" -> {
                            item { OutlinedButton(onClick = {
                                evidenceFilter = "NFC"; evidenceQuery = ""; tab = "Evidencias"
                            }) { Text("Historial NFC guardado") } }
                            item { NfcPanel(nfcSnapshot, nfc.hardwareStatus(), busy, start = { nfc.start() },
                                stop = { nfc.stop() }, save = { saveDiagnostic(it) }) }
                        }
                        "Sesiones" -> {
                            item { PeerPanel(peerState, receive = { browser.stop(); peer.receive() }, stop = { peer.stop() }, decide = { peer.decide(it) },
                                copy = { invitation ->
                                    val clip = android.content.ClipData.newPlainText("Invitación temporal CyberDeck", invitation)
                                    if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
                                        putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                                    }
                                    (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                    message = "Invitación copiada; compártela solo con la persona autorizada"
                                }, selectFile = { invitation ->
                                    runCatching { PeerProtocol.invite(invitation) }.onSuccess {
                                        pendingPeerInvitation = invitation; peerFilePicker.launch(arrayOf("*/*"))
                                    }.onFailure { message = "Invitación: ${it.message}" }
                                }, sendReport = { invitation ->
                                    run {
                                        val bytes = store.report(entries).toByteArray(Charsets.UTF_8)
                                        runOnUiThread { runCatching { peer.send(invitation, "informe-CyberDeck.md", bytes) }.onFailure { message = "Envío: ${it.message}" } }
                                        "Informe preparado para la sesión local"
                                    }
                                }) }
                        }
                        "Recibir" -> {
                            item { BrowserReceiverPanel(browserState, start = { peer.stop(); browser.start() },
                                stop = { browser.stop() }, decide = { browser.decide(it) }, copy = { address ->
                                    val clip = android.content.ClipData.newPlainText("Recepción temporal CyberDeck", address)
                                    if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
                                        putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                                    }
                                    (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                    message = "Dirección copiada · HTTP local sin cifrar"
                                }) }
                        }
                        "Mi red" -> {
                            item { LanInventoryPanel(inventorySnapshot, busy,
                                start = { inventory.start() }, stop = { inventory.stop() },
                                save = { result ->
                                    run {
                                        val loaded = store.saveBytes("diagnostico-lan-${System.currentTimeMillis()}.json",
                                            result.toEvidenceJson().toByteArray(Charsets.UTF_8))
                                        runOnUiThread { entries = loaded }
                                        "Inventario guardado con SHA-256"
                                    }
                                }) }
                        }
                        "Red", "Web" -> {
                            item {
                                DiagnosticsPanel(tab, busy, diagnostic?.takeIf { (it.kind == "web") == (tab == "Web") },
                                    onRun = { kind, target, action ->
                                        diagnostic = null
                                        run {
                                            val result = try { action() } catch (e: Exception) {
                                                val cause = e.cause ?: e
                                                DiagnosticResult(kind, target, java.time.Instant.now().toString(),
                                                    "Comprobación no completada", listOf("Error" to "${cause.javaClass.simpleName}: ${cause.message ?: "sin detalle"}"))
                                            }
                                            runOnUiThread { diagnostic = result }
                                            result.summary
                                        }
                                    }, onSave = { result ->
                                        run {
                                            val loaded = store.saveBytes("diagnostico-${result.kind}-${System.currentTimeMillis()}.json",
                                                result.toEvidenceJson().toByteArray(Charsets.UTF_8))
                                            runOnUiThread { entries = loaded }
                                            "Resultado guardado con SHA-256"
                                        }
                                    })
                            }
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
                            val filtered = entries.filter { evidenceMatches(it, evidenceQuery, evidenceFilter) }
                                .let { if (evidenceNewest) it.asReversed() else it }
                            item { EvidenceControls(evidenceQuery, evidenceFilter, evidenceNewest, filtered.size, entries.size,
                                queryChanged = { evidenceQuery = it }, filterChanged = { evidenceFilter = it },
                                orderChanged = { evidenceNewest = !evidenceNewest }) }
                            item { Text("Copias privadas: ${entries.sumOf { it.bytes }} bytes · visibles: ${filtered.sumOf { it.bytes }} bytes") }
                            item { Button(enabled = !busy,onClick = { importer.launch(arrayOf("*/*")) }) { Text("Importar archivo") } }
                            item { OutlinedButton(enabled = !busy && entries.isNotEmpty(),onClick = { run { store.verify(entries) } }) { Text("Verificar copias") } }
                            item { OutlinedButton(enabled = !busy && filtered.isNotEmpty(), onClick = { pendingDelete = filtered.toList() }) {
                                Text("Borrar resultados visibles", color = MaterialTheme.colorScheme.error)
                            } }
                            nfcReference?.let { reference -> item {
                                Info("REFERENCIA NFC", reference.second.checkedAt + "\nElige otra lectura para comparar. Coincidir no demuestra autenticidad.")
                                TextButton(onClick = { nfcReference = null }) { Text("Quitar referencia NFC") }
                            } }
                            if(filtered.isEmpty()) item { Info("SIN RESULTADOS", if(entries.isEmpty()) "Todavía no hay evidencias guardadas." else "Cambia la búsqueda o el filtro.") }
                            items(filtered,key = { it.id }) { entry ->
                                Info(entry.name,"${entry.bytes} bytes\n${entry.importedAt}\nSHA-256\n${entry.sha256}")
                                TextButton(enabled = !busy, onClick = { pendingDelete = listOf(entry) }) {
                                    Text("Borrar esta copia", color = MaterialTheme.colorScheme.error)
                                }
                                if (entry.name.startsWith("diagnostico-")) {
                                    OutlinedButton(enabled = !busy, onClick = { run {
                                        val result = store.diagnostic(entry)
                                        runOnUiThread { openedEvidence = result }
                                        "Informe abierto · SHA-256 verificado"
                                    } }) { Text("Abrir informe guardado") }
                                }
                                if (entry.name.startsWith("diagnostico-nfc-")) {
                                    OutlinedButton(enabled = !busy && entry.id != nfcReference?.first, onClick = { run {
                                        val result = store.diagnostic(entry)
                                        check(result.kind == "nfc") { "El contenido no es NFC" }
                                        val reference = nfcReference
                                        if (reference == null) {
                                            runOnUiThread { nfcReference = entry.id to result }
                                            "Referencia NFC seleccionada"
                                        } else {
                                            val referenceEntry = store.list().first { it.id == reference.first }
                                            val referenceResult = store.diagnostic(referenceEntry)
                                            val comparison = DiagnosticResult("nfc-comparison", "Dos lecturas guardadas",
                                                "${referenceResult.checkedAt} / ${result.checkedAt}",
                                                "Comparación de observaciones · no acredita autenticidad ni compara memoria no leída",
                                                compareNfc(referenceResult, result))
                                            runOnUiThread { openedEvidence = comparison }
                                            "Comparación NFC preparada · ambas copias verificadas al abrir"
                                        }
                                    } }) { Text(if (nfcReference == null) "Usar como referencia NFC" else "Comparar con referencia NFC") }
                                }
                            }
                        }
                        "Informes" -> {
                            item { Info("INFORME MARKDOWN","Incluye identificador, nombre, tamaño, fecha y hash de cada copia importada.") }
                            item { Button(enabled = !busy && entries.isNotEmpty(),onClick = {
                                run {
                                    val generated = store.report(entries)
                                    runOnUiThread { report = generated; exporter.launch("MSA-CyberDeck-informe.md") }
                                    "Informe preparado para guardar"
                                }
                            }) { Text("Guardar informe") } }
                            item { Info("ALCANCE","El hash comprueba cambios; no certifica la procedencia ni constituye una cadena de custodia pericial.") }
                        }
                    }
                }
                }
            }
            openedEvidence?.let { result -> DiagnosticDialog("INFORME GUARDADO", result) { openedEvidence = null } }
            pendingDelete?.let { selected -> AlertDialog(onDismissRequest = { pendingDelete = null },
                title = { Text("¿Borrar ${selected.size} copias?") },
                text = { Text("Se eliminarán ${selected.sumOf { it.bytes }} bytes de evidencias privadas y sus entradas del informe. No hay papelera. Los archivos originales del móvil y las exportaciones anteriores se conservan.") },
                confirmButton = { TextButton(onClick = {
                    pendingDelete = null
                    run {
                        try { selected.forEach { store.delete(it.id) } }
                        finally {
                            val loaded = store.list()
                            runOnUiThread {
                                entries = loaded; openedEvidence = null
                                if (loaded.none { it.id == nfcReference?.first }) nfcReference = null
                            }
                        }
                        "${selected.size} copias eliminadas · almacenamiento liberado"
                    }
                }) { Text("Borrar definitivamente", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Conservar copias") } }) }
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
