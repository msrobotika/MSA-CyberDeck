package com.msarobotika.cyberdeck
import android.hardware.usb.UsbManager
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Deck() }
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }

    @Composable private fun Deck() {
        val store = remember { EvidenceStore(this) }
        var entries by remember { mutableStateOf(emptyList<Evidence>()) }
        var tab by remember { mutableStateOf("Resumen") }
        var message by remember { mutableStateOf("Datos locales · sin sensores enlazados") }
        var busy by remember { mutableStateOf(false) }
        var devices by remember { mutableStateOf(emptyList<String>()) }
        var report by remember { mutableStateOf("") }
        fun run(action: () -> String) {
            busy = true
            worker.execute {
                val result = try { action() } catch (e: Exception) { "Error: ${e.message}" }
                runOnUiThread { message = result; busy = false }
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
                        listOf("Resumen","USB","Evidencias","Informes").forEach { label ->
                            FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                        }
                    } }
                    item { if(busy) LinearProgressIndicator(Modifier.fillMaxWidth()); Text(message) }
                    when(tab) {
                        "Resumen" -> {
                            item { Info("TU ESPACIO", "${entries.size} evidencias importadas · ${devices.size} periféricos USB enumerados") }
                            item { Info("EMPIEZA AQUÍ", "Importa un archivo en Evidencias. Se conserva una copia privada y se calcula su SHA-256. Puedes verificarla y exportar un informe.") }
                            item { Info("CONECTORES", "Protocolos WiFi Sentinel, BLE Scout y ChipSafe pendientes de integración.") }
                        }
                        "USB" -> {
                            item { Button(enabled = !busy, onClick = {
                                val usb = getSystemService(USB_SERVICE) as UsbManager
                                devices = usb.deviceList.values.map { "VID %04X · PID %04X\nClase %d · %d interfaces".format(it.vendorId,it.productId,it.deviceClass,it.interfaceCount) }
                                message = "${devices.size} periféricos USB detectados"
                            }) { Text("Actualizar USB") } }
                            if(devices.isEmpty()) item { Info("SIN PERIFÉRICOS", "Conecta un dispositivo con un cable OTG compatible y actualiza la lista.") }
                            items(devices) { Info("USB",it) }
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
