package com.msarobotika.cyberdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun BlePanel(snapshot: BleSnapshot, permissionGranted: Boolean, saveBusy: Boolean,
    permissions: () -> Unit, start: () -> Unit, stop: () -> Unit, inspect: (String) -> Unit,
    save: (DiagnosticResult) -> Unit) {
    var confirmAddress by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("BLUETOOTH · INSPECTOR BLE", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Búsqueda manual de 10 segundos. Los anuncios pueden usar nombres y direcciones cambiantes; no acreditan identidad ni distancia.")
        Text(snapshot.status)
        if (!permissionGranted) Button(onClick = permissions) { Text("Conceder permisos Bluetooth") }
        if (snapshot.running || snapshot.gattBusy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = stop) { Text("Detener Bluetooth") }
        } else Button(enabled = permissionGranted, onClick = start) { Text("Buscar anuncios BLE · 10 s") }
        Text("${snapshot.devices.size} direcciones BLE observadas")
        if (snapshot.devices.isNotEmpty()) Button(enabled = !saveBusy && !snapshot.running && !snapshot.gattBusy,
            onClick = { save(snapshot.evidence()) }) { Text("Guardar observaciones BLE") }
        snapshot.devices.forEach { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.name, color = MaterialTheme.colorScheme.primary)
                    Text("${row.address} · ${row.rssi} dBm")
                    Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(((row.rssi + 100) / 70f).coerceIn(.02f, 1f)).height(6.dp)
                            .background(MaterialTheme.colorScheme.primary))
                    }
                    if (row.uuids.isNotEmpty()) Text("UUID anunciados: ${row.uuids.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(enabled = !snapshot.gattBusy && !snapshot.running,
                        onClick = { confirmAddress = row.address }) { Text("Consultar servicios GATT") }
                }
            }
        }
        if (snapshot.gatt.isNotEmpty()) {
            Text("GATT · ${snapshot.gattAddress}", color = MaterialTheme.colorScheme.primary)
            snapshot.gatt.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Text("Se descubren servicios; no se leen ni escriben características. Las propiedades READ/WRITE no prueban acceso sin autenticación. Android puede filtrar algunos anuncios por privacidad.",
            style = MaterialTheme.typography.bodySmall)
    }
    confirmAddress?.let { address ->
        AlertDialog(onDismissRequest = { confirmAddress = null }, title = { Text("Confirmar dispositivo autorizado") },
            text = { Text("Se abrirá una conexión con $address para consultar servicios GATT. Confirma que es tu equipo o tienes autorización. No se leerán ni escribirán sus características.") },
            confirmButton = { TextButton(onClick = { confirmAddress = null; inspect(address) }) { Text("Consultar servicios") } },
            dismissButton = { TextButton(onClick = { confirmAddress = null }) { Text("Cancelar") } })
    }
}

@Composable fun NfcPanel(snapshot: NfcSnapshot, hardware: String, saveBusy: Boolean,
    start: () -> Unit, stop: () -> Unit, save: (DiagnosticResult) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("NFC · INSPECTOR DE ETIQUETAS", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        if (snapshot.result == null) {
            Text(hardware)
            Text("Acerca una etiqueta NFC propia. La lectura muestra las tecnologías y, si está disponible, contenido NDEF. No clona tarjetas ni explora áreas protegidas.")
        } else {
            Text("LECTURA TERMINADA · resultado disponible", color = MaterialTheme.colorScheme.primary)
            Text("El lector se detiene tras detectar una tarjeta. La app sigue abierta; el resultado está debajo.")
            snapshot.result.details.firstOrNull { it.first == "Tecnologías" }?.let { Text("Tecnologías: ${it.second}") }
        }
        Text(snapshot.status)
        if (snapshot.armed) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = stop) { Text("Detener lector NFC") }
        } else Button(onClick = start) { Text("Leer etiqueta NFC · 30 s") }
        snapshot.result?.let { result ->
            Button(enabled = !saveBusy && !snapshot.armed, onClick = { save(result) }) { Text("Guardar informe NFC") }
            Text("Guarda un informe en Evidencias. Para fichar con el móvil necesitas una credencial compatible emitida por el sistema de fichaje.", style = MaterialTheme.typography.bodySmall)
            result.details.forEach { (key, value) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) { Text(key, color = MaterialTheme.colorScheme.primary); Text(value) }
                }
            }
        }
        Text("Los enlaces se muestran como texto y no se abren. No hay escritura de etiquetas ni comandos personalizados de tarjetas.", style = MaterialTheme.typography.bodySmall)
    }
}
