package com.msarobotika.cyberdeck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable fun PhoneReviewPanel(snapshot:PhoneSnapshot,busy:Boolean,save:(DiagnosticResult)->Unit,settings:(String)->Unit){
 val context=LocalContext.current;val store=remember(context){PhoneReviewStore(context)};val scope=rememberCoroutineScope()
 var records by remember{mutableStateOf<List<PhoneReviewRecord>>(emptyList())}
 var loading by remember{mutableStateOf(false)}
 var loaded by remember{mutableStateOf(false)};var writing by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 var onlyPending by rememberSaveable{mutableStateOf(false)}
 val items=remember(snapshot){PhoneReviewPolicy.items(snapshot.device,snapshot.checks)};val byKey=records.associateBy{it.key}
 fun load(){if(loading)return;loading=true;scope.launch{try{records=withContext(Dispatchers.IO){store.read()};loaded=true;error=""}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudo leer el plan"}finally{loading=false}}}
 LaunchedEffect(Unit){load()}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("PLAN DE REVISIÓN",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("Observa. Comprueba. Anota.",style=MaterialTheme.typography.headlineSmall)
  Text("Sugerencias de reglas locales; sin generación de IA. Tus marcas registran una revisión manual, no que el móvil sea seguro.",style=MaterialTheme.typography.bodySmall)
  Text("Lectura usada: ${displayTime(snapshot.at)}. Actualiza Mi móvil para detectar cambios.",style=MaterialTheme.typography.bodySmall)
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  if(!loaded)OutlinedButton(onClick={load()},enabled=!writing && !loading){Text("Volver a cargar anotaciones")}
  else{
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("${items.size} observaciones · revisión manual",style=MaterialTheme.typography.titleMedium)
    Canvas(Modifier.fillMaxWidth().height(18.dp)){var x=0f;ReviewMark.entries.forEach{mark->val count=items.count{PhoneReviewPolicy.status(it,byKey[it.key])==mark};val width=if(items.isEmpty())0f else size.width*count/items.size;drawRect(reviewColor(mark),Offset(x,0f),Size(width,size.height));x+=width}}
    ReviewMark.entries.forEach{mark->Text("${items.count{PhoneReviewPolicy.status(it,byKey[it.key])==mark}} · ${mark.label}",color=reviewColor(mark),style=MaterialTheme.typography.bodySmall)}
   }}
   OutlinedButton(enabled=!busy && !writing,onClick={runCatching{phoneReviewDiagnostic(snapshot,records)}.onSuccess(save).onFailure{error=it.message?:"No se pudo preparar el informe"}}){Text("Guardar plan como evidencia")}
   FilterChip(onlyPending,onClick={onlyPending=!onlyPending},label={Text("Solo sin revisar o cambiadas")})
   val visible=items.filter{!onlyPending || PhoneReviewPolicy.status(it,byKey[it.key])!=ReviewMark.CURRENT}
   if(visible.isEmpty())Text("Sin observaciones pendientes con este filtro. No acredita ausencia de riesgos.")
   visible.forEach{item->ReviewItemCard(item,byKey[item.key],busy || writing,settings){reviewed,note->
    writing=true;error=""
    scope.launch{try{records=withContext(Dispatchers.IO){store.save(item,reviewed,note)}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudo guardar la anotación"}finally{writing=false}}
   }}
  }
  DeckDisclosure("Qué significa la marca","Prioriza estados para revisar y datos no disponibles según las reglas locales existentes. No usa reputación, CVE ni detección de intrusiones. Una marca es una declaración del usuario ligada al valor observado: si cambia el valor o su contexto, aparece como lectura cambiada. Ni una marca ni la huella certifican la comprobación o la procedencia. No se cambia ningún ajuste automáticamente. Las notas se guardan solo en CiberDeck; el informe abrevia las largas.")
 }
}
@Composable private fun ReviewItemCard(item:PhoneReviewItem,record:PhoneReviewRecord?,busy:Boolean,settings:(String)->Unit,save:(Boolean,String)->Unit){
 var expanded by rememberSaveable(item.key){mutableStateOf(false)}
 var marked by rememberSaveable(item.fingerprint){mutableStateOf(record?.takeIf{it.fingerprint==item.fingerprint}?.reviewed?:false)}
 var note by rememberSaveable(item.fingerprint){mutableStateOf(record?.note.orEmpty())}
 val status=PhoneReviewPolicy.status(item,record)
 Card(Modifier.fillMaxWidth().testTag("phone-review-${item.key}")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text(item.check.title,style=MaterialTheme.typography.titleMedium)
  Text(item.check.value,style=MaterialTheme.typography.bodyMedium)
  Text(when(item.check.tone){PhoneTone.REVIEW->"Regla local: revisar";PhoneTone.UNKNOWN->"Android no aporta un dato suficiente";PhoneTone.OK->"Condición informada por Android";PhoneTone.INFO->"Observación informativa"},style=MaterialTheme.typography.bodySmall,color=DeckCyan)
  Text(item.advice,style=MaterialTheme.typography.bodySmall)
  Text(status.label,color=reviewColor(status),style=MaterialTheme.typography.labelLarge)
  if(status==ReviewMark.STALE)Text("La anotación anterior corresponde a otra lectura. Revisa el dato actual antes de volver a marcarlo.",style=MaterialTheme.typography.bodySmall)
  item.check.settings?.let{action->TextButton(enabled=!busy,onClick={settings(action)}){Text("Abrir ajuste relacionado")}}
  TextButton(enabled=!busy,onClick={expanded=!expanded}){Text(if(expanded)"Cerrar anotación" else "Anotar mi comprobación")}
  if(expanded){
   Row{Checkbox(marked,onCheckedChange={marked=it},enabled=!busy,modifier=Modifier.testTag("phone-review-mark-${item.key}"));Text("He revisado esta observación",Modifier.weight(1f).padding(top=12.dp))}
   OutlinedTextField(note,{note=it.take(600)},label={Text("Nota de mi revisión")},supportingText={Text("${note.length}/600 · evita escribir claves o credenciales")},modifier=Modifier.fillMaxWidth(),enabled=!busy)
   Button(enabled=!busy,onClick={save(marked,note)},modifier=Modifier.testTag("phone-review-save-${item.key}")){Text("Guardar anotación")}
  }
  record?.let{Text("Última anotación: ${displayTime(it.at)}",style=MaterialTheme.typography.bodySmall)}
  DeckDisclosure("Dato y límites",item.check.explanation)
 }}
}
private fun reviewColor(mark:ReviewMark)=when(mark){ReviewMark.PENDING->DeckCyan;ReviewMark.CURRENT->DeckMint;ReviewMark.STALE->Color(0xFFFFD36B)}
