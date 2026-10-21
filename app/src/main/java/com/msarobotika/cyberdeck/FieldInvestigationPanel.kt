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
 var showUnchanged by rememberSaveable { mutableStateOf(false) }
 var running by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 val candidates=remember(entries,work,workOnly){entries.asReversed().filter{e->e.name.startsWith("diagnostico-") && e.bytes<=65536 && (!workOnly || work?.evidence?.any{AuditWorkPolicy.matches(it,e)}==true)}}
 var result by remember(candidates){mutableStateOf<FieldInvestigation?>(null)}
 suspend fun refresh(){
  running=true;error="";result=null
  try{result=withContext(Dispatchers.IO){investigateSavedReadings(candidates){ensureActive();EvidenceStore(context).diagnostic(it)}}}
  catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo completar la revisión. Las copias originales se conservan."}finally{running=false}
 }
 LaunchedEffect(candidates){if(candidates.isNotEmpty())refresh()}
 Column(Modifier.fillMaxWidth().testTag("investigation-panel"),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("TUS CAMBIOS",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("¿Qué ha cambiado?",style=MaterialTheme.typography.headlineSmall)
  Text("Compara tus revisiones guardadas. No inicia pruebas de red ni nuevas mediciones.",style=MaterialTheme.typography.bodySmall)
  if(work!=null)FilterChip(workOnly,onClick={workOnly=!workOnly},enabled=!running,label={Text("Solo trabajo activo")})
  Button(enabled=!busy && !running && candidates.isNotEmpty(),modifier=Modifier.fillMaxWidth().testTag("investigation-run"),onClick={
   scope.launch{refresh()}
  }){Text(if(running)"Revisando tus lecturas…" else "Volver a buscar cambios")}
  if(running)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(candidates.isEmpty()){
   Text("Empieza guardando dos lecturas del mismo tipo y destino. Por ejemplo, revisa tu móvil ahora y repite la revisión después.")
   OutlinedButton(onClick={navigate("Móvil")}){Text("Revisar mi móvil")}
  }
  if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
  result?.let{r->
   Card(Modifier.fillMaxWidth().testTag("investigation-result")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("RESUMEN",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text("${r.changed} campos diferentes",style=MaterialTheme.typography.headlineSmall)
    Text("${r.pairs.size} comparaciones · ${displayTime(r.checkedAt)}",style=MaterialTheme.typography.bodySmall)
    Text("Son diferencias entre revisiones; no confirman ataques.",style=MaterialTheme.typography.bodySmall)
    DeckDisclosure("Fuentes y comprobaciones","${r.verified} copias compatibles, ${r.unreadable} no legibles y ${r.unpaired} grupos que necesitan otra lectura. El orden es el de guardado, no una cronología acreditada.")
   }}
   if(r.pairs.isNotEmpty() && r.changed==0)Text("No hay diferencias en los campos comparados. Esto no acredita que el dispositivo sea seguro.")
   if(r.pairs.isEmpty())Text("Todavía no hay parejas del mismo tipo y destino exactos. Guarda otra lectura y vuelve a investigar.")
   OutlinedButton(enabled=!busy && !running,onClick={save(r.diagnostic())},modifier=Modifier.fillMaxWidth().testTag("investigation-save")){Text("Guardar resumen")}
   if(r.pairs.any{p->p.rows.all{it.change==FieldChange.SAME}})FilterChip(showUnchanged,onClick={showUnchanged=!showUnchanged},label={Text("Mostrar también los que no cambiaron")})
   r.pairs.filter{p->showUnchanged || p.rows.any{it.change!=FieldChange.SAME}}.forEach{p->
    var expanded by remember(p.first.id,p.second.id){mutableStateOf(false)}
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text(p.baseline.target,style=MaterialTheme.typography.titleMedium)
     Text("${p.baseline.kind} · ${p.rows.count{it.change!=FieldChange.SAME}} de ${p.rows.size} campos diferentes",style=MaterialTheme.typography.bodySmall)
     Canvas(Modifier.fillMaxWidth().height(8.dp)){var x=0f;FieldChange.entries.forEach{change->val width=if(p.rows.isEmpty())0f else size.width*p.count(change)/p.rows.size;drawRect(when(change){FieldChange.SAME->DeckMint;FieldChange.CHANGED->DeckAmber;FieldChange.ADDED->DeckCyan;FieldChange.REMOVED->DeckViolet},androidx.compose.ui.geometry.Offset(x,0f),androidx.compose.ui.geometry.Size(width,size.height));x+=width}}
     Text("${p.count(FieldChange.CHANGED)} modificados · ${p.count(FieldChange.ADDED)} añadidos · ${p.count(FieldChange.REMOVED)} ausentes · ${p.count(FieldChange.SAME)} iguales",style=MaterialTheme.typography.bodySmall)
     TextButton(onClick={expanded=!expanded}){Text(if(expanded) "Ocultar cambios" else "Ver qué cambió")}
     if(expanded){
      Text("Primera revisión: ${displayTime(p.baseline.checkedAt)}\nSegunda revisión: ${displayTime(p.current.checkedAt)}",style=MaterialTheme.typography.bodySmall)
      val changes=p.rows.filter{it.change!=FieldChange.SAME}
      changes.take(12).forEach{row->DeckDisclosure("${row.key} · ${row.change.label}","Antes: ${row.before.joinToString(" | ").take(240).ifBlank{"Sin valor"}}\nDespués: ${row.after.joinToString(" | ").take(240).ifBlank{"Sin valor"}}")}
      if(changes.size>12)Text("${changes.size-12} cambios más en la comparación completa.",style=MaterialTheme.typography.bodySmall)
      OutlinedButton(onClick={compare(p)}){Text("Ver comparación completa")}
     }
    }}
   }
   DeckDisclosure("Método y límites","Se revisan hasta 100 copias recientes del filtro. Cada archivo se verifica por tamaño y SHA-256. Se omiten informes derivados y entradas incompatibles. Se comparan las dos últimas copias guardadas de cada tipo y destino exactos, sin inferir identidad física. Las barras cuentan campos textuales, no riesgo. Abrir una pareja permite verificar de nuevo y consultar los valores originales. El informe de misión guarda referencias y huellas; no copia los valores originales. Una copia no legible puede deberse a formato o integridad; no acredita manipulación.")
  }
 }
}
