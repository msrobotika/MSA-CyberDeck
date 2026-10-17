package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap

@Composable fun BrowserReceiverPanel(state: BrowserState, start: () -> Unit, stop: () -> Unit,
    decide: (Boolean) -> Unit, copy: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("RECIBIR SIN INSTALAR APP", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        if (!state.active) Text("Usa este móvil como receptor principal. En el otro, abre el QR con la cámara y elige un archivo en su navegador. Ambos deben estar en la misma Wi-Fi.")
        Text(state.status)
        if (state.active) {
            Button(onClick = stop) { Text("Cerrar recepción de navegador") }
        } else Button(onClick = { confirm = true }) { Text("Preparar recepción desde navegador") }
        if (state.url.isNotBlank()) {
            Text("Mantén CyberDeck abierto en este móvil. Si sales a Telegram o bloqueas la pantalla, el enlace se cierra. Escanea con la cámara del otro móvil y abre en su navegador.")
            val qr = remember(state.url) {
                Bitmap.createBitmap(receiverQrPixels(state.url), 512, 512, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
            Image(qr, contentDescription = "QR de recepción temporal", modifier = Modifier.size(240.dp))
            OutlinedTextField(state.url, {}, readOnly = true, label = { Text("Dirección temporal · HTTP sin cifrar") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { copy(state.url) }) { Text("Copiar dirección · mantener CyberDeck abierto") }
            Text("Es una dirección http:// local: no funciona usando datos móviles ni fuera de esta red. Crea un enlace nuevo si la sesión anterior se cerró.", style = MaterialTheme.typography.bodySmall)
        }
        state.offer?.let { offer ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text("${offer.name}\n${offer.size} bytes\n¿Guardar este archivo en tus evidencias?")
                Button(onClick = { decide(true) }) { Text("Guardar archivo del navegador") }
                TextButton(onClick = { decide(false) }) { Text("Rechazar envío del navegador") }
            } }
        }
        Text("HTTP local sin cifrar, solo para una red propia de confianza. Un archivo de hasta 16 MiB por sesión de 2 minutos. Se cierra al salir de la app. El SHA-256 se calcula al recibir; no autentica al remitente. Para archivos confidenciales usa Sesiones TLS.", style = MaterialTheme.typography.bodySmall)
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Abrir receptor HTTP local") },
        text = { Text("El archivo y la dirección temporal viajarán sin cifrar por la red local. Usa una Wi-Fi propia y de confianza. El otro móvil seleccionará el archivo y tú confirmarás su guardado.") },
        confirmButton = { TextButton(onClick = { confirm = false; start() }) { Text("Activar en mi red de confianza") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
