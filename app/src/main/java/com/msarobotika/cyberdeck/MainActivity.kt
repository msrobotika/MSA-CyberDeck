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
import androidx.compose.ui.platform.testTag
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val permissionAction get() = "$packageName.USB_PERMISSION"
    private var permissionResult: (UsbDevice?) -> Unit = {}
    private var detached: () -> Unit = {}
    private var stopDiscovery: () -> Unit = {}
    private var refreshUsb: () -> Unit = {}
    private var callsRequested by mutableStateOf(false)
    private var receiverRequested by mutableStateOf(false)
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
        callsRequested=intent?.getBooleanExtra("open_calls",false)==true
        receiverRequested=intent?.getBooleanExtra(BrowserTransferService.SHOW_RECEIVER,false)==true
        ContextCompat.registerReceiver(this, receiver, IntentFilter(permissionAction).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { Deck() }
    }
    override fun onStop() { stopDiscovery(); super.onStop() }
    override fun onResume() {super.onResume();refreshUsb()}
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent);setIntent(intent)
        if(intent.getBooleanExtra("open_calls",false))callsRequested=true
        if(intent.getBooleanExtra(BrowserTransferService.SHOW_RECEIVER,false))receiverRequested=true
    }
    override fun onDestroy() { unregisterReceiver(receiver); worker.shutdown(); super.onDestroy() }

    @Composable private fun Deck() {
        val store = remember { EvidenceStore(this) }
        val session=remember{androidx.lifecycle.ViewModelProvider(this@MainActivity)[DeckSessionModel::class.java]}
        val phoneSnapshot by session.phone.collectAsState()
        val resourceSamples by session.samples.collectAsState()
        val phoneLive by session.phoneLive.collectAsState()
        val bridge=remember{UsbBridge(this@MainActivity)}
        DisposableEffect(bridge){onDispose{bridge.release()}}
        SideEffect{refreshUsb={bridge.refresh()}}
        val draftStore = remember { ReportDraftStore(this) }
        var reportContext by rememberSaveable(stateSaver=androidx.compose.runtime.saveable.listSaver(
            save={listOf(it.name,it.scope,it.notes)},restore={ReportContext(it[0],it[1],it[2])})) { mutableStateOf(draftStore.read()) }
        var reportSelection by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
        var usbMode by rememberSaveable { mutableStateOf("Consola") }
        var toolQuery by rememberSaveable { mutableStateOf("") }
        var manageEvidence by rememberSaveable { mutableStateOf(false) }
        var entries by remember { mutableStateOf(emptyList<Evidence>()) }
        var tab by rememberSaveable { mutableStateOf("Resumen") }
        var rootNavigationEvent by remember {mutableIntStateOf(0)}
        val screenStates=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
        LaunchedEffect(callsRequested){if(callsRequested){tab="Llamadas";callsRequested=false}}
        LaunchedEffect(receiverRequested){if(receiverRequested){tab="Recibir";receiverRequested=false}}
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
        val nfcSnapshot by session.nfc.collectAsState()
        val nfc = remember { NfcInspector(this) { session.nfc.value = it } }
        var peerState by remember { mutableStateOf(PeerState()) }
        val peer = remember { PeerSession(this, { peerState = it }, { name, bytes ->
            val loaded = store.saveBytes(name, bytes)
            runOnUiThread { entries = loaded; evidenceQuery = name; evidenceFilter = "Todas"; tab = "Evidencias";
                message = "Archivo recibido con SHA-256 verificado · abre la copia o guárdala en el móvil" }
        }) }
        val browserState by BrowserTransferService.state.collectAsState()
        val browserDelivery by BrowserTransferService.lastDelivery.collectAsState()
        var lastBrowserDelivery by rememberSaveable{mutableStateOf("")}
        LaunchedEffect(browserDelivery?.id) {
            val received=browserDelivery
            if(received!=null && received.id!=lastBrowserDelivery) {
                lastBrowserDelivery=received.id
                runCatching{store.list()}.onSuccess{loaded->
                    entries=loaded
                    if(loaded.any{it.id==received.id}){evidenceQuery=received.name;evidenceFilter="Todas";tab="Evidencias";message="Archivo recibido · abre la copia o guárdala en el móvil"}
                }.onFailure{message="No se pudo actualizar el archivo: ${it.message}"}
            }
        }
        DisposableEffect(ble, nfc, peer) { onDispose { ble.stop(); nfc.close(); peer.close() } }
        SideEffect { stopDiscovery = { inventory.stop(); ble.stop(); nfc.stop(); peer.stop();bridge.disconnect();session.phoneLive.value=false } }
        var notificationsGranted by remember {mutableStateOf(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)}
        val requestNotifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){notificationsGranted=it}
        LaunchedEffect(tab) { if (tab != "NFC") nfc.stop();if(tab!="Móvil")session.phoneLive.value=false }
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
        var pendingPeerInvitation by rememberSaveable { mutableStateOf("") }
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
                runOnUiThread { entries = loaded; evidenceFilter = "Todas"; evidenceQuery = loaded.lastOrNull()?.name.orEmpty() }
                "Archivo copiado y SHA-256 calculado"
            }
        }
        var exportingEvidenceId by rememberSaveable { mutableStateOf("") }
        val fileExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            val id = exportingEvidenceId
            exportingEvidenceId = ""
            if (uri != null && id.isNotBlank()) run {
                val entry = store.list().firstOrNull { it.id == id } ?: error("La evidencia ya no está disponible")
                val file = store.verifiedFile(entry)
                contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                    ?: error("No se pudo escribir en el destino elegido")
                "Copia guardada en el móvil: ${entry.name}"
            }
        }
        fun openEvidence(entry: Evidence) {
            busy = true
            worker.execute {
                val prepared = runCatching {
                    val file = store.verifiedFile(entry)
                    val uri = androidx.core.content.FileProvider.getUriForFile(this@MainActivity,"$packageName.evidence",file,entry.name)
                    val extension = entry.name.substringAfterLast('.', "").lowercase()
                    val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri,mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = android.content.ClipData.newRawUri(entry.name,uri)
                    }
                }
                runOnUiThread {
                    busy = false
                    prepared.fold(onSuccess = { intent ->
                        runCatching { startActivity(intent) }
                            .onSuccess { message = "Copia verificada · abierta con una aplicación del móvil" }
                            .onFailure { message = "No se pudo abrir: usa Guardar en el móvil o instala un visor compatible" }
                    }, onFailure = { message = "No se pudo abrir: ${it.message}" })
                }
            }
        }
        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            if (uri != null) run {
                contentResolver.openOutputStream(uri)?.use { it.write(report.toByteArray(Charsets.UTF_8)) }
                    ?: error("No se pudo escribir el informe")
                "Informe guardado en la ubicación elegida"
            }
        }
        DeckTheme {
            DeckShell(tab,busy,message,navigate={tab=it;rootNavigationEvent++},receiving=browserState.active) {
                screenStates.SaveableStateProvider(tab) {
                val contentScroll=rememberLazyListState()
                LaunchedEffect(rootNavigationEvent){if(tab in listOf("Resumen","Evidencias","Informes"))contentScroll.scrollToItem(0)}
                LaunchedEffect(evidenceQuery,evidenceFilter){if(tab=="Evidencias")contentScroll.scrollToItem(0)}
                LazyColumn(Modifier.fillMaxSize().testTag("deck-list"),state=contentScroll,contentPadding=PaddingValues(horizontal=18.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    when(tab) {
                        "Resumen" -> {
                            item { CiberDeckCommand(phoneSnapshot,busy,analyze={run{
                                val result=PhoneAudit.read(this@MainActivity)
                                runOnUiThread{session.phone.value=result;tab="Móvil"}
                                "Chequeo local terminado"
                            }}) }
                            item { DeckSection("Qué quieres hacer",action="Ver herramientas",click={tab="Herramientas"}) }
                            item { CiberDeckActions { tab=it } }
                            item {
                                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text("Tu conexión",style=MaterialTheme.typography.titleMedium)
                                    Text(connection?.summary ?: "Sin lectura disponible",style=MaterialTheme.typography.bodyMedium)
                                    connection?.let{Text("Última lectura: ${displayTime(it.checkedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                                    OutlinedButton(enabled=!busy,onClick={run {
                                        val current=ConnectionDiagnostic.read(this@MainActivity)
                                        runOnUiThread{connection=current}
                                        current.summary
                                    }}){Text("Comprobar mi conexión")}
                                    connection?.let { DeckDisclosure("Detalles de la conexión",it.details.joinToString("\n"){field->"${field.first}: ${field.second}"}) }
                                } }
                            }
                            item { DeckSection("Actividad reciente",subtitle="${entries.size} copias · ${displayBytes(entries.sumOf{it.bytes})}",action="Ver archivo",click={tab="Evidencias"}) }
                            if(entries.isEmpty())item{Text("Los resultados que guardes aparecerán aquí.",style=MaterialTheme.typography.bodySmall)}
                            items(entries.takeLast(3).asReversed(),key={"recent-${it.id}"}) { entry ->
                                DeckRecent(entry){evidenceQuery=entry.name;evidenceFilter="Todas";tab="Evidencias"}
                            }
                        }
                        "Llamadas" -> {item{CallGuardPanel(save={saveDiagnostic(it)})}}
                        "Enlaces" -> {item{LinkInspectionPanel(save={saveDiagnostic(it)})}}
                        "Móvil" -> {item { PhoneAuditPanel(phoneSnapshot,busy,refresh={run{
                            val result=PhoneAudit.read(this@MainActivity)
                            runOnUiThread{session.phone.value=result}
                            "Chequeo local terminado"
                        }},save={saveDiagnostic(it)},settings={action->
                            runCatching{startActivity(Intent(action))}.onFailure{message="Este ajuste no está disponible en este Android"}
                        },samples=resourceSamples,live=phoneLive,setLive={session.phoneLive.value=it},add={sample->session.samples.value=(session.samples.value+sample).takeLast(60)}) }}
                        "Herramientas" -> {
                            item { DeckToolSearch(toolQuery){toolQuery=it} }
                            val matching=deckTools.filter{toolQuery.isBlank() || "${it.title} ${it.subtitle} ${it.group}".contains(toolQuery.trim(),ignoreCase=true)}
                            if(matching.isEmpty())item{Info("Sin coincidencias","Prueba NFC, DNS, radio o archivos.")}
                            matching.groupBy{it.group}.forEach { (group,tools) ->
                                item { DeckSection(group) }
                                items(tools,key={it.route}) { tool -> DeckToolCard(tool){tab=tool.route} }
                            }
                            item { DeckDisclosure("Acerca de este kit","Las herramientas actúan cuando las inicias. Los datos observados no acreditan identidad ni ausencia de ataques. El ESP es opcional; NFC y Bluetooth usan el móvil. VPN, acceso a archivos privados de otras apps y clonación de credenciales no están implementados.") }
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
                            item { PeerPanel(peerState, receive = { BrowserTransferService.stop(this@MainActivity); peer.receive() }, stop = { peer.stop() }, decide = { peer.decide(it) },
                                copy = { invitation ->
                                    val clip = android.content.ClipData.newPlainText("Invitación temporal CiberDeck", invitation)
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
                                        val bytes = store.report(entries,reportContext).toByteArray(Charsets.UTF_8)
                                        runOnUiThread { runCatching { peer.send(invitation, "informe-CiberDeck.md", bytes) }.onFailure { message = "Envío: ${it.message}" } }
                                        "Informe preparado para la sesión local"
                                    }
                                }) }
                        }
                        "Recibir" -> {
                            item { BrowserReceiverPanel(browserState, start = { peer.stop();runCatching{BrowserTransferService.start(this@MainActivity)}.onFailure{message="No se pudo iniciar la recepción: ${it.message}"} },
                                stop = { BrowserTransferService.stop(this@MainActivity) }, decide = { BrowserTransferService.decide(it) }, copy = { address ->
                                    val clip = android.content.ClipData.newPlainText("Recepción temporal CiberDeck", address)
                                    if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
                                        putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                                    }
                                    (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                    message = "Dirección copiada · HTTP local sin cifrar"
                                },notificationsEnabled=notificationsGranted,requestNotifications={if(Build.VERSION.SDK_INT>=33)requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)}) }
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
                            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                FilterChip(selected=usbMode=="Consola",onClick={usbMode="Consola"},label={Text("Consola USB")})
                                FilterChip(selected=usbMode=="Wi-Fi",onClick={bridge.disconnect();usbMode="Wi-Fi"},label={Text("Redes del ESP32")})
                            } }
                            if(usbMode=="Consola") {
                            item { UsbBridgePanel(bridge,hostBusy=busy || permissionPending,save={
                                val bytes=bridge.export()
                                run{
                                    val loaded=store.saveBytes("superflyper-usb-${System.currentTimeMillis()}.jsonl",bytes)
                                    runOnUiThread{entries=loaded;evidenceQuery=loaded.last().name;evidenceFilter="Todas";tab="Evidencias"}
                                    "Registro USB guardado con SHA-256"
                                }
                            }) }
                            } else {
                            item { Button(enabled = !busy, onClick = {
                                devices = SentinelUsb.supported(usb)
                                sensor = null
                                message = "${devices.size} adaptadores USB-serie compatibles detectados"
                            }) { Text("Actualizar USB") } }
                            if(devices.isEmpty()) item { Info("SIN PERIFÉRICOS", "Conecta un dispositivo con un cable OTG compatible y actualiza la lista.") }
                            items(devices, key = { it.deviceId }) { device ->
                                Info("USB-SERIE", "VID %04X · PID %04X · dispositivo %d".format(device.vendorId, device.productId, device.deviceId))
                                Button(enabled = !busy && !permissionPending && !bridge.connected && !bridge.connecting, onClick = {
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
                        }
                        "Radio" -> {
                            val current = sensor
                            if (current == null) item { Info("SIN LECTURA DE RADIO", "Conecta el sensor en USB y pulsa Leer WiFi Sentinel para ver datos reales.") }
                            else item { RadioDashboard(current) }
                        }
                        "Módulos" -> {item{SuperModulesPanel()}}
                        "Evidencias" -> {
                            val filtered=entries.filter{evidenceMatches(it,evidenceQuery,evidenceFilter)}.let{if(evidenceNewest)it.asReversed() else it}
                            item { EvidenceControls(evidenceQuery,evidenceFilter,evidenceNewest,filtered.size,entries.size,
                                queryChanged={evidenceQuery=it},filterChanged={evidenceFilter=it},orderChanged={evidenceNewest=!evidenceNewest}) }
                            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(enabled=!busy,onClick={importer.launch(arrayOf("*/*"))},modifier=Modifier.weight(1f)){Text("Importar archivo")}
                                OutlinedButton(enabled=!busy,onClick={manageEvidence=!manageEvidence},modifier=Modifier.weight(1f)){Text(if(manageEvidence)"Cerrar gestión" else "Gestionar")}
                            } }
                            item { Text("${displayBytes(entries.sumOf{it.bytes})} en copias privadas · ${displayBytes(filtered.sumOf{it.bytes})} visibles",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                            if(manageEvidence) {
                                item { OutlinedButton(enabled=!busy && entries.isNotEmpty(),onClick={run{store.verify(entries)}}){Text("Verificar copias")} }
                                item { OutlinedButton(enabled=!busy && filtered.isNotEmpty(),onClick={pendingDelete=filtered.toList()}){Text("Borrar resultados visibles",color=MaterialTheme.colorScheme.error)} }
                                item { OutlinedButton(enabled=!busy && filtered.isNotEmpty(),onClick={reportSelection=ArrayList(filtered.map{it.id});tab="Informes"}){Text("Informe de estos resultados")} }
                            }
                            nfcReference?.let { reference -> item {
                                Info("Referencia NFC",reference.second.checkedAt+"\nElige otra lectura para comparar.")
                                TextButton(onClick={nfcReference=null}){Text("Quitar referencia NFC")}
                            } }
                            if(filtered.isEmpty())item{Info("Sin resultados",if(entries.isEmpty())"Guarda un diagnóstico o importa un archivo para empezar." else "Pulsa Ver todas o cambia la búsqueda.")}
                            items(filtered,key={it.id}) { entry ->
                                EvidenceFileCard(entry,busy,
                                    open={openEvidence(entry)},export={exportingEvidenceId=entry.id;fileExporter.launch(entry.name)},
                                    delete={pendingDelete=listOf(entry)},report={run{
                                        val result=store.diagnostic(entry)
                                        runOnUiThread{openedEvidence=result}
                                        "Informe abierto · SHA-256 verificado"
                                    }},reference={run{
                                        val result=store.diagnostic(entry)
                                        check(result.kind=="nfc"){"El contenido no es NFC"}
                                        val reference=nfcReference
                                        if(reference==null){runOnUiThread{nfcReference=entry.id to result};"Referencia NFC seleccionada"}
                                        else {
                                            val baseline=store.diagnostic(store.list().first{it.id==reference.first})
                                            val comparison=DiagnosticResult("nfc-comparison","Dos lecturas guardadas","${baseline.checkedAt} / ${result.checkedAt}",
                                                "Comparación de observaciones · no acredita autenticidad",compareNfc(baseline,result))
                                            runOnUiThread{openedEvidence=comparison};"Comparación NFC preparada · copias verificadas"
                                        }
                                    }},referenceLabel=if(nfcReference==null)"Usar como referencia NFC" else "Comparar con referencia NFC",referenceEnabled=entry.id!=nfcReference?.first)
                            }
                        }
                        "Informes" -> {
                            val selectedEntries=if(reportSelection.isEmpty())entries else entries.filter{it.id in reportSelection}
                            item { Info("De la observación al informe","${selectedEntries.size} copias · ${displayBytes(selectedEntries.sumOf{it.bytes})}. ${if(reportSelection.isEmpty())"Incluye tu archivo actual." else "Selección preparada desde Evidencias."}") }
                            if(reportSelection.isNotEmpty())item{TextButton(onClick={reportSelection=arrayListOf()}){Text("Usar todas las evidencias")}}
                            item { ReportContextPanel(reportContext,changed={reportContext=it},save={val snapshot=reportContext;run{draftStore.save(snapshot);"Contexto guardado en este móvil"}}) }
                            item { Button(enabled=!busy && selectedEntries.isNotEmpty(),modifier=Modifier.fillMaxWidth(),onClick={
                                val snapshot=reportContext
                                run {
                                    draftStore.save(snapshot)
                                    val generated=store.report(selectedEntries,snapshot)
                                    runOnUiThread{report=generated;exporter.launch("MSA-CiberDeck-informe.md")}
                                    "Informe preparado para guardar"
                                }
                            }){Text("Guardar informe")} }
                            item { DeckDisclosure("Qué incluye y qué acredita","El informe contiene contexto, metadatos, SHA-256 y diagnósticos compatibles. Comprueba las copias al prepararlo. La fecha depende del móvil y el hash no certifica procedencia ni una cadena de custodia. Tu contexto es un borrador único; no asigna automáticamente evidencias a proyectos.") }
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
