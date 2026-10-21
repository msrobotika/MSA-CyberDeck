package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun LinkInspectionPanel(save:(DiagnosticResult)->Unit) {
    var input by rememberSaveable{mutableStateOf("")}
    var result by remember{mutableStateOf<LinkObservation?>(null)}
    var error by remember{mutableStateOf("")}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("INSPECTOR DE ENLACES",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
        Text("Comprueba antes de abrir",style=MaterialTheme.typography.headlineSmall)
        Text("Analiza dominio real, @ engañosa, HTTP, Unicode, IP y puertos. Todo se calcula aquí: no abre el sitio, sigue redirecciones ni envía el enlace a ningún servicio.")
        OutlinedTextField(input,{input=it.take(4096);result=null;error=""},label={Text("Enlace HTTP o HTTPS")},modifier=Modifier.fillMaxWidth(),maxLines=4)
        Button(onClick={runCatching{LinkInspection.inspect(input)}.onSuccess{result=it;error=""}.onFailure{error=it.message?:"No se pudo analizar"}}){Text("Analizar sin abrir")}
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
        result?.let{r->
            Text("${r.signals} señales para revisar",style=MaterialTheme.typography.titleLarge,color=if(r.signals>0)MaterialTheme.colorScheme.tertiary else DeckCyan)
            Text("No es un veredicto de fraude ni una consulta de reputación.",style=MaterialTheme.typography.bodySmall)
            r.checks.forEach{(k,v)->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(k,color=DeckCyan);Text(v)}}}
            Button(onClick={save(r.diagnostic())}){Text("Guardar análisis sin URL completa")}
        }
    }
}
