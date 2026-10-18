package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PeerPanel(state: PeerState, receive: () -> Unit, stop: () -> Unit,
    decide: (Boolean) -> Unit, copy: (String) -> Unit,
    selectFile: (String) -> Unit, sendReport: (String) -> Unit) {
    var invitation by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MÓVIL A MÓVIL · SESIÓN LOCAL", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Ambos móviles necesitan CiberDeck y acceso entre sí en una red local con IPv4. Puedes usar un punto de acceso propio. No se envían archivos a un servidor externo.")
        Text(state.status)
        if (state.active) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = stop) { Text("Cerrar sesión local") }
        } else Button(onClick = receive) { Text("Crear sesión de recepción") }
        if (state.invitation.isNotEmpty()) {
            Text("Comparte esta invitación solo con la persona autorizada. Caduca en 2 minutos y permite un archivo aceptado por ti.")
            OutlinedTextField(state.invitation, {}, readOnly = true, label = { Text("Invitación temporal") }, maxLines = 4,
                modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { copy(state.invitation) }) { Text("Copiar invitación") }
        }
        state.offer?.let { offer ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ARCHIVO PROPUESTO", color = MaterialTheme.colorScheme.primary)
                    Text("${offer.name}\n${offer.size} bytes\nSHA-256 ${offer.sha256}")
                    Text("La invitación no identifica por sí sola a una persona. Confirma quién envía este archivo.")
                    Button(onClick = { decide(true) }) { Text("Aceptar archivo") }
                    OutlinedButton(onClick = { decide(false) }) { Text("Rechazar archivo") }
                }
            }
        }
        HorizontalDivider()
        Text("ENVIAR A OTRO MÓVIL", color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(invitation, { invitation = it.take(4096) }, label = { Text("Pega la invitación del receptor") }, maxLines = 4,
            enabled = !state.active, modifier = Modifier.fillMaxWidth())
        Button(enabled = !state.active && invitation.isNotBlank(), onClick = { selectFile(invitation) }) { Text("Elegir archivo y enviar") }
        OutlinedButton(enabled = !state.active && invitation.isNotBlank(), onClick = { sendReport(invitation) }) { Text("Enviar informe de evidencias") }
        if (state.result.isNotBlank()) Card(Modifier.fillMaxWidth()) { Text(state.result, Modifier.padding(14.dp)) }
        Text("TLS con certificado fijado por la invitación y token aleatorio. Hasta 16 MiB por archivo. El receptor confirma y verifica SHA-256 antes de guardar. Mantén la app en primer plano: al salir, las sesiones se cierran.",
            style = MaterialTheme.typography.bodySmall)
    }
}
