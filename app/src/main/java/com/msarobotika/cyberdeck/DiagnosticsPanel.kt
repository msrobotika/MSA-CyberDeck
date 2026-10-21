package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable fun DiagnosticsPanel(
    mode: String, busy: Boolean, result: DiagnosticResult?,
    onRun: (String, String, () -> DiagnosticResult) -> Unit,
    onSave: (DiagnosticResult) -> Unit
) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    val context=androidx.compose.ui.platform.LocalContext.current
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("443") }
    var url by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        result?.let { observation ->
            DiagnosticResultCard(observation,busy,onSave){text->
                val clip=android.content.ClipData.newPlainText("CiberDeck · resumen",text)
                if(android.os.Build.VERSION.SDK_INT>=33)clip.description.extras=android.os.PersistableBundle().apply{putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE,true)}
                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
            }
            HorizontalDivider()
            Text("Otra comprobación",style=MaterialTheme.typography.titleMedium)
        }
        if (mode == "Web") {
            Text("CERTIFICADO · CABECERAS · REDIRECCIÓN", color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(url, { url = it }, label = { Text("URL http:// o https://") },
                placeholder = { Text("https://tu-dominio.example/") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Text("Se consulta HEAD sobre esta URL. No se siguen redirecciones. TLS conserva la validación del sistema.",
                style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy && url.isNotBlank(), onClick = {
                focus.clearFocus(); onRun("web", url) { Diagnostics.web(url) }
            }) { Text("Revisar URL") }
        } else {
            Text("DIAGNÓSTICO DE RED", color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(host, { host = it }, label = { Text("Dominio o IP") },
                placeholder = { Text("servidor de tu laboratorio") }, singleLine = true,
                enabled = !busy, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && host.isNotBlank(), onClick = {
                focus.clearFocus(); onRun("dns", host) { Diagnostics.dns(host) }
            }) { Text("Resolver DNS / IP") }
            OutlinedTextField(port, { port = it }, label = { Text("Puerto TCP") },
                singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && host.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535,
                onClick = { focus.clearFocus(); onRun("tcp", "$host:$port") { Diagnostics.tcp(host, port.toInt()) } }) {
                Text("Comprobar puerto")
            }
            Text("Consulta solo equipos de tu ámbito autorizado. Una conexión aceptada no identifica el servicio.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
