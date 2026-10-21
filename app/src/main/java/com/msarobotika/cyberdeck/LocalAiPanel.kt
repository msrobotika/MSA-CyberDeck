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
import androidx.compose.ui.platform.testTag
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
 var includePhone by rememberSaveable{mutableStateOf(true)}
 var objective by rememberSaveable{mutableStateOf("")}
 var chosen by rememberSaveable{mutableStateOf("Sin contexto")}
 var expanded by remember{mutableStateOf(false)}
 var showInterpretation by rememberSaveable{mutableStateOf(false)}
 LaunchedEffect(request?.at){showInterpretation=false}
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
 fun ask(generate:Boolean=true){
  val questionSnapshot=question;val selectedSnapshot=selected;val modelSnapshot=model
  val includePhoneSnapshot=includePhone;val objectiveSnapshot=objective
  val source=if(savedChosen)savedContext else null
  preparing=true;preparationError=""
  preparation=scope.launch{
   try{
    val checked=source?.let{withContext(Dispatchers.IO){loadSavedAiContext(sourceStore,it.entry)}}
    val input=checked?.input()?:selectedSnapshot
    val phone=if(includePhoneSnapshot)withContext(Dispatchers.IO){PhoneAudit.read(context).diagnostic()} else null
    val environment=AiContext.snapshot(phone,objectiveSnapshot)
    val live=session.ai.value
    if(generate && (!live.connected || !live.loaded || live.busy))error("Carga el modelo local y vuelve a intentarlo")
    val prompt=if(generate)AiPolicy.modelPrompt(questionSnapshot,input,modelSnapshot?.name.orEmpty(),environment) else ""
    val capturedAt=Instant.now().toString()
    val sources=AiSources.capture(input,phone,environment,capturedAt,checked?.entry?.id.orEmpty(),checked?.entry?.sha256.orEmpty(),checked?.verifiedAt.orEmpty())
    session.ai.update{it.copy(output="")}
    session.aiRequest.value=AiRequest(questionSnapshot,input?.let{"${it.kind.take(64)}: ${it.target.take(200)} · ${it.checkedAt.take(100)} · hasta 1200 caracteres"}?:(if(environment.hasPhone)"Instantánea Android · sin evidencia adicional" else "Situación y objetivo · sin lectura adjunta"),modelSnapshot?.name?.take(300).orEmpty(),modelSnapshot?.sha256.orEmpty(),capturedAt,checked?.entry?.id.orEmpty(),checked?.entry?.sha256.orEmpty(),checked?.verifiedAt.orEmpty(),environment.text,environment.sha256,prompt,if(generate)PeerProtocol.sha256(prompt.toByteArray(Charsets.UTF_8)) else "",sources,generate)
    if(generate)client.send(AiWire.ASK,prompt) else session.ai.update{it.copy(output="",error="")}
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
  if(!agentMode)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   FilterChip(!agentMode,onClick={session.agentMode.value=false;session.ai.update{it.copy(output="",error="")};session.aiRequest.value=null;navigate("IA")},enabled=!blocked,label={Text("Analista")})
   FilterChip(agentMode,onClick={session.agentMode.value=true;session.ai.update{it.copy(output="",error="")};session.aiRequest.value=null;navigate("Agente")},enabled=!blocked,label={Text("Agente")})
  }
  if(!agentMode)Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.size(if(agentMode)40.dp else 80.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
    Text(if(agentMode)"LISTO PARA EL MÓVIL" else "ANALISTA LOCAL",color=DeckCyan,style=MaterialTheme.typography.labelLarge)
    Text(if(agentMode)"Tu agente de campo" else "Google LiteRT-LM",style=MaterialTheme.typography.titleLarge)
    if(!agentMode)Text(if(model==null)"SIN MODELO IMPORTADO" else if(state.loaded)"MODELO EN MEMORIA" else "ARCHIVO IMPORTADO · SIN CARGAR",style=MaterialTheme.typography.labelSmall)
   }
  }}
  if(!agentMode){
   Text("Consulta tus fuentes sin cargar un modelo. La interpretación de IA es opcional.",style=MaterialTheme.typography.bodySmall)
   TextButton(enabled=!blocked,onClick={engineOptions=!engineOptions}){Text(if(engineOptions)"Ocultar ajustes de IA" else "Modelo y ajustes de IA")}
   if(!engineOptions && !state.loaded && model!=null)Button(enabled=!blocked && state.runtime,onClick={client.send(AiWire.LOAD)}){Text("Cargar IA local")}
   if(!engineOptions && model==null)OutlinedButton(enabled=!blocked,onClick=pick){Text("Importar modelo .litertlm")}
  }
  if(!agentMode && !engineOptions){
   if(importing || state.busy){LinearProgressIndicator(Modifier.fillMaxWidth());Text(if(importing)importStatus.ifBlank{"Importando modelo…"} else state.phase,style=MaterialTheme.typography.bodySmall)}
   if(importStatus.isNotBlank() && !importing)Text(importStatus,style=MaterialTheme.typography.bodySmall)
   if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
   if(!state.connected && !blocked)OutlinedButton(onClick={client.disconnect();client.connect()}){Text("Reconectar motor")}
  }
  if(engineOptions){
  Text("Explica tus resultados y prepara comprobaciones defensivas. Sin enviar preguntas o informes a un servidor.")
  Text(if(importing)importStatus else state.phase,color=DeckCyan,style=MaterialTheme.typography.bodySmall)
  if(importStatus.isNotBlank() && !importing)Text(importStatus,style=MaterialTheme.typography.bodySmall)
  if(blocked)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
  if(!state.connected && !blocked)OutlinedButton(onClick={client.disconnect();client.connect()}){Text("Reconectar motor")}
  Button(enabled=!blocked && !state.loaded,onClick=pick){Text(if(model==null)"Importar modelo .litertlm" else "Cambiar modelo")}
  if(model==null){
   Text("Modelo ligero de compatibilidad: Qwen3 0,6B mixed INT4, unos 498 MB, licencia Apache 2.0. Su calidad es experimental y ha producido afirmaciones sin respaldo. Descarga el .litertlm desde LiteRT Community e impórtalo; los pesos no vienen incluidos en la APK.",style=MaterialTheme.typography.bodySmall)
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
  Text(AiGenerationPolicy.DESCRIPTION,style=MaterialTheme.typography.bodySmall)
  Text("CPU · 2 hilos · contexto limitado · se libera al salir. Cargar un modelo puede tardar y aumentar el consumo.",style=MaterialTheme.typography.bodySmall)
  }
  if(!agentMode)HorizontalDivider()
  if(agentMode){AgentPanel(session,blocked,runtime=agentRuntime,send={what,prompt->client.send(what,prompt)},save=save,navigate={route->if(route=="IA"){session.agentMode.value=false;session.ai.update{it.copy(output="")};session.aiRequest.value=null};navigate(route)})
  if(agentMode){
   TextButton(enabled=!blocked,onClick={session.agentMode.value=false;session.ai.update{it.copy(output="",error="")};session.aiRequest.value=null;navigate("IA")}){Text("Abrir analista de evidencias")}
   TextButton(enabled=!blocked,onClick={engineOptions=!engineOptions}){Text(if(engineOptions)"Ocultar ajustes de IA" else "Modelo y ajustes de IA")}
   if(!engineOptions && !state.loaded && model!=null)Button(enabled=!blocked && state.runtime,onClick={client.send(AiWire.LOAD)}){Text("Cargar IA local")}
   if(!engineOptions && model==null)OutlinedButton(enabled=!blocked,onClick=pick){Text("Importar modelo para el agente")}
   if(state.busy)Text(state.phase,color=DeckCyan)
   if(state.error.isNotBlank() && !engineOptions)Text(state.error,color=MaterialTheme.colorScheme.error)
  }
  }else{
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
  OutlinedTextField(objective,{objective=it.take(240)},label={Text("Situación y objetivo · opcional")},placeholder={Text("Qué estás revisando y para qué")},modifier=Modifier.fillMaxWidth(),enabled=!blocked,maxLines=3)
  Row{Checkbox(includePhone,onCheckedChange={includePhone=it},enabled=!blocked,modifier=Modifier.testTag("ai-include-phone"));Text("Incluir estado actual del móvil al generar",Modifier.weight(1f).padding(top=10.dp),style=MaterialTheme.typography.bodySmall)}
  FilledTonalButton(enabled=!blocked,onClick={ask(false)},modifier=Modifier.fillMaxWidth().testTag("ai-review-sources")){Text("Revisar fuentes · sin IA")}
  request?.sources?.let{AiSourcesPanel(it,!blocked,save)}
  DeckDisclosure("Qué sabe de CiberDeck",AiContext.CAPABILITIES+" Se incluyen solo Android, BLE, NFC, USB Host, VPN, DNS privado y batería; no se incluyen IP, SSID ni identificadores en este resumen. Las evidencias que elijas pueden contener datos adicionales.")
  val domainGuidance=AiContext.domainGuidance(question+" "+objective)
  if(domainGuidance.isNotBlank())DeckDisclosure("Alcance para campos y salud",domainGuidance)
  Text("Preguntas rápidas",style=MaterialTheme.typography.titleMedium)
  listOf("Explicar" to "Explica estas observaciones en español. Separa datos observados e incertidumbres.","Comprobar" to "Propón tres comprobaciones defensivas sobre mis equipos para aclarar este resultado. No afirmes haberlas ejecutado.","Preparar informe" to "Redacta una nota breve de auditoría: observaciones, límites y próximos pasos. No inventes ataques ni vulnerabilidades.").forEach{(label,prompt)->OutlinedButton(enabled=!blocked,onClick={question=prompt}){Text(label)}}
  selected?.let{result->DeckDisclosure("Ver datos incluidos",AiPolicy.observationText(result,1200))}
  OutlinedTextField(question,{question=it.take(800)},label={Text("Pregunta al analista")},supportingText={Text("${question.length}/800 · solo el contexto que elijas, hasta 1200 caracteres")},modifier=Modifier.fillMaxWidth(),maxLines=5,enabled=!blocked)
  Button(enabled=state.loaded && !blocked && question.isNotBlank(),onClick={ask()}){Text("Analizar en este móvil")}
  if(preparing)Text("Verificando la fuente seleccionada…",style=MaterialTheme.typography.bodySmall)
  if(preparationError.isNotBlank())Text(preparationError,color=MaterialTheme.colorScheme.error)
  if(state.busy && state.loaded)OutlinedButton(onClick={client.send(AiWire.STOP)}){Text("Detener generación")}
  if(!state.loaded)Text("Importa y carga un modelo para habilitar la generación. Motor nativo: ${if(state.runtime)"cargado" else "sin verificar"}.",style=MaterialTheme.typography.bodySmall)
  if(state.output.isNotEmpty() && request?.generationRequested!=false){
   Text("INTERPRETACIÓN GENERADA · REVISAR",color=DeckViolet,style=MaterialTheme.typography.labelLarge)
   request?.let{if(AiContext.domainGuidance(it.question+" "+it.environment).isNotBlank())Text("El móvil no permite evaluar la exposición sanitaria del taller. Su serie magnética no caracteriza 50 Hz; hace falta una evaluación profesional con equipo adecuado.",style=MaterialTheme.typography.bodyMedium)}
   request?.let{Text("Contexto utilizado: ${it.context}",style=MaterialTheme.typography.bodySmall)}
   request?.takeIf{it.environment.isNotBlank()}?.let{DeckDisclosure("Contexto usado en esta respuesta",it.prompt.ifBlank{it.environment}+"\nSHA-256 de la entrada: "+it.promptHash)}
   TextButton(onClick={showInterpretation=!showInterpretation},modifier=Modifier.testTag("ai-show-interpretation")){Text(if(showInterpretation)"Ocultar interpretación de IA" else "Ver interpretación de IA")}
   if(showInterpretation)SelectionContainer{Text(state.output)}
   Text("${state.elapsed/1000} s · ${state.chunks} fragmentos recibidos (no tokens). Puede contener errores; no acredita ataques ni sustituye las lecturas originales.",style=MaterialTheme.typography.bodySmall)
   OutlinedButton(enabled=!blocked,onClick={save(DiagnosticResult("ai","Modelo local",Instant.now().toString(),"Interpretación de IA; no es una medición",listOf("Motor" to "Google LiteRT-LM 0.13.1 · CPU local","Modelo" to (request?.model?:"No disponible"),"SHA-256 modelo" to (request?.modelHash?:"No disponible"),"Pregunta" to (request?.question?:"No disponible"),"Contexto seleccionado" to (request?.context?:"No disponible"),"Fecha de solicitud" to (request?.at?:"No disponible"),"ID fuente guardada" to (request?.sourceId?.ifBlank{"No adjunta"}?:"No disponible"),"SHA-256 fuente guardada" to (request?.sourceHash?.ifBlank{"No adjunta"}?:"No disponible"),"Fecha de verificación de fuente" to (request?.sourceVerifiedAt?.ifBlank{"No adjunta"}?:"No disponible"),"Estado" to if(state.error.isBlank())"Finalizada por el motor; pendiente de revisión" else "Interrumpida: ${state.error}","Contexto de entorno utilizado" to request?.environment.orEmpty(),"SHA-256 del contexto de entorno" to request?.environmentHash.orEmpty(),"Entrada enviada al motor" to request?.prompt.orEmpty(),"SHA-256 de la entrada al motor" to request?.promptHash.orEmpty(),"Respuesta generada" to state.output,"Límite" to "Texto de IA no verificado. Sin ejecución de herramientas ni acciones sobre equipos.")))}){Text("Guardar interpretación")}
  }
  DeckDisclosure("Privacidad y límites","No incluye Gemini en la nube, clave API ni automatización de herramientas. El modelo se copia a almacenamiento privado y no se distribuye en GitHub. El motor corre en otro proceso de CiberDeck para contener fallos nativos; tiene los permisos de la app, pero esta integración no realiza peticiones de red. Puede equivocarse o seguir texto malicioso del contexto: revisa la respuesta. Ninguna salida se ejecuta. No se graba micrófono ni se leen llamadas, mensajes o archivos no seleccionados para la IA. Las respuestas pueden ser inexactas y no son un diagnóstico de seguridad.")
  }
 }
 if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("¿Eliminar el modelo importado?")},text={Text("Borra solo la copia privada de CiberDeck. El archivo que seleccionaste permanece intacto.")},confirmButton={TextButton(onClick={runCatching{AiModelStore(context).delete()}.onSuccess{session.aiModel.value=null;session.ai.update{it.copy(output="")}}.onFailure{error->session.ai.update{it.copy(error=error.message?:"No se pudo eliminar")}};deleting=false}){Text("Eliminar")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancelar")}})
}
