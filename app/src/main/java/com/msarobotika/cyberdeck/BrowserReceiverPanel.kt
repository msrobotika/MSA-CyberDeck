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
    decide: (Boolean) -> Unit, copy: (String) -> Unit,notificationsEnabled:Boolean=true,requestNotifications:()->Unit={}) {
    var confirm by remember { mutableStateOf(false) }
    var showAddress by remember { mutableStateOf(false) }
    var seconds by remember(state.expiresAt){mutableStateOf(0L)}
    LaunchedEffect(state.expiresAt,state.active) {
        while(state.active && state.expiresAt>0) {
            seconds=((state.expiresAt-System.currentTimeMillis()+999)/1000).coerceAtLeast(0)
            if(seconds==0L)break
            kotlinx.coroutines.delay(1000)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DESDE OTRO NAVEGADOR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (!state.active) Text("Trae un archivo sin instalar CiberDeck en el otro móvil. Ambos necesitan la misma Wi-Fi.",style=MaterialTheme.typography.bodyMedium)
        Text(state.status)
        if(state.active && state.expiresAt>0)Text("Enlace activo · %02d:%02d".format(seconds/60,seconds%60),style=MaterialTheme.typography.titleMedium,color=DeckMint)
        if (state.active) {
            Button(onClick = stop) { Text("Cerrar recepción de navegador") }
        } else Button(onClick = { confirm = true }) { Text("Preparar recepción desde navegador") }
        state.offer?.let { offer ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text("${offer.name}\n${offer.size} bytes\n¿Guardar este archivo en tus evidencias?")
                Button(onClick = { decide(true) }) { Text("Guardar archivo del navegador") }
                TextButton(onClick = { decide(false) }) { Text("Rechazar envío del navegador") }
            } }
        }
        if (state.url.isNotBlank() && state.offer==null) {
            Text("Escanea con la cámara del otro móvil. La sesión sigue activa si cambias de app.",style=MaterialTheme.typography.bodyMedium)
            val qr = remember(state.url) {
                Bitmap.createBitmap(receiverQrPixels(state.url), 512, 512, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
            Image(qr, contentDescription = "QR de recepción temporal", modifier = Modifier.size(240.dp))
            OutlinedButton(onClick = { copy(state.url) },modifier=Modifier.fillMaxWidth()) { Text("Copiar dirección temporal") }
            TextButton(onClick={showAddress=!showAddress}){Text(if(showAddress)"Ocultar dirección" else "Ver dirección")}
            if(showAddress)OutlinedTextField(state.url, {}, readOnly = true, label = { Text("Dirección temporal · HTTP sin cifrar") }, modifier = Modifier.fillMaxWidth())
        }
        if(!notificationsEnabled)OutlinedButton(onClick=requestNotifications){Text("Activar avisos de recepción")}
        DeckDisclosure("Conexión y privacidad","HTTP local sin cifrar: usa una red propia de confianza. Máximo 16 MiB y 2 minutos. Android mantiene la sesión como tarea temporal al cambiar de app. Puedes detenerla aquí o desde su notificación; cerrar CiberDeck desde recientes también la detiene. Activa avisos para ver cuándo debes aceptar un archivo. El hash se calcula al recibir y no autentica al remitente. Para documentos confidenciales usa Sesiones TLS. No funciona fuera de esta red ni se reinicia sola.")
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Abrir receptor HTTP local") },
        text = { Text("El archivo y la dirección temporal viajarán sin cifrar por la red local. Usa una Wi-Fi propia y de confianza. El otro móvil seleccionará el archivo y tú confirmarás su guardado.") },
        confirmButton = { TextButton(onClick = { confirm = false; start() }) { Text("Activar en mi red de confianza") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
