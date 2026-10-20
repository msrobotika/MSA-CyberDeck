package com.msarobotika.cyberdeck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable fun FieldInvestigationPanel(entries:List<Evidence>,work:AuditWork?,busy:Boolean,save:(DiagnosticResult)->Unit,compare:(SavedComparison)->Unit,navigate:(String)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var workOnly by rememberSaveable(work?.id){mutableStateOf(work!=null)}
 var running by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 val candidates=remember(entries,work,workOnly){entries.asReversed().filter{e->e.name.startsWith("diagnostico-") && e.bytes<=65536 && (!workOnly || work?.evidence?.any{AuditWorkPolicy.matches(it,e)}==true)}}
 var result by remember(candidates){mutableStateOf<FieldInvestigation?>(null)}
 Column(Modifier.fillMaxWidth().testTag("investigation-panel"),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("MISIÓN · QUÉ CAMBIÓ",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("De las lecturas a las pruebas.",style=MaterialTheme.typography.headlineSmall)
  Text("Revisa tus copias guardadas, reúne parejas compatibles y abre cada cambio con sus fuentes. Todo se calcula aquí, sin IA ni nuevas pruebas de red.")
  if(work!=null)FilterChip(workOnly,onClick={workOnly=!workOnly},enabled=!running,label={Text("Solo trabajo activo")})
  Text("1 · Verificar copias   2 · Reunir parejas   3 · Revisar cambios",style=MaterialTheme.typography.bodySmall)
  Button(enabled=!busy && !running && candidates.isNotEmpty(),modifier=Modifier.fillMaxWidth().testTag("investigation-run"),onClick={
   running=true;error="";result=null
   scope.launch{try{val read=withContext(Dispatchers.IO){investigateSavedReadings(candidates){ensureActive();EvidenceStore(context).diagnostic(it)}};result=read}
    catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo completar la revisión. Las copias originales se conservan."}finally{running=false}}
  }){Text(if(running)"Verificando fuentes…" else "Investigar lecturas guardadas")}
  if(running)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(candidates.isEmpty()){
   Text("Empieza guardando dos lecturas del mismo tipo y destino. Por ejemplo, revisa tu móvil ahora y repite la revisión después.")
   OutlinedButton(onClick={navigate("Móvil")}){Text("Revisar mi móvil")}
  }
  if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
  result?.let{r->
   Card(Modifier.fillMaxWidth().testTag("investigation-result")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("FUENTES REVISADAS",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text("${r.changed} campos diferentes",style=MaterialTheme.typography.headlineSmall)
    Text("${r.pairs.size} parejas · ${r.verified} copias compatibles · ${r.unreadable} no legibles")
    Text("${r.unpaired} grupos necesitan otra lectura · ${displayTime(r.checkedAt)}",style=MaterialTheme.typography.bodySmall)
    Text("Un cambio no demuestra un ataque. El orden mostrado es el de guardado, no una cronología acreditada.",style=MaterialTheme.typography.bodySmall)
   }}
   if(r.pairs.isEmpty())Text("Todavía no hay parejas del mismo tipo y destino exactos. Guarda otra lectura y vuelve a investigar.")
   r.pairs.forEach{p->
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text(p.baseline.target,style=MaterialTheme.typography.titleMedium)
     Text("${p.baseline.kind} · ${p.rows.count{it.change!=FieldChange.SAME}} de ${p.rows.size} campos diferentes",style=MaterialTheme.typography.bodySmall)
     Canvas(Modifier.fillMaxWidth().height(8.dp)){var x=0f;FieldChange.entries.forEach{change->val width=if(p.rows.isEmpty())0f else size.width*p.count(change)/p.rows.size;drawRect(when(change){FieldChange.SAME->DeckMint;FieldChange.CHANGED->DeckAmber;FieldChange.ADDED->DeckCyan;FieldChange.REMOVED->DeckViolet},androidx.compose.ui.geometry.Offset(x,0f),androidx.compose.ui.geometry.Size(width,size.height));x+=width}}
     Text("${p.count(FieldChange.CHANGED)} modificados · ${p.count(FieldChange.ADDED)} añadidos · ${p.count(FieldChange.REMOVED)} ausentes · ${p.count(FieldChange.SAME)} iguales",style=MaterialTheme.typography.bodySmall)
     Text("A · ${displayTime(p.baseline.checkedAt)}\nB · ${displayTime(p.current.checkedAt)}",style=MaterialTheme.typography.bodySmall)
     OutlinedButton(onClick={compare(p)}){Text("Abrir pareja y verificar valores")}
    }}
   }
   OutlinedButton(enabled=!busy && !running,onClick={save(r.diagnostic())},modifier=Modifier.testTag("investigation-save")){Text("Guardar informe de la misión")}
   DeckDisclosure("Método y límites","Se revisan hasta 100 copias recientes del filtro. Cada archivo se verifica por tamaño y SHA-256. Se omiten informes derivados y entradas incompatibles. Se comparan las dos últimas copias guardadas de cada tipo y destino exactos, sin inferir identidad física. Las barras cuentan campos textuales, no riesgo. Abrir una pareja permite verificar de nuevo y consultar los valores originales. El informe de misión guarda referencias y huellas; no copia los valores originales. Una copia no legible puede deberse a formato o integridad; no acredita manipulación.")
  }
 }
}
