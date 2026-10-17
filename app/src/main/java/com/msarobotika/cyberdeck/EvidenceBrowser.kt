package com.msarobotika.cyberdeck

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.saveable.rememberSaveable

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
        val focus=LocalFocusManager.current
        OutlinedTextField(value = query, onValueChange = queryChanged, label = { Text("Buscar por nombre, fecha o hash") },
            singleLine = true, keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={focus.clearFocus()}),modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "NFC", "BLE", "Diagnósticos", "Archivos").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filterChanged(label) }, label = { Text(label) })
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("$shown de $total copias",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(query.isNotBlank() || filter!="Todas")TextButton(onClick={queryChanged("");filterChanged("Todas")}){Text("Ver todas")}
        }
        TextButton(onClick = orderChanged) { Text(if (newest) "Orden: recientes primero" else "Orden: antiguas primero") }
    }
}

@Composable fun EvidenceFileCard(entry:Evidence,busy:Boolean,open:()->Unit,export:()->Unit,delete:()->Unit,
    report:()->Unit,reference:()->Unit,referenceLabel:String,referenceEnabled:Boolean) {
    var details by rememberSaveable(entry.id){mutableStateOf(false)}
    var menu by remember {mutableStateOf(false)}
    val kind=when {
        entry.name.startsWith("diagnostico-nfc-")->"NFC"
        entry.name.startsWith("diagnostico-")->"DIAGNÓSTICO"
        entry.name.substringAfterLast('.').lowercase() in listOf("mp3","m4a","wav","ogg","opus")->"AUDIO"
        entry.name.substringAfterLast('.').lowercase() in listOf("png","jpg","jpeg","webp")->"IMAGEN"
        else->"ARCHIVO"
    }
    Card(Modifier.fillMaxWidth().testTag("evidence-${entry.id}")) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text(kind,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=DeckViolet)
                Box {
                    TextButton(enabled=!busy,onClick={menu=true},modifier=Modifier.testTag("evidence-menu-${entry.id}")){Text("Más")}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text("Borrar esta copia",color=MaterialTheme.colorScheme.error)},onClick={menu=false;delete()})
                    }
                }
            }
            Text(entry.name,style=MaterialTheme.typography.titleMedium)
            Text("${displayBytes(entry.bytes)} · ${displayTime(entry.importedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(enabled=!busy,onClick=open,modifier=Modifier.weight(1f)){Text("Abrir archivo")}
                OutlinedButton(enabled=!busy,onClick=export,modifier=Modifier.weight(1f)){Text("Guardar en el móvil")}
            }
            if(entry.name.startsWith("diagnostico-"))OutlinedButton(enabled=!busy,onClick=report,modifier=Modifier.fillMaxWidth()){Text("Abrir informe guardado")}
            if(entry.name.startsWith("diagnostico-nfc-"))TextButton(enabled=!busy && referenceEnabled,onClick=reference){Text(referenceLabel)}
            TextButton(onClick={details=!details}){Text(if(details) "Ocultar detalles" else "Detalles y SHA-256")}
            if(details)SelectionContainer {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Identificador: ${entry.id}",style=MaterialTheme.typography.bodySmall)
                Text("Fecha de importación: ${entry.importedAt}",style=MaterialTheme.typography.bodySmall)
                Text("SHA-256\n${entry.sha256}",style=MaterialTheme.typography.bodySmall)
                Text("El hash se comprueba al abrir o exportar. La fecha procede del móvil.",style=MaterialTheme.typography.bodySmall)
            }}
        }
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
