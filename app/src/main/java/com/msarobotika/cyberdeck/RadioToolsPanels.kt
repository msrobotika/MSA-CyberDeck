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
        Text("Observa anuncios cercanos durante 10 segundos.",style=MaterialTheme.typography.bodyMedium)
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
        Text("NFC · INSPECTOR DE ETIQUETAS",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        val result=snapshot.result
        if(result==null) {
            Text(hardware,style=MaterialTheme.typography.bodyMedium)
            Text("Acerca tu etiqueta después de iniciar la lectura.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } else Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Lectura terminada",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
                Text(result.summary,style=MaterialTheme.typography.titleMedium)
                result.details.firstOrNull{it.first=="Tecnologías"}?.let{Text(it.second,style=MaterialTheme.typography.bodyMedium)}
                Button(enabled=!saveBusy && !snapshot.armed,onClick={save(result)},modifier=Modifier.fillMaxWidth()){Text("Guardar informe NFC")}
            }
        }
        if(snapshot.armed) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(snapshot.status)
            OutlinedButton(onClick=stop,modifier=Modifier.fillMaxWidth()){Text("Detener lector NFC")}
        } else Button(onClick=start,modifier=Modifier.fillMaxWidth()){Text(if(result==null)"Leer etiqueta NFC · 30 s" else "Leer otra etiqueta · 30 s")}
        if(result==null && !snapshot.armed)Text(snapshot.status,style=MaterialTheme.typography.bodySmall)
        result?.details?.filterNot{it.first in listOf("Alcance","Guardar informe","Tarjetas de acceso o fichaje","Tecnologías")}?.forEach{(key,value)->
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(key,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.text.selection.SelectionContainer{Text(value,style=MaterialTheme.typography.bodyMedium)}
            }}
        }
        DeckDisclosure("Qué puedo leer y guardar","Se observan identificador, tecnologías y NDEF disponible. Guardar conserva un informe, no una credencial utilizable para fichar o pagar. No se escriben datos, exploran sectores ni envían comandos personalizados. Los enlaces se muestran como texto y no se abren. El identificador no acredita identidad ni autenticidad.")
    }
}
