package com.msarobotika.cyberdeck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** Keep only evidence IDs across navigation/recreation; always reverify files on compare. */
class ComparisonSelectionModel(private val saved:SavedStateHandle):ViewModel(){
 val first=saved.getStateFlow<String?>("comparison.first",null)
 val second=saved.getStateFlow<String?>("comparison.second",null)
 fun chooseFirst(id:String?){saved["comparison.first"]=id}
 fun chooseSecond(id:String?){saved["comparison.second"]=id}
}

@Composable fun ComparisonPanel(selection:ComparisonSelectionModel,entries:List<Evidence>,work:AuditWork?,busy:Boolean,save:(DiagnosticResult)->Unit){
 val context=LocalContext.current
 val store=remember(context){EvidenceStore(context)}
 val scope=rememberCoroutineScope()
 val first by selection.first.collectAsState();val second by selection.second.collectAsState()
 var workOnly by rememberSaveable(work?.id){mutableStateOf(work!=null)}
 var query by rememberSaveable{mutableStateOf("")};var changesOnly by rememberSaveable{mutableStateOf(true)}
 var running by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 var result by remember{mutableStateOf<SavedComparison?>(null)}
 var suggesting by remember{mutableStateOf(false)}
 var suggestion by remember{mutableStateOf<ComparisonSuggestion?>(null)}
 var manual by rememberSaveable{mutableStateOf(true)}
 val candidates=entries.asReversed().filter{it.name.startsWith("diagnostico-") && !it.name.startsWith("diagnostico-comparison-") && it.bytes<=65536 && (!workOnly || work?.evidence?.any{ref->AuditWorkPolicy.matches(ref,it)}==true)}
 LaunchedEffect(candidates){
  result?.let{if(it.first !in candidates || it.second !in candidates)result=null}
 }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("CAMBIOS ENTRE LECTURAS",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("Antes y después, con evidencia",style=MaterialTheme.typography.headlineSmall)
  Text("Compara dos diagnósticos guardados del mismo tipo y destino. Las copias se verifican por SHA-256 antes de leer sus datos.")
  if(work!=null)FilterChip(selected=workOnly,onClick={workOnly=!workOnly;selection.chooseFirst(null);selection.chooseSecond(null);result=null;suggestion=null;error=""},enabled=!running && !suggesting,label={Text("Solo trabajo activo")})
  if(candidates.size<2)Text("Necesitas dos diagnósticos compatibles guardados. Repite una revisión desde su herramienta y guarda ambas lecturas.")
  OutlinedButton(enabled=!running && !suggesting && !busy && candidates.size>=2,modifier=Modifier.testTag("comparison-suggest"),onClick={
   suggesting=true;error=""
   scope.launch{try{
    val proposed=withContext(Dispatchers.IO){suggestComparison(candidates){entry->ensureActive();store.diagnostic(entry)}}
    if(proposed==null)error="No hay dos lecturas verificables del mismo tipo y destino entre las últimas 100 copias de este filtro. Puedes elegir otras manualmente."
    else{selection.chooseFirst(proposed.baseline.entry.id);selection.chooseSecond(proposed.current.entry.id);suggestion=proposed;manual=false;result=null}
   }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudo preparar la pareja"}finally{suggesting=false}}
  }){Text(if(suggesting)"Buscando copias compatibles…" else "Preparar pareja compatible")}
  Text("Prepara dos lecturas del mismo tipo y destino. Puedes cambiar la selección.",style=MaterialTheme.typography.bodySmall)
  if(suggesting)LinearProgressIndicator(Modifier.fillMaxWidth())
  DeckDisclosure("Cómo se prepara la pareja","Revisa hasta 100 copias recientes del filtro. Elige la lectura guardada más reciente que tenga otra del mismo tipo y destino exactos. A es la copia anterior por orden de guardado y B la posterior; no acredita el orden de observación ni identidad física. Omite copias no verificables y formatos incompatibles. Verificar y comparar comprueba de nuevo ambas copias. No inicia pruebas de red.")
  suggestion?.takeIf{it.baseline.entry.id==first && it.current.entry.id==second && candidates.contains(it.baseline.entry) && candidates.contains(it.current.entry)}?.let{pair->
   Card(Modifier.fillMaxWidth().testTag("comparison-pair-preview")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("PAREJA PREPARADA",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text("${pair.current.diagnostic.kind} · ${pair.current.diagnostic.target}",style=MaterialTheme.typography.titleMedium)
    Text("A · Guardada ${displayTime(pair.baseline.entry.importedAt)}\nB · Guardada ${displayTime(pair.current.entry.importedAt)}",style=MaterialTheme.typography.bodySmall)
    DeckDisclosure("Ver resúmenes de las lecturas","A: ${pair.baseline.diagnostic.summary.take(240)}\nB: ${pair.current.diagnostic.summary.take(240)}")
   }}
  }
  TextButton(enabled=!running && !suggesting && !busy,onClick={manual=!manual}){Text(if(manual)"Ocultar selección manual" else "Elegir otras lecturas")}
  if(manual){
  ComparisonChoice("A · Referencia","before",candidates,first,!running && !suggesting && !busy){selection.chooseFirst(it);suggestion=null;result=null;error=""}
  ComparisonChoice("B · Otra lectura","after",candidates,second,!running && !suggesting && !busy){selection.chooseSecond(it);suggestion=null;result=null;error=""}
  }
  Button(enabled=!running && !suggesting && !busy && first!=second && candidates.any{it.id==first} && candidates.any{it.id==second},onClick={
   val a=candidates.firstOrNull{it.id==first};val b=candidates.firstOrNull{it.id==second}
   if(a==null || b==null){error="La selección ya no está disponible"}else{running=true;error="";result=null
    scope.launch{try{result=withContext(Dispatchers.IO){loadSavedComparison(store,a,b)}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudo comparar"}finally{running=false}}
   }
  }){Text("Verificar y comparar")}
  if(running)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  result?.let{comparison->
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("COPIAS VERIFICADAS",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text("Comprobación: ${displayTime(comparison.verifiedAt)}",style=MaterialTheme.typography.bodySmall)
    Text("${comparison.baseline.kind} · ${comparison.baseline.target}",style=MaterialTheme.typography.titleMedium)
    Text("A: ${displayTime(comparison.baseline.checkedAt)}\nB: ${displayTime(comparison.current.checkedAt)}",style=MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(18.dp)){var x=0f;FieldChange.entries.forEach{change->val width=if(comparison.rows.isEmpty())0f else size.width*comparison.count(change)/comparison.rows.size;drawRect(changeColor(change),androidx.compose.ui.geometry.Offset(x,0f),androidx.compose.ui.geometry.Size(width,size.height));x+=width}}
    FieldChange.entries.forEach{change->Text("${comparison.count(change)} · ${change.label}",color=changeColor(change),style=MaterialTheme.typography.bodySmall)}
    Text("Campos observados; no es una puntuación de seguridad.",style=MaterialTheme.typography.bodySmall)
   }}
   OutlinedButton(enabled=!busy,onClick={save(comparison.diagnostic())}){Text("Guardar comparación")}
   FilterChip(changesOnly,onClick={changesOnly=!changesOnly},label={Text("Solo diferencias")})
   OutlinedTextField(query,{query=it.take(160)},label={Text("Buscar campo")},modifier=Modifier.fillMaxWidth())
   val rows=comparison.rows.filter{(!changesOnly || it.change!=FieldChange.SAME) && it.key.contains(query,true)}
   Text("${rows.size} campos · se muestran hasta 30; usa la búsqueda",style=MaterialTheme.typography.bodySmall)
   if(rows.isEmpty())Text("No hay campos que coincidan con este filtro.")
   rows.take(30).forEach{row->DeckDisclosure("${row.key.ifBlank{"(sin etiqueta)"}} · ${row.change.label}","A:\n${row.before.joinToString("\n").ifEmpty{"(campo ausente)"}}\n\nB:\n${row.after.joinToString("\n").ifEmpty{"(campo ausente)"}}")}
   DeckDisclosure("Resúmenes originales","A: ${comparison.baseline.summary}\n\nB: ${comparison.current.summary}")
  }
  DeckDisclosure("Método y límites","Compara valores textuales por etiqueta, conservando duplicados e ignorando el orden dentro de cada grupo. El destino y tipo deben coincidir exactamente; esto no prueba identidad física. No ordena las lecturas por fecha: A es la referencia elegida. Un campo ausente puede reflejar cambios de formato. Recursos y tiempos fluctúan de forma normal. El resultado es puntual; para regenerarlo se vuelven a verificar ambas copias. Guardar conserva referencias y hasta 50 diferencias abreviadas. No modifica originales ni envía tráfico.")
 }
}
@Composable private fun ComparisonChoice(label:String,tag:String,entries:List<Evidence>,selected:String?,enabled:Boolean,choose:(String)->Unit){
 var menu by remember{mutableStateOf(false)};var query by rememberSaveable{mutableStateOf("")}
 Column{
  OutlinedTextField(query,{query=it.take(160)},enabled=enabled,label={Text("Buscar $label")},modifier=Modifier.fillMaxWidth())
  Box{
   OutlinedButton(enabled=enabled && entries.isNotEmpty(),onClick={menu=true},modifier=Modifier.testTag("comparison-$tag-choice")){Text(label+": "+(entries.firstOrNull{it.id==selected}?.name?:"Elegir copia"),maxLines=2)}
   DropdownMenu(menu,onDismissRequest={menu=false}){val options=entries.filter{it.name.contains(query,true)};options.take(50).forEach{entry->DropdownMenuItem(text={Text(entry.name+" · "+displayTime(entry.importedAt),maxLines=3)},onClick={choose(entry.id);menu=false},modifier=Modifier.testTag("comparison-$tag-${entry.id}"))};if(options.size>50)DropdownMenuItem(text={Text("Hay más resultados; concreta la búsqueda")},onClick={},enabled=false)}
  }
 }
}
private fun changeColor(change:FieldChange)=when(change){FieldChange.SAME->DeckMint;FieldChange.CHANGED->Color(0xFFFFD36B);FieldChange.ADDED->DeckCyan;FieldChange.REMOVED->Color(0xFFC7ABFF)}
