package com.msarobotika.cyberdeck

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun evidenceMatches(entry: Evidence, query: String, filter: String): Boolean {
    val category = when {
        entry.name.startsWith("diagnostico-nfc-") -> "NFC"
        entry.name.startsWith("diagnostico-ble-") -> "BLE"
        entry.name.startsWith("diagnostico-") -> "Diagnósticos"
        else -> "Archivos"
    }
    return (filter == "Todas" || category == filter) &&
        listOf(entry.name, entry.sha256, entry.importedAt).any { it.contains(query.trim(), ignoreCase = true) }
}

@Composable fun EvidenceControls(query: String, filter: String, newest: Boolean, shown: Int, total: Int,
    queryChanged: (String) -> Unit, filterChanged: (String) -> Unit, orderChanged: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ARCHIVO DE EVIDENCIAS", color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(value = query, onValueChange = queryChanged, label = { Text("Buscar por nombre, fecha o hash") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "NFC", "BLE", "Diagnósticos", "Archivos").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filterChanged(label) }, label = { Text(label) })
            }
        }
        Text("$shown de $total evidencias · filtro por nombre de archivo")
        TextButton(onClick = orderChanged) { Text(if (newest) "Más recientes primero" else "Más antiguas primero") }
    }
}

@Composable fun DiagnosticDialog(title: String, result: DiagnosticResult, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(result.checkedAt, style = MaterialTheme.typography.bodySmall)
            Text(result.summary)
            result.details.forEach { (key, value) ->
                Text(key, color = MaterialTheme.colorScheme.primary)
                Text(value)
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar informe") } })
}
