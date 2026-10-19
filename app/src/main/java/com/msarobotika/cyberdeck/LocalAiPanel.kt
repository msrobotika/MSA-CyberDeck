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
import java.time.Instant

@Composable fun LocalAiPanel(session:DeckSessionModel,contexts:List<Pair<String,DiagnosticResult>>,pick:()->Unit,save:(DiagnosticResult)->Unit) {
 val context=LocalContext.current
 val state by session.ai.collectAsState()
 val model by session.aiModel.collectAsState()
 val request by session.aiRequest.collectAsState()
 val importing by session.aiImporting.collectAsState()
 val importStatus by session.aiImportStatus.collectAsState()
 val client=remember{LocalAiClient(context,session.ai)}
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 var question by rememberSaveable{mutableStateOf("Explica este resultado y qué debería comprobar después.")}
 var chosen by rememberSaveable{mutableStateOf("Sin contexto")}
 var expanded by remember{mutableStateOf(false)}
 var deleting by remember{mutableStateOf(false)}
 val selected=contexts.firstOrNull{it.first==chosen}?.second
 val blocked=state.busy || importing
 DisposableEffect(client,owner){
  client.connect()
  val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_STOP->client.disconnect();Lifecycle.Event.ON_RESUME->client.connect();else->Unit}}
  owner.lifecycle.addObserver(observer)
  onDispose{owner.lifecycle.removeObserver(observer);client.disconnect()}
 }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.size(80.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
    Text("ANALISTA LOCAL",color=DeckCyan,style=MaterialTheme.typography.labelLarge)
    Text("Google LiteRT-LM",style=MaterialTheme.typography.titleLarge)
    Text(if(model==null)"SIN MODELO IMPORTADO" else if(state.loaded)"MODELO EN MEMORIA" else "ARCHIVO IMPORTADO · SIN CARGAR",style=MaterialTheme.typography.labelSmall)
   }
  }}
  Text("Explica tus resultados y prepara comprobaciones defensivas. Sin enviar preguntas o informes a un servidor.")
  Text(if(importing)importStatus else state.phase,color=DeckCyan,style=MaterialTheme.typography.bodySmall)
  if(importStatus.isNotBlank() && !importing)Text(importStatus,style=MaterialTheme.typography.bodySmall)
  if(blocked)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
  if(!state.connected && !blocked)OutlinedButton(onClick={client.disconnect();client.connect()}){Text("Reconectar motor")}
  Button(enabled=!blocked && !state.loaded,onClick=pick){Text(if(model==null)"Importar modelo .litertlm" else "Cambiar modelo")}
  if(model==null){
   Text("Prepara Gemma 3 1B IT INT4 desde su página oficial, acepta allí sus condiciones y elige el archivo descargado. Esta versión no contiene los pesos ni genera ejemplos de respuesta.",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/litert-community/Gemma3-1B-IT")))}){Text("Abrir página del modelo")}
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
  HorizontalDivider()
  Text("Qué quieres analizar",style=MaterialTheme.typography.titleMedium)
  Box{OutlinedButton(onClick={expanded=true},enabled=!blocked){Text(if(selected==null)"Sin contexto adjunto" else "Contexto: $chosen")};DropdownMenu(expanded,onDismissRequest={expanded=false}){
   (listOf("Sin contexto")+contexts.map{it.first}).forEach{name->DropdownMenuItem(text={Text(name)},onClick={chosen=name;expanded=false})}
  }}
  selected?.let{result->DeckDisclosure("Ver datos incluidos",AiPolicy.prompt("Explicar resultado",result))}
  OutlinedTextField(question,{question=it.take(800)},label={Text("Pregunta al analista")},supportingText={Text("${question.length}/800 · solo el contexto que elijas, hasta 2200 caracteres")},modifier=Modifier.fillMaxWidth(),maxLines=5,enabled=!blocked)
  Button(enabled=state.loaded && !blocked && question.isNotBlank(),onClick={session.aiRequest.value=AiRequest(question,selected?.let{"${it.kind}: ${it.target} · ${it.checkedAt} · primeros 2200 caracteres"}?:"Ninguno",model?.name.orEmpty(),model?.sha256.orEmpty(),Instant.now().toString());client.send(AiWire.ASK,AiPolicy.prompt(question,selected))}){Text("Analizar en este móvil")}
  if(state.busy && state.loaded)OutlinedButton(onClick={client.send(AiWire.STOP)}){Text("Detener generación")}
  if(!state.loaded)Text("Importa y carga un modelo para habilitar la generación. Motor nativo: ${if(state.runtime)"cargado" else "sin verificar"}.",style=MaterialTheme.typography.bodySmall)
  if(state.output.isNotEmpty()){
   Text("INTERPRETACIÓN GENERADA · REVISAR",color=DeckViolet,style=MaterialTheme.typography.labelLarge)
   SelectionContainer{Text(state.output)}
   Text("${state.elapsed/1000} s · ${state.chunks} fragmentos recibidos (no tokens). Puede contener errores; no acredita ataques ni sustituye las lecturas originales.",style=MaterialTheme.typography.bodySmall)
   OutlinedButton(enabled=!blocked,onClick={save(DiagnosticResult("ai","Modelo local",Instant.now().toString(),"Interpretación de IA; no es una medición",listOf("Motor" to "Google LiteRT-LM 0.13.1 · CPU local","Modelo" to (request?.model?:"No disponible"),"SHA-256 modelo" to (request?.modelHash?:"No disponible"),"Pregunta" to (request?.question?:"No disponible"),"Contexto seleccionado" to (request?.context?:"No disponible"),"Fecha de solicitud" to (request?.at?:"No disponible"),"Estado" to if(state.error.isBlank())"Finalizada por el motor; pendiente de revisión" else "Interrumpida: ${state.error}","Respuesta generada" to state.output,"Límite" to "Texto de IA no verificado. Sin ejecución de herramientas ni acciones sobre equipos.")))}){Text("Guardar interpretación")}
  }
  DeckDisclosure("Privacidad y límites","No incluye Gemini en la nube, clave API ni automatización de herramientas. El modelo se copia a almacenamiento privado y no se distribuye en GitHub. El motor corre en otro proceso de CiberDeck para contener fallos nativos; tiene los permisos de la app, pero esta integración no realiza peticiones de red. Puede equivocarse o seguir texto malicioso del contexto: revisa la respuesta. Ninguna salida se ejecuta. No se graba micrófono ni se leen llamadas, mensajes o archivos no seleccionados para la IA. Generación pendiente de validación física con un modelo real.")
 }
 if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("¿Eliminar el modelo importado?")},text={Text("Borra solo la copia privada de CiberDeck. El archivo que seleccionaste permanece intacto.")},confirmButton={TextButton(onClick={runCatching{AiModelStore(context).delete()}.onSuccess{session.aiModel.value=null;session.ai.update{it.copy(output="")}}.onFailure{error->session.ai.update{it.copy(error=error.message?:"No se pudo eliminar")}};deleting=false}){Text("Eliminar")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancelar")}})
}
