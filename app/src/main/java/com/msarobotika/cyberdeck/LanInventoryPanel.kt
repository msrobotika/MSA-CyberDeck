package com.msarobotika.cyberdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun LanInventoryPanel(snapshot: LanSnapshot, busy: Boolean,
    start: () -> Unit, stop: () -> Unit, save: (DiagnosticResult) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MI RED · SERVICIOS ANUNCIADOS", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary)
        Text("Explora tu red o una red autorizada. Busca durante 15 segundos servicios web, impresión, multimedia y estaciones de trabajo publicados mediante mDNS/DNS-SD.")
        Text("No es una lista completa de equipos conectados. Un equipo puede publicar varios servicios; otros pueden no anunciar nada.",
            style = MaterialTheme.typography.bodySmall)
        if (snapshot.running) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = stop, modifier = Modifier.fillMaxWidth()) { Text("Detener descubrimiento") }
        } else Button(enabled = !busy, onClick = start, modifier = Modifier.fillMaxWidth()) {
            Text(if (snapshot.startedAt.isEmpty()) "Descubrir servicios · 15 s" else "Repetir descubrimiento · 15 s")
        }
        if (snapshot.startedAt.isEmpty()) {
            Text("Sin búsqueda iniciada. No se consulta la red automáticamente.")
        } else {
            Text("${snapshot.services.size} servicios observados", style = MaterialTheme.typography.titleMedium)
            Text(if (snapshot.running) "Búsqueda en curso" else "Búsqueda finalizada")
            Text("Inicio: ${snapshot.startedAt}", style = MaterialTheme.typography.bodySmall)
            if (!snapshot.running && snapshot.services.isEmpty()) {
                Text("No se observaron servicios de los tipos consultados. Puede haber equipos silenciosos, aislamiento entre clientes o restricciones de Android. Esto no demuestra que la red esté vacía ni sea segura.")
            }
            if (snapshot.services.isNotEmpty()) {
                Text("DISTRIBUCIÓN POR SERVICIO", color = MaterialTheme.colorScheme.primary)
                val counts = LanInventory.TYPES.associateWith { type -> snapshot.services.count { it.type == type } }
                val max = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                counts.filterValues { it > 0 }.forEach { (type, count) ->
                    Column {
                        Text("$type · $count", style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.fillMaxWidth().height(7.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxWidth(count.toFloat() / max).height(7.dp).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
            snapshot.issues.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = !snapshot.running && !busy, modifier = Modifier.fillMaxWidth(),
                onClick = { save(snapshot.evidence()) }) { Text("Guardar inventario como evidencia") }
            snapshot.services.forEach { service ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(service.name, color = MaterialTheme.colorScheme.primary)
                        Text(service.type)
                        Text("IP anunciada: ${service.addresses.joinToString().ifEmpty { "No resuelta" }}")
                        Text("Puerto anunciado: ${service.port ?: "No resuelto"}")
                        Text(service.resolution, style = MaterialTheme.typography.bodySmall)
                        if (!service.available) Text("Dejó de anunciarse durante esta sesión")
                    }
                }
            }
        }
        Text("Los anuncios no autentican la identidad de un equipo. Esta búsqueda no abre sus puertos ni lee archivos o conversaciones.",
            style = MaterialTheme.typography.bodySmall)
    }
}
