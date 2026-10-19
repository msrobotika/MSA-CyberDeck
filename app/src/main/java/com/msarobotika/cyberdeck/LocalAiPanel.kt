package com.msarobotika.cyberdeck

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.*
import java.time.Instant

@Composable fun LocalAiPanel(session:DeckSessionModel,contexts:List<Pair<String,DiagnosticResult>>,pick:()->Unit,save:(DiagnosticResult)->Unit,savedContext:SavedAiContext?=null,clearSaved:()->Unit={},navigate:(String)->Unit={},agentRuntime:AgentActionRuntime?=null) {
 val context=LocalContext.current
 val state by session.ai.collectAsState()
 val model by session.aiModel.collectAsState()
 val request by session.aiRequest.collectAsState()
 val importing by session.aiImporting.collectAsState()
 val importStatus by session.aiImportStatus.collectAsState()
 val agent by session.agent.collectAsState()
 val agentMode by session.agentMode.collectAsState()
 val client=remember{LocalAiClient(context,session.ai)}
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 var question by rememberSaveable{mutableStateOf("Explica este resultado en tres frases e indica qué comprobar después.")}
 var chosen by rememberSaveable{mutableStateOf("Sin contexto")}
 var expanded by remember{mutableStateOf(false)}
 var deleting by remember{mutableStateOf(false)}
 var engineOptions by rememberSaveable{mutableStateOf(false)}
 val savedChosen=chosen=="Evidencia seleccionada" && savedContext!=null
 val selected=if(savedChosen)savedContext?.input() else contexts.firstOrNull{it.first==chosen}?.second
 var preparing by remember{mutableStateOf(false)}
 var preparationError by remember{mutableStateOf("")}
 var preparation by remember{mutableStateOf<Job?>(null)}
 val scope=rememberCoroutineScope()
 val sourceStore=remember(context){EvidenceStore(context)}
 val blocked=state.busy || importing || preparing || agent.running
 LaunchedEffect(savedContext?.entry?.id){
  if(savedContext!=null)chosen="Evidencia seleccionada" else if(chosen=="Evidencia seleccionada")chosen="Sin contexto"
 }
 fun ask(){
  val questionSnapshot=question;val selectedSnapshot=selected;val modelSnapshot=model
  val source=if(savedChosen)savedContext else null
  preparing=true;preparationError=""
  preparation=scope.launch{
   try{
    val checked=source?.let{withContext(Dispatchers.IO){loadSavedAiContext(sourceStore,it.entry)}}
    val input=checked?.input()?:selectedSnapshot
    val live=session.ai.value
    if(!live.connected || !live.loaded || live.busy)error("Carga el modelo local y vuelve a intentarlo")
    session.aiRequest.value=AiRequest(questionSnapshot,input?.let{"${it.kind.take(64)}: ${it.target.take(200)} · ${it.checkedAt.take(100)} · primeros 2200 caracteres"}?:"Ninguno",modelSnapshot?.name?.take(300).orEmpty(),modelSnapshot?.sha256.orEmpty(),Instant.now().toString(),checked?.entry?.id.orEmpty(),checked?.entry?.sha256.orEmpty(),checked?.verifiedAt.orEmpty())
    client.send(AiWire.ASK,AiPolicy.modelPrompt(questionSnapshot,input,modelSnapshot?.name.orEmpty()))
   }catch(e:CancellationException){throw e}
   catch(e:Exception){preparationError=e.message?:"No se pudo verificar el contexto"}
   finally{preparing=false}
  }
 }
 DisposableEffect(client,owner){
  client.connect()
  val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_STOP->{preparation?.cancel();client.disconnect()};Lifecycle.Event.ON_RESUME->client.connect();else->Unit}}
  owner.lifecycle.addObserver(observer)
  onDispose{preparation?.cancel();owner.lifecycle.removeObserver(observer);client.disconnect()}
 }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   FilterChip(!agentMode,onClick={session.agentMode.value=false;session.ai.update{it.copy(output="",error="")};session.aiRequest.value=null;navigate("IA")},enabled=!blocked,label={Text("Analista")})
   FilterChip(agentMode,onClick={session.agentMode.value=true;session.ai.update{it.copy(output="",error="")};session.aiRequest.value=null;navigate("Agente")},enabled=!blocked,label={Text("Agente")})
  }
  Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.size(80.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
    Text(if(agentMode)"AGENTE LOCAL" else "ANALISTA LOCAL",color=DeckCyan,style=MaterialTheme.typography.labelLarge)
    Text(if(agentMode)"Tu agente de campo" else "Google LiteRT-LM",style=MaterialTheme.typography.titleLarge)
    Text(if(model==null)"SIN MODELO IMPORTADO" else if(state.loaded)"MODELO EN MEMORIA" else "ARCHIVO IMPORTADO · SIN CARGAR",style=MaterialTheme.typography.labelSmall)
   }
  }}
  if(agentMode){
   TextButton(enabled=!blocked,onClick={engineOptions=!engineOptions}){Text(if(engineOptions)"Ocultar ajustes de IA" else "Modelo y ajustes de IA")}
   if(!engineOptions && !state.loaded && model!=null)Button(enabled=!blocked && state.runtime,onClick={client.send(AiWire.LOAD)}){Text("Cargar IA local")}
   if(!engineOptions && model==null)OutlinedButton(enabled=!blocked,onClick=pick){Text("Importar modelo para el agente")}
   if(state.busy)Text(state.phase,color=DeckCyan)
   if(state.error.isNotBlank() && !engineOptions)Text(state.error,color=MaterialTheme.colorScheme.error)
  }
  if(!agentMode || engineOptions){
  Text("Explica tus resultados y prepara comprobaciones defensivas. Sin enviar preguntas o informes a un servidor.")
  Text(if(importing)importStatus else state.phase,color=DeckCyan,style=MaterialTheme.typography.bodySmall)
  if(importStatus.isNotBlank() && !importing)Text(importStatus,style=MaterialTheme.typography.bodySmall)
  if(blocked)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
  if(!state.connected && !blocked)OutlinedButton(onClick={client.disconnect();client.connect()}){Text("Reconectar motor")}
  Button(enabled=!blocked && !state.loaded,onClick=pick){Text(if(model==null)"Importar modelo .litertlm" else "Cambiar modelo")}
  if(model==null){
   Text("Modelo recomendado sin registro: Qwen3 0,6B mixed INT4, unos 498 MB, licencia Apache 2.0. Descarga el archivo .litertlm desde LiteRT Community e impórtalo. Los pesos no vienen incluidos en la APK.",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/litert-community/Qwen3-0.6B/blob/e6687177e00897731e7716267eebd4eba0dde7c2/qwen3_0_6b_mixed_int4.litertlm")))}){Text("Abrir modelo sin registro")}
  }else model?.let{m->
   Text(m.name+" · "+displayBytes(m.bytes),style=MaterialTheme.typography.bodySmall)
   DeckDisclosure("Huella y almacenamiento","SHA-256: ${m.sha256}\nImportado: ${m.importedAt}. Copia privada; el original no se modifica. La cabecera y el hash no acreditan procedencia ni compatibilidad; cargarlo comprobará si el motor lo acepta.")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    if(!state.loaded)Button(enabled=!blocked && state.runtime,onClick={client.send(AiWire.LOAD)}){Text("Cargar IA local")}
    else OutlinedButton(enabled=!blocked,onClick={client.send(AiWire.UNLOAD)}){Text("Liberar memoria")}
    TextButton(enabled=!blocked && !state.loaded,onClick={deleting=true}){Text("Eliminar copia")}
   }
  }
  Text("CPU · 2 hilos · contexto limitado · se libera al salir. Cargar un modelo puede tardar y aumentar el consumo.",style=MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider()
  if(agentMode){AgentPanel(session,blocked,runtime=agentRuntime,send={what,prompt->client.send(what,prompt)},save=save,navigate={route->if(route=="IA"){session.agentMode.value=false;session.ai.update{it.copy(output="")};session.aiRequest.value=null};navigate(route)})}else{
  Text("Qué quieres analizar",style=MaterialTheme.typography.titleMedium)
  Box{OutlinedButton(onClick={expanded=true},enabled=!blocked){Text(if(selected==null)"Sin contexto adjunto" else "Contexto: $chosen")};DropdownMenu(expanded,onDismissRequest={expanded=false}){
   (listOf("Sin contexto")+(if(savedContext!=null)listOf("Evidencia seleccionada") else emptyList())+contexts.map{it.first}).forEach{name->DropdownMenuItem(text={Text(name)},onClick={chosen=name;expanded=false})}
  }}
  if(savedChosen)savedContext?.let{source->
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("FUENTE GUARDADA",style=MaterialTheme.typography.labelLarge,color=DeckMint)
    Text(source.entry.name,style=MaterialTheme.typography.bodyMedium,maxLines=2)
    Text("Verificada: ${displayTime(source.verifiedAt)}. Se vuelve a comprobar antes de generar.",style=MaterialTheme.typography.bodySmall)
    DeckDisclosure("Referencia de la fuente","ID: ${source.entry.id}\nSHA-256: ${source.entry.sha256}")
    TextButton(enabled=!blocked,onClick=clearSaved){Text("Retirar contexto guardado")}
   }}
  }
  Text("Preguntas rápidas",style=MaterialTheme.typography.titleMedium)
  listOf("Explicar" to "Explica estas observaciones en español. Separa datos observados e incertidumbres.","Comprobar" to "Propón tres comprobaciones defensivas sobre mis equipos para aclarar este resultado. No afirmes haberlas ejecutado.","Preparar informe" to "Redacta una nota breve de auditoría: observaciones, límites y próximos pasos. No inventes ataques ni vulnerabilidades.").forEach{(label,prompt)->OutlinedButton(enabled=!blocked,onClick={question=prompt}){Text(label)}}
  selected?.let{result->DeckDisclosure("Ver datos incluidos",AiPolicy.prompt("Explicar resultado",result))}
  OutlinedTextField(question,{question=it.take(800)},label={Text("Pregunta al analista")},supportingText={Text("${question.length}/800 · solo el contexto que elijas, hasta 2200 caracteres")},modifier=Modifier.fillMaxWidth(),maxLines=5,enabled=!blocked)
  Button(enabled=state.loaded && !blocked && question.isNotBlank(),onClick={ask()}){Text("Analizar en este móvil")}
  if(preparing)Text("Verificando la fuente seleccionada…",style=MaterialTheme.typography.bodySmall)
  if(preparationError.isNotBlank())Text(preparationError,color=MaterialTheme.colorScheme.error)
  if(state.busy && state.loaded)OutlinedButton(onClick={client.send(AiWire.STOP)}){Text("Detener generación")}
  if(!state.loaded)Text("Importa y carga un modelo para habilitar la generación. Motor nativo: ${if(state.runtime)"cargado" else "sin verificar"}.",style=MaterialTheme.typography.bodySmall)
  if(state.output.isNotEmpty()){
   Text("INTERPRETACIÓN GENERADA · REVISAR",color=DeckViolet,style=MaterialTheme.typography.labelLarge)
   request?.let{Text("Contexto utilizado: ${it.context}",style=MaterialTheme.typography.bodySmall)}
   SelectionContainer{Text(state.output)}
   Text("${state.elapsed/1000} s · ${state.chunks} fragmentos recibidos (no tokens). Puede contener errores; no acredita ataques ni sustituye las lecturas originales.",style=MaterialTheme.typography.bodySmall)
   OutlinedButton(enabled=!blocked,onClick={save(DiagnosticResult("ai","Modelo local",Instant.now().toString(),"Interpretación de IA; no es una medición",listOf("Motor" to "Google LiteRT-LM 0.13.1 · CPU local","Modelo" to (request?.model?:"No disponible"),"SHA-256 modelo" to (request?.modelHash?:"No disponible"),"Pregunta" to (request?.question?:"No disponible"),"Contexto seleccionado" to (request?.context?:"No disponible"),"Fecha de solicitud" to (request?.at?:"No disponible"),"ID fuente guardada" to (request?.sourceId?.ifBlank{"No adjunta"}?:"No disponible"),"SHA-256 fuente guardada" to (request?.sourceHash?.ifBlank{"No adjunta"}?:"No disponible"),"Fecha de verificación de fuente" to (request?.sourceVerifiedAt?.ifBlank{"No adjunta"}?:"No disponible"),"Estado" to if(state.error.isBlank())"Finalizada por el motor; pendiente de revisión" else "Interrumpida: ${state.error}","Respuesta generada" to state.output,"Límite" to "Texto de IA no verificado. Sin ejecución de herramientas ni acciones sobre equipos.")))}){Text("Guardar interpretación")}
  }
  DeckDisclosure("Privacidad y límites","No incluye Gemini en la nube, clave API ni automatización de herramientas. El modelo se copia a almacenamiento privado y no se distribuye en GitHub. El motor corre en otro proceso de CiberDeck para contener fallos nativos; tiene los permisos de la app, pero esta integración no realiza peticiones de red. Puede equivocarse o seguir texto malicioso del contexto: revisa la respuesta. Ninguna salida se ejecuta. No se graba micrófono ni se leen llamadas, mensajes o archivos no seleccionados para la IA. Las respuestas pueden ser inexactas y no son un diagnóstico de seguridad.")
  }
 }
 if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("¿Eliminar el modelo importado?")},text={Text("Borra solo la copia privada de CiberDeck. El archivo que seleccionaste permanece intacto.")},confirmButton={TextButton(onClick={runCatching{AiModelStore(context).delete()}.onSuccess{session.aiModel.value=null;session.ai.update{it.copy(output="")}}.onFailure{error->session.ai.update{it.copy(error=error.message?:"No se pudo eliminar")}};deleting=false}){Text("Eliminar")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancelar")}})
}
