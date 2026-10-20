package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import java.time.Instant

@Composable fun AgentPanel(session:DeckSessionModel,blocked:Boolean,send:(Int,String)->Unit,save:(DiagnosticResult)->Unit,navigate:(String)->Unit={},runtime:AgentActionRuntime?=null){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 val ai by session.ai.collectAsState();val agent by session.agent.collectAsState();val model by session.aiModel.collectAsState()
 var request by rememberSaveable{mutableStateOf("")}
 var showResults by rememberSaveable{mutableStateOf(false)}
 var work by remember{mutableStateOf<AuditWork?>(null)};var scopeLoaded by remember{mutableStateOf(false)}
 var scopeError by remember{mutableStateOf("")};var executionJob by remember{mutableStateOf<Job?>(null)}
 var quickExpanded by rememberSaveable{mutableStateOf(false)};var queuedTask by remember{mutableStateOf<AgentTask?>(null)}
 var catalog by rememberSaveable{mutableStateOf(false)};var search by rememberSaveable{mutableStateOf("")}
 var picked by rememberSaveable{mutableStateOf(arrayListOf<String>())};var manualNote by rememberSaveable{mutableStateOf("")}
 val actionStatus by (runtime?.status?:remember{ kotlinx.coroutines.flow.MutableStateFlow("") }).collectAsState()
 val actionLink by (runtime?.liveLink?:remember{ kotlinx.coroutines.flow.MutableStateFlow("") }).collectAsState()
 val editing=!blocked && !agent.running && !agent.planning
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 DisposableEffect(owner,runtime){val listener=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP && runtime?.waitingForAndroid!=true)executionJob?.cancel()};owner.lifecycle.addObserver(listener);onDispose{owner.lifecycle.removeObserver(listener)}}
 LaunchedEffect(agent.running,agent.plan){try{val works=withContext(Dispatchers.IO){AuditWorkStore(context).read()};work=works.works.firstOrNull{it.id==works.activeId};scopeLoaded=true}catch(e:CancellationException){throw e}catch(e:Exception){scopeError="No se pudo leer el alcance: ${e.message}"}}
 LaunchedEffect(ai.busy,ai.output,ai.error){if(session.agent.value.planning && !ai.busy){val plan=runCatching{check(ai.error.isBlank()){ai.error};AgentTasks.checkHardwareRequest(session.agent.value.request,AgentPolicy.parse(ai.output))};session.agent.update{it.copy(planning=false,raw=ai.output,plan=plan.getOrNull(),error=if(plan.isFailure)"No he podido preparar esa tarea. Prueba una acción directa o concreta qué quieres comprobar." else "",notice="")}}}
 DisposableEffect(Unit){onDispose{session.agent.update{if(it.planning)it.copy(planning=false,error="Plan interrumpido al salir") else it}}}
 fun execute(completion:String?=null){
  val snapshot=session.agent.value;val plan=snapshot.plan?:return
  val prior=snapshot.run;val pending=prior?.steps?.lastOrNull()?.takeIf{it.awaiting}
  val id=if(prior==null)work?.id else snapshot.workId
  session.agent.update{it.copy(running=true,activeTool=null,error="",evidenceScope=if(prior==null)work?.let{w->"${w.name} [${w.id}]"}?:"Todas las evidencias locales" else snapshot.evidenceScope,workId=id)}
  executionJob=scope.launch{try{
   val run=withContext(Dispatchers.Default){
    validateAgentInputs(plan,snapshot.inputs)
    val started=Instant.now().toString()
    val previous=if(pending==null)emptyList() else{
     val completed=when(completion){
      "saved"->{val entry=snapshot.resumeEvidence?:error("No hay un resultado guardado");loadAgentStepResult(EvidenceStore(context),pending,entry)}
      "manual"->AgentStepResult(pending.tool,manual=true,result=DiagnosticResult("manual-step",pending.tool.label,Instant.now().toString(),"Interacción confirmada por el usuario; no verificada por el agente",listOf("Nota manual" to manualNote.take(400))))
      "skip"->AgentStepResult(pending.tool,skipped=true)
      else->error("El paso está pendiente de interacción")
     }
     prior!!.steps.dropLast(1)+completed
    }
    val progress:(List<AgentStepResult>)->Unit={steps->session.agent.update{it.copy(results=steps,run=AgentRun(plan,prior?.startedAt?:started,Instant.now().toString(),steps))}}
    if(runtime!=null)executeAgentActions(plan,perform={tool->ensureActive();runtime.execute(tool,snapshot.inputs,id,send)},progress=progress,previous=previous,startedAt=prior?.startedAt?:started,active={tool->session.agent.update{it.copy(activeTool=tool)}})
    else executeAgentPlan(plan,perform={tool->ensureActive();performAgentTool(context,tool,id,snapshot.inputs){!isActive}},progress=progress,previous=previous,startedAt=prior?.startedAt?:started)
   }
   session.agent.update{it.copy(run=run,results=run.steps,resumeEvidence=null)}
  }catch(e:CancellationException){session.agent.update{it.copy(error="",notice="Detenido. Los pasos pendientes no se han ejecutado.")};throw e}
   catch(e:Exception){session.agent.update{it.copy(error=e.message?:"Error de ejecución")}}
   finally{session.agent.update{it.copy(running=false,activeTool=null)}}}
 }
 fun chooseTask(task:AgentTask){if(session.agent.value.running || session.agent.value.planning || blocked)return;request=task.title;session.agent.value=task.session();queuedTask=if(task.instant)task else null}
 LaunchedEffect(queuedTask,scopeLoaded,blocked){if(queuedTask!=null && scopeLoaded && !blocked && !session.agent.value.running){queuedTask=null;execute()}}
 fun planWithAi(){val environment=AiContext.snapshot(agent.results.lastOrNull{it.tool==AgentTool.PHONE && it.result!=null}?.result?:session.phone.value?.diagnostic());session.agent.value=AgentSession(request=request,modelHash=model?.sha256.orEmpty(),planning=true,contextSnapshot=environment.text);session.aiRequest.value=null;send(AiWire.PLAN,AgentPolicy.prompt(request,model?.name.orEmpty(),environment))}
 LaunchedEffect(agent.run?.startedAt){showResults=false}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  AgentTeamPanel(agent)
  AgentSummary.from(agent)?.let{summary->
   Card(Modifier.fillMaxWidth().testTag("agent-summary")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text(summary.title,style=MaterialTheme.typography.titleLarge,color=if(summary.title=="Revisión con error")MaterialTheme.colorScheme.error else DeckCyan)
    Text(agent.request,style=MaterialTheme.typography.bodyMedium,maxLines=2)
    LinearProgressIndicator(progress={summary.progress},modifier=Modifier.fillMaxWidth())
    Text("${summary.results} de ${summary.total} pasos con resultado",style=MaterialTheme.typography.titleMedium)
    if(summary.skipped>0 || summary.manual>0 || summary.generated>0)Text("${summary.skipped} omitidos · ${summary.manual} manuales · ${summary.generated} interpretaciones de IA",style=MaterialTheme.typography.bodySmall)
    if(agent.error.isNotBlank())Text(agent.error,color=MaterialTheme.colorScheme.error)
    if(agent.notice.isNotBlank())Text(agent.notice,style=MaterialTheme.typography.bodySmall)
    if(actionStatus.isNotBlank())Text(actionStatus,color=DeckCyan)
    agent.results.forEach{step->
     Text(step.tool.label,style=MaterialTheme.typography.titleSmall)
     Text(when{step.error.isNotBlank()->"Error: ${step.error}";step.awaiting->"Espera tu intervención";step.skipped->"Paso omitido";step.manual->"Confirmación manual · sin lectura verificada";step.tool==AgentTool.ANALYST && step.result!=null->"Interpretación de IA · revisar";else->step.result?.summary?:"Sin resultado"},maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
    }
    Text("Progreso de la tarea; no es una puntuación de seguridad.",style=MaterialTheme.typography.bodySmall)
    if(agent.running)OutlinedButton(onClick={executionJob?.cancel()}){Text("Detener ejecución")}
    else {
     if(agent.results.isNotEmpty())TextButton(onClick={showResults=!showResults}){Text(if(showResults)"Ocultar detalles de resultados" else "Ver detalles de resultados")}
     agent.run?.let{run->OutlinedButton(enabled=editing,onClick={runCatching{run.diagnostic(agent.request,agent.modelHash,agent.evidenceScope,agent.origin,agent.contextSnapshot)}.onSuccess(save).onFailure{e->session.agent.update{it.copy(error=e.message?:"No se pudo preparar el informe")}}}){Text("Guardar ejecución como evidencia")}}
     TextButton(enabled=editing,onClick={session.agent.value=AgentSession(request=agent.request,plan=agent.plan,inputs=agent.inputs,origin=agent.origin,modelHash=agent.modelHash,contextSnapshot=agent.contextSnapshot)}){Text("Preparar de nuevo")}
    }
   }}
  }
  Text("Tu móvil es el equipo principal. El ESP32 es opcional.",style=MaterialTheme.typography.bodySmall)
  FilledTonalButton(enabled=editing,onClick={chooseTask(AgentTasks.phone)},modifier=Modifier.fillMaxWidth().testTag("agent-task-phone")){Column(Modifier.padding(vertical=4.dp)){Text("Revisar mi móvil");Text("Ajustes, conexión, batería y APK · comprobar ahora",style=MaterialTheme.typography.bodySmall)}}
  OutlinedTextField(request,{request=it.take(800)},label={Text("Qué quieres que haga CiberDeck")},placeholder={Text("Revisa mi móvil, busca Bluetooth…")},modifier=Modifier.fillMaxWidth(),enabled=editing,maxLines=3)
  AiContext.domainGuidance(request).takeIf{it.isNotBlank()}?.let{DeckDisclosure("Alcance para campos y salud",it)}
  Button(modifier=Modifier.fillMaxWidth(),enabled=editing && request.isNotBlank(),onClick={
   val task=AgentTasks.match(request)
   if(task!=null){session.agent.value=task.session(request);queuedTask=if(task.instant)task else null}
   else if(ai.connected && ai.loaded)planWithAi()
   else session.agent.update{it.copy(error="",notice="Prueba una tarea de abajo. Para otras peticiones puedes cargar la IA local en sus ajustes.")}
  }){Text("Preparar tarea")}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(enabled=editing,onClick={chooseTask(AgentTasks.connection)},modifier=Modifier.weight(1f).testTag("agent-task-connection")){Text("Mi conexión")}
   OutlinedButton(enabled=editing,onClick={chooseTask(AgentTasks.ble)},modifier=Modifier.weight(1f).testTag("agent-task-ble")){Text("Bluetooth")}
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(enabled=editing,onClick={chooseTask(AgentTasks.nfc)},modifier=Modifier.weight(1f).testTag("agent-task-nfc")){Text("Leer NFC")}
   OutlinedButton(enabled=editing,onClick={chooseTask(AgentTasks.apk)},modifier=Modifier.weight(1f).testTag("agent-task-apk")){Text("Elegir APK")}
  }
  TextButton(enabled=editing,onClick={quickExpanded=!quickExpanded}){Text(if(quickExpanded)"Menos tareas" else "Enlaces y servicios de red")}
  if(quickExpanded)listOf(AgentTasks.link,AgentTasks.lan).forEach{task->OutlinedButton(enabled=editing,onClick={chooseTask(task)}){Text(task.title)}}
  if(ai.loaded)TextButton(enabled=editing && request.isNotBlank(),onClick={planWithAi()}){Text("Preparar plan con IA")}
  TextButton(onClick={catalog=!catalog},enabled=editing){Text(if(catalog)"Cerrar catálogo" else "Elegir funciones · ${AgentTool.entries.size}")}
  if(catalog){
   OutlinedTextField(search,{search=it.take(100)},label={Text("Buscar función del agente")},modifier=Modifier.fillMaxWidth(),enabled=editing)
   AgentTool.entries.filter{"${it.label} ${it.group} ${it.wire}".contains(search,true)}.groupBy{it.group}.forEach{(group,tools)->
    Text(group,color=DeckCyan,style=MaterialTheme.typography.titleSmall)
    tools.forEach{tool->Row(Modifier.fillMaxWidth()){
     Checkbox(tool.wire in picked,enabled=editing && (picked.size<8 || tool.wire in picked),onCheckedChange={yes->picked=ArrayList(if(yes)picked+tool.wire else picked-tool.wire)},modifier=Modifier.testTag("agent-pick-${tool.wire}"))
     Column(Modifier.weight(1f).padding(top=8.dp)){Text(tool.label);Text(if(runtime!=null && tool==AgentTool.MODULES)"Consulta del catálogo futuro · no ejecuta RF/IR" else if(runtime!=null && tool.assisted)"Ejecutable · puede pedir permiso o selección" else if(tool.assisted)"Asistida · se pausa en su pantalla" else if(tool.network)"Directa · requiere destino autorizado" else "Directa · lectura o cálculo local",style=MaterialTheme.typography.bodySmall)}
    }}
   }
   Button(enabled=editing && picked.isNotEmpty(),onClick={session.agent.value=AgentSession(request="Plan elegido: "+picked.joinToString{wire->AgentTool.entries.first{it.wire==wire}.label},plan=AgentPlan(picked.map{wire->AgentTool.entries.first{it.wire==wire}}),origin="Selección manual");catalog=false}){Text("Preparar selección · ${picked.size}/8")}
  }
  if(agent.planning){LinearProgressIndicator(Modifier.fillMaxWidth());OutlinedButton(onClick={send(AiWire.STOP,"")}){Text("Detener planificación")}}
  if(agent.error.isNotBlank())Text(agent.error,color=MaterialTheme.colorScheme.error)
  if(agent.notice.isNotBlank())Text(agent.notice,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
  if(scopeError.isNotBlank())Text(scopeError,color=MaterialTheme.colorScheme.error)
  agent.plan?.takeIf{agent.run==null || showResults || agent.results.any{it.awaiting}}?.let{plan->
   val input=agent.inputs;val editableInputs=editing && agent.run==null
   fun inputChanged(value:AgentInputs){session.agent.update{it.copy(inputs=value)}}
   if(plan.steps.any{it==AgentTool.DNS || it==AgentTool.TCP || it==AgentTool.EXPOSURE})OutlinedTextField(input.host,{inputChanged(input.copy(host=it.take(253),networkApproved=false))},label={Text("Dominio o IP autorizado")},modifier=Modifier.fillMaxWidth(),enabled=editableInputs,singleLine=true)
   if(AgentTool.TCP in plan.steps || AgentTool.EXPOSURE in plan.steps)OutlinedTextField(input.port,{inputChanged(input.copy(port=it.take(100),networkApproved=false))},label={Text(if(AgentTool.EXPOSURE in plan.steps)"Puertos TCP · hasta ocho, separados por comas" else "Puerto TCP")},enabled=editableInputs,singleLine=true)
   if(plan.steps.any{it==AgentTool.WEB || it==AgentTool.LINK})OutlinedTextField(input.url,{inputChanged(input.copy(url=it.take(4096),networkApproved=false))},label={Text("URL elegida")},modifier=Modifier.fillMaxWidth(),enabled=editableInputs)
   if(AgentTool.CIDR in plan.steps)OutlinedTextField(input.cidr,{inputChanged(input.copy(cidr=it.take(32)))},label={Text("IPv4 y prefijo CIDR")},modifier=Modifier.fillMaxWidth(),enabled=editableInputs,singleLine=true)
   if(AgentTool.BLE in plan.steps && agent.run==null)Row{Checkbox(input.bleGatt,onCheckedChange={inputChanged(input.copy(bleGatt=it))},enabled=editableInputs);Text("Consultar también servicios GATT de un equipo autorizado",Modifier.weight(1f).padding(top=10.dp),style=MaterialTheme.typography.bodySmall)}
   if(plan.steps.any{it.network})Row{Checkbox(input.networkApproved,onCheckedChange={inputChanged(input.copy(networkApproved=it))},enabled=editableInputs);Text("Tengo autorización sobre los destinos y la red utilizados. Las operaciones seleccionadas pueden enviar tráfico o abrir una recepción temporal.",Modifier.weight(1f).padding(top=10.dp),style=MaterialTheme.typography.bodySmall)}
   val inputError=runCatching{validateAgentInputs(plan,input)}.exceptionOrNull()?.message
   Card(Modifier.fillMaxWidth().testTag("agent-plan")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("PLAN PROPUESTO · ${plan.steps.size} PASOS",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text(agent.request);Text("Origen: ${agent.origin}",style=MaterialTheme.typography.bodySmall)
    plan.steps.forEachIndexed{i,tool->Text("${i+1}. ${AgentTeam.workerFor(tool).label} → ${tool.label}${if(runtime!=null && tool==AgentTool.MODULES)" · solo información" else if(runtime!=null && tool.assisted)" · espera interacción si hace falta" else if(tool.assisted)" · asistida" else ""}")}
    Text("Copias: ${if(agent.run!=null)agent.evidenceScope else work?.name?:if(scopeLoaded)"Todas las evidencias locales" else "Leyendo alcance…"}",style=MaterialTheme.typography.bodySmall)
    if(inputError!=null)Text(inputError,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    if(agent.run==null)Button(enabled=scopeLoaded && editing && inputError==null,onClick={execute()}){Text(if(agent.running)"Ejecutando…" else "Ejecutar este plan")}
   }}
  }
  if(actionStatus.isNotBlank())Text(actionStatus,color=DeckCyan,modifier=Modifier.testTag("agent-action-status"))
  if(actionLink.isNotBlank()){Text("Enlace temporal · solo para tu red de confianza");androidx.compose.foundation.text.selection.SelectionContainer{Text(actionLink)}}
  if(showResults || agent.results.any{it.awaiting})agent.results.forEach{step->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(step.tool.label,style=MaterialTheme.typography.titleMedium)
   Text(when{step.awaiting->"PENDIENTE DE INTERACCIÓN";step.skipped->"OMITIDO";step.manual->"CONFIRMADO POR TI · SIN VERIFICACIÓN";step.result!=null && step.tool==AgentTool.ANALYST->"INTERPRETACIÓN GENERADA · REVISAR";step.result!=null->"COMPLETADO · RESULTADO DE HERRAMIENTA";else->"ERROR · PLAN DETENIDO"},color=if(step.result!=null)DeckMint else DeckCyan,style=MaterialTheme.typography.labelLarge)
   step.result?.let{r->Text(r.summary);DeckDisclosure("Ver resultado y método",listOf("Destino: ${r.target}","Lectura: ${r.checkedAt}").plus(r.details.map{"${it.first}: ${it.second}"}).joinToString("\n"))}
   if(step.error.isNotBlank())Text(step.error,color=MaterialTheme.colorScheme.error)
   if(step.awaiting && !agent.running){
    Text("Abre la herramienta y realiza la operación. Si guarda un diagnóstico, podrás incorporarlo verificando su copia. Abrirla no ejecuta ni completa el paso.",style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick={navigate(step.tool.route!!)},enabled=editing,modifier=Modifier.testTag("agent-open-${step.tool.wire}")){Text("Abrir herramienta")}
    if(agent.resumeEvidence!=null)Button(onClick={execute("saved")},enabled=editing){Text("Usar resultado guardado y continuar")}
    OutlinedTextField(manualNote,{manualNote=it.take(400)},label={Text("Nota de la interacción manual")},modifier=Modifier.fillMaxWidth(),enabled=editing)
    TextButton(onClick={execute("manual")},enabled=editing){Text("Confirmar manualmente y continuar")}
    TextButton(onClick={execute("skip")},enabled=editing){Text("Omitir este paso")}
   }
  }}}
  if(agent.raw.isNotBlank())DeckDisclosure("Plan recibido de la IA",agent.raw)
  if(agent.contextSnapshot.isNotBlank())DeckDisclosure("Contexto usado para preparar el plan",agent.contextSnapshot)
  DeckDisclosure("Acceso y límites","El agente utiliza las funciones implementadas de CiberDeck y permisos concedidos por Android. No tiene root, shell ni acceso a datos privados de otras apps. Las operaciones integradas conservan los permisos y selectores de Android; se espera la respuesta real antes de continuar. Borrar una copia requiere confirmación específica. Los resultados guardados se vuelven a verificar; confirmaciones manuales y texto generado se distinguen de lecturas. RF/IR y otros módulos del catálogo futuro siguen pendientes. La sesión permanece en memoria hasta guardarla; salir durante una ejecución la interrumpe. Un plan puede no corresponder a tu intención: revísalo.")
 }
}
