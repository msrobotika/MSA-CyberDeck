package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable fun AuditWorkPanel(state:AuditWorkState,store:AuditWorkStore,entries:List<Evidence>,loaded:Boolean,busy:Boolean,change:(String,()->AuditWorkState)->Unit,export:(AuditWork)->Unit,navigate:(String)->Unit){
 var creating by rememberSaveable{mutableStateOf(false)}
 var newName by rememberSaveable{mutableStateOf("")};var newScope by rememberSaveable{mutableStateOf("")};var newTargets by rememberSaveable{mutableStateOf("")}
 var selectedId by rememberSaveable{mutableStateOf<String?>(null)}
 var menu by remember{mutableStateOf(false)}
 LaunchedEffect(state.activeId){if(state.activeId!=null)selectedId=state.activeId}
 LaunchedEffect(state.works){if(state.works.none{it.id==selectedId})selectedId=state.activeId?:state.works.firstOrNull()?.id}
 val selected=state.works.firstOrNull{it.id==selectedId}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("TRABAJOS DE AUDITORÍA",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("Cada prueba en su sitio",style=MaterialTheme.typography.headlineSmall)
  Text("Agrupa el alcance, tus objetivos y las copias que sustentan el informe. Todo permanece en este móvil.")
  if(!loaded){Text("Los trabajos aún no están disponibles. Si hubo un error, no se sobrescribe su archivo.");OutlinedButton(enabled=!busy,onClick={change("Trabajos cargados"){store.read()}}){Text("Volver a cargar")}}
  else{
   Text(state.activeId?.let{id->"Activo: "+state.works.first{it.id==id}.name}?:"Sin trabajo activo · los diagnósticos se guardan en Evidencias",color=DeckCyan,style=MaterialTheme.typography.bodySmall)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    if(state.works.isNotEmpty())Button(enabled=!busy && state.works.size<50,onClick={if(!creating){newName="";newScope="";newTargets=""};creating=!creating}){Text(if(creating)"Cerrar formulario" else "Nuevo trabajo")}
    if(state.activeId!=null)TextButton(enabled=!busy,onClick={change("Se ha detenido la asociación automática"){store.activate(null)}}){Text("Desactivar")}
   }
   if(creating || state.works.isEmpty()){
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text("Crear trabajo",style=MaterialTheme.typography.titleMedium)
     OutlinedTextField(newName,{newName=it.take(160)},label={Text("Nombre del trabajo nuevo")},singleLine=true,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(newScope,{newScope=it.take(2000)},label={Text("Alcance del trabajo nuevo")},minLines=2,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(newTargets,{newTargets=it.take(2000)},label={Text("Objetivos declarados")},supportingText={Text("Equipos propios o autorizados, uno por línea. No inicia comprobaciones.")},modifier=Modifier.fillMaxWidth())
     Button(enabled=!busy && newName.isNotBlank(),onClick={change("Trabajo creado y activado"){store.create(newName,newScope,newTargets)};creating=false}){Text("Crear y activar")}
    }}
   }
   if(state.works.isNotEmpty())Box{
    OutlinedButton(enabled=!busy,onClick={menu=true}){Text(selected?.name?:"Elegir trabajo",maxLines=2,overflow=TextOverflow.Ellipsis)}
    DropdownMenu(menu,onDismissRequest={menu=false}){state.works.asReversed().forEach{work->DropdownMenuItem(text={Text(work.name+if(work.archived)" · archivado" else "",maxLines=2)},onClick={selectedId=work.id;menu=false})}}
   }
   selected?.let{work->WorkDetail(work,state.activeId==work.id,store,entries,busy,change,export,navigate)}
  }
  DeckDisclosure("Qué queda asociado","Los nuevos diagnósticos guardados desde las herramientas se asocian al trabajo activo capturado al pulsar Guardar. Los archivos importados, recibidos y registros USB se añaden manualmente. Desactivar o archivar un trabajo no borra evidencias. Las referencias fijan ID, tamaño y SHA-256: no se sustituyen silenciosamente si cambia el archivo. El informe vuelve a comprobar las copias. Es organización local, no certificación de procedencia ni cadena de custodia.")
 }
}
@Composable private fun WorkDetail(work:AuditWork,active:Boolean,store:AuditWorkStore,entries:List<Evidence>,busy:Boolean,change:(String,()->AuditWorkState)->Unit,export:(AuditWork)->Unit,navigate:(String)->Unit){
 var editing by rememberSaveable(work.id){mutableStateOf(false)}
 var name by rememberSaveable(work.id){mutableStateOf(work.name)};var scope by rememberSaveable(work.id){mutableStateOf(work.scope)}
 var targets by rememberSaveable(work.id){mutableStateOf(work.targets)};var notes by rememberSaveable(work.id){mutableStateOf(work.notes)}
 var picking by rememberSaveable(work.id){mutableStateOf(false)};var query by rememberSaveable(work.id){mutableStateOf("")}
 var picked by rememberSaveable(work.id){mutableStateOf(arrayListOf<String>())}
 val matching=work.evidence.count{ref->entries.any{AuditWorkPolicy.matches(ref,it)}}
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
  Text(work.name,style=MaterialTheme.typography.titleLarge)
  Text(if(work.archived)"ARCHIVADO" else if(active)"TRABAJO ACTIVO" else "ABIERTO",color=if(active)DeckMint else DeckCyan,style=MaterialTheme.typography.labelLarge)
  Text("${work.evidence.size} referencias · $matching coinciden con el índice",style=MaterialTheme.typography.bodySmall)
  Text("Creado ${displayTime(work.createdAt)} · la integridad se verifica al informar",style=MaterialTheme.typography.bodySmall)
  if(work.scope.isNotBlank())Text(work.scope,style=MaterialTheme.typography.bodySmall)
  if(work.targets.isNotBlank())Text("Objetivos: ${work.targets}",style=MaterialTheme.typography.bodySmall)
  if(!active && !work.archived)Button(enabled=!busy,onClick={change("Trabajo activado"){store.activate(work.id)}}){Text("Activar este trabajo")}
  OutlinedButton(enabled=!busy,onClick={export(work)}){Text("Guardar informe del trabajo")}
  TextButton(enabled=!busy,onClick={editing=!editing}){Text(if(editing)"Cerrar ficha" else "Editar ficha y notas")}
  if(editing){
   OutlinedTextField(name,{name=it.take(160)},label={Text("Nombre del trabajo")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(scope,{scope=it.take(2000)},label={Text("Alcance acordado")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(targets,{targets=it.take(2000)},label={Text("Objetivos")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(notes,{notes=it.take(6000)},label={Text("Notas y conclusiones propias")},minLines=3,modifier=Modifier.fillMaxWidth())
   Button(enabled=!busy && name.isNotBlank(),onClick={change("Ficha guardada"){store.edit(work.copy(name=name,scope=scope,targets=targets,notes=notes))}}){Text("Guardar ficha")}
  }
  TextButton(enabled=!busy,onClick={change(if(work.archived)"Trabajo restaurado" else "Trabajo archivado sin borrar copias"){store.archive(work.id,!work.archived)}}){Text(if(work.archived)"Restaurar trabajo" else "Archivar trabajo")}
 }}
 WorkIntegrityPanel(work,busy)
 if(!work.archived){
  Text("Continuar la revisión",style=MaterialTheme.typography.titleMedium)
  Text(if(active)"Los diagnósticos que guardes se vinculan a este trabajo." else "Activa este trabajo para asociarle automáticamente los nuevos diagnósticos.",style=MaterialTheme.typography.bodySmall)
  listOf("Comparar" to "Comparar lecturas guardadas","Móvil" to "Revisar este móvil","Equipo" to "Comprobar un equipo autorizado","APK" to "Inspeccionar una APK","USB" to "Consultar el ESP32 por USB").forEach{(route,label)->OutlinedButton(enabled=!busy && active,onClick={navigate(route)},modifier=Modifier.fillMaxWidth()){Text(label)}}
  TextButton(onClick={picking=!picking}){Text(if(picking)"Cerrar selector de evidencias" else "Añadir evidencias existentes")}
  if(picking){
   OutlinedTextField(query,{query=it.take(160)},label={Text("Buscar copia por nombre")},modifier=Modifier.fillMaxWidth())
   val options=entries.asReversed().filter{e->work.evidence.none{it.id==e.id} && e.name.contains(query,true)}
   Text("${options.size} copias disponibles · se muestran hasta 25; usa la búsqueda",style=MaterialTheme.typography.bodySmall)
   options.take(25).forEach{entry->Row(Modifier.fillMaxWidth()){
    Checkbox(entry.id in picked,onCheckedChange={yes->picked=ArrayList(if(yes)picked+entry.id else picked-entry.id)},enabled=!busy)
    Text(entry.name,Modifier.weight(1f).padding(top=9.dp),style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
   }}
   Button(enabled=!busy && picked.isNotEmpty(),onClick={val selected=entries.filter{it.id in picked};change("Referencias asociadas al trabajo"){store.attach(work.id,selected)};picked=arrayListOf()}){Text("Asociar seleccionadas")}
  }
 }
 if(work.evidence.isNotEmpty()){
  Text("Evidencias vinculadas",style=MaterialTheme.typography.titleMedium)
  work.evidence.takeLast(30).asReversed().forEach{ref->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
   Text(ref.name,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
   Text(if(entries.any{AuditWorkPolicy.matches(ref,it)})"Referencia coincide · archivo por verificar al informar" else "Ausente o diferente del registro fijado",style=MaterialTheme.typography.bodySmall,color=DeckCyan)
   DeckDisclosure("SHA-256 fijado",ref.sha256)
   TextButton(enabled=!busy,onClick={change("Referencia retirada; la copia sigue en Evidencias"){store.detach(work.id,setOf(ref.id))}}){Text("Desvincular")}
  }}}
  if(work.evidence.size>30)Text("Se muestran las 30 referencias más recientes. El informe incluye todas.",style=MaterialTheme.typography.bodySmall)
 }
}
