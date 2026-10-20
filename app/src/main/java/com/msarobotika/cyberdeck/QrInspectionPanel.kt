package com.msarobotika.cyberdeck

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable fun QrInspectionPanel(session:DeckSessionModel,save:(DiagnosticResult)->Unit) {
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 val result by session.qr.collectAsState()
 var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var reveal by remember(result){mutableStateOf(false)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){
  busy=true;error="";session.qr.value=null
  scope.launch{try{session.qr.value=runInterruptible(Dispatchers.IO){QrImageReader.read(context,uri)}}catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo leer un QR de esta imagen. Comprueba el formato, el tamaño (hasta 20 MiB) y la nitidez del código."}finally{busy=false}}
 }}
 Card(Modifier.fillMaxWidth().testTag("qr-panel")){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("QR · REVISAR ANTES DE ACTUAR",color=DeckViolet,style=MaterialTheme.typography.labelLarge)
   Text("¿Qué esconde ese código?",style=MaterialTheme.typography.titleLarge)
   Text("Elige una captura o imagen. Se lee un QR y se revisa su contenido localmente; no se abre ni ejecuta.",style=MaterialTheme.typography.bodySmall)
   Button(enabled=!busy,onClick={picker.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth().testTag("qr-pick")){Text("Leer QR de una imagen")}
   if(busy){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Decodificando la imagen…",style=MaterialTheme.typography.bodySmall)}
   if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
   result?.let{r->
    Text(r.type,style=MaterialTheme.typography.titleMedium,color=DeckCyan)
    Text("${displayTime(r.at)} · ${r.payload.length} caracteres",style=MaterialTheme.typography.bodySmall)
    Text(r.note,style=MaterialTheme.typography.bodySmall)
    r.link?.let{link->Text("Destino real: ${link.asciiHost}",style=MaterialTheme.typography.titleMedium);Text("${link.signals} señales sintácticas para revisar · no es un veredicto de fraude",style=MaterialTheme.typography.bodySmall);DeckDisclosure("Ver análisis del enlace",link.checks.joinToString("\n"){"${it.first}: ${it.second}"})}
    TextButton(onClick={reveal=!reveal},modifier=Modifier.testTag("qr-reveal")){Text(if(reveal)"Ocultar contenido" else "Mostrar contenido completo · puede contener claves")}
    if(reveal)SelectionContainer{Text(r.payload,style=MaterialTheme.typography.bodySmall)}
    OutlinedButton(enabled=!busy,onClick={save(r.diagnostic())},modifier=Modifier.testTag("qr-save")){Text("Guardar informe sin contenido completo")}
    TextButton(enabled=!busy,onClick={session.qr.value=null;reveal=false;error=""}){Text("Descartar lectura QR")}
   }
   Text("No conecta Wi-Fi, añade contactos, sigue enlaces ni lanza aplicaciones. Los QR pequeños, borrosos o con poco margen pueden no leerse.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
