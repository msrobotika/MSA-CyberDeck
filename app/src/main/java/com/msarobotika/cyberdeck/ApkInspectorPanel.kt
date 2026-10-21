package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun ApkInspectorPanel(snapshot:ApkSnapshot?,busy:Boolean,pick:()->Unit,self:()->Unit,save:(DiagnosticResult)->Unit) {
 var section by rememberSaveable{mutableStateOf("Resumen")}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("INSPECTOR APK",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("Revisa antes de instalar",style=MaterialTheme.typography.headlineSmall)
  Text("Lee el manifiesto, identifica los certificados y calcula SHA-256. Sin instalar, ejecutar ni enviar el archivo.")
  Button(enabled=!busy,onClick=pick){Text("Elegir APK")}
  OutlinedButton(enabled=!busy,onClick=self){Text("Inspeccionar CiberDeck instalada")}
  Text("APK único · hasta 128 MiB · no XAPK/APKS. La copia temporal se elimina al terminar.",style=MaterialTheme.typography.bodySmall)
  snapshot?.let{r->
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text(r.label,style=MaterialTheme.typography.titleLarge);Text(r.packageName,color=DeckCyan);Text(r.version)
    Text("${displayBytes(r.fingerprint.bytes)} · ${displayTime(r.at)}",style=MaterialTheme.typography.bodySmall)
    Button(enabled=!busy,onClick={save(r.diagnostic())}){Text("Guardar informe APK")}
   }}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Resumen","Permisos","Firma").forEach{label->FilterChip(selected=section==label,onClick={section=label},label={Text(label)},modifier=Modifier.weight(1f))}}
   when(section){
    "Resumen"->{
     ApkInfo("API mínima / objetivo","${r.minSdk} / ${r.targetSdk}. No determina si tiene vulnerabilidades.")
     ApkInfo("Opciones de compilación","Depurable: ${r.debug}. Solo pruebas: ${r.testOnly}. Una app depurable requiere revisión, no implica malware.")
     ApkInfo("Tráfico HTTP","Bandera general de texto claro: ${r.cleartext}. No se analiza la configuración XML ni el tráfico real.")
     DeckDisclosure("Componentes exportados (${r.exportedCount})",r.exported.joinToString("\n"){"${it.first}: ${it.second}"}.ifEmpty{"Ninguno declarado"}+"\nHasta 40 registros. Exportado no implica una vulnerabilidad; puede haber controles de permisos o dentro del código.")
    }
    "Permisos"->{Text("${r.permissions.size} permisos declarados · máximo 500. No son permisos concedidos ni evidencias de uso.");r.permissions.forEach{p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(p.name.removePrefix("android.permission."),color=DeckCyan);Text(p.kind,style=MaterialTheme.typography.bodySmall)}}}}
    "Firma"->{ApkInfo("SHA-256 del archivo",r.fingerprint.sha256);ApkInfo("Certificados actuales SHA-256",r.signers.joinToString("\n").ifEmpty{"Android no devolvió los certificados"});Text("Compara estas huellas con una fuente independiente de confianza. Identificar una firma no acredita al editor ni descarta malware.")}
   }
  }
 }
}

@Composable private fun ApkInfo(title:String,body:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,style=MaterialTheme.typography.titleMedium,color=DeckCyan);Text(body,style=MaterialTheme.typography.bodySmall)}}}
