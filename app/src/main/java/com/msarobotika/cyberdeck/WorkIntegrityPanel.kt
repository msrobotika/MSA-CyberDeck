package com.msarobotika.cyberdeck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

@Composable fun WorkIntegrityPanel(work:AuditWork,busy:Boolean){
 val context=LocalContext.current
 val store=remember(context){EvidenceStore(context)}
 val scope=rememberCoroutineScope()
 var result by remember(work){mutableStateOf<WorkVerification?>(null)}
 var running by remember(work){mutableStateOf(false)}
 var error by remember(work){mutableStateOf("")}
 val stop=remember(work){AtomicBoolean(false)}
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 DisposableEffect(work,owner){
  val listener=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP)stop.set(true)}
  owner.lifecycle.addObserver(listener)
  onDispose{stop.set(true);owner.lifecycle.removeObserver(listener)}
 }
 Card(Modifier.fillMaxWidth()){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("INTEGRIDAD DEL TRABAJO",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
   Text("${work.evidence.size} referencias fijadas",style=MaterialTheme.typography.titleMedium)
   Text("Lee las copias privadas y compara su tamaño y SHA-256. No evalúa si el contenido es seguro.",style=MaterialTheme.typography.bodySmall)
   if(running){
    LinearProgressIndicator(Modifier.fillMaxWidth())
    Text("Verificando archivos…",style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={stop.set(true)}){Text("Detener verificación")}
   }else OutlinedButton(enabled=!busy && work.evidence.isNotEmpty(),onClick={
    running=true;error="";result=null;stop.set(false)
    scope.launch{
     try{val checked=withContext(Dispatchers.IO){verifyWorkCopies(work,store){stop.get()}};result=checked}
     catch(e:CancellationException){error="Verificación detenida; no se presenta un resultado parcial"}
     catch(e:Exception){error=e.message?:"No se pudo verificar el trabajo"}
     finally{running=false}
    }
   }){Text("Comprobar copias del trabajo")}
   if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
   result?.let{verified->
    Text("Comprobación: ${displayTime(verified.at)}",style=MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
     Canvas(Modifier.size(90.dp)){
      var angle=-90f;val stroke=16f
      WorkCopyState.entries.forEach{state->val count=verified.count(state);if(count>0){val sweep=count*360f/verified.checks.size;drawArc(copyColor(state),angle,sweep,false,Offset(stroke/2,stroke/2),Size(size.width-stroke,size.height-stroke),style=Stroke(stroke));angle+=sweep}}
     }
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
      WorkCopyState.entries.forEach{state->Text("${verified.count(state)} · ${state.label}",color=copyColor(state),style=MaterialTheme.typography.bodySmall)}
     }
    }
    Text("Integridad de copias; no análisis de malware.",style=MaterialTheme.typography.bodySmall)
    verified.checks.filter{it.state!=WorkCopyState.VERIFIED}.take(20).forEach{check->Text("${check.name}: ${check.state.label}",style=MaterialTheme.typography.bodySmall)}
    if(verified.checks.count{it.state!=WorkCopyState.VERIFIED}>20)Text("Se muestran hasta 20 incidencias; el informe revisa todas las referencias.",style=MaterialTheme.typography.bodySmall)
   }
   DeckDisclosure("Método y límites","Detener o salir descarta el resultado y termina tras el archivo en curso. Puede tardar con archivos grandes. El resultado corresponde a la fecha mostrada: repite la comprobación después de cambiar o borrar copias. Una copia verificada no acredita su procedencia ni descarta malware.")
  }
 }
}
private fun copyColor(state:WorkCopyState)=when(state){WorkCopyState.VERIFIED->DeckMint;WorkCopyState.UNVERIFIED->Color(0xFFFF9FAB);WorkCopyState.INDEX_MISMATCH->Color(0xFFFFD36B)}
