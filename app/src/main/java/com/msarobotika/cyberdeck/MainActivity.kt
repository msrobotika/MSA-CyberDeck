package com.msarobotika.cyberdeck

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoho.android.usbserial.driver.UsbSerialDriver
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant

private val Navy = Color(0xFF071018)
private val Panel = Color(0xFF101E2D)
private val Cyan = Color(0xFF5DE7FF)
private val Green = Color(0xFF62F5B0)
private val Amber = Color(0xFFFFD166)
private val Muted = Color(0xFF9AAEC2)

class MainActivity : ComponentActivity() {
    private lateinit var bridge: UsbBridge
    private lateinit var evidence: EvidenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        evidence = EvidenceStore(applicationContext)
        bridge = UsbBridge(this)
        setContent { SuperFlyper(bridge, evidence) }
    }

    override fun onDestroy() {
        bridge.release()
        super.onDestroy()
    }
}

/** Persists genuine received frames, never synthetic counts or demo records. */
private class EvidenceStore(context: Context) {
    private val file = File(context.filesDir, "superflyper-evidence.jsonl")
    var count by mutableIntStateOf(if (file.exists()) file.useLines { it.count() } else 0)
        private set
    var message by mutableStateOf("Sin evidencias nuevas")
        private set

    fun save(packet: String) {
        if (packet.isBlank()) {
            message = "No hay respuesta USB que conservar"
            return
        }
        try {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(packet.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val record = JSONObject().put("utc", Instant.now().toString())
                .put("sha256", digest).put("source", "USB-serial").put("payload", packet)
            file.appendText(record.toString() + "\n")
            count++
            message = "Guardada localmente con SHA-256"
        } catch (e: Exception) {
            message = "Error al guardar: " + (e.message ?: "desconocido")
        }
    }
}

@Composable
private fun SuperFlyper(bridge: UsbBridge, evidence: EvidenceStore) {
    var tab by remember { mutableStateOf("Inicio") }
    val tabs = listOf("Inicio", "USB", "Módulos", "Evidencias")
    MaterialTheme(colorScheme = darkColorScheme(background = Navy, surface = Panel, primary = Cyan)) {
        Surface(color = Navy, modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("SUPERFLYPER", color = Cyan, fontSize = 29.sp,
                        fontWeight = FontWeight.Bold)
                    Text("MS ROBOTIKA · ANDROID + ESP32 · V0.1", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    StatusPill(if (bridge.connected) "● USB CONECTADO" else "○ MODO LOCAL",
                        if (bridge.connected) Green else Amber)
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEach { name ->
                            FilterChip(selected = name == tab, onClick = { tab = name },
                                label = { Text(name) })
                        }
                    }
                }
                item {
                    when (tab) {
                        "USB" -> UsbPanel(bridge, evidence)
                        "Módulos" -> ModulePanel()
                        "Evidencias" -> EvidencePanel(evidence, bridge)
                        else -> HomePanel(bridge, evidence)
                    }
                }
                item {
                    Text("Prototipo · usar únicamente en equipos propios o autorizados",
                        color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HomePanel(bridge: UsbBridge, evidence: EvidenceStore) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard("ESTADO REAL DEL ESP32", bridge.status,
            if (bridge.connected) Green else Amber)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric(bridge.drivers.size.toString(), "USB detectados", Modifier.weight(1f))
            Metric(evidence.count.toString(), "Evidencias", Modifier.weight(1f))
        }
        InfoCard("ENLACE", "USB Host de Android + serie a 115200 baudios. Admite adaptadores CH340, CP210x y CDC compatibles.", Cyan)
        InfoCard("IMPLEMENTADO", "Inventario USB, permisos Android, conexión serie, protocolo SF1, recepción real y SHA-256 local.", Green)
        InfoCard("SIGUIENTE", "NFC · BLE · Wi-Fi · IR · RFID · Sub-GHz · PLC/IoT · IA: módulos pendientes de integración.", Amber)
    }
}

@Composable
private fun UsbPanel(bridge: UsbBridge, evidence: EvidenceStore) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard("CONEXIÓN USB-C / OTG", bridge.status,
            if (bridge.connected) Green else Amber)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { bridge.refresh() }) { Text("BUSCAR USB") }
            OutlinedButton(onClick = { bridge.disconnect() },
                enabled = bridge.connected) { Text("DESCONECTAR") }
        }
        if (bridge.drivers.isEmpty()) {
            InfoCard("NO SE DETECTAN PUERTOS", "Conecta un ESP32 por USB de datos. En placas clásicas se usa el convertidor USB-UART integrado.", Amber)
        } else {
            bridge.drivers.forEach { driver -> DeviceCard(driver) { bridge.connect(driver) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { bridge.send("HELLO") },
                enabled = bridge.connected) { Text("HELLO") }
            OutlinedButton(onClick = { bridge.send("STATUS") },
                enabled = bridge.connected) { Text("STATUS") }
        }
        OutlinedButton(onClick = { bridge.send("CAPABILITIES") },
            enabled = bridge.connected) { Text("CAPACIDADES") }
        OutlinedButton(onClick = { bridge.send("LEGACY_STATUS") },
            enabled = bridge.connected) { Text("DETECTAR FIRMWARE MSA ANTERIOR") }
        InfoCard("ÚLTIMA RESPUESTA REAL",
            bridge.lastPacket.ifBlank { "Aún no hay respuesta del firmware." }, Cyan)
        Button(onClick = { evidence.save(bridge.lastPacket) },
            enabled = bridge.lastPacket.isNotBlank()) { Text("GUARDAR EVIDENCIA") }
        Text("CONSOLA SERIE · 8 LÍNEAS RECIENTES", color = Muted, fontSize = 12.sp)
        bridge.receivedLines.takeLast(8).forEach { line ->
            Text(line, modifier = Modifier.fillMaxWidth()
                .background(Panel, RoundedCornerShape(8.dp)).padding(10.dp),
                color = Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DeviceCard(driver: UsbSerialDriver, connect: () -> Unit) {
    val device = driver.device
    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ADAPTADOR USB " + device.deviceId,
                color = Color.White, fontWeight = FontWeight.Bold)
            Text("VID %04X · PID %04X · %d puerto(s)".format(
                device.vendorId, device.productId, driver.ports.size),
                color = Muted, fontSize = 12.sp)
            Button(onClick = connect) { Text("CONECTAR ESP32") }
        }
    }
}

@Composable
private fun EvidencePanel(evidence: EvidenceStore, bridge: UsbBridge) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard("EVIDENCIAS REALES",
            evidence.count.toString() + " tramas persistentes en almacenamiento privado de Android.", Green)
        InfoCard("INTEGRIDAD", "Cada registro conserva fecha UTC, fuente, contenido y SHA-256 de la trama recibida.", Cyan)
        Button(onClick = { evidence.save(bridge.lastPacket) },
            enabled = bridge.lastPacket.isNotBlank()) { Text("CONSERVAR ÚLTIMA TRAMA") }
        Text(evidence.message, color = Muted, fontSize = 12.sp)
        InfoCard("EXPORTACIÓN", "Pendiente: exportación por selector de documentos de Android.", Amber)
    }
}

@Composable
private fun ModulePanel() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard("USB + ESP32", "Enlace serie SF1: identidad, estado y capacidades (MVP).", Green)
        InfoCard("BLE", "Integrar MSA-BLE-Scout: publicidad BLE y consultas GATT autorizadas.", Amber)
        InfoCard("WIFI", "Integrar MSA-WiFi-Sentinel y MSA-RogueWatch para observación pasiva.", Amber)
        InfoCard("RFID / NFC", "NFC integrado en el móvil; RFID 125 kHz requiere hardware externo.", Amber)
        InfoCard("INFRARROJOS / SUB-GHZ", "Ampliaciones de hardware IR y CC1101 por ESP32.", Amber)
        InfoCard("PLC / DOMÓTICA", "Futuros clientes Modbus TCP, MQTT y registro de instrumentación.", Amber)
        InfoCard("IA", "Asistente futuro para análisis de registros e informes, sin resultados inventados.", Amber)
    }
}

@Composable
private fun InfoCard(title: String, body: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(body, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Text(label, color = color,
        modifier = Modifier.background(color.copy(alpha = 0.12f),
            RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        fontSize = 12.sp)
}
